package com.trenits.fivestar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trenits.fivestar.R
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.DayOutcome
import com.trenits.fivestar.domain.DayRecord
import com.trenits.fivestar.domain.MAX_STARS
import com.trenits.fivestar.domain.RelationshipAspect
import com.trenits.fivestar.domain.STREAK_DAYS_FOR_REWARD
import com.trenits.fivestar.domain.Task
import com.trenits.fivestar.domain.rewardDaysForTier
import com.trenits.fivestar.domain.TaskStatus
import com.trenits.fivestar.domain.TaskType
import com.trenits.fivestar.domain.relationshipScorePercent
import com.trenits.fivestar.domain.relationshipStarsOf
import com.trenits.fivestar.ui.theme.Amber
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.week_streak, streakInCurrentCycle, STREAK_DAYS_FOR_REWARD),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        softWrap = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    val progress = (streakInCurrentCycle.toFloat() / STREAK_DAYS_FOR_REWARD.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (weekRewardUnlocked) {
                            stringResource(R.string.week_reward_unlocked)
                        } else {
                            stringResource(R.string.week_no_reward, remainingForReward)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        softWrap = true,
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
                            text = localizedCatalogTitle(reward.title),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            softWrap = true,
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
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "archiveChevron",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DayStatusChip(hasAnyData = hasAnyData, outcome = outcome, percent = myRecord?.scorePercent)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = stringResource(R.string.archive_tap_details),
                        modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (hasAnyData) {
                Spacer(Modifier.height(12.dp))
                ArchiveScoreRow(
                    label = myLabel,
                    score = myRecord?.scorePercent,
                    accent = MaterialTheme.colorScheme.primary,
                )
                if (showPartner) {
                    Spacer(Modifier.height(10.dp))
                    ArchiveScoreRow(
                        label = partnerLabel,
                        score = partnerRecord?.scorePercent,
                        accent = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Spacer(Modifier.height(10.dp))
                ArchiveScoreRow(
                    label = stringResource(R.string.status_relationship_title),
                    score = myRecord?.let { relationshipScorePercent(relationshipStarsOf(it)) }
                        ?: partnerRecord?.let { relationshipScorePercent(relationshipStarsOf(it)) },
                    accent = MaterialTheme.colorScheme.secondary,
                )
            }
            dayTasks.filter { it.type == TaskType.REWARD }.forEach { task ->
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(
                        R.string.archive_day_reward_line,
                        if (task.title.isEmpty()) stringResource(R.string.no_action_chosen) else localizedCatalogTitle(task.title),
                        statusLabel(task.status),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(Modifier.padding(top = 8.dp)) {
                    if (!hasAnyData) {
                        Text(
                            stringResource(R.string.archive_no_entry),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        if (mySnapshots.isNotEmpty()) {
                            ArchiveSectionTitle(stringResource(R.string.archive_section_categories, myLabel))
                            mySnapshots.forEach { s ->
                                ArchiveCategoryRow(localizedCategorySnapshotName(s.name), s.stars)
                            }
                        }
                        if (partnerSnapshots.isNotEmpty()) {
                            ArchiveSectionTitle(stringResource(R.string.archive_section_categories, partnerLabel))
                            partnerSnapshots.forEach { s ->
                                ArchiveCategoryRow(localizedCategorySnapshotName(s.name), s.stars)
                            }
                        }
                        val counterEntries = myRecord?.counters?.mapNotNull { (categoryId, values) ->
                            categories.firstOrNull { it.id == categoryId }?.let { it to values }
                        }.orEmpty()
                        if (counterEntries.isNotEmpty()) {
                            ArchiveSectionTitle(stringResource(R.string.archive_section_details))
                            counterEntries.forEach { (category, values) ->
                                CounterSummary(localizedCategoryName(category), values)
                            }
                        }
                        if (myRecord != null) ArchiveRelationshipDetails(myLabel, myRecord)
                        if (showPartner && partnerRecord != null) ArchiveRelationshipDetails(partnerLabel, partnerRecord)
                        if (dayTasks.isNotEmpty()) {
                            ArchiveSectionTitle(stringResource(R.string.archive_section_tasks))
                            dayTasks.forEach { task ->
                                Spacer(Modifier.height(6.dp))
                                ArchiveTaskRow(
                                    who = if (task.ownerUid == myUid) myLabel else partnerLabel,
                                    typeLabel = typeLabel(task.type) + if (task.tier > 0) " · " + stringResource(R.string.reward_tier_label, task.tier, rewardDaysForTier(task.tier)) else "",
                                    action = if (task.title.isEmpty()) stringResource(R.string.no_action_chosen) else localizedCatalogTitle(task.title),
                                    statusLabel = statusLabel(task.status),
                                    isReward = task.type == TaskType.REWARD,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveRelationshipDetails(label: String, record: DayRecord) {
    val stars = relationshipStarsOf(record)
    val total = relationshipScorePercent(stars)
    Spacer(Modifier.height(8.dp))
    ArchiveSectionTitle(stringResource(R.string.archive_section_relationship, label))
    Text(
        stringResource(R.string.archive_relationship_total, formatPercent(total)),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = statusGradeColor(total ?: 0.0),
    )
    RelationshipAspect.entries.forEach { aspect ->
        ArchiveCategoryRow(relationshipAspectLabel(aspect), stars[aspect] ?: 0)
    }
}

@Composable
private fun DayStatusChip(hasAnyData: Boolean, outcome: DayOutcome?, percent: Double?) {
    val (text, color) = when {
        !hasAnyData -> stringResource(R.string.archive_no_entry) to MaterialTheme.colorScheme.onSurfaceVariant
        outcome == DayOutcome.REWARD -> stringResource(R.string.archive_status_reached) to statusGradeColor(percent ?: 100.0)
        outcome == DayOutcome.MISSED -> stringResource(R.string.archive_status_missed) to statusGradeColor(percent ?: 0.0)
        else -> stringResource(R.string.archive_status_pending) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun ArchiveScoreRow(label: String, score: Double?, accent: Color) {
    val value = (score ?: 0.0).coerceIn(0.0, 100.0)
    val stars = (value / 100.0 * MAX_STARS).roundToInt()
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatPercent(score),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "★".repeat(stars) + "☆".repeat(MAX_STARS - stars),
                    style = MaterialTheme.typography.bodySmall,
                    color = Amber,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (value / 100.0).toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.small),
            color = accent,
        )
    }
}

@Composable
private fun ArchiveSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
    )
}

@Composable
private fun ArchiveCategoryRow(name: String, stars: Int) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            "★".repeat(stars) + "☆".repeat(MAX_STARS - stars),
            style = MaterialTheme.typography.bodyMedium,
            color = Amber,
        )
    }
}

@Composable
private fun ArchiveTaskRow(
    who: String,
    typeLabel: String,
    action: String,
    statusLabel: String,
    isReward: Boolean,
) {
    val accent = if (isReward) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(MaterialTheme.shapes.extraSmall)
                .background(accent),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.padding(vertical = 2.dp)) {
            Text("$who · $typeLabel", style = MaterialTheme.typography.labelMedium, color = accent)
            Text(action, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                statusLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    val accent = if (task.type == TaskType.REWARD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(action, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("$who · ${typeLabel(task.type)}", style = MaterialTheme.typography.labelMedium, color = accent)
            if (task.tier > 0) {
                Text(
                    stringResource(R.string.reward_tier_label, task.tier, rewardDaysForTier(task.tier)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.archive_reward_date, formatDay(task.dayKey)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.archive_redeemed_date, formatDay(task.redeemedDayKey ?: task.dayKey)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                statusLabel(task.status),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
