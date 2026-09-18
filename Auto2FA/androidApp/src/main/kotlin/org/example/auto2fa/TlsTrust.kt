package org.example.auto2fa

import android.util.Base64
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

/**
 * There's no CA here -- the desktop's TLS certificate is self-signed. So instead of validating
 * a chain, we pin the exact certificate the user pasted from the desktop's Settings screen:
 * an SSLContext that trusts ONLY that one certificate, nothing from the system trust store.
 */
object TlsTrust {
    private const val CERTIFICATE_ALIAS = "auto2fa-server"

    fun pinnedSslContext(certificateBase64: String): SSLContext {
        val certificateBytes = Base64.decode(certificateBase64, Base64.DEFAULT)
        val certificate = CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(certificateBytes))

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry(CERTIFICATE_ALIAS, certificate)
        }

        val trustManagerFactory = TrustManagerFactory
            .getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(keyStore) }

        return SSLContext.getInstance("TLS").apply {
            init(null, trustManagerFactory.trustManagers, null)
        }
    }
}
