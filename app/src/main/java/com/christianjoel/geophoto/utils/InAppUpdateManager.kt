package com.christianjoel.geophoto.utils

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

class InAppUpdateManager(
    private val activity: Activity,
    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest>
) {

    private val appUpdateManager: AppUpdateManager =
        AppUpdateManagerFactory.create(activity)

    private val installStateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showRestartSnackbar()
        }
    }

    fun checkForUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->

            val updateAvailable =
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE

            val flexibleAllowed =
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

            if (updateAvailable && flexibleAllowed) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    updateLauncher,
                    AppUpdateOptions
                        .newBuilder(AppUpdateType.FLEXIBLE)
                        .build()
                )
            }
        }
    }

    fun registerListener() {
        appUpdateManager.registerListener(installStateListener)
    }

    fun unregisterListener() {
        appUpdateManager.unregisterListener(installStateListener)
    }

    private fun showRestartSnackbar() {
        Snackbar.make(
            activity.findViewById(android.R.id.content),
            "Update downloaded",
            Snackbar.LENGTH_INDEFINITE
        ).setAction("Restart") {
            appUpdateManager.completeUpdate()
        }.show()
    }
}
