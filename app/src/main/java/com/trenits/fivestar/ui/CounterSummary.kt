package com.trenits.fivestar.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trenits.fivestar.R
import com.trenits.fivestar.domain.DEMI
import com.trenits.fivestar.domain.DINNER_HOUR
import com.trenits.fivestar.domain.DINNER_MINUTE
import com.trenits.fivestar.domain.GLASSES
import com.trenits.fivestar.domain.LITERS
import com.trenits.fivestar.domain.MEAL_BREAKFAST
import com.trenits.fivestar.domain.MEAL_DINNER
import com.trenits.fivestar.domain.MEAL_LUNCH
import com.trenits.fivestar.domain.MealLevel
import com.trenits.fivestar.domain.PAGES
import com.trenits.fivestar.domain.PINT
import com.trenits.fivestar.domain.ROTATION_STARS
import com.trenits.fivestar.domain.ROTATION_TASK_INDEX
import com.trenits.fivestar.domain.RotationTask
import com.trenits.fivestar.domain.SPORT_MINUTES
import com.trenits.fivestar.domain.alcoholLiters
import com.trenits.fivestar.domain.waterLiters

/**
 * Zeigt die Zählerwerte (Alkohol, Wasser, Wechsel-Aufgabe, Abendessen-Uhrzeit)
 * einer Person für eine Kategorie an.
 */
@Composable
fun CounterSummary(name: String, values: Map<String, Double>) {
    Spacer(Modifier.height(8.dp))
    Text(
        name,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val isAlcohol = values.containsKey(PINT) || values.containsKey(DEMI)
    val isWater = !isAlcohol && (values.containsKey(GLASSES) || values.containsKey(LITERS))
    val isRotation = values.containsKey(ROTATION_TASK_INDEX) || values.containsKey(ROTATION_STARS)
    val isDinner = values.containsKey(DINNER_HOUR)
    val isMeals = values.containsKey(MEAL_BREAKFAST) || values.containsKey(MEAL_LUNCH) || values.containsKey(MEAL_DINNER)
    val isReading = values.containsKey(PAGES)
    val isSport = values.containsKey(SPORT_MINUTES)

    if (isAlcohol) {
        Text(stringResource(R.string.counter_pint) + ": " + (values[PINT]?.toInt() ?: 0), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.counter_demi) + ": " + (values[DEMI]?.toInt() ?: 0), style = MaterialTheme.typography.bodySmall)
        if ((values[LITERS] ?: 0.0) > 0.0) {
            Text(stringResource(R.string.counter_liters) + ": " + (values[LITERS] ?: 0.0), style = MaterialTheme.typography.bodySmall)
        }
        Text(stringResource(R.string.counter_total_liters, alcoholLiters(values)), style = MaterialTheme.typography.bodySmall)
    }
    if (isWater) {
        Text(stringResource(R.string.counter_glasses) + ": " + (values[GLASSES]?.toInt() ?: 0), style = MaterialTheme.typography.bodySmall)
        if ((values[LITERS] ?: 0.0) > 0.0) {
            Text(stringResource(R.string.counter_liters) + ": " + (values[LITERS] ?: 0.0), style = MaterialTheme.typography.bodySmall)
        }
        Text(stringResource(R.string.counter_total_liters, waterLiters(values)), style = MaterialTheme.typography.bodySmall)
    }
    if (isRotation) {
        val task = RotationTask.fromIndex(values[ROTATION_TASK_INDEX]?.toInt() ?: 0)
        val stars = values[ROTATION_STARS]?.toInt() ?: 0
        val answer = if (stars > 0) stringResource(R.string.rotation_yes) else stringResource(R.string.rotation_no)
        Text("${rotationTaskLabel(task)}: $answer", style = MaterialTheme.typography.bodySmall)
    }
    if (isDinner) {
        val hour = values[DINNER_HOUR]?.toInt() ?: -1
        val minute = values[DINNER_MINUTE]?.toInt() ?: 0
        if (hour >= 0) {
            Text(
                stringResource(R.string.dinner_time_current, "%02d:%02d".format(hour, minute)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    if (isMeals) {
        val b = mealLevelLabel(MealLevel.fromPoints(values[MEAL_BREAKFAST]?.toInt() ?: 0))
        val l = mealLevelLabel(MealLevel.fromPoints(values[MEAL_LUNCH]?.toInt() ?: 0))
        val d = mealLevelLabel(MealLevel.fromPoints(values[MEAL_DINNER]?.toInt() ?: 0))
        Text(
            "${stringResource(R.string.meal_breakfast)}: $b · ${stringResource(R.string.meal_lunch)}: $l · ${stringResource(R.string.meal_dinner)}: $d",
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (isReading) {
        val pages = (values[PAGES] ?: 0.0).toInt()
        Text(
            stringResource(R.string.reading_pages_summary, pages),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (isSport) {
        val mins = (values[SPORT_MINUTES] ?: 0.0).toInt()
        Text(
            stringResource(R.string.sport_minutes_summary, mins),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
