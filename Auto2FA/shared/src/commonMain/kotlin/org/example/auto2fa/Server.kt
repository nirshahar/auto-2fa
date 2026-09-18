package org.example.auto2fa

data class OtpPush(val sender: String, val message: String)

expect fun launchServer(port: Int = 8080, onOtpReceived: (OtpPush) -> Unit)