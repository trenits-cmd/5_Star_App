package com.trenits.fivestar.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.unit.dp
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
        item { Text("Kategorien", style = MaterialTheme.typography.titleMedium) }
        item { Text("Gewicht 1 bis 5. Archivierte Kategorien bleiben in der Historie erhalten.") }
        items(categories, key = { it.id }) { category ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${category.name} (Gewicht ${category.weight})", modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.archiveCategory(category.id) }) { Text("Archivieren") }
            }
        }
        item {
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Neue Kategorie") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { categoryWeight = (categoryWeight - 1).coerceAtLeast(1) }) { Text("−") }
                Text("  Gewicht $categoryWeight  ")
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
            ) { Text("Kategorie hinzufügen") }
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

        item { Text("Belohnungen und Strafen", style = MaterialTheme.typography.titleMedium) }
        items(catalog, key = { it.id }) { item ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${typeLabel(item.type)}: ${item.title}", modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.deleteCatalogItem(item.id) }) { Text("Löschen") }
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
                label = { Text("Neuer Eintrag") },
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
            ) { Text("Eintrag hinzufügen") }
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

        item { Text("Erinnerung", style = MaterialTheme.typography.titleMedium) }
        item {
            Text("Täglich um ${String.format("%02d:%02d", reminder.hour, reminder.minute)}. Sie kommt nur, wenn heute noch nichts bewertet wurde.")
        }
        item {
            OutlinedButton(onClick = {
                TimePickerDialog(context, { _, hour, minute ->
                    val time = LocalTime.of(hour, minute)
                    reminder = time
                    ReminderPrefs(context).save(time)
                    ReminderScheduler.schedule(context, time)
                }, reminder.hour, reminder.minute, true).show()
            }) { Text("Uhrzeit ändern") }
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

        item { Text("Sync", style = MaterialTheme.typography.titleMedium) }
        item {
            Text(
                if (days.fromCache) "Offline. Es wird der letzte bekannte Stand angezeigt."
                else "Verbunden.",
            )
        }
        item {
            Text("Letzte Synchronisation: " + (lastSync?.let { formatTime(it) } ?: "noch nicht"))
        }
        item {
            OutlinedButton(onClick = { vm.syncNow() }, enabled = !vm.busy) { Text("Jetzt synchronisieren") }
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

        item {
            Button(onClick = { vm.signOut() }) { Text("Abmelden") }
            Spacer(Modifier.height(16.dp))
        }
    }
}
