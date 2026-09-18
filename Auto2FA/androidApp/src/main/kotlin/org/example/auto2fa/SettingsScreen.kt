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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.settings_saved_message)

    var serverIp by remember { mutableStateOf("") }
    var publicKey by remember { mutableStateOf("") }
    var tlsCertificate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val settings = repository.settings.first()
        serverIp = settings.serverIp
        publicKey = settings.publicKeyBase64
        tlsCertificate = settings.tlsCertificateBase64
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        SettingsForm(
            modifier = Modifier.padding(innerPadding),
            serverIp = serverIp,
            onServerIpChange = { serverIp = it },
            publicKey = publicKey,
            onPublicKeyChange = { publicKey = it },
            tlsCertificate = tlsCertificate,
            onTlsCertificateChange = { tlsCertificate = it },
            onSave = {
                scope.launch {
                    repository.save(
                        Settings(
                            serverIp = serverIp.trim(),
                            publicKeyBase64 = publicKey.trim(),
                            tlsCertificateBase64 = tlsCertificate.trim(),
                        )
                    )
                    snackbarHostState.showSnackbar(savedMessage)
                }
            },
        )
    }
}

@Composable
private fun SettingsForm(
    serverIp: String,
    onServerIpChange: (String) -> Unit,
    publicKey: String,
    onPublicKeyChange: (String) -> Unit,
    tlsCertificate: String,
    onTlsCertificateChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.settings_description),
            style = MaterialTheme.typography.bodyMedium,
        )
        ServerIpField(value = serverIp, onValueChange = onServerIpChange)
        PublicKeyField(value = publicKey, onValueChange = onPublicKeyChange)
        TlsCertificateField(value = tlsCertificate, onValueChange = onTlsCertificateChange)
        SaveButton(onClick = onSave)
    }
}

@Composable
private fun ServerIpField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.settings_server_ip_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PublicKeyField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.settings_public_key_label)) },
        minLines = 4,
        maxLines = 8,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TlsCertificateField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.settings_tls_certificate_label)) },
        minLines = 4,
        maxLines = 8,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SaveButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.settings_save_button))
    }
}
