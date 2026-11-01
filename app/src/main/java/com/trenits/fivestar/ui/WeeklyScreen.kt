package com.trenits.fivestar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextStyle
import androidx.compose.ui.unit.dp
import com.trenits.fivestar.R
import com.trenits.fivestar.domain.DayOutcome
import com.trenits.fivestar.domain.Task
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeeklyScreen(vm: AppViewModel) {
    val weekDays by vm.weekDays.collectAsState()
    val weekOutcomes by vm.weekOutcomes.collectAsState()
    val weekSuccessfulDays by vm.weekSuccessfulDays.collectAsState()
    val weekRewardUnlocked by vm.weekRewardUnlocked.collectAsState()
    val currentWeeklyReward by vm.currentWeeklyReward.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            stringResource(R.string.week_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        // Wochen-Streak
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    stringResource(R.string.week_streak, weekSuccessfulDays, 7),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(8.dp))
                val progress = (weekSuccessfulDays / 7f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
            }
        }

        // Tage der Woche
        Text(
            stringResource(R.string.week_successful_days),
            style = MaterialTheme.typography.titleMedium,
        )
        if (weekDays.isEmpty()) {
            Text(stringResource(R.string.week_no_data), style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(weekDays, key = { it }) { dayKey ->
                    val outcome = weekOutcomes[dayKey]
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            weekdayShort(dayKey),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = width(40.dp),
                        )
                        when (outcome) {
                            DayOutcome.REWARD -> {
                                Text(
                                    "✓ ${stringResource(R.string.percent_value, 100.0)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            DayOutcome.PENALTY -> {
                                Text(
                                    "✗ ${stringResource(R.string.percent_value, 0.0)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            null -> {
                                Text(
                                    "-",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Wochenbelohnung
        Spacer(Modifier.height(8.dp))
        if (weekRewardUnlocked) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.week_reward_unlocked),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    currentWeeklyReward?.let { task ->
                        Text(
                            task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    } ?: Text(
                        stringResource(R.string.week_no_reward),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        } else if (weekOutcomes.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.week_no_reward),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        "Mindestens 5 von 7 Tagen müssen erfolgreich sein.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun weekdayShort(dayKey: String): String {
    val date = LocalDate.parse(dayKey)
    return date.dayOfWeek
        .getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())
        .take(2)
        .uppercase(Locale.getDefault())
}
