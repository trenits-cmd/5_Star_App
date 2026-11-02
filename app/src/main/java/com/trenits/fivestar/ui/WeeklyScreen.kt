package com.trenits.fivestar.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trenits.fivestar.R
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.DayOutcome
import com.trenits.fivestar.domain.DayRecord
import com.trenits.fivestar.domain.STREAK_DAYS_FOR_REWARD
import com.trenits.fivestar.domain.Task
import com.trenits.fivestar.domain.TaskStatus
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
    val tasks by vm.tasks.collectAsState()
    val days by vm.days.collectAsState()
    val targeted by vm.targeted.collectAsState()
    val partnerUid by vm.partnerUid.collectAsState()
    val myName by vm.myName.collectAsState()
    val partnerName by vm.partnerName.collectAsState()

    val myLabel = myName ?: stringResource(R.string.who_you)
    val partnerLabel = partnerName ?: stringResource(R.string.who_partner)
    val openTasks = tasks.items
        .filter { it.status == TaskStatus.OPEN }
        .sortedWith(compareByDescending<Task> { it.dayKey }.thenByDescending { it.createdAt })

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
            items(weekDays.reversed(), key = { "day_$it" }) { dayKey ->
                ExpandableArchiveDayCard(
                    dayKey = dayKey,
                    outcome = weekOutcomes[dayKey],
                    myRecord = days.items.firstOrNull { it.dayKey == dayKey && it.ownerUid == vm.uid },
                    partnerRecord = partnerUid?.let { p -> days.items.firstOrNull { it.dayKey == dayKey && it.ownerUid == p } },
                    dayTasks = tasks.items.filter { it.dayKey == dayKey },
                    myUid = vm.uid,
                    myLabel = myLabel,
                    partnerLabel = partnerLabel,
                    categories = targeted,
                    showPartner = partnerUid != null,
                )
            }
        }

        if (openTasks.isNotEmpty()) {
            item {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text(
                    stringResource(R.string.section_open_tasks),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(openTasks, key = { "open_${it.id}" }) { task ->
                TaskCard(
                    vm = vm,
                    task = task,
                    partnerUid = partnerUid,
                    myLabel = myLabel,
                    partnerLabel = partnerLabel,
                )
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
private fun ExpandableArchiveDayCard(
    dayKey: String,
    outcome: DayOutcome?,
    myRecord: DayRecord?,
    partnerRecord: DayRecord?,
    dayTasks: List<Task>,
    myUid: String,
    myLabel: String,
    partnerLabel: String,
    categories: List<Category>,
    showPartner: Boolean,
) {
    var expanded by remember(dayKey) { mutableStateOf(false) }
    val mySnapshots = myRecord?.snapshots?.filter { it.weight > 0 }.orEmpty()
    val partnerSnapshots = partnerRecord?.snapshots?.filter { it.weight > 0 }.orEmpty()
    val hasAnyData = mySnapshots.isNotEmpty() ||
        partnerSnapshots.isNotEmpty() ||
        myRecord?.counters?.isNotEmpty() == true ||
        partnerRecord?.counters?.isNotEmpty() == true ||
        dayTasks.isNotEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${weekdayShort(dayKey)} (${formatDay(dayKey)})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                when {
                    !hasAnyData -> Text(
                        stringResource(R.string.archive_no_entry),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    outcome == DayOutcome.REWARD -> Text(
                        "✓ ≥ 80 %",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    outcome == DayOutcome.PENALTY -> Text(
                        "✗ < 80 %",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    else -> Text(
                        stringResource(R.string.archive_tap_details),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (hasAnyData) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.history_you, myLabel, formatPercent(myRecord?.scorePercent)),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (showPartner) {
                    Text(
                        stringResource(R.string.history_partner, partnerLabel, formatPercent(partnerRecord?.scorePercent)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    stringResource(R.string.archive_tap_details),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                if (!hasAnyData) {
                    Text(
                        stringResource(R.string.archive_no_entry),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    if (mySnapshots.isNotEmpty()) {
                        val itemsText = mySnapshots.map { s -> "${localizedCategorySnapshotName(s.name)} ${s.stars}★" }.joinToString(" · ")
                        Text(
                            stringResource(R.string.history_your_categories, itemsText),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (partnerSnapshots.isNotEmpty()) {
                        val itemsText = partnerSnapshots.map { s -> "${localizedCategorySnapshotName(s.name)} ${s.stars}★" }.joinToString(" · ")
                        Text(
                            stringResource(R.string.history_partner_categories, itemsText),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    myRecord?.counters?.forEach { (categoryId, values) ->
                        val category = categories.firstOrNull { it.id == categoryId }
                        if (category != null) {
                            CounterSummary(localizedCategoryName(category), values)
                        }
                    }
                    dayTasks.forEach { task ->
                        Spacer(Modifier.height(6.dp))
                        val who = if (task.ownerUid == myUid) myLabel else partnerLabel
                        val action = if (task.title.isEmpty()) stringResource(R.string.no_action_chosen) else localizedCatalogTitle(task.title)
                        Text(
                            stringResource(
                                R.string.history_task_line,
                                who,
                                typeLabel(task.type),
                                action,
                                statusLabel(task.status),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
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
