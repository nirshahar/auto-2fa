package org.example.auto2fa

import java.io.File
import java.security.KeyStore
import java.util.Properties
import kotlin.io.encoding.Base64

data class DesktopKeys(
    val publicKeyBase64: String,
    val privateKeyBase64: String,
    val tlsKeyStore: KeyStore,
    val tlsKeyStorePassword: CharArray,
    val tlsCertificateBase64: String,
)

/**
 * Persists the desktop app's own RSA keypair (for decrypting pushed OTP codes) and its
 * self-signed TLS keypair/certificate (for HTTPS) under the user's home directory. Generates
 * both on first run. Desktop-only -- not shared with Android, which never holds a private key
 * and whose settings describe a different thing (a remote server to talk to).
 */
object DesktopKeyStore {
    private const val PUBLIC_KEY_PROPERTY = "publicKey"
    private const val PRIVATE_KEY_PROPERTY = "privateKey"

    // Protects the TLS keystore file's format only -- this file lives in the user's own home
    // directory, so the OS's file permissions are the real access boundary, same as the plain
    // base64 OTP keypair file below.
    private val TLS_KEYSTORE_PASSWORD = "auto2fa".toCharArray()

    private val appDir = File(System.getProperty("user.home"), ".auto2fa")
    private val otpKeyPairFile = File(appDir, "keypair.properties")
    private val tlsKeyStoreFile = File(appDir, "tls-keystore.p12")

    fun loadOrCreate(): DesktopKeys {
        val (publicKeyBase64, privateKeyBase64) = loadOrCreateOtpKeyPair()
        val tls = loadOrCreateTlsKeyMaterial()

        return DesktopKeys(
            publicKeyBase64 = publicKeyBase64,
            privateKeyBase64 = privateKeyBase64,
            tlsKeyStore = tls.keyStore,
            tlsKeyStorePassword = TLS_KEYSTORE_PASSWORD,
            tlsCertificateBase64 = tls.certificateBase64,
        )
    }

    private fun loadOrCreateOtpKeyPair(): Pair<String, String> {
        loadExistingOtpKeyPair()?.let { return it }

        val keyPair = RsaCrypto.generateKeyPair()
        saveOtpKeyPair(keyPair.publicKeyBase64, keyPair.privateKeyBase64)
        return keyPair.publicKeyBase64 to keyPair.privateKeyBase64
    }

    private fun loadExistingOtpKeyPair(): Pair<String, String>? {
        if (!otpKeyPairFile.exists()) return null

        val props = Properties().apply { otpKeyPairFile.inputStream().use { load(it) } }
        val publicKey = props.getProperty(PUBLIC_KEY_PROPERTY)
        val privateKey = props.getProperty(PRIVATE_KEY_PROPERTY)
        return if (publicKey != null && privateKey != null) publicKey to privateKey else null
    }

    private fun saveOtpKeyPair(publicKeyBase64: String, privateKeyBase64: String) {
        appDir.mkdirs()
        val props = Properties().apply {
            setProperty(PUBLIC_KEY_PROPERTY, publicKeyBase64)
            setProperty(PRIVATE_KEY_PROPERTY, privateKeyBase64)
        }
        otpKeyPairFile.outputStream().use { props.store(it, "Auto2FA RSA keypair") }
    }

    private fun loadOrCreateTlsKeyMaterial(): TlsKeyMaterial {
        loadExistingTlsKeyMaterial()?.let { return it }

        val tls = TlsCertificate.generateSelfSigned(TLS_KEYSTORE_PASSWORD)
        appDir.mkdirs()
        tlsKeyStoreFile.outputStream().use { tls.keyStore.store(it, TLS_KEYSTORE_PASSWORD) }
        return tls
    }

    private fun loadExistingTlsKeyMaterial(): TlsKeyMaterial? {
        if (!tlsKeyStoreFile.exists()) return null

        val keyStore = KeyStore.getInstance("PKCS12").apply {
            tlsKeyStoreFile.inputStream().use { load(it, TLS_KEYSTORE_PASSWORD) }
        }
        val certificate = keyStore.getCertificate(TlsCertificate.KEY_ALIAS) ?: return null
        return TlsKeyMaterial(keyStore, Base64.Default.encode(certificate.encoded))
    }
}
