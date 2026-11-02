package com.trenits.fivestar.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lokaler Speicher für profildaten (Name & Foto), der nur auf diesem Gerät existiert.
 * Nach einer Neuinstallation sind alle Daten weg → "Bei Neuinstallation geht es von vorne los".
 */
class LocalProfileStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _displayName = MutableStateFlow(prefs.getString(KEY_DISPLAY_NAME, null))
    val displayName: StateFlow<String?> = _displayName.asStateFlow()

    private val _photoBase64 = MutableStateFlow(prefs.getString(KEY_PHOTO_BASE64, null))
    val photoBase64: StateFlow<String?> = _photoBase64.asStateFlow()

    private val _partnerDisplayName = MutableStateFlow(prefs.getString(KEY_PARTNER_DISPLAY_NAME, null))
    val partnerDisplayName: StateFlow<String?> = _partnerDisplayName.asStateFlow()

    private val _partnerPhotoBase64 = MutableStateFlow(prefs.getString(KEY_PARTNER_PHOTO_BASE64, null))
    val partnerPhotoBase64: StateFlow<String?> = _partnerPhotoBase64.asStateFlow()

    fun saveDisplayName(name: String) {
        prefs.edit().putString(KEY_DISPLAY_NAME, name.trim()).apply()
        _displayName.value = name.trim()
    }

    fun savePhotoBase64(base64: String?) {
        prefs.edit().putString(KEY_PHOTO_BASE64, base64 ?: "").apply()
        _photoBase64.value = base64
    }

    fun savePartnerDisplayName(name: String) {
        prefs.edit().putString(KEY_PARTNER_DISPLAY_NAME, name.trim()).apply()
        _partnerDisplayName.value = name.trim()
    }

    fun savePartnerPhotoBase64(base64: String?) {
        prefs.edit().putString(KEY_PARTNER_PHOTO_BASE64, base64 ?: "").apply()
        _partnerPhotoBase64.value = base64
    }

    companion object {
        private const val PREFS_NAME = "local_profile"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_PHOTO_BASE64 = "photo_base64"
        private const val KEY_PARTNER_DISPLAY_NAME = "partner_display_name"
        private const val KEY_PARTNER_PHOTO_BASE64 = "partner_photo_base64"
    }
}