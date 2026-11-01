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
import com.trenits.fivestar.domain.GLASSES
import com.trenits.fivestar.domain.LITERS
import com.trenits.fivestar.domain.PINT
import com.trenits.fivestar.domain.alcoholLiters

/**
 * Zeigt die Zählerwerte (Alkohol/Wasser) einer Person für eine Kategorie an:
 * - Alkohol: Anzahl Pinte/Demi sowie Gesamt in Liter.
 * - Wasser: Anzahl Gläser sowie Liter.
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
    val isWater = values.containsKey(GLASSES) || values.containsKey(LITERS)
    if (isAlcohol) {
        Text(stringResource(R.string.counter_pint) + ": " + (values[PINT] ?: 0.0), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.counter_demi) + ": " + (values[DEMI] ?: 0.0), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.counter_total_liters, alcoholLiters(values)), style = MaterialTheme.typography.bodySmall)
    }
    if (isWater) {
        Text(stringResource(R.string.counter_glasses) + ": " + (values[GLASSES] ?: 0.0), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.counter_liters) + ": " + (values[LITERS] ?: 0.0), style = MaterialTheme.typography.bodySmall)
    }
}
