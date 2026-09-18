package org.example.auto2fa

import java.security.KeyFactory
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import kotlin.io.encoding.Base64

/**
 * Encrypts short plaintext (e.g. an OTP code) against an RSA public key, for the desktop
 * app's private key to decrypt. Encrypt-only: the phone never holds a private key.
 *
 * Lives in the android+jvm shared source set because it needs java.security/javax.crypto,
 * which aren't available from commonMain.
 */
object RsaCrypto {
    private const val TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"

    /** [publicKeyBase64] is the base64-encoded X.509 (SubjectPublicKeyInfo) DER form of the key. */
    fun encrypt(publicKeyBase64: String, plaintext: String): String {
        val keyBytes = Base64.Default.decode(publicKeyBase64)
        val publicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(keyBytes))

        val oaepParams = OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT
        )
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, publicKey, oaepParams)
        }

        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.Default.encode(encrypted)
    }
}
