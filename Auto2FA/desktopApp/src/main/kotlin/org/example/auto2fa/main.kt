package org.example.auto2fa

import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import java.util.concurrent.atomic.AtomicLong

private data class PendingOtp(val id: Long, val code: String)

// Tall enough that both the public key and TLS certificate cards, plus their copy buttons, are
// visible without the user having to resize the window manually.
private val MIN_WINDOW_SIZE = Dimension(480, 700)

fun main() {
    val keys = DesktopKeyStore.loadOrCreate()
    val pendingOtps = mutableStateListOf<PendingOtp>()
    val nextId = AtomicLong(0)

    launchServer(
        tlsKeyStore = keys.tlsKeyStore,
        tlsKeyStorePassword = keys.tlsKeyStorePassword,
        decrypt = { ciphertext -> RsaCrypto.decrypt(keys.privateKeyBase64, ciphertext) },
        onOtpReceived = { code ->
            pendingOtps.add(PendingOtp(id = nextId.getAndIncrement(), code = code))
            println("Got a new code:")
            println(code)
        },
    )

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Auto2FA",
            state = rememberWindowState(width = MIN_WINDOW_SIZE.width.dp, height = MIN_WINDOW_SIZE.height.dp),
        ) {
            window.minimumSize = MIN_WINDOW_SIZE
            SettingsScreen(
                publicKeyBase64 = keys.publicKeyBase64,
                tlsCertificateBase64 = keys.tlsCertificateBase64,
            )
        }

        for (pending in pendingOtps) {
            key(pending.id) {
                OtpNotificationWindow(
                    code = pending.code,
                    onDismiss = { pendingOtps.remove(pending) },
                )
            }
        }
    }
}
