package org.example.auto2fa

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import javax.net.ssl.HostnameVerifier

/**
 * Client for the desktop app's HTTPS API, bound to a specific host and pinned to the
 * desktop's self-signed TLS certificate (there's no CA here -- see [TlsTrust]). Callers interact
 * with the server's API (e.g. [pushOtp]) without knowing request shapes, paths, or content types
 * -- those are formatted internally. Fire-and-forget: no retries.
 */
class OtpPushClient(
    private val host: String,
    private val tlsCertificateBase64: String,
) : ServerApi {
    private val baseUrl = "https://$host:$AUTO2FA_SERVER_PORT"

    // Trust depends on which certificate the user pasted, so the client (and its SSL context)
    // is built per-instance rather than shared, unlike a client with no per-server trust config.
    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) { json() }
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            requestTimeoutMillis = 5_000
        }
        engine {
            sslManager = { connection ->
                connection.sslSocketFactory = TlsTrust.pinnedSslContext(tlsCertificateBase64).socketFactory
                // We're pinning the exact certificate rather than validating a CA chain, so
                // hostname verification (which guards against a *different*, validly-signed
                // cert) doesn't add anything here -- the phone connects by raw IP anyway.
                connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
            }
        }
    }

    override suspend fun pushOtp(request: OtpPushRequest) {
        httpClient.post("$baseUrl/push") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
