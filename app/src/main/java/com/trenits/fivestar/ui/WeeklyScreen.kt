package com.trenits.fivestar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trenits.fivestar.R
import com.trenits.fivestar.domain.DayOutcome
import com.trenits.fivestar.domain.STREAK_DAYS_FOR_REWARD
import com.trenits.fivestar.domain.Task
import com.trenits.fivestar.domain.TaskType
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeeklyScreen(vm: AppViewModel) {
    val weekDays by vm.weekDays.collectAsState()
    val weekOutcomes by vm.weekOutcomes.collectAsState()
    val streak by vm.weekSuccessfulDays.collectAsState()
    val weekRewardUnlocked by vm.weekRewardUnlocked.collectAsState()
    val currentWeeklyReward by vm.currentWeeklyReward.collectAsState()
    val archivedTasks by vm.archivedTasks.collectAsState()
    val myName by vm.myName.collectAsState()
    val partnerName by vm.partnerName.collectAsState()

    val myLabel = myName ?: stringResource(R.string.who_you)
    val partnerLabel = partnerName ?: stringResource(R.string.who_partner)

    val streakInCurrentCycle = if (streak > 0 && streak % STREAK_DAYS_FOR_REWARD == 0) {
        STREAK_DAYS_FOR_REWARD
    } else {
        streak % STREAK_DAYS_FOR_REWARD
    }
    val remainingForReward = (STREAK_DAYS_FOR_REWARD - streakInCurrentCycle).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                stringResource(R.string.week_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        // 7-Tage-Streak Karte (Wochenstand)
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        stringResource(R.string.week_streak, streakInCurrentCycle, STREAK_DAYS_FOR_REWARD),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    val progress = (streakInCurrentCycle.toFloat() / STREAK_DAYS_FOR_REWARD.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (weekRewardUnlocked) {
                            stringResource(R.string.week_reward_unlocked)
                        } else {
                            stringResource(R.string.week_no_reward, remainingForReward)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (weekRewardUnlocked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    val reward = currentWeeklyReward
                    if (weekRewardUnlocked && reward != null && reward.title.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            localizedCatalogTitle(reward.title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        // Letzte 7 Tage Übersicht (Tagesstand)
        item {
            Text(
                stringResource(R.string.week_successful_days),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (weekDays.isEmpty()) {
            item {
                Text(stringResource(R.string.week_no_data), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        weekDays.forEach { dayKey ->
                            val outcome = weekOutcomes[dayKey]
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "${weekdayShort(dayKey)} (${formatDay(dayKey)})",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                when (outcome) {
                                    DayOutcome.REWARD -> {
                                        Text(
                                            "✓ ≥ 80 %",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    DayOutcome.PENALTY -> {
                                        Text(
                                            "✗ < 80 %",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                    null -> {
                                        Text(
                                            "–",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Archiv (Eingelöste & vom Partner bestätigte Strafen & Belohnungen)
        item {
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text(
                stringResource(R.string.archive_title),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (archivedTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Text(
                        stringResource(R.string.archive_empty),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            items(archivedTasks, key = { it.id }) { task ->
                ArchivedTaskCard(
                    task = task,
                    myUid = vm.uid,
                    myLabel = myLabel,
                    partnerLabel = partnerLabel,
                )
            }
        }
    }
}

@Composable
private fun ArchivedTaskCard(
    task: Task,
    myUid: String,
    myLabel: String,
    partnerLabel: String,
) {
    val who = if (task.ownerUid == myUid) myLabel else partnerLabel
    val action = if (task.title.isEmpty()) stringResource(R.string.no_action_chosen) else localizedCatalogTitle(task.title)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$who · ${typeLabel(task.type)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (task.type == TaskType.REWARD) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                Text(
                    statusLabel(task.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                action,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(
                    if (task.type == TaskType.PENALTY) R.string.archive_penalty_date else R.string.archive_reward_date,
                    formatDay(task.dayKey),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(
                    R.string.archive_redeemed_date,
                    formatDay(task.redeemedDayKey ?: task.dayKey),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
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
