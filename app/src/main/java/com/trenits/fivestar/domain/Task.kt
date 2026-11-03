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
    /** Zeitpunkt, an dem der Besitzer die Strafe/Belohnung gewählt hat. */
    val selectedAt: Instant? = null,
    /** Verschobene Frist („Not yet“). Null = Standardfrist 48 h ab Auswahl. */
    val deadlineAt: Instant? = null,
    /** Wie oft der Partner „Not yet“ gewählt hat. */
    val postponeCount: Int = 0,
    /** Belohnungsstufe (1–3); 0 = ohne Stufe (alt). */
    val tier: Int = 0,
) {
    /** Gilt die Aufgabe für diese Person? */
    fun appliesTo(uid: String): Boolean = uid in targets

    /** Ist die Aufgabe bestätigt/eingelöst und damit im Archiv? */
    val isArchived: Boolean
        get() = status == TaskStatus.CONFIRMED || status == TaskStatus.AUTO_CONFIRMED
}

/** Erste Erinnerung an den Partner, nachdem die Aktion gewählt wurde. */
val CONFIRM_REMINDER_AFTER: Duration = Duration.ofHours(3)
/** Standardfrist: danach gilt die Strafe als ausgeführt. */
val AUTO_CONFIRM_AFTER: Duration = Duration.ofHours(48)
/** Frühwarnung vor Ablauf der Frist. */
val DEADLINE_WARNING_BEFORE: Duration = Duration.ofHours(6)
/** Verlängerung durch „Not yet“. */
val POSTPONE_BY: Duration = Duration.ofHours(24)

/** Start der Standardfrist: Auswahl der Aktion, ältere Einträge ohne Auswahl ab Erstellung. */
val Task.timerStart: Instant
    get() = selectedAt ?: createdAt

/** Zeitpunkt, ab dem die Strafe/Belohnung automatisch als ausgeführt gilt. */
val Task.autoConfirmAt: Instant
    get() = deadlineAt ?: timerStart.plus(AUTO_CONFIRM_AFTER)

/** Erste Erinnerung an den Partner (nach 3 h, nur solange nicht verschoben). */
fun Task.needsPartnerConfirmationReminder(now: Instant): Boolean =
    status == TaskStatus.OPEN &&
        title.isNotBlank() &&
        postponeCount == 0 &&
        !now.isBefore(timerStart.plus(CONFIRM_REMINDER_AFTER)) &&
        now.isBefore(autoConfirmAt)

/** Frühwarnung: die Frist läuft in weniger als 6 Stunden ab. */
fun Task.needsDeadlineWarning(now: Instant): Boolean =
    status == TaskStatus.OPEN &&
        title.isNotBlank() &&
        !now.isBefore(autoConfirmAt.minus(DEADLINE_WARNING_BEFORE)) &&
        now.isBefore(autoConfirmAt)

/** Ohne Bestätigung oder „Not yet“ gilt eine offene Aufgabe zum Fristende als ausgeführt. */
fun Task.withAutoConfirmation(now: Instant): Task =
    if (status == TaskStatus.OPEN && !now.isBefore(autoConfirmAt)) {
        copy(status = TaskStatus.AUTO_CONFIRMED)
    } else {
        this
    }

/** Partner wählt „Not yet“: die Frist wird um 24 h verschoben, beliebig oft. */
fun Task.postponed(now: Instant): Task {
    val base = if (autoConfirmAt.isAfter(now)) autoConfirmAt else now
    return copy(deadlineAt = base.plus(POSTPONE_BY), postponeCount = postponeCount + 1)
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
