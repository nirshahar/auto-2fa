package org.example.auto2fa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState

/** A small always-on-top popup shown for one incoming OTP code, with a copy-to-clipboard action. */
@Composable
fun OtpNotificationWindow(code: String, onDismiss: () -> Unit) {
    val windowState = rememberWindowState(size = DpSize.Unspecified)

    Window(
        onCloseRequest = onDismiss,
        state = windowState,
        title = "New 2FA Code",
        alwaysOnTop = true,
        resizable = false,
    ) {
        // LocalClipboardManager is only provided inside a Window's own composition, not in the
        // outer `application {}` scope this Window is created from -- must be read in here.
        val clipboardManager = LocalClipboardManager.current

        MaterialTheme {
            Surface {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("New 2FA code received", style = MaterialTheme.typography.titleMedium)
                    Text(code, style = MaterialTheme.typography.headlineMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(code))
                                onDismiss()
                            },
                        ) {
                            Text("Copy to Clipboard")
                        }
                        OutlinedButton(onClick = onDismiss) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}
