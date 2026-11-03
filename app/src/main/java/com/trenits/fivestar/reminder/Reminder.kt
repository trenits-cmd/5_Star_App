package com.trenits.fivestar.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.trenits.fivestar.R
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.CategoryKind
import com.trenits.fivestar.domain.Task
import com.trenits.fivestar.domain.dayKeyOf
import com.trenits.fivestar.domain.isWithinQuietHours
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "reminder"
private const val BASE_NOTIFICATION_ID = 1001

class ReminderPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("reminder", Context.MODE_PRIVATE)

    fun enabled(): Boolean = prefs.getBoolean("enabled", true)

    fun saveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("enabled", enabled).apply()
    }

    fun time(): LocalTime = LocalTime.of(prefs.getInt("hour", 22), prefs.getInt("minute", 0))

    fun save(time: LocalTime) {
        prefs.edit().putInt("hour", time.hour).putInt("minute", time.minute).apply()
    }

    /** 0 = einmal täglich zur festen Uhrzeit, > 0 = alle X Stunden. */
    fun intervalHours(): Int = prefs.getInt("intervalHours", 0)

    fun saveIntervalHours(hours: Int) {
        prefs.edit().putInt("intervalHours", hours.coerceIn(0, 12)).apply()
    }

    /** Ruhepause aktiv (Standard: true, 23:00 bis 09:00 Uhr). */
    fun quietHoursEnabled(): Boolean = prefs.getBoolean("quietEnabled", true)

    fun saveQuietHoursEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("quietEnabled", enabled).apply()
    }

    fun quietStart(): LocalTime = LocalTime.of(
        prefs.getInt("quietStartHour", 23),
        prefs.getInt("quietStartMinute", 0),
    )

    fun saveQuietStart(time: LocalTime) {
        prefs.edit()
            .putInt("quietStartHour", time.hour)
            .putInt("quietStartMinute", time.minute)
            .apply()
    }

    fun quietEnd(): LocalTime = LocalTime.of(
        prefs.getInt("quietEndHour", 9),
        prefs.getInt("quietEndMinute", 0),
    )

    fun saveQuietEnd(time: LocalTime) {
        prefs.edit()
            .putInt("quietEndHour", time.hour)
            .putInt("quietEndMinute", time.minute)
            .apply()
    }

    fun lastNotifiedEpochHour(categoryId: String): Long =
        prefs.getLong("last_notify_$categoryId", -1L)

    fun markNotifiedEpochHour(categoryId: String, epochHour: Long) {
        prefs.edit().putLong("last_notify_$categoryId", epochHour).apply()
    }

    fun hasNotifiedConfirmForTask(taskId: String): Boolean =
        prefs.getBoolean("confirm_notify_$taskId", false)

    fun markNotifiedConfirmForTask(taskId: String) {
        prefs.edit().putBoolean("confirm_notify_$taskId", true).apply()
    }
}

