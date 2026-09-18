package org.example.auto2fa

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import kotlin.io.encoding.Base64

/**
 * RSA-2048/OAEP-SHA256 for encrypting/decrypting short plaintext (an OTP code): the phone
 * encrypts with the desktop's public key, the desktop decrypts with its private key.
 *
 * Lives in the android+jvm shared source set because it needs java.security/javax.crypto,
 * which aren't available from commonMain.
 */
object RsaCrypto {
    private const val TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    private const val KEY_SIZE_BITS = 2048

    data class KeyPair(val publicKeyBase64: String, val privateKeyBase64: String)

    /** Generates a fresh keypair. Only the desktop app calls this -- the phone never holds a private key. */
    fun generateKeyPair(): KeyPair {
        val keyPair = KeyPairGenerator.getInstance("RSA")
            .apply { initialize(KEY_SIZE_BITS) }
            .generateKeyPair()

        return KeyPair(
            publicKeyBase64 = Base64.Default.encode(keyPair.public.encoded),
            privateKeyBase64 = Base64.Default.encode(keyPair.private.encoded),
        )
    }

    /** [publicKeyBase64] is the base64-encoded X.509 (SubjectPublicKeyInfo) DER form of the key. */
    fun encrypt(publicKeyBase64: String, plaintext: String): String {
        val keyBytes = Base64.Default.decode(publicKeyBase64)
        val publicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(keyBytes))

        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, publicKey, oaepParams())
        }

        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.Default.encode(encrypted)
    }

    /** [privateKeyBase64] is the base64-encoded PKCS8 DER form of the key. */
    fun decrypt(privateKeyBase64: String, ciphertextBase64: String): String {
        val keyBytes = Base64.Default.decode(privateKeyBase64)
        val privateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(keyBytes))

        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, privateKey, oaepParams())
        }

        val decrypted = cipher.doFinal(Base64.Default.decode(ciphertextBase64))
        return decrypted.toString(Charsets.UTF_8)
    }

    private fun oaepParams() = OAEPParameterSpec(
        "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT
    )
}
