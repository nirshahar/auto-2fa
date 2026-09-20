package org.example.auto2fa

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.first

private val OTP_CODE_REGEX = Regex("""\b\d{4,8}\b""")

fun extractOtpCode(message: String): String? = OTP_CODE_REGEX.find(message)?.value

/** Encrypts and pushes an OTP found in a sms message to the configured desktop, or drops it (with a log) if nothing matches or nothing's configured yet. */
object OtpForwarder {
    suspend fun forward(context: Context, message: String) {
        val code = extractOtpCode(message)
        if (code == null) {
            Log.d("SMS_LOG", "No OTP-looking code found in consented SMS")
            return
        }

        val settings = SettingsRepository(context.applicationContext).settings.first()
        if (settings.serverIp.isBlank() || settings.publicKeyBase64.isBlank() ||
            settings.tlsCertificateBase64.isBlank()
        ) {
            Log.w("SMS_LOG", "Server IP, public key, or TLS certificate not configured yet, dropping code")
            return
        }

        val encryptedCode = RsaCrypto.encrypt(settings.publicKeyBase64, code)
        val client = OtpPushClient(settings.serverIp, settings.tlsCertificateBase64)

        try {
            client.pushOtp(OtpPushRequest(code = encryptedCode))
            Log.i("SMS_LOG", "Pushed OTP to ${settings.serverIp}:$AUTO2FA_SERVER_PORT")
        } catch (e: Exception) {
            Log.e("SMS_LOG", "Failed to push OTP to ${settings.serverIp}:$AUTO2FA_SERVER_PORT", e)
        }
    }
}
