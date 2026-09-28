package com.christianjoel.geophoto.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import com.christianjoel.geophoto.R

class PhotoViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("geophoto_prefs", Context.MODE_PRIVATE)

    private val _language = mutableStateOf(prefs.getString("pref_language", "en") ?: "en")
    val language: State<String> = _language

    private val fetchingString: String
        get() = getApplication<Application>().getString(R.string.fetching_address)

    fun setLanguage(lang: String) {
        if (_language.value == lang) return
        _language.value = lang
        prefs.edit().putString("pref_language", lang).apply()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
        retryLocation()
    }

    private val _imageUri = mutableStateOf<Uri?>(null)
    val imageUri: State<Uri?> = _imageUri

    private val _imageUris = mutableStateOf<List<Uri>>(emptyList())
    val imageUris: State<List<Uri>> = _imageUris

    private val _address = mutableStateOf("")
    val address: State<String> = _address

    private val _captureTime = mutableStateOf("")
    val captureTime: State<String> = _captureTime

    private val _hasPermissions = mutableStateOf(false)
    val hasPermissions: State<Boolean> = _hasPermissions

    private val _isAddressFetched = mutableStateOf(false)
    val isAddressFetched: State<Boolean> = _isAddressFetched

    private val _requestLocationUpdate = mutableStateOf(false)
    val requestLocationUpdate: State<Boolean> = _requestLocationUpdate

    fun setCaptureTime(time: String) {
        _captureTime.value = time
    }

    fun requestFreshLocation() {
        _requestLocationUpdate.value = true
    }

    fun retryLocation() {
        _address.value = fetchingString
        _isAddressFetched.value = false
        requestFreshLocation()
    }

    fun onLocationFetched() {
        _requestLocationUpdate.value = false
    }

    fun setPermissionsGranted(granted: Boolean) {
        _hasPermissions.value = granted
    }

    fun setImage(uri: Uri) {
        _imageUri.value = uri
        _imageUris.value = listOf(uri)

        // Reset state for new photo
        _address.value = fetchingString
        _captureTime.value = ""
        _isAddressFetched.value = false
    }

    fun setImages(uris: List<Uri>) {
        _imageUris.value = uris
        _imageUri.value = uris.firstOrNull()

        // Reset state for new photo
        _address.value = fetchingString
        _captureTime.value = ""
        _isAddressFetched.value = false
    }

    fun addImage(uri: Uri) {
        _imageUris.value = _imageUris.value + uri
        if (_imageUri.value == null) {
            _imageUri.value = uri
        }
        if (_address.value.isBlank() || _address.value.contains("not available", true) || _address.value.contains("கிடைக்கவில்லை", true)) {
            _address.value = fetchingString
            _isAddressFetched.value = false
            requestFreshLocation()
        }
    }

    fun addImages(uris: List<Uri>) {
        _imageUris.value = _imageUris.value + uris
        if (_imageUri.value == null && uris.isNotEmpty()) {
            _imageUri.value = uris.first()
        }
        if (_address.value.isBlank() || _address.value.contains("not available", true) || _address.value.contains("கிடைக்கவில்லை", true)) {
            _address.value = fetchingString
            _isAddressFetched.value = false
            requestFreshLocation()
        }
    }

    fun removeImage(uri: Uri) {
        _imageUris.value = _imageUris.value - uri
        if (_imageUri.value == uri) {
            _imageUri.value = _imageUris.value.firstOrNull()
        }
    }

    fun setAddress(text: String) {
        _address.value = text

        _isAddressFetched.value =
            text.isNotBlank() &&
                    !text.contains("Fetching", ignoreCase = true) &&
                    !text.contains("பெறப்படுகிறது", ignoreCase = true) &&
                    !text.contains("Location not available", ignoreCase = true) &&
                    !text.contains("Address not available", ignoreCase = true) &&
                    !text.contains("கிடைக்கவில்லை", ignoreCase = true)
    }

    fun resetAddress() {
        _address.value = ""
        _captureTime.value = ""
        _isAddressFetched.value = false
    }
}