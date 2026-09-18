package org.example.auto2fa

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() {
    launchServer { push ->
        println("OTP push from ${push.sender}: ${push.message}")
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Auto2FA",
        ) {
            App()
        }
    }
}