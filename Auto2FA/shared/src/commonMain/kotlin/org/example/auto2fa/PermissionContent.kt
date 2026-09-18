package org.example.auto2fa

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import auto2fa.shared.generated.resources.Res
import auto2fa.shared.generated.resources.need_permissions_message
import org.jetbrains.compose.resources.stringResource

@Composable
fun NeedPermissionContent() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Text(
            text = stringResource(Res.string.need_permissions_message),
            modifier = Modifier.padding(innerPadding)
        )
    }
}
