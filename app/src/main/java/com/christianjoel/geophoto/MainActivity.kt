package com.christianjoel.geophoto

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.christianjoel.geophoto.data.location.LocationHelper
import com.christianjoel.geophoto.ui.navigation.AppNavGraph
import com.christianjoel.geophoto.ui.permission.PermissionManager
import com.christianjoel.geophoto.utils.InAppUpdateManager
import com.christianjoel.geophoto.viewmodel.PhotoViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: PhotoViewModel by viewModels()

    private lateinit var locationHelper: LocationHelper
    private lateinit var permissionManager: PermissionManager

    // In-App Update
    private lateinit var updateLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var inAppUpdateManager: InAppUpdateManager
    private var updateCheckedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationHelper = LocationHelper(this)
        permissionManager = PermissionManager(this)

        setupUpdate()
        setupUI()
        observeLocationRequests()
    }

    // --------------------------------
    // UI
    // --------------------------------

    private fun setupUI() {
        setContent {
            AppNavGraph(viewModel)
        }
    }

    // --------------------------------
    // Lifecycle
    // --------------------------------

    override fun onResume() {
        super.onResume()

        checkPermissions()

        inAppUpdateManager.registerListener()

        if (!updateCheckedOnce) {
            updateCheckedOnce = true
            inAppUpdateManager.checkForUpdate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        inAppUpdateManager.unregisterListener()
    }

    // --------------------------------
    // Permissions
    // --------------------------------

    private fun checkPermissions() {
        permissionManager.camera(mandatory = true) { cameraGranted ->
            if (!cameraGranted) {
                viewModel.setPermissionsGranted(false)
                return@camera
            }

            permissionManager.location(mandatory = true) { locationGranted ->
                viewModel.setPermissionsGranted(locationGranted)
                if (locationGranted) {
                    fetchLocationOnce()
                }
            }
        }
    }

    // --------------------------------
    // Location
    // --------------------------------

    private fun observeLocationRequests() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                snapshotFlow { viewModel.requestLocationUpdate.value }
                    .collect { shouldFetch ->
                        if (shouldFetch) fetchLocationOnce()
                    }
            }
        }
    }

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

    // --------------------------------
    // In-App Update
    // --------------------------------

    private fun setupUpdate() {
        updateLauncher =
            registerForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                if (result.resultCode != RESULT_OK) {
                    Toast.makeText(
                        this,
                        "Update cancelled",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        inAppUpdateManager = InAppUpdateManager(this, updateLauncher)
    }
}
