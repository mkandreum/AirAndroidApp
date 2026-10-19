package com.xyon.airandroid.core.crypto

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Crypto {
    const val PIN_LENGTH = 6
    const val KEY_BYTES = 32
    const val IV_BYTES = 12
    const val GCM_TAG_BITS = 128
    const val NONCE_BYTES = 16
    const val SALT_BYTES = 16

    private const val PBKDF2_ITERATIONS = 210_000
    private const val PBKDF2_BITS = 256
    private const val INFO = "AirAndroid/v1"

    private val random = SecureRandom()

    fun randomBytes(size: Int): ByteArray = ByteArray(size).also(random::nextBytes)

    fun randomPin(): String = (random.nextInt(1_000_000)).toString().padStart(PIN_LENGTH, '0')

    fun generateEcKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        return generator.generateKeyPair()
    }

    fun encodePublicKey(key: PublicKey): String =
        Base64.getEncoder().encodeToString(key.encoded)

    fun decodePublicKey(encoded: String): PublicKey {
        val bytes = Base64.getDecoder().decode(encoded)
        return KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(bytes))
    }

    fun encodeBytes(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    fun decodeBytes(encoded: String): ByteArray = Base64.getDecoder().decode(encoded)

    fun ecdh(privateKey: PrivateKey, peerPublicKey: PublicKey): ByteArray {
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privateKey)
        agreement.doPhase(peerPublicKey, true)
        return agreement.generateSecret()
    }

    fun deriveSessionKey(
        sharedSecret: ByteArray,
        pin: String,
        clientNonce: ByteArray,
        serverNonce: ByteArray,
    ): ByteArray {
        val salt = MessageDigest.getInstance("SHA-256").digest(clientNonce + serverNonce)
        val pinKey = pbkdf2(pin, salt)
        return hkdf(sharedSecret, pinKey, INFO.toByteArray(Charsets.UTF_8), KEY_BYTES)
    }

    fun pbkdf2(secret: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
    }

    fun hkdf(
        ikm: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        length: Int,
    ): ByteArray {
        val actualSalt = if (salt.isEmpty()) ByteArray(32) else salt
        val prk = hmacSha256(actualSalt, ikm)
        val output = ByteArray(length)
        var previous = ByteArray(0)
        var offset = 0
        var counter = 1
        while (offset < length) {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(prk, "HmacSHA256"))
            mac.update(previous)
            mac.update(info)
            mac.update(counter.toByte())
            previous = mac.doFinal()
            val copy = minOf(previous.size, length - offset)
            System.arraycopy(previous, 0, output, offset, copy)
            offset += copy
            counter++
        }
        return output
    }

    fun aesGcmEncrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, iv),
        )
        return cipher.doFinal(plaintext)
    }

    fun aesGcmDecrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, iv),
        )
        return cipher.doFinal(ciphertext)
    }

    fun sha256(bytes: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(bytes)

    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean =
        MessageDigest.isEqual(a, b)

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }
}
