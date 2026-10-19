package com.xyon.airandroid.core

import com.xyon.airandroid.core.crypto.Crypto
import com.xyon.airandroid.core.crypto.SecureChannel
import com.xyon.airandroid.model.NfcPairing
import com.xyon.airandroid.util.FileNames
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class CryptoTest {

    @Test
    fun ecdhDerivesTheSameSessionKey() {
        val alice = Crypto.generateEcKeyPair()
        val bob = Crypto.generateEcKeyPair()

        val aliceSecret = Crypto.ecdh(alice.private, bob.public)
        val bobSecret = Crypto.ecdh(bob.private, alice.public)
        assertArrayEquals(aliceSecret, bobSecret)

        val clientNonce = Crypto.randomBytes(Crypto.NONCE_BYTES)
        val serverNonce = Crypto.randomBytes(Crypto.NONCE_BYTES)
        val aliceKey = Crypto.deriveSessionKey(aliceSecret, "123456", clientNonce, serverNonce)
        val bobKey = Crypto.deriveSessionKey(bobSecret, "123456", clientNonce, serverNonce)
        assertArrayEquals(aliceKey, bobKey)

        val wrongKey = Crypto.deriveSessionKey(aliceSecret, "654321", clientNonce, serverNonce)
        assertNotEquals(aliceKey.toList(), wrongKey.toList())
    }

    @Test
    fun aesGcmRoundTrip() {
        val key = Crypto.randomBytes(Crypto.KEY_BYTES)
        val iv = Crypto.randomBytes(Crypto.IV_BYTES)
        val plaintext = "AirAndroid seguro".toByteArray(Charsets.UTF_8)
        val ciphertext = Crypto.aesGcmEncrypt(key, iv, plaintext)
        assertNotEquals(plaintext.toList(), ciphertext.toList())
        assertArrayEquals(plaintext, Crypto.aesGcmDecrypt(key, iv, ciphertext))
    }

    @Test
    fun secureChannelFramesRoundTrip() {
        val key = Crypto.randomBytes(Crypto.KEY_BYTES)
        val buffer = ByteArrayOutputStream()
        val writer = SecureChannel(
            DataInputStream(ByteArrayInputStream(ByteArray(0))),
            DataOutputStream(buffer),
            key,
        )
        writer.send("hola AirAndroid".toByteArray(Charsets.UTF_8))

        val reader = SecureChannel(
            DataInputStream(ByteArrayInputStream(buffer.toByteArray())),
            DataOutputStream(ByteArrayOutputStream()),
            key,
        )
        assertEquals(
            "hola AirAndroid",
            String(reader.receive(), Charsets.UTF_8),
        )
    }

    @Test
    fun pinIsSixDigits() {
        repeat(50) {
            val pin = Crypto.randomPin()
            assertEquals(6, pin.length)
            assertEquals(true, pin.all(Char::isDigit))
        }
    }

    @Test
    fun hkdfHasRequestedLengthAndIsDeterministic() {
        val ikm = Crypto.randomBytes(32)
        val salt = Crypto.randomBytes(16)
        val info = "info".toByteArray()
        val first = Crypto.hkdf(ikm, salt, info, 64)
        val second = Crypto.hkdf(ikm, salt, info, 64)
        assertEquals(64, first.size)
        assertArrayEquals(first, second)
    }

    @Test
    fun nfcPairingJsonRoundTrip() {
        val pairing = NfcPairing(
            ssid = "DIRECT-AirAndroid",
            pin = "123456",
            deviceName = "Pixel de Daniel",
        )
        assertEquals(pairing, NfcPairing.fromJson(pairing.toJson()))
        assertNull(NfcPairing.fromJson("{\"t\":\"otro\"}"))
        assertNull(NfcPairing.fromJson("{\"t\":\"airandroid-pair\",\"pin\":\"12\"}"))
    }

    @Test
    fun fileNamesAreSanitized() {
        assertEquals("evil.txt", FileNames.sanitize("../../evil.txt"))
        assertEquals("passwd", FileNames.sanitize("/etc/passwd"))
        assertEquals("a_b.txt", FileNames.sanitize("a:b.txt"))
    }
}
