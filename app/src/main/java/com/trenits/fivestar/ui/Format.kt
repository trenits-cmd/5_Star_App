package com.trenits.fivestar.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trenits.fivestar.R
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.CategoryIcon
import com.trenits.fivestar.domain.CategoryKind
import com.trenits.fivestar.domain.MealLevel
import com.trenits.fivestar.domain.Period
import com.trenits.fivestar.domain.RelationshipAspect
import com.trenits.fivestar.domain.RotationTask
import com.trenits.fivestar.domain.TaskStatus
import com.trenits.fivestar.domain.TaskType
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

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
    /** Return the task type label. Since penalties are removed, always shows reward. */
    fun typeLabel(type: TaskType): String =
        stringResource(R.string.task_reward)
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
fun mealLevelLabel(level: MealLevel): String = stringResource(
    when (level) {
        MealLevel.NONE -> R.string.meal_level_none
        MealLevel.LITTLE -> R.string.meal_level_little
        MealLevel.MEDIUM -> R.string.meal_level_medium
        MealLevel.HEALTHY -> R.string.meal_level_healthy
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
 * Übersetzt Standard-Kategorienamen in die englische Oberfläche (ältere deutsche Namen werden erkannt).
 */
@Composable
fun localizedCategoryName(category: Category): String {
    val trimmed = category.name.trim()
    return when {
        trimmed in setOf("Wasser trinken", "Drink water", "Boire de l'eau") ||
            category.kind == CategoryKind.WATER ->
            stringResource(R.string.cat_preset_water)
        trimmed in setOf("Alkohol-Grenze", "Alkohol Grenze", "Alcohol limit", "Limite d'alcool") ||
            category.kind == CategoryKind.ALCOHOL ->
            stringResource(R.string.cat_preset_alcohol)
        trimmed.startsWith("Wechsel-Aufgaben") ||
            trimmed.startsWith("Weitere Aufgaben") ||
            trimmed.startsWith("Alternating tasks") ||
            trimmed.startsWith("Tâches alternées") ||
            trimmed.startsWith("Autres tâches") ||
        trimmed in setOf("Abendessen vor 20h", "Dinner before 20h", "Dîner avant 20h") ||
            category.kind == CategoryKind.DINNER_TIME ->
            stringResource(R.string.cat_preset_dinner)
        trimmed in setOf("Essen", "Regelmäßig essen", "Meals", "Eat regularly", "Repas", "Manger régulièrement") ||
            category.kind == CategoryKind.MEALS ->
            stringResource(R.string.cat_preset_food)
        trimmed in setOf("Sport", "Fitness") ||
            category.kind == CategoryKind.SPORT ->
            stringResource(R.string.cat_preset_sport)
        trimmed in setOf("Lesen", "Reading", "Lecture") ||
            category.kind == CategoryKind.READING ->
            stringResource(R.string.cat_preset_reading)
        else -> category.name
    }
}

@Composable
fun localizedCategorySnapshotName(rawName: String): String {
    val trimmed = rawName.trim()
    return when {
        trimmed in setOf("Wasser trinken", "Drink water", "Boire de l'eau") ->
            stringResource(R.string.cat_preset_water)
        trimmed in setOf("Alkohol-Grenze", "Alkohol Grenze", "Alcohol limit", "Limite d'alcool") ->
            stringResource(R.string.cat_preset_alcohol)
        trimmed.startsWith("Wechsel-Aufgaben") ||
            trimmed.startsWith("Weitere Aufgaben") ||
            trimmed.startsWith("Alternating tasks") ||
            trimmed.startsWith("Tâches alternées") ||
            trimmed.startsWith("Autres tâches") ->
            stringResource(R.string.cat_preset_rotation)
        trimmed in setOf("Abendessen vor 20h", "Dinner before 20h", "Dîner avant 20h") ->
            stringResource(R.string.cat_preset_dinner)
        trimmed in setOf("Essen", "Regelmäßig essen", "Meals", "Eat regularly", "Repas", "Manger régulièrement") ->
            stringResource(R.string.cat_preset_food)
        trimmed in setOf("Sport", "Fitness") ->
            stringResource(R.string.cat_preset_sport)
        trimmed in setOf("Lesen", "Reading", "Lecture") ->
            stringResource(R.string.cat_preset_reading)
        else -> rawName
    }
}

fun translateCatalogTitle(context: Context, rawTitle: String): String {
    val trimmed = rawTitle.trim()
    val lower = trimmed.lowercase()
    return when {
        lower.startsWith("vinted budget") || lower.startsWith("budget vinted") ->
            context.getString(R.string.seed_reward_1)
        lower.startsWith("blumen:") || lower.startsWith("flowers:") || lower.startsWith("fleurs") ->
            context.getString(R.string.seed_reward_2)
        lower == "massage für 30 minuten" || lower == "massage (20 min)" ||
            lower == "massage for 30 minutes" || lower.startsWith("massage de 30") ->
            context.getString(R.string.seed_reward_3)
        lower.startsWith("essen gehen") || lower.startsWith("eat out:") || lower.startsWith("sortie au restaurant") ->
            context.getString(R.string.seed_reward_4)
        lower.startsWith("gemeinsames kochen") || lower.startsWith("cooking of choice:") || lower.startsWith("cuisine au choix") ->
            context.getString(R.string.seed_reward_5)
        lower.startsWith("kuscheln") || lower.startsWith("cuddling for 30") || lower.startsWith("câlins") ->
            context.getString(R.string.seed_reward_andi_1)
        lower.startsWith("lieblingsgericht") || lower == "lieblingsessen" ||
            lower.startsWith("favourite dish:") || lower.startsWith("plat préféré") ->
            context.getString(R.string.seed_reward_andi_7)
        else -> trimmed
    }
}

@Composable
fun localizedCatalogTitle(rawTitle: String): String {
    val context = LocalContext.current
    return translateCatalogTitle(context, rawTitle)
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

/**
 * Zeigt das hochgeladene Foto des Nutzers (oder einen runden Initialen-Avatar) als Icon an.
 */
@Composable
fun UserAvatar(
    photoBase64: String?,
    name: String?,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(photoBase64) {
        if (photoBase64.isNullOrBlank()) {
            null
        } else {
            runCatching {
                val bytes = Base64.decode(photoBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape),
        )
    } else {
        val initial = name?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "★"
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initial,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Liest ein Bild aus einer Galerie-URI, schneidet es quadratisch zu, skaliert es auf 128×128
 * und kodiert es als Base64-String für Firestore.
 */
fun encodeAvatarUri(context: Context, uri: Uri): String? = runCatching {
    val raw = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        ?: return null
    val edge = min(raw.width, raw.height)
    val x = (raw.width - edge) / 2
    val y = (raw.height - edge) / 2
    val cropped = Bitmap.createBitmap(raw, x, y, edge, edge)
    val scaled = Bitmap.createScaledBitmap(cropped, 128, 128, true)
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 75, out)
    Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}.getOrNull()

/**
 * Öffnet die E-Mail-App mit vorausgefüllter Einladung und Code für den Partner.
 */
fun launchInviteEmail(context: Context, email: String, code: String) {
    val subject = context.getString(R.string.invite_email_subject)
    val body = context.getString(R.string.invite_email_body, code)
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:${Uri.encode(email.trim())}?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }.onFailure {
        val fallback = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email.trim()))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(Intent.createChooser(fallback, subject)) }
    }
}