object ReminderScheduler {
    private const val WORK_NAME = "daily-reminder"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * Plant den periodischen Check für tägliche Uhrzeiten und mehrstündige Intervalle
     * (z. B. alle 2 Std. fürs Wassertrinken unter Beachtung der Ruhepause 23:00–09:00 Uhr).
     */
    fun schedule(context: Context, time: LocalTime, now: ZonedDateTime = ZonedDateTime.now()) {
        val prefs = ReminderPrefs(context)
        if (!prefs.enabled()) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            return
        }
        val interval = prefs.intervalHours()
        val request = if (interval > 0) {
            PeriodicWorkRequestBuilder<ReminderWorker>(interval.toLong().coerceAtLeast(1L), TimeUnit.HOURS)
                .build()
        } else {
            var next = now.toLocalDate().atTime(time).atZone(now.zone)
            if (!next.isAfter(now)) next = next.plusDays(1)
            PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.HOURS)
                .setInitialDelay(Duration.between(now, next).toMillis().coerceAtMost(3600_000L), TimeUnit.MILLISECONDS)
                .build()
        }
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** Sendet für jede übergebene aktive Kategorie ihre personalisierten Push-Nachrichten. */
    @SuppressLint("MissingPermission")
    fun sendCategoryNotifications(context: Context, categories: List<Category>) {
        createChannel(context)
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!allowed) return

        val manager = NotificationManagerCompat.from(context)
        var seq = 0
        categories.filter { it.pushEnabled }.forEach { category ->
            val messages = category.notifications
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .ifEmpty { listOf(context.getString(R.string.reminder_default_category, category.name)) }

            messages.forEach { message ->
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(category.name)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setAutoCancel(true)
                    .build()
                manager.notify(BASE_NOTIFICATION_ID + seq, notification)
                seq++
            }
        }
    }

    /**
     * Sendet nach 3 Stunden eine Push-Benachrichtigung an den Partner
     * (z. B. „Rosie wartet auf deine Bestätigung!“), wenn eine gewählte Strafe/Belohnung noch offen ist.
     */
    @SuppressLint("MissingPermission")
    fun sendConfirmationReminderNotification(
        context: Context,
        partnerDisplayName: String,
        task: Task,
    ) {
        createChannel(context)
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!allowed) return

        val title = context.getString(R.string.confirm_push_title, partnerDisplayName)
        val body = context.getString(R.string.confirm_push_body, partnerDisplayName, task.title)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(BASE_NOTIFICATION_ID + 500 + (task.id.hashCode() and 0xFF), notification)
    }

    /**
     * Sendet die tägliche Erinnerungs-Push um die eingestellte Uhrzeit (Standard 22:00),
     * wenn der heutige Tag noch nicht vollständig ausgefüllt ist.
     */
    @SuppressLint("MissingPermission")
    fun sendDailyReminderNotification(context: Context) {
        createChannel(context)
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!allowed) return

        val title = context.getString(R.string.reminder_daily_title)
        val body = context.getString(R.string.reminder_daily_body)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(BASE_NOTIFICATION_ID + 900, notification)
    }

    /**
     * Benachrichtigt den Einlader per Push, sobald sich der eingeladene Partner mit dem Code
     * registriert/angemeldet hat und die Verbindung automatisch hergestellt wurde.
     */
    @SuppressLint("MissingPermission")
    fun sendPartnerJoinedNotification(context: Context, partnerLabel: String) {
        createChannel(context)
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!allowed) return

        val title = context.getString(R.string.partner_joined_push_title)
        val body = context.getString(R.string.partner_joined_push_body, partnerLabel)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(BASE_NOTIFICATION_ID + 950, notification)
    }
}

