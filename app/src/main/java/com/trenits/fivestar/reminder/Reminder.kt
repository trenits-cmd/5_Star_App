package com.trenits.fivestar.reminder

import android.app.PendingIntent
import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.firestore
import com.trenits.fivestar.R
import com.trenits.fivestar.data.AppClock
import com.trenits.fivestar.data.LocalProfileStore
import com.trenits.fivestar.MainActivity
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.DinnerConfirmation
import com.trenits.fivestar.domain.needsWarning
import com.trenits.fivestar.domain.CategoryKind
import com.trenits.fivestar.domain.Task
import com.trenits.fivestar.domain.TaskStatus
import com.trenits.fivestar.domain.TaskType
import com.trenits.fivestar.domain.dayKeyOf
import com.trenits.fivestar.domain.isWithinQuietHours
import com.trenits.fivestar.domain.needsDeadlineWarning
import com.trenits.fivestar.domain.postponed
import com.trenits.fivestar.domain.autoConfirmAt
import com.trenits.fivestar.domain.needsPartnerConfirmationReminder
import com.trenits.fivestar.domain.withAutoConfirmation
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

    fun hasNotified(key: String): Boolean = prefs.getBoolean("notified_$key", false)

    fun markNotified(key: String) {
        prefs.edit().putBoolean("notified_$key", true).apply()
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
    fun schedule(context: Context, time: LocalTime, now: ZonedDateTime = AppClock.now().atZone(ZoneId.systemDefault())) {
        val prefs = ReminderPrefs(context)
        if (!prefs.enabled()) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            return
        }
        // Der Worker prüft stündlich: Intervall-Erinnerungen, die 22:00-Erinnerung,
        // die 3-Stunden-Erinnerung an den Partner und die 48-Stunden-Auto-Bestätigung.
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.HOURS).build()
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

    /** Frühwarnung: bestaetigen oder "Not yet" waehlen, sonst gilt die Strafe zum Fristende als ausgefuehrt. */
    /** Push an die bestätigende Person: Abendessen-Uhrzeit bestätigen (oder Frühwarnung). */
    @SuppressLint("MissingPermission")
    fun sendDinnerConfirmNotification(context: Context, partnerDisplayName: String, dc: DinnerConfirmation, warning: Boolean) {
        createChannel(context)
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!allowed) return
        val time = "%02d:%02d".format(dc.hour, dc.minute)
        val until = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault()).format(dc.deadline)
        val title = context.getString(
            if (warning) R.string.dinner_warn_title else R.string.dinner_confirm_title,
            partnerDisplayName,
        )
        val body = context.getString(R.string.dinner_confirm_body, time, until)
        // Intent to open the app to the dinner confirmation screen
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("dinner_day_key", dc.dayKey)
            putExtra("dinner_confirm", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(BASE_NOTIFICATION_ID + 700 + (dc.dayKey.hashCode() and 0xFF), notification)
    }

    /**
     * Push an die eintragende Person (Partner): die Uhrzeit wurde abgelehnt,
     * sie muss eine neue Zeit eingeben. Läuft auch bei geschlossener App über den ReminderWorker.
     */
    @SuppressLint("MissingPermission")
    fun sendDinnerRejectedNotification(context: Context, dc: DinnerConfirmation) {
        createChannel(context)
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!allowed) return

        val time = "%02d:%02d".format(dc.hour, dc.minute)
        val title = context.getString(R.string.dinner_rejected_title)
        val body = context.getString(R.string.dinner_rejected_body_time, time)
        // Tap öffnet den Abendessen-Tag, damit direkt eine neue Zeit eingetragen werden kann.
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("dinner_day_key", dc.dayKey)
            putExtra("dinner_confirm", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context)
            .notify(BASE_NOTIFICATION_ID + 710 + (dc.dayKey.hashCode() and 0xFF), notification)
    }

    /** Frühwarnung: bestaetigen oder "Not yet" waehlen, sonst gilt die Strafe zum Fristende als ausgefuehrt. */
    @SuppressLint("MissingPermission")
    fun sendDeadlineWarningNotification(context: Context, partnerDisplayName: String, task: Task, deadline: Instant) {
        createChannel(context)
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!allowed) return

        val time = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault()).format(deadline)
        val title = context.getString(R.string.confirm_warn_title, partnerDisplayName)
        val body = context.getString(R.string.confirm_warn_body, task.title, time)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(BASE_NOTIFICATION_ID + 600 + (task.id.hashCode() and 0xFF), notification)
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

        val now = AppClock.now().atZone(ZoneId.systemDefault())
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
                val today = dayKeyOf(AppClock.now(), ZoneId.systemDefault())
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

                val currentEpochHour = AppClock.now().epochSecond / 3600L
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
                checkOpenTasks(coupleRef, uid, now)
                checkDinnerConfirmations(coupleRef, uid, now)
                Result.success()
            }
        } catch (_: Exception) {
            Result.success()
        }
    }

    /**
     * Offene Aufgaben (laeuft auch bei geschlossener App):
     * - 3 h nach Auswahl: Push an den Partner ("... is waiting for your confirmation!").
     * - 6 h vor Fristende: Frueh-Warnung an den Partner (bestaetigen oder gilt als ausgefuehrt).
     * - Fristende (48 h ab Auswahl bzw. verschoben durch "Not yet"): automatisch als ausgefuehrt.
     */
    private suspend fun checkOpenTasks(coupleRef: DocumentReference, uid: String, now: ZonedDateTime) {
        val prefs = ReminderPrefs(applicationContext)
        val instant = now.toInstant()
        val todayKey = dayKeyOf(instant, ZoneId.systemDefault())
        val docs = coupleRef.collection("tasks")
            .whereEqualTo("status", TaskStatus.OPEN.name)
            .get()
            .await()
        for (doc in docs.documents) {
            runCatching {
                val owner = doc.getString("ownerUid") ?: return@runCatching
                val type = doc.getString("type")?.takeIf { it == TaskType.REWARD.name }?.let { TaskType.REWARD } ?: return@runCatching
                val createdAt = doc.getTimestamp("createdAt")
                    ?.let { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) }
                    ?: return@runCatching
                val task = Task(
                    id = doc.id,
                    ownerUid = owner,
                    type = type,
                    title = doc.getString("title") ?: "",
                    dayKey = doc.getString("dayKey") ?: "",
                    status = TaskStatus.OPEN,
                    createdAt = createdAt,
                    targets = listOf(owner),
                    redeemedDayKey = null,
                    selectedAt = doc.getTimestamp("selectedAt")
                        ?.let { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) },
                    deadlineAt = doc.getTimestamp("deadlineAt")
                        ?.let { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) },
                    postponeCount = doc.getLong("postponeCount")?.toInt() ?: 0,
                )
                if (task.withAutoConfirmation(instant).status != task.status) {
                    doc.reference.update(
                        mapOf("status" to TaskStatus.AUTO_CONFIRMED.name, "redeemedDayKey" to todayKey),
                    ).await()
                    return@runCatching
                }
                if (owner == uid) return@runCatching
                val partnerName = LocalProfileStore(applicationContext).partnerDisplayName.value
                    ?: applicationContext.getString(R.string.who_partner)
                if (task.needsPartnerConfirmationReminder(instant) && !prefs.hasNotified("confirm_${task.id}")) {
                    ReminderScheduler.sendConfirmationReminderNotification(applicationContext, partnerName, task)
                    prefs.markNotified("confirm_${task.id}")
                }
                if (task.needsDeadlineWarning(instant)) {
                    val key = "warn_${task.id}_${task.autoConfirmAt.epochSecond}"
                    if (!prefs.hasNotified(key)) {
                        ReminderScheduler.sendDeadlineWarningNotification(
                            applicationContext, partnerName, task, task.autoConfirmAt,
                        )
                        prefs.markNotified(key)
                    }
                }
            }
        }
    }

    /**
     * Abendessen-Uhrzeit: Push an die bestätigende Person, Frühwarnung und Auto-Bestätigung nach 48 h.
     * Abgelehnte Einträge: Push an die eintragende Person ("Bitte korrigieren"), keine Auto-Bestätigung,
     * bis eine neue Zeit eingetragen wurde – der neue Eintrag löst erneut eine Push an main_user aus.
     */
    private suspend fun checkDinnerConfirmations(coupleRef: DocumentReference, uid: String, now: ZonedDateTime) {
        val prefs = ReminderPrefs(applicationContext)
        val instant = now.toInstant()
        val docs = coupleRef.collection("dinnerConfirmations").get().await()
        for (doc in docs.documents) {
            runCatching {
                val enteredBy = doc.getString("enteredBy") ?: return@runCatching
                val hour = doc.getLong("hour")?.toInt() ?: return@runCatching
                val minute = doc.getLong("minute")?.toInt() ?: 0
                val enteredAt = doc.getTimestamp("enteredAt")
                    ?.let { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) } ?: return@runCatching
                val confirmedAt = doc.getTimestamp("confirmedAt")
                    ?.let { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) }
                val rejectedAt = doc.getTimestamp("rejectedAt")
                    ?.let { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) }
                val dc = DinnerConfirmation(
                    dayKey = doc.id,
                    enteredBy = enteredBy,
                    hour = hour,
                    minute = minute,
                    enteredAt = enteredAt,
                    confirmedAt = confirmedAt,
                    rejectedAt = rejectedAt,
                )
                if (dc.isConfirmed) return@runCatching

                if (dc.isRejected) {
                    // Nur der Eintragende muss korrigieren: Push an ihn (einmal pro Ablehnung).
                    if (dc.enteredBy != uid) return@runCatching
                    val rejectedKey = "dinner_rejected_${dc.dayKey}_${(dc.rejectedAt ?: Instant.EPOCH).epochSecond}"
                    if (!prefs.hasNotified(rejectedKey)) {
                        ReminderScheduler.sendDinnerRejectedNotification(applicationContext, dc)
                        prefs.markNotified(rejectedKey)
                    }
                    return@runCatching
                }

                if (enteredBy == uid) return@runCatching
                if (!dc.withAutoConfirmation(instant).isPending) {
                    doc.reference.update(
                        mapOf(
                            "confirmedAt" to Timestamp(instant.epochSecond, instant.nano),
                            "confirmedBy" to uid,
                            "autoConfirmed" to true,
                        ),
                    ).await()
                    return@runCatching
                }
                val partnerName = LocalProfileStore(applicationContext).partnerDisplayName.value
                    ?: applicationContext.getString(R.string.who_partner)
                // Schlüssel je Eintrag: eine korrigierte Uhrzeit löst erneut eine Push aus.
                val entryKey = dc.enteredAt.epochSecond
                if (!prefs.hasNotified("dinner_${dc.dayKey}_$entryKey")) {
                    ReminderScheduler.sendDinnerConfirmNotification(applicationContext, partnerName, dc, warning = false)
                    prefs.markNotified("dinner_${dc.dayKey}_$entryKey")
                }
                if (dc.needsWarning(instant) && !prefs.hasNotified("dinner_warn_${dc.dayKey}_$entryKey")) {
                    ReminderScheduler.sendDinnerConfirmNotification(applicationContext, partnerName, dc, warning = true)
                    prefs.markNotified("dinner_warn_${dc.dayKey}_$entryKey")
                }
            }
        }
    }
}
