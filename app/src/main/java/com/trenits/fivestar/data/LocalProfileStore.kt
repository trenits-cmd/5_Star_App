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

    /** Liest einen Textwert; ist der gespeicherte Typ falsch, wird der Eintrag verworfen statt die App abstürzen zu lassen. */
    private fun readString(key: String): String? = try {
        prefs.getString(key, null)
    } catch (e: ClassCastException) {
        prefs.edit().remove(key).apply()
        null
    }

    private val _displayName = MutableStateFlow(readString(KEY_DISPLAY_NAME))
    val displayName: StateFlow<String?> = _displayName.asStateFlow()

    private val _photoBase64 = MutableStateFlow(readString(KEY_PHOTO_BASE64))
    val photoBase64: StateFlow<String?> = _photoBase64.asStateFlow()

    private val _partnerDisplayName = MutableStateFlow(readString(KEY_PARTNER_DISPLAY_NAME))
    val partnerDisplayName: StateFlow<String?> = _partnerDisplayName.asStateFlow()

    private val _partnerPhotoBase64 = MutableStateFlow(readString(KEY_PARTNER_PHOTO_BASE64))
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

    fun savePendingRedeemCode(code: String?) {
        val cleaned = code?.trim()?.uppercase().orEmpty()
        prefs.edit().putString(KEY_PENDING_REDEEM_CODE, cleaned).apply()
    }

    fun savePendingEmail(email: String) {
        prefs.edit().putString(KEY_PENDING_EMAIL, email.trim()).apply()
    }

    fun pendingEmail(): String? = readString(KEY_PENDING_EMAIL)?.takeIf { it.isNotBlank() }

    fun clearPendingEmail() {
        prefs.edit().remove(KEY_PENDING_EMAIL).apply()
    }

    fun consumePendingRedeemCode(): String? {
        val code = readString(KEY_PENDING_REDEEM_CODE)?.trim()?.takeIf { it.isNotEmpty() }
        if (code != null) {
            prefs.edit().remove(KEY_PENDING_REDEEM_CODE).apply()
        }
        return code
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        _displayName.value = null
        _photoBase64.value = null
        _partnerDisplayName.value = null
        _partnerPhotoBase64.value = null
    }

    companion object {
        private const val PREFS_NAME = "local_profile"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_PHOTO_BASE64 = "photo_base64"
        private const val KEY_PARTNER_DISPLAY_NAME = "partner_display_name"
        private const val KEY_PARTNER_PHOTO_BASE64 = "partner_photo_base64"
        private const val KEY_PENDING_REDEEM_CODE = "pending_redeem_code"
        private const val KEY_PENDING_EMAIL = "pending_email"
    }
}