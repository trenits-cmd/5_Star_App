package com.trenits.fivestar.ui

import android.app.TimePickerDialog
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
import com.trenits.fivestar.R
import com.trenits.fivestar.domain.TaskType
import com.trenits.fivestar.reminder.ReminderPrefs
import com.trenits.fivestar.reminder.ReminderScheduler
import java.time.LocalTime

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val categories by vm.myCategories.collectAsState()
    val catalog by vm.myCatalog.collectAsState()
    val days by vm.days.collectAsState()
    val lastSync by vm.lastSync.collectAsState()
    var reminder by remember { mutableStateOf(ReminderPrefs(context).time()) }

    var categoryName by rememberSaveable { mutableStateOf("") }
    var categoryWeight by rememberSaveable { mutableStateOf(1) }
    var catalogTitle by rememberSaveable { mutableStateOf("") }
    var catalogType by remember { mutableStateOf(TaskType.REWARD) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(Modifier.height(40.dp))
            Text(stringResource(R.string.settings_categories), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.settings_categories_hint))
        }
        items(categories, key = { it.id }) { category ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${category.name} (${stringResource(R.string.weight_label, category.weight)})",
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { vm.archiveCategory(category.id) }) {
                    Text(stringResource(R.string.settings_archive))
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
            Button(
                onClick = {
                    vm.saveCategory(categoryName, categoryWeight)
                    categoryName = ""
                },
                enabled = categoryName.isNotBlank(),
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
    }
}
