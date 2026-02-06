package com.christianjoel.geophoto.ui.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(
    private val activity: ComponentActivity
) {

    private var callback: ((Boolean) -> Unit)? = null
    private var mandatory = false

    private val launcher =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val granted = result.values.all { it }

            if (!granted && mandatory && isPermanentlyDenied(result)) {
                openSettings()
            }

            callback?.invoke(granted)
            reset()
        }

    // -------------------------
    // Public APIs
    // -------------------------

    fun camera(mandatory: Boolean = false, result: (Boolean) -> Unit) =
        request(arrayOf(Manifest.permission.CAMERA), mandatory, result)

    fun location(mandatory: Boolean = false, result: (Boolean) -> Unit) =
        request(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            mandatory,
            result
        )

    fun photo(mandatory: Boolean = false, result: (Boolean) -> Unit) =
        request(photoPermissions(), mandatory, result)

    // -------------------------
    // Internal
    // -------------------------

    private fun request(
        permissions: Array<String>,
        mandatory: Boolean,
        result: (Boolean) -> Unit
    ) {
        this.mandatory = mandatory
        callback = result

        if (hasPermissions(permissions)) {
            result(true)
            reset()
        } else {
            launcher.launch(permissions)
        }
    }

    private fun hasPermissions(perms: Array<String>): Boolean =
        perms.all {
            ContextCompat.checkSelfPermission(activity, it) ==
                    PackageManager.PERMISSION_GRANTED
        }

    private fun isPermanentlyDenied(result: Map<String, Boolean>): Boolean =
        result.any { (permission, granted) ->
            !granted &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        permission
                    )
        }

    private fun photoPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    private fun openSettings() {
        activity.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
        )
    }

    private fun reset() {
        callback = null
        mandatory = false
    }
}
