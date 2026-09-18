package org.example.auto2fa

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.Base64

data class TlsKeyMaterial(val keyStore: KeyStore, val certificateBase64: String)

/**
 * Generates the desktop app's self-signed TLS certificate -- there's no CA here, so the phone
 * pins this exact certificate (see the Android app's TlsTrust) instead of validating a chain.
 */
object TlsCertificate {
    const val KEY_ALIAS = "auto2fa"
    private const val KEY_SIZE_BITS = 2048
    private const val VALIDITY_YEARS = 10L

    init {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    fun generateSelfSigned(keyStorePassword: CharArray): TlsKeyMaterial {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(KEY_SIZE_BITS) }.generateKeyPair()

        val now = System.currentTimeMillis()
        val subject = X500Name("CN=Auto2FA Desktop")
        val certificateHolder = JcaX509v3CertificateBuilder(
            /* issuer = */ subject,
            /* serial = */ BigInteger.valueOf(now),
            /* notBefore = */ Date(now),
            /* notAfter = */ Date(now + TimeUnit.DAYS.toMillis(365 * VALIDITY_YEARS)),
            /* subject = */ subject,
            /* publicKey = */ keyPair.public,
        ).build(JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.private))

        val certificate: X509Certificate = JcaX509CertificateConverter()
            .setProvider("BC")
            .getCertificate(certificateHolder)

        val keyStore = KeyStore.getInstance("PKCS12").apply {
            load(null, null)
            setKeyEntry(KEY_ALIAS, keyPair.private, keyStorePassword, arrayOf(certificate))
        }

        return TlsKeyMaterial(
            keyStore = keyStore,
            certificateBase64 = Base64.Default.encode(certificate.encoded),
        )
    }
}
