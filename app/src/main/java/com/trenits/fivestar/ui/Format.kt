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
import com.trenits.fivestar.domain.CategoryKind
import com.trenits.fivestar.domain.Period
import com.trenits.fivestar.domain.RelationshipAspect
import com.trenits.fivestar.domain.RotationTask
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

@Composable
fun priorityLabel(weight: Int): String = stringResource(
    when (weight.coerceIn(1, 5)) {
        5 -> R.string.priority_5
        4 -> R.string.priority_4
        3 -> R.string.priority_3
        2 -> R.string.priority_2
        else -> R.string.priority_1
    },
)

@Composable
fun rotationTaskLabel(task: RotationTask): String = stringResource(
    when (task) {
        RotationTask.LESSON_PLANNING -> R.string.rotation_task_lesson
        RotationTask.CLEAN_APARTMENT -> R.string.rotation_task_clean
        RotationTask.TAKE_OUT_TRASH -> R.string.rotation_task_trash
        RotationTask.COOKING -> R.string.rotation_task_cook
        RotationTask.WASH_DISHES -> R.string.rotation_task_dishes
    },
)

@Composable
fun relationshipAspectLabel(aspect: RelationshipAspect): String = stringResource(
    when (aspect) {
        RelationshipAspect.MOOD_GENERAL -> R.string.rel_mood_general
        RelationshipAspect.MOOD_RELATIONSHIP -> R.string.rel_mood_relationship
        RelationshipAspect.COMMUNICATION -> R.string.rel_communication
        RelationshipAspect.INTIMACY -> R.string.rel_intimacy
        RelationshipAspect.SEX -> R.string.rel_sex
    },
)

/**
 * Übersetzt Standard-Kategorienamen automatisch in die aktuell gewählte App-Sprache (DE/EN/FR).
 */
@Composable
fun localizedCategoryName(category: Category): String {
    val trimmed = category.name.trim()
    return when {
        trimmed in setOf("Wasser trinken", "Drink water", "Boire de l'eau") ->
            stringResource(R.string.cat_preset_water)
        trimmed in setOf("Alkohol-Grenze", "Alkohol Grenze", "Alcohol limit", "Limite d'alcool") ->
            stringResource(R.string.cat_preset_alcohol)
        trimmed.startsWith("Wechsel-Aufgaben") ||
            trimmed.startsWith("Alternating tasks") ||
            trimmed.startsWith("Tâches alternées") ->
            stringResource(R.string.cat_preset_rotation)
        trimmed in setOf("Abendessen vor 20h", "Dinner before 20h", "Dîner avant 20h") ||
            category.kind == CategoryKind.DINNER_TIME ->
            stringResource(R.string.cat_preset_dinner)
        trimmed in setOf("Regelmäßig essen", "Eat regularly", "Manger régulièrement") ->
            stringResource(R.string.cat_preset_food)
        else -> category.name
    }
}

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
