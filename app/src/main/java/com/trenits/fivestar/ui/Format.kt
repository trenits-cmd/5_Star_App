package com.trenits.fivestar.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.trenits.fivestar.R
import com.trenits.fivestar.domain.Period
import com.trenits.fivestar.domain.TaskStatus
import com.trenits.fivestar.domain.TaskType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun formatPercent(value: Double?): String =
    if (value == null) "–" else stringResource(R.string.percent_value, value)

fun formatDay(dayKey: String): String =
    LocalDate.parse(dayKey).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

fun formatTime(instant: Instant): String =
    DateTimeFormatter.ofPattern("dd.MM. HH:mm").withZone(ZoneId.systemDefault()).format(instant)

fun dateHeader(today: LocalDate): String =
    today.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.getDefault()))

@Composable
fun typeLabel(type: TaskType): String =
    stringResource(if (type == TaskType.REWARD) R.string.task_reward else R.string.task_penalty)

@Composable
fun statusLabel(status: TaskStatus): String = stringResource(
    when (status) {
        TaskStatus.OPEN -> R.string.status_open
        TaskStatus.CONFIRMED -> R.string.status_confirmed
        TaskStatus.AUTO_CONFIRMED -> R.string.status_auto_confirmed
    },
)

fun periodLabelRes(period: Period): Int = when (period) {
    Period.SEVEN_DAYS -> R.string.period_7
    Period.THIRTY_DAYS -> R.string.period_30
    Period.THIS_YEAR -> R.string.period_year
    Period.ALL -> R.string.period_all
}
