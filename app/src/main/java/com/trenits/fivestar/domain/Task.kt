package com.trenits.fivestar.domain

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

enum class TaskType { REWARD, PENALTY }

enum class TaskStatus { OPEN, CONFIRMED, AUTO_CONFIRMED }

data class Task(
    val id: String,
    val ownerUid: String,
    val type: TaskType,
    val title: String,
    val dayKey: String,
    val status: TaskStatus,
    val createdAt: Instant,
    val targets: List<String> = listOf(ownerUid),
    val redeemedDayKey: String? = null,
    /** Zeitpunkt, an dem der Besitzer die Strafe/Belohnung gewählt hat. Frist (3 h / 48 h) läuft ab hier. */
    val selectedAt: Instant? = null,
) {
    /** Gilt die Aufgabe für diese Person? */
    fun appliesTo(uid: String): Boolean = uid in targets

    /** Ist die Aufgabe bestätigt/eingelöst und damit im Archiv? */
    val isArchived: Boolean
        get() = status == TaskStatus.CONFIRMED || status == TaskStatus.AUTO_CONFIRMED
}

val CONFIRM_REMINDER_AFTER: Duration = Duration.ofHours(3)
val AUTO_CONFIRM_AFTER: Duration = Duration.ofHours(48)

/**
 * Prüft, ob eine offene Aufgabe mit gewählter Aktion seit mindestens 3 Stunden
 * auf Bestätigung durch den Partner wartet (für die Push-Erinnerung „Rosie wartet auf deine Bestätigung!“).
 */
fun Task.needsPartnerConfirmationReminder(now: Instant): Boolean =
    status == TaskStatus.OPEN &&
        title.isNotBlank() &&
        !now.isBefore(timerStart.plus(CONFIRM_REMINDER_AFTER)) &&
        now.isBefore(timerStart.plus(AUTO_CONFIRM_AFTER))

/** Start der Fristen: Auswahl der Aktion, ältere Einträge ohne Auswahl ab Erstellung. */
val Task.timerStart: Instant
    get() = selectedAt ?: createdAt

/** Ohne Bestätigung gilt eine offene Aufgabe 48 Stunden nach Auswahl (bzw. Erstellung) als erfüllt. */
fun Task.withAutoConfirmation(now: Instant): Task =
    if (status == TaskStatus.OPEN && !now.isBefore(timerStart.plus(AUTO_CONFIRM_AFTER))) {
        copy(status = TaskStatus.AUTO_CONFIRMED)
    } else {
        this
    }

/** Der Partner bestätigt eine offene Aufgabe und hält optional den Tag der Einlösung fest. */
fun Task.confirmedByPartner(redeemedOn: String? = null): Task =
    if (status == TaskStatus.OPEN) {
        copy(status = TaskStatus.CONFIRMED, redeemedDayKey = redeemedOn ?: redeemedDayKey)
    } else {
        this
    }

/**
 * Start-Tagesschlüssel für den aktuellen Strafen-Pool-Durchlauf:
 * Der Strafen-Pool wird jeden Sonntagabend (ab 20:00 Uhr, nach der Wochenbilanz) zurückgesetzt.
 */
fun penaltyCycleStartDayKey(now: ZonedDateTime): String {
    val date = now.toLocalDate()
    return if (date.dayOfWeek == DayOfWeek.SUNDAY && now.hour >= 20) {
        date.toString()
    } else {
        date.with(DayOfWeek.MONDAY).toString()
    }
}

/**
 * Berechnet die aktuell im Dropdown wählbaren Strafen/Belohnungen aus [pool].
 * Jede bereits in [usedTitlesChronological] gewählte Option verschwindet aus der Auswahl,
 * bis alle Optionen aus [pool] einmal verbraucht wurden – danach stehen wieder alle zur Verfügung.
 */
fun availablePoolOptions(
    pool: List<String>,
    usedTitlesChronological: List<String>,
): List<String> {
    val distinctPool = pool.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    if (distinctPool.isEmpty()) return emptyList()
    val usedInCycle = mutableSetOf<String>()
    for (rawTitle in usedTitlesChronological) {
        val clean = rawTitle.trim()
        if (clean in distinctPool) {
            usedInCycle.add(clean)
            if (usedInCycle.size >= distinctPool.size) {
                usedInCycle.clear()
            }
        }
    }
    return distinctPool.filter { it !in usedInCycle }
}
