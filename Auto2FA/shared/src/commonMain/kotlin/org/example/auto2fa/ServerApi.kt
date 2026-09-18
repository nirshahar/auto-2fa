package org.example.auto2fa

/** The desktop app's HTTP API, as seen by a client -- implemented per-platform by whatever HTTP stack that platform uses. */
interface ServerApi {
    /** Sends an RSA-encrypted (base64) OTP code to the server. */
    suspend fun pushOtp(encryptedCode: String)
}
