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
import com.trenits.fivestar.domain.PINT
import com.trenits.fivestar.domain.ROTATION_STARS
import com.trenits.fivestar.domain.ROTATION_TASK_INDEX
import com.trenits.fivestar.domain.RotationTask
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
        Text("${rotationTaskLabel(task)}: ${stars}/5 ★", style = MaterialTheme.typography.bodySmall)
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
}
