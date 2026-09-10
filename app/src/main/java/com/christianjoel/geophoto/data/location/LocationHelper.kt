package com.christianjoel.geophoto.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationHelper(private val context: Context) {

    private val client =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(onResult: (Location?) -> Unit) {
        client.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            null
        )
            .addOnSuccessListener { onResult(it) }
            .addOnFailureListener { onResult(null) }
    }


    fun getAddress(
        lat: Double,
        lng: Double,
        onResult: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    geocoder.getFromLocation(
                        lat,
                        lng,
                        1,
                        object : Geocoder.GeocodeListener {

                            override fun onGeocode(addresses: MutableList<Address>) {
                                val address =
                                    addresses.firstOrNull()?.getAddressLine(0)
                                        ?: "Unknown location"
                                onResult(address)
                            }

                            override fun onError(errorMessage: String?) {
                                onResult("Address not available")
                            }
                        }
                    )
                } else {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    val address = addresses?.firstOrNull()?.getAddressLine(0) ?: "Unknown location"
                    withContext(Dispatchers.Main) {
                        onResult(address)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult("Address not available")
                }
            }
        }
    }
}
