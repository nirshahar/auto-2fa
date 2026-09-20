package org.example.auto2fa

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Invisible host for the system's SMS User Consent dialog: [SmsConsentService] can't call
 * `startActivityForResult` itself (it's not an Activity), so it hands the consent [Intent] here
 * (via its full-screen-intent notification -- see that class for why it can't just start this
 * activity directly). This activity's only job is to show that system dialog, forward an allowed
 * message to [OtpForwarder], and immediately finish -- nothing here is ever meant to be visibly
 * "its own screen".
 */
class SmsConsentActivity : ComponentActivity() {

    private val consentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val message = if (result.resultCode == RESULT_OK) {
            result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
        } else {
            null
        }

        if (message == null) {
            finish()
            return@registerForActivityResult
        }

        CoroutineScope(Dispatchers.IO).launch {
            OtpForwarder.forward(applicationContext, message)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationManagerCompat.from(this).cancel(SmsConsentService.PROMPT_NOTIFICATION_ID)

        @Suppress("DEPRECATION")
        val consentIntent = intent.getParcelableExtra<Intent>(EXTRA_CONSENT_INTENT)
        if (consentIntent == null) {
            finish()
            return
        }

        consentLauncher.launch(consentIntent)
    }

    companion object {
        const val EXTRA_CONSENT_INTENT = "consent_intent"
    }
}
