package com.example.auto2fa

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.auto2fa.ui.theme.Auto2FATheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Auto2FATheme {
                App()
            }
        }
    }
}

@Composable
fun Greeting(name: String?, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}


@Composable
fun AppContent() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Greeting(
            name = "Android",
            modifier = Modifier.padding(innerPadding)
        )

//        val context = LocalContext.current
//        val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        val network = connectivityManager.activeNetwork
//        val capabilities = connectivityManager.getNetworkCapabilities(network)
//        val wifiInfo = capabilities?.transportInfo as? WifiInfo
//
//        if (wifiInfo?.hiddenSSID == true) {
//            Greeting(
//                name = "SSID is hidded!",
//                modifier = Modifier.padding(innerPadding)
//            )
//        } else {
//            val ssid = wifiInfo?.ssid
//            Greeting(
//                name = ssid,
//                modifier = Modifier.padding(innerPadding)
//            )
//        }
    }
}

@Composable
fun NeedPermissionContent() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Greeting(
            name = "Need permissions!",
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun App() {
    var isGranted by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        isGranted = it
    }

    LaunchedEffect(isGranted) {
        if (!isGranted) {
            launcher.launch(Manifest.permission.RECEIVE_SMS)
        }
    }

    if (isGranted) {
        AppContent()
    } else {
        NeedPermissionContent()
    }
}


// TODO - relaunch app on boot
// TODO - http to communicate with server