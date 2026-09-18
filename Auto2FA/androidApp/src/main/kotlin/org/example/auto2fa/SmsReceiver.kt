package org.example.auto2fa

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val OTP_CODE_REGEX = Regex("""\b\d{4,8}\b""")

fun extractOtpCode(message: String): String? = OTP_CODE_REGEX.find(message)?.value

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val appContext = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(appContext).settings.first()

                for (sms in messages) {
                    val sender = sms.originatingAddress ?: "Unknown"
                    val body = sms.messageBody ?: ""
                    val code = extractOtpCode(body)

                    if (code == null) {
                        Log.d("SMS_LOG", "No OTP-looking code found in SMS from $sender")
                        continue
                    }

                    if (settings.serverIp.isBlank() || settings.publicKeyBase64.isBlank()) {
                        Log.w("SMS_LOG", "Server IP or public key not configured yet, dropping code from $sender")
                        continue
                    }

                    val encryptedCode = RsaCrypto.encrypt(settings.publicKeyBase64, code)
                    val client = OtpPushClient(settings.serverIp, settings.serverPort)

                    try {
                        client.pushOtp(encryptedCode)
                        Log.d("SMS_LOG", "Pushed OTP from $sender to ${settings.serverIp}:${settings.serverPort}")
                    } catch (e: Exception) {
                        Log.e("SMS_LOG", "Failed to push OTP to ${settings.serverIp}:${settings.serverPort}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("SMS_LOG", "Failed to process incoming SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
