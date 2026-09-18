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

/**
 * Client for the desktop app's HTTP API, bound to a specific host/port. Callers interact with
 * the server's API (e.g. [pushOtp]) without knowing request shapes, paths, or content types --
 * those are formatted internally. Fire-and-forget: no retries.
 */
class OtpPushClient(private val host: String, private val port: Int) : ServerApi {
    private val baseUrl = "http://$host:$port"

    override suspend fun pushOtp(request: OtpPushRequest) {
        httpClient.post("$baseUrl/push") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    private companion object {
        val httpClient = HttpClient(Android) {
            install(ContentNegotiation) { json() }
            install(HttpTimeout) {
                connectTimeoutMillis = 5_000
                requestTimeoutMillis = 5_000
            }
        }
    }
}
