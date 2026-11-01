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
)

val AUTO_CONFIRM_AFTER: Duration = Duration.ofHours(48)

/** Ohne Bestätigung gilt eine offene Aufgabe 48 Stunden nach Erstellung als bestätigt. */
fun Task.withAutoConfirmation(now: Instant): Task =
    if (status == TaskStatus.OPEN && !now.isBefore(createdAt.plus(AUTO_CONFIRM_AFTER))) {
        copy(status = TaskStatus.AUTO_CONFIRMED)
    } else {
        this
    }

/** Der Partner bestätigt eine offene Aufgabe. Bereits bestätigte bleiben unverändert. */
fun Task.confirmedByPartner(): Task =
    if (status == TaskStatus.OPEN) copy(status = TaskStatus.CONFIRMED) else this
