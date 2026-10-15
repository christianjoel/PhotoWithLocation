package com.christianjoel.geophoto.utils

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.christianjoel.geophoto.R
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Industry-standard Lifecycle-aware In-App Update Manager.
 * Automatically handles registration/unregistration via DefaultLifecycleObserver
 * and uses Coroutines structured concurrency bound to the Activity's lifecycleScope
 * to guarantee crash-free update checks.
 */
class InAppUpdateManager(
    private val activity: ComponentActivity,
    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest>
) : DefaultLifecycleObserver {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private var updateChecked = false

    private val installStateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showRestartSnackbar()
        }
    }

    init {
        activity.lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        appUpdateManager.registerListener(installStateListener)

        if (!updateChecked) {
            checkForUpdate()
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        appUpdateManager.unregisterListener(installStateListener)
    }

    fun checkForUpdate() {
        activity.lifecycleScope.launch {
            try {
                val appUpdateInfo: AppUpdateInfo = appUpdateManager.appUpdateInfo.await()

                // Guard against destroyed or non-started activity lifecycle state
                if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) ||
                    activity.isFinishing || activity.isDestroyed
                ) {
                    return@launch
                }

                when {
                    // 1. Update downloaded -> prompt user to restart
                    appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED -> {
                        showRestartSnackbar()
                    }

                    // 2. In-progress immediate update -> resume flow
                    appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        startUpdateFlow(appUpdateInfo, AppUpdateType.IMMEDIATE)
                    }

                    // 3. New update available -> start flexible update
                    appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                            appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                        updateChecked = true
                        startUpdateFlow(appUpdateInfo, AppUpdateType.FLEXIBLE)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check for in-app updates", e)
            }
        }
    }

    private fun startUpdateFlow(appUpdateInfo: AppUpdateInfo, updateType: Int) {
        if (activity.isFinishing || activity.isDestroyed) return

        try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                updateLauncher,
                AppUpdateOptions.newBuilder(updateType).build()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start update flow", e)
        }
    }

    private fun showRestartSnackbar() {
        if (activity.isFinishing || activity.isDestroyed) return

        Snackbar.make(
            activity.findViewById(android.R.id.content),
            activity.getString(R.string.update_downloaded),
            Snackbar.LENGTH_INDEFINITE
        ).setAction(activity.getString(R.string.restart)) {
            appUpdateManager.completeUpdate()
        }.show()
    }

    companion object {
        private const val TAG = "InAppUpdateManager"
    }
}
