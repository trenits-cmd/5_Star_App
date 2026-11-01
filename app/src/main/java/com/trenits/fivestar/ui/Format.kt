package com.trenits.fivestar.ui

import com.trenits.fivestar.domain.TaskStatus
import com.trenits.fivestar.domain.TaskType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatPercent(value: Double?): String = value?.let { String.format(Locale.GERMANY, "%.0f %%", it) } ?: "–"

fun formatDay(dayKey: String): String =
    LocalDate.parse(dayKey).format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY))

fun formatTime(instant: Instant): String =
    DateTimeFormatter.ofPattern("dd.MM. HH:mm", Locale.GERMANY).withZone(ZoneId.systemDefault()).format(instant)

fun typeLabel(type: TaskType): String = if (type == TaskType.REWARD) "Belohnung" else "Strafe"

fun statusLabel(status: TaskStatus): String = when (status) {
    TaskStatus.OPEN -> "offen"
    TaskStatus.CONFIRMED -> "bestätigt"
    TaskStatus.AUTO_CONFIRMED -> "automatisch bestätigt"
}
