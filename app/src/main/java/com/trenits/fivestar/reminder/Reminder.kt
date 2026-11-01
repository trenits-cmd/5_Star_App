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
import com.trenits.fivestar.domain.dayKeyOf
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

    fun time(): LocalTime = LocalTime.of(prefs.getInt("hour", 20), prefs.getInt("minute", 0))

    fun save(time: LocalTime) {
        prefs.edit().putInt("hour", time.hour).putInt("minute", time.minute).apply()
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

    /** Täglich zur gewählten Uhrzeit. Ein bereits geplanter Lauf wird mit der neuen Zeit ersetzt. */
    fun schedule(context: Context, time: LocalTime, now: ZonedDateTime = ZonedDateTime.now()) {
        var next = now.toLocalDate().atTime(time).atZone(now.zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** Sendet für jede übergebene Kategorie ihre personalisierten Push-Nachrichten. */
    @SuppressLint("MissingPermission")
    fun sendCategoryNotifications(context: Context, categories: List<Category>) {
        createChannel(context)
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!allowed) return

        val manager = NotificationManagerCompat.from(context)
        var seq = 0
        categories.forEach { category ->
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
}

/**
 * Sendet personalisierte Push-Nachrichten für alle aktiven Kategorien der Person
 * (auch mehrere Nachrichten pro Kategorie), für die heute noch keine Bewertung vorliegt
 * oder für die eigene Push-Nachrichten hinterlegt sind.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
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
                        kind = doc.getString("kind")?.let { runCatching { CategoryKind.valueOf(it) }.getOrNull() } ?: CategoryKind.STARS,
                        targets = if (targets.isNullOrEmpty()) listOf(owner) else targets,
                        goal = doc.getDouble("goal") ?: 0.0,
                        icon = doc.getString("icon") ?: "",
                        notifications = notifications,
                    )
                    cat.takeIf { it.appliesTo(uid) }
                }

                val record = coupleRef.collection("days").document("${today}_$uid").get().await()
                @Suppress("UNCHECKED_CAST")
                val snapshots = (record.get("snapshots") as? List<Map<String, Any>>).orEmpty()
                val ratedIds = snapshots
                    .filter { ((it["stars"] as? Number)?.toInt() ?: 0) > 0 }
                    .mapNotNull { it["categoryId"] as? String }
                    .toSet()

                val toNotify = myCategories.filter { it.id !in ratedIds || it.notifications.isNotEmpty() }
                if (toNotify.isNotEmpty()) {
                    ReminderScheduler.sendCategoryNotifications(applicationContext, toNotify)
                }
                Result.success()
            }
        } catch (e: Exception) {
            Result.success()
        }
    }
}
