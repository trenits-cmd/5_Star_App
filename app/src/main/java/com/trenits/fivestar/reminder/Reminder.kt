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
private const val NOTIFICATION_ID = 1001

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
        val channel = NotificationChannel(CHANNEL_ID, "Tägliche Erinnerung", NotificationManager.IMPORTANCE_DEFAULT)
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
}

/** Erinnert nur, wenn für heute noch keine Bewertung mit Kategorien gespeichert ist. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = Firebase.auth.currentUser?.uid ?: return Result.success()
        val db = Firebase.firestore
        return try {
            withTimeout(15_000) {
                val coupleId = db.collection("users").document(uid).get().await().getString("coupleId")
                    ?: return@withTimeout Result.success()
                val today = dayKeyOf(Instant.now(), ZoneId.systemDefault())
                val record = db.collection("couples").document(coupleId)
                    .collection("days").document("${today}_$uid").get().await()
                val rated = (record.get("snapshots") as? List<*>)?.isNotEmpty() == true
                if (!rated) showNotification()
                Result.success()
            }
        } catch (e: Exception) {
            Result.success()
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification() {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!allowed) return
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("5 Star")
            .setContentText("Deine Bewertung für heute fehlt noch.")
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }
}
