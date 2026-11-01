package com.trenits.fivestar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trenits.fivestar.R
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.CategoryKind
import com.trenits.fivestar.domain.CategorySnapshot
import com.trenits.fivestar.domain.DEMI
import com.trenits.fivestar.domain.GLASSES
import com.trenits.fivestar.domain.LITERS
import com.trenits.fivestar.domain.MAX_STARS
import com.trenits.fivestar.domain.PINT
import com.trenits.fivestar.domain.Task
import com.trenits.fivestar.domain.TaskStatus
import com.trenits.fivestar.domain.TaskType
import com.trenits.fivestar.domain.alcoholLiters
import com.trenits.fivestar.domain.counterStars
import com.trenits.fivestar.ui.theme.Amber
import java.time.LocalDate

@Composable
fun DashboardScreen(vm: AppViewModel) {
    val targeted by vm.targeted.collectAsState()
    val categories = targeted.filter { it.kind == CategoryKind.STARS }
    val counterCategories = targeted.filter { it.kind != CategoryKind.STARS }
    val partnerName by vm.partnerName.collectAsState()
    val myToday by vm.myToday.collectAsState()
    val partnerToday by vm.partnerToday.collectAsState()
    val partnerUid by vm.partnerUid.collectAsState()
    val members by vm.members.collectAsState()
    val days by vm.days.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val catalog by vm.catalog.collectAsState()

    val stored = myToday?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
    // Offene Strafen & Belohnungen bleiben hier sichtbar, bis der Partner die Einlösung bestätigt hat.
    val openTasks = tasks.items
        .filter { it.status == TaskStatus.OPEN }
        .sortedWith(compareByDescending<Task> { it.dayKey }.thenByDescending { it.createdAt })

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text(stringResource(R.string.tab_today), style = MaterialTheme.typography.headlineMedium)
                Text(
                    dateHeader(LocalDate.now()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (partnerUid != null) {
                    Text(
                        stringResource(
                            R.string.connected_with,
                            partnerName ?: stringResource(R.string.who_partner),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (!members.fromCache && partnerUid == null) {
            item { WaitingForPartnerCard(vm) }
        }

        item { ScoreHero(myToday?.scorePercent, stringResource(R.string.score_yours)) }

        item { SectionTitle(stringResource(R.string.section_categories)) }
        if (categories.isEmpty() && counterCategories.isEmpty()) {
            item { HintCard(stringResource(R.string.categories_empty)) }
        }
        items(categories, key = { it.id }) { category ->
            CategoryCard(category, stored[category.id] ?: 0) { vm.setStars(category.id, it) }
        }

        items(counterCategories, key = { it.id }) { category ->
            CounterCard(
                category = category,
                values = myToday?.counters?.get(category.id).orEmpty(),
                onChange = { key, value -> vm.setCounter(category.id, key, value) },
            )
        }

        item { SectionTitle(stringResource(R.string.section_partner)) }
        item {
            PartnerCard(
                score = partnerToday?.scorePercent,
                snapshots = partnerToday?.snapshots,
                counters = partnerToday?.counters,
                categories = targeted,
                offline = days.fromCache,
            )
        }

        item { SectionTitle(stringResource(R.string.section_open_tasks)) }
        if (openTasks.isEmpty()) {
            item { HintCard(stringResource(R.string.no_open_tasks)) }
        }
        items(openTasks, key = { it.id }) { task ->
            // catalog wird mitbeobachtet, damit das Dropdown bei Katalogänderungen sofort aktualisiert.
            @Suppress("UNUSED_EXPRESSION")
            catalog
            TaskCard(vm, task, partnerUid)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun ScoreHero(score: Double?, title: String) {
    val progress = ((score ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            )
            Text(
                formatPercent(score),
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
                color = Amber,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.score_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun WaitingForPartnerCard(vm: AppViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.waiting_partner), style = MaterialTheme.typography.titleMedium)
            vm.inviteCode?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(stringResource(R.string.invite_share), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { vm.createInvite() }, enabled = !vm.busy) {
                Text(
                    stringResource(
                        if (vm.inviteCode == null) R.string.invite_create else R.string.invite_new,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(category: Category, value: Int, onChange: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CategoryImage(category = category, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.weight_label, category.weight),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StarRating(value, onChange)
        }
    }
}

@Composable
private fun CounterCard(category: Category, values: Map<String, Double>, onChange: (String, Double) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryImage(category = category, size = 42.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(category.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.weight_label, category.weight),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (category.kind == CategoryKind.ALCOHOL) {
                CounterRow(stringResource(R.string.counter_pint), values[PINT] ?: 0.0, step = 1.0) {
                    onChange(PINT, it)
                }
                CounterRow(stringResource(R.string.counter_demi), values[DEMI] ?: 0.0, step = 1.0) {
                    onChange(DEMI, it)
                }
                Text(
                    stringResource(R.string.counter_total_liters, alcoholLiters(values)),
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                CounterRow(stringResource(R.string.counter_glasses), values[GLASSES] ?: 0.0, step = 1.0) {
                    onChange(GLASSES, it)
                }
                LitersField(values[LITERS] ?: 0.0) { onChange(LITERS, it) }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(
                    R.string.counter_rating,
                    "★".repeat(counterStars(category, values)) + "☆".repeat(MAX_STARS - counterStars(category, values)),
                ),
                color = Amber,
            )
        }
    }
}

@Composable
private fun CounterRow(label: String, value: Double, step: Double, onChange: (Double) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { onChange((value - step).coerceAtLeast(0.0)) }) { Text("−") }
        Text(
            "%d".format(value.toInt()),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Button(onClick = { onChange(value + step) }) { Text("+") }
    }
}

@Composable
private fun LitersField(value: Double, onChange: (Double) -> Unit) {
    var text by remember { mutableStateOf(if (value == 0.0) "" else value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input
            input.replace(',', '.').toDoubleOrNull()?.let(onChange)
        },
        label = { Text(stringResource(R.string.counter_liters)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StarRating(value: Int, onChange: (Int) -> Unit) {
    Row {
        for (n in 1..5) {
            val filled = n <= value
            Text(
                "★",
                fontSize = 26.sp,
                color = if (filled) Amber else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .clickable { onChange(if (value == n) 0 else n) }
                    .padding(horizontal = 2.dp),
            )
        }
    }
}

@Composable
private fun PartnerCard(
    score: Double?,
    snapshots: List<CategorySnapshot>?,
    counters: Map<String, Map<String, Double>>?,
    categories: List<Category>,
    offline: Boolean,
    partnerUid: String? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatPercent(score),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    stringResource(if (snapshots == null) R.string.partner_no_rating else R.string.partner_today),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (offline) {
                Text(
                    stringResource(R.string.offline_stale),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            snapshots?.takeIf { it.isNotEmpty() }?.let { list ->
                Spacer(Modifier.height(8.dp))
                list.forEach { s ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val cat = categories.find { it.id == s.categoryId }
                            ?: Category(s.categoryId, "", s.name, s.weight)
                        CategoryImage(category = cat, size = 28.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(s.name, modifier = Modifier.weight(1f))
                        Text(
                            "★".repeat(s.stars) + "☆".repeat(5 - s.stars),
                            color = Amber,
                        )
                    }
                }
            }
            counters?.forEach { (categoryId, values) ->
                val category = categories.find { it.id == categoryId && (partnerUid == null || it.appliesTo(partnerUid)) }
                if (category != null) {
                    CounterSummary(category.name, values)
                }
            }
        }
    }
}

@Composable
fun PartnerTodayScreen(vm: AppViewModel) {
    val partnerName by vm.partnerName.collectAsState()
    val partnerToday by vm.partnerToday.collectAsState()
    val days by vm.days.collectAsState()
    val partnerUid by vm.partnerUid.collectAsState()
    val allCategories by vm.categories.collectAsState()
    val tasks by vm.tasks.collectAsState()

    val partnerCategories = allCategories.items.filter { it.appliesTo(partnerUid ?: vm.uid) }
    val partnerOpenTasks = tasks.items.filter {
        it.status == TaskStatus.OPEN && (it.ownerUid == partnerUid || (partnerUid != null && it.appliesTo(partnerUid!!)))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(stringResource(R.string.tab_partner), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.connected_with, partnerName ?: stringResource(R.string.who_partner)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        item {
            ScoreHero(
                score = partnerToday?.scorePercent,
                title = partnerName ?: stringResource(R.string.who_partner),
            )
        }
        item {
            PartnerCard(
                score = partnerToday?.scorePercent,
                snapshots = partnerToday?.snapshots,
                counters = partnerToday?.counters,
                categories = partnerCategories,
                offline = days.fromCache,
                partnerUid = partnerUid,
            )
        }
        if (partnerOpenTasks.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.section_open_tasks)) }
            items(partnerOpenTasks, key = { it.id }) { task ->
                TaskCard(vm, task, partnerUid)
            }
        }
    }
}

@Composable
private fun HintCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TaskCard(vm: AppViewModel, task: Task, partnerUid: String?) {
    val isMine = task.ownerUid == vm.uid
    val (availableOptions, totalPoolSize) = vm.availableChoicesFor(task)
    var dropdownExpanded by remember(task.id) { mutableStateOf(false) }
    var selectedOption by remember(task.id, availableOptions) {
        mutableStateOf(availableOptions.firstOrNull().orEmpty())
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            val whoText = stringResource(if (isMine) R.string.who_you else R.string.who_partner)
            Text(
                "$whoText · ${stringResource(R.string.task_title_format, typeLabel(task.type), formatDay(task.dayKey))}",
                style = MaterialTheme.typography.titleSmall,
                color = if (task.type == TaskType.REWARD) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            Text(
                stringResource(
                    if (task.type == TaskType.PENALTY) R.string.archive_penalty_date else R.string.archive_reward_date,
                    formatDay(task.dayKey),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                when {
                    task.targets.size > 1 -> stringResource(R.string.target_both)
                    task.targets.firstOrNull() == vm.uid -> stringResource(R.string.target_me)
                    partnerUid != null && task.targets.firstOrNull() == partnerUid -> stringResource(R.string.target_partner)
                    else -> stringResource(R.string.target_partner)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            when {
                isMine && task.title.isEmpty() -> {
                    Text(
                        stringResource(R.string.task_choose_action),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (availableOptions.isEmpty()) {
                        Text(stringResource(R.string.task_catalog_empty))
                    } else {
                        Text(
                            stringResource(R.string.task_remaining_count, availableOptions.size, totalPoolSize),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { dropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    selectedOption.ifEmpty { stringResource(R.string.task_select_placeholder) },
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                            ) {
                                availableOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            selectedOption = option
                                            dropdownExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (selectedOption.isNotBlank()) {
                                    vm.chooseTaskTitle(task.id, selectedOption)
                                }
                            },
                            enabled = selectedOption.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.task_select_confirm))
                        }
                    }
                }
                isMine -> Text(stringResource(R.string.task_waiting_confirm, task.title))
                task.title.isEmpty() -> Text(stringResource(R.string.task_partner_chooses))
                else -> {
                    Text(
                        stringResource(R.string.task_partner_label, task.title),
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.confirmTask(task.id) }) {
                        Text(stringResource(R.string.task_confirm))
                    }
                }
            }
        }
    }
}
