package com.xyon.airandroid.core.net

import android.os.SystemClock
import com.xyon.airandroid.core.crypto.BadPinException
import com.xyon.airandroid.core.crypto.SecureHandshake
import com.xyon.airandroid.core.crypto.SecureSession
import com.xyon.airandroid.data.ReceivedFileStore
import com.xyon.airandroid.model.HistoryItem
import com.xyon.airandroid.model.TransferDirection
import com.xyon.airandroid.model.TransferProgress
import com.xyon.airandroid.model.TransferStatus
import com.xyon.airandroid.util.FileNames
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class FileTransferServer(
    private val store: ReceivedFileStore,
) {
    sealed interface State {
        data object Idle : State
        data class Waiting(val pin: String) : State
        data class Connected(val peerName: String) : State
        data class Transferring(val progress: TransferProgress) : State
        data class Completed(val fileCount: Int, val totalBytes: Long) : State
        data class Failed(val message: String) : State
    }

    data class Callbacks(
        val onState: (State) -> Unit,
        val onFile: (HistoryItem) -> Unit,
        val onBadPin: (Int) -> Unit,
        val onSessionEnd: () -> Unit,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val busy = AtomicBoolean(false)
    private val badPinCount = AtomicInteger(0)
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var sessionJob: Job? = null
    private var activeSocket: Socket? = null
    private var activeSession: SecureSession? = null
    @Volatile private var running = false

    fun start(
        pinProvider: () -> String,
        deviceNameProvider: () -> String,
        callbacks: Callbacks,
    ) {
        if (running) return
        running = true
        acceptJob = scope.launch {
            try {
                val server = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(TransferProtocol.PORT))
                }
                serverSocket = server
                callbacks.onState(State.Waiting(pinProvider()))
                while (isActive && running) {
                    val socket = try {
                        server.accept()
                    } catch (e: IOException) {
                        if (running) callbacks.onState(State.Failed("Servidor detenido: ${e.message}"))
                        break
                    }
                    if (!isP2pAddress(socket)) {
                        runCatching { socket.close() }
                        continue
                    }
                    if (!busy.compareAndSet(false, true)) {
                        runCatching { socket.close() }
                        continue
                    }
                    sessionJob = launch {
                        handle(socket, pinProvider, deviceNameProvider, callbacks)
                    }
                }
            } catch (e: Exception) {
                callbacks.onState(State.Failed("No se pudo abrir el puerto ${TransferProtocol.PORT}: ${e.message}"))
            } finally {
                running = false
                runCatching { serverSocket?.close() }
                serverSocket = null
            }
        }
    }

    fun cancelActive() {
        runCatching { activeSocket?.close() }
        activeSocket = null
        runCatching { activeSession?.channel?.close() }
        activeSession = null
    }

    fun stop() {
        running = false
        runCatching { serverSocket?.close() }
        serverSocket = null
        runCatching { activeSocket?.close() }
        activeSocket = null
        runCatching { activeSession?.channel?.close() }
        activeSession = null
        acceptJob?.cancel()
        sessionJob?.cancel()
        acceptJob = null
        sessionJob = null
        busy.set(false)
    }

    fun destroy() {
        stop()
        scope.cancel()
    }

    private suspend fun handle(
        socket: Socket,
        pinProvider: () -> String,
        deviceNameProvider: () -> String,
        callbacks: Callbacks,
    ) {
        var sink: ReceivedFileStore.Sink? = null
        var session: SecureSession? = null
        var fileCount = 0
        var totalBytes = 0L
        try {
            activeSocket = socket
            session = SecureHandshake.server(
                socket = socket,
                pin = pinProvider(),
                deviceName = deviceNameProvider(),
            )
            activeSession = session
            badPinCount.set(0)
            callbacks.onState(State.Connected(session.peerName))
            val channel = session.channel

            while (true) {
                val message = channel.receiveJson()
                when (message.optString("t")) {
                    TransferProtocol.TYPE_FILE -> {
                        val rawName = message.optString("name", "archivo")
                        val size = message.optLong("size", -1L)
                        val mime = message.optString("mime", "application/octet-stream")
                        if (size < 0L || size > TransferProtocol.MAX_FILE_BYTES) {
                            throw IOException("Tamaño de archivo no válido")
                        }
                        if (fileCount >= TransferProtocol.MAX_FILES) {
                            throw IOException("Demasiados archivos")
                        }
                        if (!store.hasSpace(size)) {
                            throw IOException("No hay espacio suficiente en el dispositivo")
                        }

                        val safeName = FileNames.sanitize(rawName)
                        sink = store.createSink(safeName, mime)
                        val started = SystemClock.elapsedRealtime()
                        var received = 0L
                        var lastUpdate = started
                        var lastBytes = 0L
                        callbacks.onState(
                            State.Transferring(
                                TransferProgress(
                                    fileName = sink.displayName,
                                    bytes = 0L,
                                    total = size,
                                    speedBytesPerSecond = 0L,
                                    fileIndex = fileCount + 1,
                                    fileCount = 0,
                                ),
                            ),
                        )

                        while (received < size) {
                            val remaining = size - received
                            val chunk = channel.receive()
                            if (chunk.isEmpty()) continue
                            if (chunk.size.toLong() > remaining) {
                                throw IOException("El emisor envió más datos de los declarados")
                            }
                            sink.output.write(chunk)
                            received += chunk.size
                            val now = SystemClock.elapsedRealtime()
                            if (now - lastUpdate >= 100L || received == size) {
                                val elapsed = (now - lastUpdate).coerceAtLeast(1L)
                                val speed = ((received - lastBytes) * 1000L) / elapsed
                                callbacks.onState(
                                    State.Transferring(
                                        TransferProgress(
                                            fileName = sink.displayName,
                                            bytes = received,
                                            total = size,
                                            speedBytesPerSecond = speed,
                                            fileIndex = fileCount + 1,
                                            fileCount = 0,
                                        ),
                                    ),
                                )
                                lastUpdate = now
                                lastBytes = received
                            }
                        }

                        sink.finish()
                        val finished = sink
                        sink = null
                        fileCount++
                        totalBytes += size
                        callbacks.onFile(
                            HistoryItem(
                                id = UUID.randomUUID().toString(),
                                fileName = finished.displayName,
                                size = size,
                                direction = TransferDirection.RECEIVE,
                                deviceName = session.peerName,
                                status = TransferStatus.COMPLETED,
                                timestamp = System.currentTimeMillis(),
                                uri = finished.uri.toString(),
                            ),
                        )
                    }

                    TransferProtocol.TYPE_DONE -> {
                        channel.sendJson(JSONObject().put("t", TransferProtocol.TYPE_OK))
                        callbacks.onState(State.Completed(fileCount, totalBytes))
                        break
                    }

                    TransferProtocol.TYPE_ERROR -> {
                        throw IOException(message.optString("message", "Error del emisor"))
                    }

                    else -> throw IOException("Mensaje de protocolo no válido")
                }
            }
        } catch (_: BadPinException) {
            runCatching { sink?.abort() }
            sink = null
            val attempts = badPinCount.incrementAndGet()
            callbacks.onBadPin(attempts)
            callbacks.onState(
                State.Failed(
                    if (attempts >= 3) {
                        "Código incorrecto. Se ha generado uno nuevo."
                    } else {
                        "Código incorrecto. Inténtalo de nuevo."
                    },
                ),
            )
        } catch (e: CancellationException) {
            runCatching { sink?.abort() }
            throw e
        } catch (e: Exception) {
            runCatching { sink?.abort() }
            sink = null
            callbacks.onState(State.Failed(e.message ?: "Error de transferencia"))
        } finally {
            runCatching { session?.channel?.close() }
            runCatching { socket.close() }
            activeSocket = null
            activeSession = null
            sink?.let { runCatching { it.abort() } }
            busy.set(false)
            if (running) {
                callbacks.onSessionEnd()
                callbacks.onState(State.Waiting(pinProvider()))
            }
        }
    }

    private fun isP2pAddress(socket: Socket): Boolean {
        val host = socket.inetAddress?.hostAddress ?: return false
        return host.startsWith(TransferProtocol.P2P_PREFIX)
    }
}