/**
 * Prüft stündlich bzw. zur gewählten Uhrzeit, welche Kategorien eine Push-Benachrichtigung senden sollen:
 * - Beachtet den globalen Kippschalter sowie den Kippschalter je Kategorie.
 * - Beachtet die Ruhepause (Standard: 23:00 bis 09:00 Uhr – keine Benachrichtigungen).
 * - Unterstützt sowohl feste tägliche Uhrzeiten als auch Intervalle (z. B. alle 2 Std. Wasser trinken).
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = ReminderPrefs(applicationContext)
        if (!prefs.enabled()) return Result.success()

        val now = ZonedDateTime.now()
        if (prefs.quietHoursEnabled() &&
            isWithinQuietHours(now.toLocalTime(), prefs.quietStart(), prefs.quietEnd())
        ) {
            // Während der Ruhepause (z. B. 23:00–09:00 Uhr) keine Benachrichtigungen senden
            return Result.success()
        }

        val uid = Firebase.auth.currentUser?.uid ?: return Result.success()
        val db = Firebase.firestore
        return try {
            withTimeout(15_000) {
                val coupleId = db.collection("users").document(uid).get().await().getString("coupleId")
                    ?: return@withTimeout Result.success()
                val today = dayKeyOf(Instant.now(), ZoneId.systemDefault())
                val coupleRef = db.collection("couples").document(coupleId)

                val catDocs = coupleRef.collection("categories").get().await()
                val myCategories = catDocs.documents.mapNotNull { doc ->
                    val owner = doc.getString("ownerUid") ?: return@mapNotNull null
                    val targets = (doc.get("targets") as? List<*>)?.filterIsInstance<String>()
                    val notifications = (doc.get("notifications") as? List<*>)?.filterIsInstance<String>().orEmpty()
                    val cat = Category(
                        id = doc.id,
                        ownerUid = owner,
                        name = doc.getString("name") ?: "",
                        weight = doc.getLong("weight")?.toInt() ?: 1,
                        archived = doc.getBoolean("archived") ?: false,
                        kind = doc.getString("kind")?.let { runCatching { CategoryKind.valueOf(it) }.getOrNull() }
                            ?: CategoryKind.STARS,
                        targets = if (targets.isNullOrEmpty()) listOf(owner) else targets,
                        goal = doc.getDouble("goal") ?: 0.0,
                        icon = doc.getString("icon") ?: "",
                        notifications = notifications,
                        pushEnabled = doc.getBoolean("pushEnabled") ?: true,
                        pushIntervalHours = doc.getLong("pushIntervalHours")?.toInt() ?: 0,
                        pushHour = doc.getLong("pushHour")?.toInt() ?: prefs.time().hour,
                        pushMinute = doc.getLong("pushMinute")?.toInt() ?: prefs.time().minute,
                    )
                    cat.takeIf { it.appliesTo(uid) && it.pushEnabled }
                }

                val record = coupleRef.collection("days").document("${today}_$uid").get().await()
                @Suppress("UNCHECKED_CAST")
                val snapshots = (record.get("snapshots") as? List<Map<String, Any>>).orEmpty()
                val fullStarIds = snapshots
                    .filter { ((it["stars"] as? Number)?.toInt() ?: 0) >= 5 }
                    .mapNotNull { it["categoryId"] as? String }
                    .toSet()

                val currentEpochHour = Instant.now().epochSecond / 3600L
                val dueCategories = myCategories.filter { cat ->
                    if (cat.id in fullStarIds) return@filter false
                    val lastHour = prefs.lastNotifiedEpochHour(cat.id)
                    val interval = if (cat.pushIntervalHours > 0) cat.pushIntervalHours else prefs.intervalHours()
                    if (interval > 0) {
                        lastHour < 0L || (currentEpochHour - lastHour) >= interval
                    } else {
                        now.hour >= cat.pushHour && (lastHour < 0L || (currentEpochHour - lastHour) >= 20L)
                    }
                }

                if (dueCategories.isNotEmpty()) {
                    ReminderScheduler.sendCategoryNotifications(applicationContext, dueCategories)
                    dueCategories.forEach { prefs.markNotifiedEpochHour(it.id, currentEpochHour) }
                }

                // Tägliche Erinnerung um die eingestellte Uhrzeit (Standard 22:00),
                // falls der heutige Tag noch nicht vollständig ausgefüllt ist.
                val reminderTime = prefs.time()
                if (now.toLocalTime().hour == reminderTime.hour) {
                    val allFilled = snapshots.isNotEmpty() && snapshots.all {
                        ((it["stars"] as? Number)?.toInt() ?: 0) > 0
                    }
                    if (!allFilled) {
                        val lastHour = prefs.lastNotifiedEpochHour("daily_reminder")
                        if (currentEpochHour - lastHour >= 1L) {
                            ReminderScheduler.sendDailyReminderNotification(applicationContext)
                            prefs.markNotifiedEpochHour("daily_reminder", currentEpochHour)
                        }
                    }
                }
                Result.success()
            }
        } catch (_: Exception) {
            Result.success()
        }
    }
}
