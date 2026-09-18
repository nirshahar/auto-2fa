package org.example.auto2fa

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.security.KeyStore

/**
 * Starts the desktop HTTPS server, on all interfaces. [decrypt] turns a `POST /push` request's
 * ciphertext into the plaintext OTP code (or throws, if it doesn't decrypt); [onOtpReceived] is
 * called with the result. The server itself has no idea what a keypair is -- that's [decrypt]'s
 * job -- so it can drop bad payloads without knowing anything about RSA.
 *
 * There's no CA involved: [tlsKeyStore] holds the desktop's own self-signed certificate (see
 * TlsCertificate), which the phone pins directly instead of validating a chain.
 */
fun launchServer(
    tlsKeyStore: KeyStore,
    tlsKeyStorePassword: CharArray,
    decrypt: (String) -> String,
    onOtpReceived: (String) -> Unit,
) {
    embeddedServer(
        factory = Netty,
        configure = {
            connectors.clear()
            sslConnector(
                keyStore = tlsKeyStore,
                keyAlias = TlsCertificate.KEY_ALIAS,
                keyStorePassword = { tlsKeyStorePassword },
                privateKeyPassword = { tlsKeyStorePassword },
            ) {
                host = "0.0.0.0"
                port = AUTO2FA_SERVER_PORT
            }
        },
    ) {
        install(ContentNegotiation) { json() }

        routing {
            post("/push") {
                val request = call.receive<OtpPushRequest>()

                val code = try {
                    decrypt(request.code)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                onOtpReceived(code)
                call.respond(HttpStatusCode.OK)
            }
        }
    }.start(wait = false)
}
