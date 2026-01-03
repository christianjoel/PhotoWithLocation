package com.christianjoel.geophoto

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.christianjoel.geophoto.data.location.LocationHelper
import com.christianjoel.geophoto.ui.navigation.AppNavGraph
import com.christianjoel.geophoto.utils.InAppUpdateManager
import com.christianjoel.geophoto.viewmodel.PhotoViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: PhotoViewModel by viewModels()
    private lateinit var locationHelper: LocationHelper

    private var permissionRequestedOnce = false

    // In-App Update
    private lateinit var updateLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var inAppUpdateManager: InAppUpdateManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationHelper = LocationHelper(this)

        setContent {
            AppNavGraph(viewModel)
        }

        checkPermissions()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                snapshotFlow { viewModel.requestLocationUpdate.value }
                    .collect { shouldFetch ->
                        if (shouldFetch) {
                            fetchLocationOnce()
                        }
                    }
            }
        }

        // In-App Update setup
        initInAppUpdate()
    }

    private fun initInAppUpdate() {
        updateLauncher =
            registerForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                if (result.resultCode != RESULT_OK) {
                    // User cancelled or update failed
                }
            }

        inAppUpdateManager = InAppUpdateManager(this, updateLauncher)
        inAppUpdateManager.checkForUpdate()
    }


    override fun onResume() {
        super.onResume()
        checkPermissions()
        inAppUpdateManager.registerListener()
    }

    private fun checkPermissions() {
        if (hasAllPermissions()) {
            viewModel.setPermissionsGranted(true)
            fetchLocationOnce()
        } else {
            viewModel.setPermissionsGranted(false)
            if (!permissionRequestedOnce) {
                permissionRequestedOnce = true
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            }
        }
    }

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val granted =
                result[Manifest.permission.CAMERA] == true &&
                        result[Manifest.permission.ACCESS_FINE_LOCATION] == true

            viewModel.setPermissionsGranted(granted)
            if (granted) {
                fetchLocationOnce()
            }
        }

    private fun hasAllPermissions(): Boolean =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED


    private fun fetchLocationOnce() {

        locationHelper.getCurrentLocation { location ->
            if (location == null) {
                viewModel.setAddress("Location not available")
                viewModel.onLocationFetched()
                return@getCurrentLocation
            }

            locationHelper.getAddress(
                lat = location.latitude,
                lng = location.longitude
            ) { address ->
                viewModel.setAddress(address)
                viewModel.onLocationFetched()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        inAppUpdateManager.unregisterListener()
    }
}
