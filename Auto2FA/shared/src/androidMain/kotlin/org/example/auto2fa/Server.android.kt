package org.example.auto2fa

actual fun launchServer(port: Int, onOtpReceived: (OtpPush) -> Unit) {
    throw UnsupportedOperationException("The server runs on the desktop app, not on Android")
}
