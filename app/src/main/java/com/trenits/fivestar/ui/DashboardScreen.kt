package com.trenits.fivestar.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trenits.fivestar.R
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.CategoryKind
import com.trenits.fivestar.domain.CategorySnapshot
import com.trenits.fivestar.domain.DEFAULT_ALCOHOL_LIMIT
import com.trenits.fivestar.domain.DEFAULT_WATER_GOAL
import com.trenits.fivestar.domain.DEMI
import com.trenits.fivestar.domain.DINNER_HOUR
import com.trenits.fivestar.domain.DINNER_MINUTE
import com.trenits.fivestar.domain.DayRecord
import com.trenits.fivestar.domain.GLASSES
import com.trenits.fivestar.domain.LITERS
import com.trenits.fivestar.domain.MAX_STARS
import com.trenits.fivestar.domain.PINT
import com.trenits.fivestar.domain.ROTATION_STARS
import com.trenits.fivestar.domain.ROTATION_TASK_INDEX
import com.trenits.fivestar.domain.RotationTask
import com.trenits.fivestar.domain.Task
import com.trenits.fivestar.domain.TaskStatus
import com.trenits.fivestar.domain.TaskType
import com.trenits.fivestar.domain.WeightedCategory
import com.trenits.fivestar.domain.alcoholLiters
import com.trenits.fivestar.domain.availableRotationTasks
import com.trenits.fivestar.domain.counterStars
import com.trenits.fivestar.domain.rotationCountsInWeek
import com.trenits.fivestar.domain.suggestedRotationTask
import com.trenits.fivestar.domain.waterLiters
import com.trenits.fivestar.domain.weightSharePercent
import com.trenits.fivestar.ui.theme.Amber
import java.time.LocalDate

