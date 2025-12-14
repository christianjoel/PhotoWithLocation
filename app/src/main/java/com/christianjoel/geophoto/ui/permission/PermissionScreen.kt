package com.christianjoel.geophoto.ui.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun PermissionScreen() {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {},
        title = { Text("Permission Required") },
        text = {
            Text("Camera and Location permissions are required.")
        },
        confirmButton = {
            Button(
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                    )
                }
            ) {
                Text("Open Settings")
            }
        }
    )
}
