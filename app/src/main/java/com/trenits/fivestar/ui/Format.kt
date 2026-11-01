package com.trenits.fivestar.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trenits.fivestar.R
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.CategoryIcon
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
    runCatching { LocalDate.parse(dayKey).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) }
        .getOrDefault(dayKey)

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

fun categoryIconRes(icon: CategoryIcon): Int = when (icon) {
    CategoryIcon.FOOD -> R.drawable.cat_food
    CategoryIcon.WATER -> R.drawable.cat_water
    CategoryIcon.ALCOHOL -> R.drawable.cat_alcohol
    CategoryIcon.FITNESS -> R.drawable.cat_fitness
    CategoryIcon.HOME -> R.drawable.cat_home
    CategoryIcon.MIND -> R.drawable.cat_mind
    CategoryIcon.DETOX -> R.drawable.cat_detox
    CategoryIcon.STAR -> R.drawable.cat_star
}

fun categoryIconLabelRes(icon: CategoryIcon): Int = when (icon) {
    CategoryIcon.FOOD -> R.string.icon_food
    CategoryIcon.WATER -> R.string.icon_water
    CategoryIcon.ALCOHOL -> R.string.icon_alcohol
    CategoryIcon.FITNESS -> R.string.icon_fitness
    CategoryIcon.HOME -> R.string.icon_home
    CategoryIcon.MIND -> R.string.icon_mind
    CategoryIcon.DETOX -> R.string.icon_detox
    CategoryIcon.STAR -> R.string.icon_star
}

@Composable
fun CategoryImage(category: Category, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val icon = category.resolvedIcon()
    Image(
        painter = painterResource(categoryIconRes(icon)),
        contentDescription = category.name,
        modifier = modifier.size(size),
    )
}
