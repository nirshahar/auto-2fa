package org.example.auto2fa

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            Auto2FAApp()
        }
    }
}

@Preview
@Composable
fun Auto2FAAppPreview() {
    Auto2FAApp()
}

@Composable
fun Auto2FAApp() {
    var isGranted by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        isGranted = it
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.RECEIVE_SMS)
    }

    MaterialTheme {
        if (isGranted) {
            SettingsScreen()
        } else {
            NeedPermissionContent()
        }
    }
}
