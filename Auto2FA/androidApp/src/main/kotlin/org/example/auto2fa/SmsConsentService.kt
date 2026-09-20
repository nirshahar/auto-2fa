package org.example.auto2fa

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

/**
 * Keeps [SmsRetriever]'s User Consent listener continuously armed. That listener only covers a
 * 5-minute window per call (it's designed for "listen right after I triggered an OTP", not
 * "listen forever") -- so every time it fires (a match, or a timeout with no match), this
 * re-arms it immediately. Runs as a foreground service (with its own notification) so Android
 * doesn't tear the listener down while the app is backgrounded, which is the normal state here.
 */
class SmsConsentService : Service() {

    private var consentReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        registerConsentReceiver()
        armConsentListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        consentReceiver?.let { unregisterReceiver(it) }
        consentReceiver = null
        super.onDestroy()
    }

    private fun armConsentListener() {
        SmsRetriever.getClient(this).startSmsUserConsent(null)
    }

    // ContextCompat.RECEIVER_EXPORTED is a valid flag for this call (this exact registration is
    // confirmed live via `adb shell dumpsys activity broadcasts` -- the receiver shows up
    // correctly under com.google.android.gms.auth.api.phone.SMS_RETRIEVED); lint's WrongConstant
    // check misidentifying it here is a tooling false positive, not a real constant mismatch.
    @SuppressLint("WrongConstant")
    private fun registerConsentReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != SmsRetriever.SMS_RETRIEVED_ACTION) return
                val extras = intent.extras ?: return
                val status = extras.get(SmsRetriever.EXTRA_STATUS) as? Status ?: return

                if (status.statusCode == CommonStatusCodes.SUCCESS) {
                    @Suppress("DEPRECATION")
                    val consentIntent = extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                    if (consentIntent != null) {
                        launchConsentActivity(consentIntent)
                    }
                }

                // Whether it matched, timed out, or errored, the 5-minute window is now spent
                // either way -- re-arm so the next SMS still gets caught.
                armConsentListener()
            }
        }
        consentReceiver = receiver
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    // A plain startActivity() from here gets silently blocked by Android's background-activity-
    // launch restrictions -- being a *foreground service* doesn't exempt it, only a visible
    // activity or specific trampolines do. A full-screen-intent notification is the sanctioned
    // trampoline for exactly this (it's how incoming-call/alarm UIs pop over other apps): when
    // the screen is off/locked it launches automatically, otherwise it surfaces as a heads-up
    // notification the user taps -- covers the common case here (phone idle while the user 2FAs
    // on desktop) automatically, and degrades to one tap otherwise.
    private fun launchConsentActivity(consentIntent: Intent) {
        val activityIntent = Intent(this, SmsConsentActivity::class.java).apply {
            putExtra(SmsConsentActivity.EXTRA_CONSENT_INTENT, consentIntent)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            PROMPT_NOTIFICATION_ID,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(PROMPT_CHANNEL_ID, getString(R.string.otp_prompt_channel_name), NotificationManager.IMPORTANCE_HIGH)
        )
        val notification = NotificationCompat.Builder(this, PROMPT_CHANNEL_ID)
            .setContentTitle(getString(R.string.otp_prompt_notification_title))
            .setContentText(getString(R.string.otp_prompt_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(PROMPT_NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): Notification {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(LISTENING_CHANNEL_ID, getString(R.string.otp_listener_channel_name), NotificationManager.IMPORTANCE_LOW)
        )
        return NotificationCompat.Builder(this, LISTENING_CHANNEL_ID)
            .setContentTitle(getString(R.string.otp_listener_notification_title))
            .setContentText(getString(R.string.otp_listener_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val LISTENING_CHANNEL_ID = "sms_consent_listener"

        const val PROMPT_NOTIFICATION_ID = 2
        private const val PROMPT_CHANNEL_ID = "sms_consent_prompt"

        fun start(context: Context) {
            val intent = Intent(context, SmsConsentService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
