package org.example.auto2fa

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

actual fun launchServer(port: Int, onOtpReceived: (OtpPush) -> Unit) {
    embeddedServer(Netty, port = port) {
        routing {
            post("/push") {
                val parameters = call.receiveParameters()
                val sender = parameters["sender"] ?: "unknown"
                val message = parameters["message"] ?: ""
                onOtpReceived(OtpPush(sender, message))
                call.respondText("OK")
            }
        }
    }.start(wait = false)
}
