package com.xyon.airandroid.core.net

import android.content.Context
import android.os.SystemClock
import com.xyon.airandroid.core.crypto.BadPinException
import com.xyon.airandroid.core.crypto.BusyException
import com.xyon.airandroid.core.crypto.SecureHandshake
import com.xyon.airandroid.model.SelectedFile
import com.xyon.airandroid.model.TransferProgress
import com.xyon.airandroid.model.TransferResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.SocketTimeoutException

class FileTransferClient(private val context: Context) {

    @Volatile
    private var activeSocket: Socket? = null

    fun cancel() {
        runCatching { activeSocket?.close() }
        activeSocket = null
    }

    suspend fun send(
        host: InetAddress,
        pin: String,
        deviceName: String,
        files: List<SelectedFile>,
        onConnected: (String) -> Unit,
        onProgress: (TransferProgress) -> Unit,
    ): TransferResult = withContext(Dispatchers.IO) {
        if (files.isEmpty()) return@withContext TransferResult(false, "No hay archivos", 0, 0)
        if (files.size > TransferProtocol.MAX_FILES) {
            return@withContext TransferResult(false, "Demasiados archivos", 0, 0)
        }

        var socket: Socket? = null
        try {
            var lastError: Exception? = null
            var attempt = 0
            while (attempt < 12 && socket == null) {
                ensureActive()
                try {
                    val candidate = Socket().apply {
                        tcpNoDelay = true
                        sendBufferSize = 256 * 1024
                    }
                    candidate.connect(
                        InetSocketAddress(host, TransferProtocol.PORT),
                        3_000,
                    )
                    socket = candidate
                    activeSocket = candidate
                } catch (e: ConnectException) {
                    lastError = e
                    runCatching { socket?.close() }
                    socket = null
                    if (attempt < 11) delay(280)
                } catch (e: IOException) {
                    lastError = e
                    runCatching { socket?.close() }
                    socket = null
                    if (attempt < 11) delay(280)
                }
                attempt++
            }
            val connected = socket ?: throw (lastError ?: ConnectException("Sin conexión"))
            connected.soTimeout = 15_000
            val session = SecureHandshake.client(connected, pin, deviceName)
            onConnected(session.peerName)
            val channel = session.channel

            var totalSent = 0L
            files.forEachIndexed { index, file ->
                ensureActive()
                if (file.size < 0L || file.size > TransferProtocol.MAX_FILE_BYTES) {
                    throw IOException("Archivo demasiado grande: ${file.name}")
                }
                channel.sendJson(
                    JSONObject()
                        .put("t", TransferProtocol.TYPE_FILE)
                        .put("name", file.name.take(TransferProtocol.MAX_NAME_CHARS))
                        .put("size", file.size)
                        .put("mime", file.mimeType),
                )

                val started = SystemClock.elapsedRealtime()
                var sent = 0L
                var lastUpdate = started
                var lastBytes = 0L
                context.contentResolver.openInputStream(file.uri)?.use { input ->
                    val buffer = ByteArray(TransferProtocol.CHUNK_BYTES)
                    while (true) {
                        ensureActive()
                        val read = input.read(buffer)
                        if (read <= 0) break
                        if (sent + read > file.size) {
                            throw IOException("El archivo cambió durante la transferencia")
                        }
                        val chunk = if (read == buffer.size) buffer else buffer.copyOf(read)
                        channel.send(chunk)
                        sent += read
                        totalSent += read
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastUpdate >= 100L || sent == file.size) {
                            val elapsed = (now - lastUpdate).coerceAtLeast(1L)
                            val speed = ((sent - lastBytes) * 1000L) / elapsed
                            onProgress(
                                TransferProgress(
                                    fileName = file.name,
                                    bytes = sent,
                                    total = file.size,
                                    speedBytesPerSecond = speed,
                                    fileIndex = index + 1,
                                    fileCount = files.size,
                                ),
                            )
                            lastUpdate = now
                            lastBytes = sent
                        }
                    }
                } ?: throw IOException("No se pudo abrir ${file.name}")

                if (sent != file.size) {
                    throw IOException("El archivo cambió durante la transferencia")
                }
            }

            channel.sendJson(
                JSONObject()
                    .put("t", TransferProtocol.TYPE_DONE)
                    .put("files", files.size),
            )
            val response = channel.receiveJson()
            if (response.optString("t") != TransferProtocol.TYPE_OK) {
                throw IOException(response.optString("message", "El receptor no confirmó"))
            }
            TransferResult(true, null, files.size, totalSent)
        } catch (e: CancellationException) {
            throw e
        } catch (_: BadPinException) {
            TransferResult(false, "Código incorrecto. Revisa el PIN del otro móvil.", 0, 0)
        } catch (_: BusyException) {
            TransferResult(false, "El otro móvil está ocupado. Espera un momento.", 0, 0)
        } catch (_: ConnectException) {
            TransferResult(
                false,
                "No se pudo conectar. Activa Recibir en el otro móvil y vuelve a intentarlo.",
                0,
                0,
            )
        } catch (_: NoRouteToHostException) {
            TransferResult(false, "Los móviles no están enlazados por Wi-Fi Direct.", 0, 0)
        } catch (_: SocketTimeoutException) {
            TransferResult(false, "La conexión tardó demasiado. Vuelve a intentarlo.", 0, 0)
        } catch (e: Exception) {
            TransferResult(false, e.message ?: "Error de transferencia", 0, 0)
        } finally {
            activeSocket = null
            runCatching { socket?.close() }
        }
    }
}
