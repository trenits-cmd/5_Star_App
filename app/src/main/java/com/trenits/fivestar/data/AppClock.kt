package com.trenits.fivestar.data

import android.content.Context
import android.content.SharedPreferences
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Zentrale Uhr der App. Standard ist die echte Systemzeit.
 * Testzweck (Datums-Simulator): Ein Tagesversatz verschiebt alle Zeitangaben der App,
 * damit Tage, Strafen, Auto-Bestätigung und Auswertungen ohne Wartezeit geprüft werden können.
 * Der Versatz wird lokal gespeichert und vom Hintergrund-Worker mitgenutzt.
 */
object AppClock {
    private const val PREFS_NAME = "app_clock"
    private const val KEY_OFFSET_DAYS = "offset_days"

    @Volatile
    private var offsetDays: Long = 0L
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        offsetDays = p.getLong(KEY_OFFSET_DAYS, 0L)
    }

    fun offsetDays(): Long = offsetDays

    fun setOffsetDays(days: Long) {
        offsetDays = days
        prefs?.edit()?.putLong(KEY_OFFSET_DAYS, days)?.apply()
    }

    fun now(): Instant = Instant.now().plus(Duration.ofDays(offsetDays))

    fun today(zone: ZoneId = ZoneId.systemDefault()): LocalDate = now().atZone(zone).toLocalDate()
}
