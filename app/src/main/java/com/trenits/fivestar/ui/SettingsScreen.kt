package com.trenits.fivestar.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trenits.fivestar.BuildConfig
import com.trenits.fivestar.R
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.CategoryKind
import com.trenits.fivestar.domain.TaskType
import com.trenits.fivestar.reminder.ReminderPrefs
import com.trenits.fivestar.reminder.ReminderScheduler
import java.time.LocalTime

private enum class Audience { ME, PARTNER, BOTH }

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val created by vm.created.collectAsState()
    val catalog by vm.myCatalog.collectAsState()
    val days by vm.days.collectAsState()
    val lastSync by vm.lastSync.collectAsState()
    val partnerUid by vm.partnerUid.collectAsState()
    var reminder by remember { mutableStateOf(ReminderPrefs(context).time()) }

    var categoryName by rememberSaveable { mutableStateOf("") }
    var categoryWeight by rememberSaveable { mutableStateOf(1) }
    var categoryKind by remember { mutableStateOf(CategoryKind.STARS) }
    var goalText by rememberSaveable { mutableStateOf("") }
    var audience by remember { mutableStateOf(Audience.ME) }
    var editTargetCategory by remember { mutableStateOf<Category?>(null) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var catalogTitle by rememberSaveable { mutableStateOf("") }
    var catalogType by remember { mutableStateOf(TaskType.REWARD) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(Modifier.height(40.dp))
            Text(stringResource(R.string.settings_display_name), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.settings_display_name_hint))
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = { vm.saveDisplayName(displayName) },
                enabled = displayName.isNotBlank(),
            ) { Text(stringResource(R.string.settings_save)) }
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

        item { Text(stringResource(R.string.settings_categories), style = MaterialTheme.typography.titleMedium) }
        item { Text(stringResource(R.string.settings_categories_hint)) }
        items(created, key = { it.id }) { category ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${category.name} (${stringResource(R.string.weight_label, category.weight)})")
                    Text(
                        stringResource(
                            R.string.settings_target_label,
                            kindLabel(category.kind),
                            targetLabel(category, vm.uid, partnerUid),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = { vm.archiveCategory(category.id) }) {
                    Text(stringResource(R.string.settings_archive))
                }
                if (partnerUid != null) {
                    TextButton(onClick = { editTargetCategory = category }) {
                        Text(stringResource(R.string.settings_edit_target))
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text(stringResource(R.string.settings_new_category)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { categoryWeight = (categoryWeight - 1).coerceAtLeast(1) }) { Text("−") }
                Text("  ${stringResource(R.string.weight_label, categoryWeight)}  ")
                OutlinedButton(onClick = { categoryWeight = (categoryWeight + 1).coerceAtMost(5) }) { Text("+") }
            }
        }
        item {
            Text(stringResource(R.string.settings_kind))
            Row {
                CategoryKind.entries.forEach { kind ->
                    FilterChip(
                        selected = categoryKind == kind,
                        onClick = {
                            categoryKind = kind
                            goalText = defaultGoal(kind)
                        },
                        label = { Text(kindLabel(kind)) },
                    )
                }
            }
        }
        if (categoryKind != CategoryKind.STARS) {
            item {
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it },
                    label = {
                        Text(
                            stringResource(
                                if (categoryKind == CategoryKind.ALCOHOL) R.string.settings_goal_alcohol
                                else R.string.settings_goal_water,
                            ),
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Text(stringResource(R.string.settings_targets))
            Row {
                FilterChip(
                    selected = audience == Audience.ME,
                    onClick = { audience = Audience.ME },
                    label = { Text(stringResource(R.string.target_me)) },
                )
                if (partnerUid != null) {
                    FilterChip(
                        selected = audience == Audience.PARTNER,
                        onClick = { audience = Audience.PARTNER },
                        label = { Text(stringResource(R.string.target_partner)) },
                    )
                    FilterChip(
                        selected = audience == Audience.BOTH,
                        onClick = { audience = Audience.BOTH },
                        label = { Text(stringResource(R.string.target_both)) },
                    )
                }
            }
        }
        item {
            Button(
                onClick = {
                    val partner = partnerUid
                    val targets = when (audience) {
                        Audience.ME -> listOf(vm.uid)
                        Audience.PARTNER -> listOfNotNull(partner)
                        Audience.BOTH -> listOfNotNull(vm.uid, partner)
                    }
                    val goal = goalText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    vm.saveCategory(categoryName, categoryWeight, categoryKind, targets, goal)
                    categoryName = ""
                },
                enabled = categoryName.isNotBlank() && (audience == Audience.ME || partnerUid != null),
            ) { Text(stringResource(R.string.settings_add_category)) }
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

        item { Text(stringResource(R.string.settings_rewards), style = MaterialTheme.typography.titleMedium) }
        items(catalog, key = { it.id }) { item ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.catalog_item_label, typeLabel(item.type), item.title),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { vm.deleteCatalogItem(item.id) }) {
                    Text(stringResource(R.string.settings_delete))
                }
            }
        }
        item {
            Row {
                TaskType.entries.forEach { type ->
                    FilterChip(
                        selected = catalogType == type,
                        onClick = { catalogType = type },
                        label = { Text(typeLabel(type)) },
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = catalogTitle,
                onValueChange = { catalogTitle = it },
                label = { Text(stringResource(R.string.settings_new_entry)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(
                onClick = {
                    vm.saveCatalogItem(catalogType, catalogTitle)
                    catalogTitle = ""
                },
                enabled = catalogTitle.isNotBlank(),
            ) { Text(stringResource(R.string.settings_add_entry)) }
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

        item { Text(stringResource(R.string.settings_reminder), style = MaterialTheme.typography.titleMedium) }
        item {
            Text(
                stringResource(
                    R.string.settings_reminder_text,
                    String.format("%02d:%02d", reminder.hour, reminder.minute),
                ),
            )
        }
        item {
            OutlinedButton(onClick = {
                TimePickerDialog(context, { _, hour, minute ->
                    val time = LocalTime.of(hour, minute)
                    reminder = time
                    ReminderPrefs(context).save(time)
                    ReminderScheduler.schedule(context, time)
                }, reminder.hour, reminder.minute, true).show()
            }) { Text(stringResource(R.string.settings_change_time)) }
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

        item { Text(stringResource(R.string.settings_sync), style = MaterialTheme.typography.titleMedium) }
        item {
            Text(
                stringResource(if (days.fromCache) R.string.sync_offline else R.string.sync_online),
            )
        }
        item {
            Text(
                stringResource(
                    R.string.sync_last,
                    lastSync?.let { formatTime(it) } ?: stringResource(R.string.sync_never),
                ),
            )
        }
        item {
            OutlinedButton(onClick = { vm.syncNow() }, enabled = !vm.busy) {
                Text(stringResource(R.string.sync_now))
            }
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

        item {
            Button(onClick = { vm.signOut() }) { Text(stringResource(R.string.settings_sign_out)) }
            Spacer(Modifier.height(16.dp))
        }
        item {
            Text(
                stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    /** Zielgruppe einer bestehenden Kategorie ändern (Für mich / Partner / Beide). */
    editTargetCategory?.let { category ->
        var audience by remember(category.id) {
            mutableStateOf(
                when {
                    category.targets.size > 1 -> Audience.BOTH
                    category.targets.firstOrNull() == vm.uid -> Audience.ME
                    else -> Audience.PARTNER
                },
            )
        }
        AlertDialog(
            onDismissRequest = { editTargetCategory = null },
            title = { Text(stringResource(R.string.edit_target_for, category.name)) },
            text = {
                Column(Modifier.width(232.dp)) {
                    Text(stringResource(R.string.settings_targets), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        FilterChip(
                            selected = audience == Audience.ME,
                            onClick = { audience = Audience.ME },
                            label = { Text(stringResource(R.string.target_me)) },
                        )
                        FilterChip(
                            selected = audience == Audience.PARTNER,
                            onClick = { audience = Audience.PARTNER },
                            label = { Text(stringResource(R.string.target_partner)) },
                        )
                        FilterChip(
                            selected = audience == Audience.BOTH,
                            onClick = { audience = Audience.BOTH },
                            label = { Text(stringResource(R.string.target_both)) },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targets = when (audience) {
                            Audience.ME -> listOf(vm.uid)
                            Audience.PARTNER -> listOfNotNull(partnerUid)
                            Audience.BOTH -> listOfNotNull(vm.uid, partnerUid)
                        }
                        vm.saveCategory(
                            category.name,
                            category.weight,
                            category.kind,
                            targets,
                            category.goal,
                            category.id,
                        )
                        editTargetCategory = null
                    },
                ) { Text(stringResource(R.string.settings_save)) }
            },
            dismissButton = {
                TextButton(onClick = { editTargetCategory = null }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }
}

private fun defaultGoal(kind: CategoryKind): String = when (kind) {
    CategoryKind.STARS -> ""
    CategoryKind.ALCOHOL -> "0"
    CategoryKind.WATER -> "2"
}

@Composable
private fun kindLabel(kind: CategoryKind): String = stringResource(
    when (kind) {
        CategoryKind.STARS -> R.string.kind_stars
        CategoryKind.ALCOHOL -> R.string.kind_alcohol
        CategoryKind.WATER -> R.string.kind_water
    },
)

@Composable
private fun targetLabel(category: Category, uid: String, partnerUid: String?): String = stringResource(
    when {
        category.targets.size > 1 -> R.string.target_both
        category.targets.firstOrNull() == uid -> R.string.target_me
        partnerUid != null && category.targets.firstOrNull() == partnerUid -> R.string.target_partner
        else -> R.string.target_partner
    },
)
