package com.xyon.airandroid.core.crypto

import org.json.JSONObject
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.net.Socket
import java.security.GeneralSecurityException
import javax.crypto.AEADBadTagException

class SecureChannel(
    private val input: DataInputStream,
    private val output: DataOutputStream,
    private val key: ByteArray,
) : Closeable {

    fun send(payload: ByteArray) {
        require(payload.size <= MAX_FRAME_BYTES) { "Frame demasiado grande" }
        val iv = Crypto.randomBytes(Crypto.IV_BYTES)
        val ciphertext = Crypto.aesGcmEncrypt(key, iv, payload)
        output.writeInt(ciphertext.size)
        output.write(iv)
        output.write(ciphertext)
        output.flush()
    }

    fun receive(): ByteArray {
        val length = input.readInt()
        if (length < Crypto.IV_BYTES + 16 || length > MAX_FRAME_BYTES + 16) {
            throw IOException("Frame inválido")
        }
        val iv = ByteArray(Crypto.IV_BYTES)
        input.readFully(iv)
        val ciphertext = ByteArray(length)
        input.readFully(ciphertext)
        return try {
            Crypto.aesGcmDecrypt(key, iv, ciphertext)
        } catch (e: AEADBadTagException) {
            throw SecurityException("No se pudo autenticar el mensaje", e)
        } catch (e: GeneralSecurityException) {
            throw SecurityException("No se pudo descifrar el mensaje", e)
        }
    }

    fun sendJson(json: JSONObject) {
        send(json.toString().toByteArray(Charsets.UTF_8))
    }

    fun receiveJson(): JSONObject {
        val bytes = receive()
        return JSONObject(String(bytes, Charsets.UTF_8))
    }

    override fun close() {
        runCatching { output.close() }
        runCatching { input.close() }
    }

    companion object {
        const val MAX_FRAME_BYTES = 256 * 1024
    }
}

open class HandshakeException(message: String, cause: Throwable? = null) : IOException(message, cause)

class BadPinException : HandshakeException("El código de 6 dígitos no es correcto")

class BusyException : HandshakeException("El otro móvil está ocupado con otra transferencia")

data class SecureSession(
    val channel: SecureChannel,
    val peerName: String,
)

object SecureHandshake {
    private const val VERSION = 1
    private const val HANDSHAKE_TIMEOUT_MS = 15_000
    private const val SESSION_TIMEOUT_MS = 30_000

    fun client(socket: Socket, pin: String, deviceName: String): SecureSession {
        socket.soTimeout = HANDSHAKE_TIMEOUT_MS
        val input = DataInputStream(socket.getInputStream())
        val output = DataOutputStream(socket.getOutputStream())

        val keyPair = Crypto.generateEcKeyPair()
        val clientNonce = Crypto.randomBytes(Crypto.NONCE_BYTES)
        val hello = JSONObject()
            .put("v", VERSION)
            .put("t", "hello")
            .put("role", "sender")
            .put("name", deviceName.take(48))
            .put("pub", Crypto.encodePublicKey(keyPair.public))
            .put("nonce", Crypto.encodeBytes(clientNonce))

        output.writeUTF(hello.toString())
        output.flush()

        val serverHello = JSONObject(input.readUTF())
        if (serverHello.optString("t") == "busy") throw BusyException()
        if (serverHello.optInt("v") != VERSION) {
            throw HandshakeException("Versión de protocolo no compatible")
        }
        if (serverHello.optString("t") != "hello") {
            throw HandshakeException("Respuesta de conexión no válida")
        }

        val serverNonce = Crypto.decodeBytes(serverHello.getString("nonce"))
        val serverPublic = Crypto.decodePublicKey(serverHello.getString("pub"))
        val shared = Crypto.ecdh(keyPair.private, serverPublic)
        val key = Crypto.deriveSessionKey(shared, pin, clientNonce, serverNonce)
        val channel = SecureChannel(input, output, key)

        channel.sendJson(
            JSONObject()
                .put("t", "auth")
                .put("name", deviceName.take(48)),
        )

        val ready = try {
            channel.receiveJson()
        } catch (e: EOFException) {
            throw BadPinException()
        } catch (e: SecurityException) {
            throw BadPinException()
        } catch (e: IOException) {
            throw BadPinException()
        }

        if (ready.optString("t") != "ready") {
            throw HandshakeException("El receptor rechazó la conexión")
        }

        socket.soTimeout = SESSION_TIMEOUT_MS
        return SecureSession(channel, serverHello.optString("name").take(48))
    }

    fun server(socket: Socket, pin: String, deviceName: String): SecureSession {
        socket.soTimeout = HANDSHAKE_TIMEOUT_MS
        val input = DataInputStream(socket.getInputStream())
        val output = DataOutputStream(socket.getOutputStream())

        val hello = JSONObject(input.readUTF())
        if (hello.optInt("v") != VERSION) {
            output.writeUTF(JSONObject().put("t", "error").put("message", "versión").toString())
            output.flush()
            throw HandshakeException("Versión de protocolo no compatible")
        }
        if (hello.optString("t") != "hello") {
            throw HandshakeException("Saludo no válido")
        }

        val keyPair = Crypto.generateEcKeyPair()
        val serverNonce = Crypto.randomBytes(Crypto.NONCE_BYTES)
        val reply = JSONObject()
            .put("v", VERSION)
            .put("t", "hello")
            .put("name", deviceName.take(48))
            .put("pub", Crypto.encodePublicKey(keyPair.public))
            .put("nonce", Crypto.encodeBytes(serverNonce))

        output.writeUTF(reply.toString())
        output.flush()

        val clientNonce = Crypto.decodeBytes(hello.getString("nonce"))
        val clientPublic = Crypto.decodePublicKey(hello.getString("pub"))
        val shared = Crypto.ecdh(keyPair.private, clientPublic)
        val key = Crypto.deriveSessionKey(shared, pin, clientNonce, serverNonce)
        val channel = SecureChannel(input, output, key)

        val auth = try {
            channel.receiveJson()
        } catch (e: SecurityException) {
            throw BadPinException()
        } catch (e: IOException) {
            throw BadPinException()
        }

        if (auth.optString("t") != "auth") {
            throw HandshakeException("Autenticación no válida")
        }

        channel.sendJson(JSONObject().put("t", "ready"))
        socket.soTimeout = SESSION_TIMEOUT_MS
        return SecureSession(channel, hello.optString("name").take(48))
    }
}
