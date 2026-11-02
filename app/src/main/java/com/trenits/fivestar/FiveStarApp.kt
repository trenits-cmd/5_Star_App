package com.trenits.fivestar

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestore
import com.trenits.fivestar.reminder.ReminderScheduler

class FiveStarApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Standardsprache Englisch bei Erstinstallation
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("language_chosen", false)) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
            prefs.edit().putBoolean("language_chosen", true).apply()
        }

        Firebase.firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
        ReminderScheduler.createChannel(this)
    }
}
