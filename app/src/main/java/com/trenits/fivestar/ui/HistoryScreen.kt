package com.trenits.fivestar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trenits.fivestar.R
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.DayRecord
import com.trenits.fivestar.domain.Period
import com.trenits.fivestar.domain.Task
import java.time.LocalDate

@Composable
fun HistoryScreen(vm: AppViewModel) {
    val days by vm.days.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val targeted by vm.targeted.collectAsState()
    val partnerUid by vm.partnerUid.collectAsState()
    val myName by vm.myName.collectAsState()
    val partnerName by vm.partnerName.collectAsState()
    val myLabel = myName ?: stringResource(R.string.who_you)
    val partnerLabel = partnerName ?: stringResource(R.string.who_partner)
    var period by rememberSaveable { mutableStateOf(Period.SEVEN_DAYS) }
    val today = remember { LocalDate.now() }
    val start = period.startDay(today)

    val dayKeys = (days.items.map { it.dayKey } + tasks.items.map { it.dayKey })
        .filter { it.isNotEmpty() && (start == null || !LocalDate.parse(it).isBefore(start)) }
        .toSortedSet(reverseOrder())

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(40.dp))
            Row(Modifier.fillMaxWidth()) {
                Period.entries.forEach { p ->
                    FilterChip(
                        selected = period == p,
                        onClick = { period = p },
                        label = { Text(stringResource(periodLabelRes(p))) },
                    )
                }
            }
        }
        if (dayKeys.isEmpty()) {
            item { Text(stringResource(R.string.history_empty)) }
        }
        items(dayKeys.toList(), key = { it }) { key ->
            DayCard(
                dayKey = key,
                myScore = days.items.firstOrNull { it.dayKey == key && it.ownerUid == vm.uid },
                partnerScore = partnerUid?.let { p -> days.items.firstOrNull { it.dayKey == key && it.ownerUid == p } },
                dayTasks = tasks.items.filter { it.dayKey == key },
                myUid = vm.uid,
                partnerUid = partnerUid,
                myLabel = myLabel,
                partnerLabel = partnerLabel,
                categories = targeted,
                showPartner = partnerUid != null,
            )
        }
    }
}

@Composable
private fun DayCard(
    dayKey: String,
    myScore: DayRecord?,
    partnerScore: DayRecord?,
    dayTasks: List<Task>,
    myUid: String,
    partnerUid: String?,
    myLabel: String,
    partnerLabel: String,
    categories: List<Category>,
    showPartner: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }

    val mySnapshots = myScore?.snapshots?.filter { it.weight > 0 } ?: emptyList()
    val partnerSnapshots = partnerScore?.snapshots?.filter { it.weight > 0 } ?: emptyList()
    val hasAnyData = mySnapshots.isNotEmpty() || partnerSnapshots.isNotEmpty() || myScore?.counters?.isNotEmpty() == true || partnerScore?.counters?.isNotEmpty() == true || dayTasks.isNotEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    formatDay(dayKey),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(
                        if (expanded) R.string.archive_tap_details else R.string.archive_tap_details,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (!expanded) {
                // Kompakte Übersicht
                Text(
                    stringResource(R.string.history_you, myLabel, formatPercent(myScore?.scorePercent)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (showPartner) {
                    Text(
                        stringResource(R.string.history_partner, partnerLabel, formatPercent(partnerScore?.scorePercent)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (!hasAnyData) {
                    Text(
                        stringResource(R.string.archive_no_entry),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                // Ausgeklappt: alle Details
                Text(
                    stringResource(R.string.history_you, myLabel, formatPercent(myScore?.scorePercent)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (showPartner) {
                    Text(
                        stringResource(R.string.history_partner, partnerLabel, formatPercent(partnerScore?.scorePercent)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (!hasAnyData) {
                    Text(
                        stringResource(R.string.archive_no_entry),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                    mySnapshots.takeIf { it.isNotEmpty() }?.let { list ->
                        val itemsText = list.map { s -> "${localizedCategorySnapshotName(s.name)} ${s.stars}★" }.joinToString(" · ")
                        Text(
                            stringResource(
                                R.string.history_your_categories,
                                itemsText,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    partnerSnapshots.takeIf { it.isNotEmpty() }?.let { list ->
                        val itemsText = list.map { s -> "${localizedCategorySnapshotName(s.name)} ${s.stars}★" }.joinToString(" · ")
                        Text(
                            stringResource(
                                R.string.history_partner_categories,
                                itemsText,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    myScore?.counters?.forEach { (categoryId, values) ->
                        val category = categories.firstOrNull { it.id == categoryId && it.appliesTo(myUid) }
                        if (category != null) {
                            CounterSummary(localizedCategoryName(category), values)
                        }
                    }
                    partnerScore?.counters?.forEach { (categoryId, values) ->
                        val partner = partnerUid ?: return@forEach
                        val category = categories.firstOrNull { it.id == categoryId && it.appliesTo(partner) }
                        if (category != null) {
                            CounterSummary(localizedCategoryName(category), values)
                        }
                    }
                    dayTasks.forEach { task ->
                        Spacer(Modifier.height(8.dp))
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
                        )
                    }
                }
            }
        }
    }
}
