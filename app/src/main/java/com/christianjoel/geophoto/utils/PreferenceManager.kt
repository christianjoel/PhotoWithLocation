package com.christianjoel.geophoto.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "geophoto_prefs")

/**
 * PreferenceManager powered by Jetpack Preferences DataStore.
 */
class PreferenceManager(private val context: Context) {

    /**
     * Flow emitting the currently selected app language (defaults to "en").
     */
    val languageFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_LANGUAGE] ?: DEFAULT_LANGUAGE
        }

    /**
     * Asynchronously updates the selected app language.
     */
    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = lang
        }
    }

    /**
     * Synchronously retrieves current language (primarily for Activity initialization).
     */
    fun getLanguageSync(): String = runBlocking {
        languageFlow.first()
    }

    companion object {
        private val KEY_LANGUAGE = stringPreferencesKey("pref_language")
        const val DEFAULT_LANGUAGE = "en"
    }
}
