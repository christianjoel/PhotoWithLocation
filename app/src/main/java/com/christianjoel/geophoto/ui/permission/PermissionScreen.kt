package com.christianjoel.geophoto.ui.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.christianjoel.geophoto.R

@Composable
fun PermissionScreen() {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.permission_required)) },
        text = {
            Text(stringResource(R.string.permission_message))
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
                Text(stringResource(R.string.open_settings))
            }
        }
    )
}
