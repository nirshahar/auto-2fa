package org.example.auto2fa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(publicKeyBase64: String, tlsCertificateBase64: String) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Auto2FA Settings") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Paste this public key into the Auto2FA app on your phone.",
                style = MaterialTheme.typography.bodyMedium,
            )
            CopyableValueCard(
                value = publicKeyBase64,
                buttonLabel = "Copy Public Key",
                copiedMessage = "Public key copied",
                clipboardManager = clipboardManager,
                snackbarHostState = snackbarHostState,
                scope = scope,
            )

            Text(
                "Paste this TLS certificate into the Auto2FA app on your phone -- it's how the " +
                    "phone verifies it's really talking to this desktop over HTTPS.",
                style = MaterialTheme.typography.bodyMedium,
            )
            CopyableValueCard(
                value = tlsCertificateBase64,
                buttonLabel = "Copy TLS Certificate",
                copiedMessage = "TLS certificate copied",
                clipboardManager = clipboardManager,
                snackbarHostState = snackbarHostState,
                scope = scope,
            )
        }
    }
}

@Composable
private fun CopyableValueCard(
    value: String,
    buttonLabel: String,
    copiedMessage: String,
    clipboardManager: ClipboardManager,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = value,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }

    Button(
        onClick = {
            clipboardManager.setText(AnnotatedString(value))
            scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(buttonLabel)
    }
}