@Composable
fun DashboardScreen(vm: AppViewModel) {
    val targeted by vm.targeted.collectAsState()
    val sortedCategories = remember(targeted) { targeted.sortedByDescending { it.weight } }
    val weightedList = remember(sortedCategories) {
        sortedCategories.map { WeightedCategory(it.id, it.weight) }
    }
    val myName by vm.myName.collectAsState()
    val partnerName by vm.partnerName.collectAsState()
    val myToday by vm.myToday.collectAsState()
    val partnerToday by vm.partnerToday.collectAsState()
    val partnerUid by vm.partnerUid.collectAsState()
    val members by vm.members.collectAsState()
    val days by vm.days.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val catalog by vm.catalog.collectAsState()
    val weekRecordsBeforeToday by vm.isoWeekRecordsBeforeToday.collectAsState()

    val stored = myToday?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
    // Offene Strafen & Belohnungen bleiben sichtbar, bis der Partner die Einlösung bestätigt hat.
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
        if (sortedCategories.isEmpty()) {
            item { HintCard(stringResource(R.string.categories_empty)) }
        }

        items(sortedCategories, key = { it.id }) { category ->
            val sharePercent = weightSharePercent(category.weight, weightedList)
            val values = myToday?.counters?.get(category.id).orEmpty()
            when (category.kind) {
                CategoryKind.STARS -> {
                    CategoryCard(
                        category = category,
                        sharePercent = sharePercent,
                        value = stored[category.id] ?: 0,
                        onChange = { vm.setStars(category.id, it) },
                    )
                }
                CategoryKind.WATER, CategoryKind.ALCOHOL -> {
                    CounterCard(
                        category = category,
                        sharePercent = sharePercent,
                        values = values,
                        onChange = { key, value -> vm.setCounter(category.id, key, value) },
                    )
                }
                CategoryKind.ROTATION -> {
                    RotationCategoryCard(
                        category = category,
                        sharePercent = sharePercent,
                        values = values,
                        weekRecordsBeforeToday = weekRecordsBeforeToday,
                        onSelectAndRate = { rotTask, stars ->
                            vm.setRotation(category.id, rotTask, stars)
                        },
                    )
                }
                CategoryKind.DINNER_TIME -> {
                    val partnerDinnerValues = partnerToday?.counters?.get(category.id)
                        ?: partnerToday?.counters?.values?.firstOrNull { it.containsKey(DINNER_HOUR) }
                    val effectiveValues = if (values.containsKey(DINNER_HOUR)) {
                        values
                    } else {
                        partnerDinnerValues.orEmpty()
                    }
                    // Andi gibt die Uhrzeit ein (oder solange kein Partner namens "Andi" exklusiv gesetzt ist)
                    val partnerIsAndi = partnerName?.trim()?.equals("Andi", ignoreCase = true) == true
                    val iAmRosie = myName?.trim()?.equals("Rosie", ignoreCase = true) == true
                    val canEditTime = !(iAmRosie && partnerIsAndi)
                    DinnerTimeCard(
                        category = category,
                        sharePercent = sharePercent,
                        values = effectiveValues,
                        canEditTime = canEditTime,
                        onTimeSelected = { h, m -> vm.setDinnerTime(category.id, h, m) },
                    )
                }
            }
        }

        item { SectionTitle(stringResource(R.string.relationship_title)) }
        item { RelationshipTodaySection(vm) }

        item { SectionTitle(stringResource(R.string.section_partner)) }
        item {
            PartnerCard(
                score = partnerToday?.scorePercent,
                snapshots = partnerToday?.snapshots,
                counters = partnerToday?.counters,
                categories = sortedCategories,
                offline = days.fromCache,
            )
        }

        item { SectionTitle(stringResource(R.string.section_open_tasks)) }
        if (openTasks.isEmpty()) {
            item { HintCard(stringResource(R.string.no_open_tasks)) }
        }
        items(openTasks, key = { it.id }) { task ->
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
private fun CategoryWeightSubtitle(weight: Int, sharePercent: Int) {
    Text(
        stringResource(R.string.weight_with_share, weight, priorityLabel(weight), sharePercent),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun CategoryCard(
    category: Category,
    sharePercent: Int,
    value: Int,
    onChange: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CategoryImage(category = category, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(localizedCategoryName(category), style = MaterialTheme.typography.titleMedium)
                CategoryWeightSubtitle(category.weight, sharePercent)
            }
            StarRating(value, onChange)
        }
    }
}

@Composable
private fun CounterCard(
    category: Category,
    sharePercent: Int,
    values: Map<String, Double>,
    onChange: (String, Double) -> Unit,
) {
    val stars = counterStars(category, values)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryImage(category = category, size = 42.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(localizedCategoryName(category), style = MaterialTheme.typography.titleMedium)
                    CategoryWeightSubtitle(category.weight, sharePercent)
                }
            }
            Spacer(Modifier.height(8.dp))
            if (category.kind == CategoryKind.ALCOHOL) {
                val limit = if (category.goal > 0.0) category.goal else DEFAULT_ALCOHOL_LIMIT
                Text(
                    stringResource(R.string.counter_alcohol_goal_hint, limit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                CounterRow(stringResource(R.string.counter_pint), values[PINT] ?: 0.0, step = 1.0) {
                    onChange(PINT, it)
                }
                CounterRow(stringResource(R.string.counter_demi), values[DEMI] ?: 0.0, step = 1.0) {
                    onChange(DEMI, it)
                }
                LitersField(
                    categoryId = category.id,
                    value = values[LITERS] ?: 0.0,
                    onChange = { onChange(LITERS, it) },
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.counter_total_liters, alcoholLiters(values)),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
            } else {
                val goal = if (category.goal > 0.0) category.goal else DEFAULT_WATER_GOAL
                Text(
                    stringResource(R.string.counter_water_goal_hint, goal),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                CounterRow(stringResource(R.string.counter_glasses), values[GLASSES] ?: 0.0, step = 1.0) {
                    onChange(GLASSES, it)
                }
                LitersField(
                    categoryId = category.id,
                    value = values[LITERS] ?: 0.0,
                    onChange = { onChange(LITERS, it) },
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.counter_total_liters, waterLiters(values)),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(
                    R.string.counter_rating,
                    "★".repeat(stars) + "☆".repeat(MAX_STARS - stars),
                    stars,
                ),
                color = Amber,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RotationCategoryCard(
    category: Category,
    sharePercent: Int,
    values: Map<String, Double>,
    weekRecordsBeforeToday: List<DayRecord>,
    onSelectAndRate: (RotationTask, Int) -> Unit,
) {
    val available = remember(category.id, weekRecordsBeforeToday) {
        availableRotationTasks(category.id, weekRecordsBeforeToday)
    }
    val countsInWeek = remember(category.id, weekRecordsBeforeToday) {
        rotationCountsInWeek(category.id, weekRecordsBeforeToday)
    }
    val suggested = remember(available) {
        suggestedRotationTask(LocalDate.now().dayOfWeek, available)
    }
    val selectedTask = values[ROTATION_TASK_INDEX]?.toInt()?.let { RotationTask.fromIndex(it) } ?: suggested
    val stars = (values[ROTATION_STARS]?.toInt() ?: 0).coerceIn(0, MAX_STARS)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryImage(category = category, size = 42.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(localizedCategoryName(category), style = MaterialTheme.typography.titleMedium)
                    CategoryWeightSubtitle(category.weight, sharePercent)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.rotation_today_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RotationTask.entries.forEach { task ->
                    val doneCount = countsInWeek[task] ?: 0
                    val remaining = (task.weeklyTarget - doneCount).coerceAtLeast(0)
                    val isAvailable = task in available
                    FilterChip(
                        selected = selectedTask == task,
                        onClick = { onSelectAndRate(task, stars) },
                        enabled = isAvailable || selectedTask == task,
                        label = {
                            val statusText = if (remaining > 0) {
                                stringResource(R.string.rotation_remaining_week, remaining)
                            } else {
                                stringResource(R.string.rotation_done_week)
                            }
                            Text("${rotationTaskLabel(task)} · $statusText")
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    rotationTaskLabel(selectedTask),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                StarRating(stars) { newStars ->
                    onSelectAndRate(selectedTask, newStars)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DinnerTimeCard(
    category: Category,
    sharePercent: Int,
    values: Map<String, Double>,
    canEditTime: Boolean,
    onTimeSelected: (Int, Int) -> Unit,
) {
    val context = LocalContext.current
    val hour = values[DINNER_HOUR]?.toInt() ?: -1
    val minute = values[DINNER_MINUTE]?.toInt() ?: 0
    val stars = counterStars(category, values)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryImage(category = category, size = 42.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(localizedCategoryName(category), style = MaterialTheme.typography.titleMedium)
                    CategoryWeightSubtitle(category.weight, sharePercent)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.dinner_rules_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (hour >= 0) {
                    stringResource(R.string.dinner_time_current, "%02d:%02d".format(hour, minute))
                } else {
                    stringResource(R.string.dinner_time_not_set)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                stringResource(R.string.dinner_tracked_by_andi),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            if (canEditTime) {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onTimeSelected(19, 30) }) {
                        Text(stringResource(R.string.dinner_quick_before_20))
                    }
                    OutlinedButton(onClick = { onTimeSelected(20, 30) }) {
                        Text(stringResource(R.string.dinner_quick_before_21))
                    }
                    OutlinedButton(onClick = { onTimeSelected(21, 15) }) {
                        Text(stringResource(R.string.dinner_quick_after_21))
                    }
                    Button(
                        onClick = {
                            val initH = if (hour >= 0) hour else 19
                            val initM = if (hour >= 0) minute else 30
                            TimePickerDialog(
                                context,
                                { _, h, m -> onTimeSelected(h, m) },
                                initH,
                                initM,
                                true,
                            ).show()
                        },
                    ) {
                        Text(stringResource(R.string.dinner_pick_time))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(
                    R.string.counter_rating,
                    "★".repeat(stars) + "☆".repeat(MAX_STARS - stars),
                    stars,
                ),
                color = Amber,
                fontWeight = FontWeight.Bold,
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
private fun LitersField(categoryId: String, value: Double, onChange: (Double) -> Unit) {
    var text by remember(categoryId) { mutableStateOf(if (value == 0.0) "" else value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input
            if (input.isBlank()) {
                onChange(0.0)
            } else {
                input.replace(',', '.').toDoubleOrNull()?.let(onChange)
            }
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
                        Text(localizedCategoryName(cat), modifier = Modifier.weight(1f))
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
                    CounterSummary(localizedCategoryName(category), values)
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

    val partnerCategories = allCategories.items
        .filter { it.appliesTo(partnerUid ?: vm.uid) }
        .sortedByDescending { it.weight }
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
