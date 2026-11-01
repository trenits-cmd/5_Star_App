package com.trenits.fivestar.domain

import java.time.Duration
import java.time.Instant

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
) {
    /** Gilt die Aufgabe für diese Person? */
    fun appliesTo(uid: String): Boolean = uid in targets

    /** Ist die Aufgabe bestätigt/eingelöst und damit im Archiv? */
    val isArchived: Boolean
        get() = status == TaskStatus.CONFIRMED || status == TaskStatus.AUTO_CONFIRMED
}

val AUTO_CONFIRM_AFTER: Duration = Duration.ofHours(48)

/** Ohne Bestätigung gilt eine offene Aufgabe 48 Stunden nach Erstellung als bestätigt. */
fun Task.withAutoConfirmation(now: Instant): Task =
    if (status == TaskStatus.OPEN && !now.isBefore(createdAt.plus(AUTO_CONFIRM_AFTER))) {
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
