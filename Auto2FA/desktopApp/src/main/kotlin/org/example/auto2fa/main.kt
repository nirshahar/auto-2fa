package org.example.auto2fa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicLong

private data class PendingOtp(val id: Long, val code: String)

// Tall enough that both the public key and TLS certificate cards, plus their copy buttons, are
// visible without the user having to resize the window manually.
private val MIN_WINDOW_SIZE = Dimension(480, 700)

fun main() {
    val keys = DesktopKeyStore.loadOrCreate()
    val pendingOtps = mutableStateListOf<PendingOtp>()
    val nextId = AtomicLong(0)

    // Started before `application {}` and never tied to any window's lifecycle, so it keeps
    // running as long as the process is alive -- including while the settings window is hidden.
    launchServer(
        tlsKeyStore = keys.tlsKeyStore,
        tlsKeyStorePassword = keys.tlsKeyStorePassword,
        decrypt = { ciphertext -> RsaCrypto.decrypt(keys.privateKeyBase64, ciphertext) },
        onOtpReceived = { code ->
            pendingOtps.add(PendingOtp(id = nextId.getAndIncrement(), code = code))
        },
    )

    application {
        // Closing the settings window only hides it (see onCloseRequest below) -- the tray icon
        // is what keeps the process, and therefore the server, alive. Only "Quit" in the tray
        // menu calls exitApplication().
        var isWindowVisible by remember { mutableStateOf(true) }

        // Note: on Windows, onAction only fires on a double-click of the icon once a popup menu
        // is attached (a java.awt.TrayIcon quirk under the hood) -- a single left-click does
        // nothing. Left as-is rather than working around it with a raw AWT MouseListener.
        Tray(
            icon = trayIcon(),
            tooltip = "Auto2FA (server running)",
            onAction = { isWindowVisible = true },
            menu = {
                Item("Open Settings", onClick = { isWindowVisible = true })
                Item("Quit", onClick = ::exitApplication)
            },
        )

        if (isWindowVisible) {
            Window(
                onCloseRequest = { isWindowVisible = false },
                title = "Auto2FA",
                state = rememberWindowState(width = MIN_WINDOW_SIZE.width.dp, height = MIN_WINDOW_SIZE.height.dp),
            ) {
                window.minimumSize = MIN_WINDOW_SIZE
                SettingsScreen(
                    publicKeyBase64 = keys.publicKeyBase64,
                    tlsCertificateBase64 = keys.tlsCertificateBase64,
                )
            }
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

/** A small generated "2" badge icon, so the tray doesn't need a bundled image resource. */
private fun trayIcon(): Painter {
    val size = 32
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.color = Color(0x2E, 0x7D, 0x32)
    g.fillOval(2, 2, size - 4, size - 4)
    g.color = Color.WHITE
    g.font = Font("SansSerif", Font.BOLD, 16)
    val text = "2"
    val metrics = g.fontMetrics
    val x = (size - metrics.stringWidth(text)) / 2f
    val y = (size + metrics.ascent - metrics.descent) / 2f
    g.drawString(text, x, y)
    g.dispose()
    return BitmapPainter(image.toComposeImageBitmap())
}
