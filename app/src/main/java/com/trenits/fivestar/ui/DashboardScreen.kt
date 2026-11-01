package com.trenits.fivestar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.CategorySnapshot
import com.trenits.fivestar.domain.CatalogItem
import com.trenits.fivestar.domain.Task
import com.trenits.fivestar.domain.TaskStatus

@Composable
fun DashboardScreen(vm: AppViewModel) {
    val categories by vm.myCategories.collectAsState()
    val myToday by vm.myToday.collectAsState()
    val partnerToday by vm.partnerToday.collectAsState()
    val partnerUid by vm.partnerUid.collectAsState()
    val members by vm.members.collectAsState()
    val days by vm.days.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val catalog by vm.myCatalog.collectAsState()

    val stored = myToday?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
    val openTasks = tasks.items.filter { it.status == TaskStatus.OPEN }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!members.fromCache && partnerUid == null) {
            item { WaitingForPartnerCard(vm) }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Dein Score heute", style = MaterialTheme.typography.titleMedium)
                    Text(formatPercent(myToday?.scorePercent), fontSize = 32.sp)
                    Text(
                        "Ab 80 % gibt es eine Belohnung, darunter eine Strafe.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        item { Text("Kategorien", style = MaterialTheme.typography.titleMedium) }
        if (categories.isEmpty()) {
            item { Text("Lege unter Einstellungen zuerst Kategorien an.") }
        }
        items(categories, key = { it.id }) { category ->
            CategoryRow(category, stored[category.id] ?: 0) { vm.setStars(category.id, it) }
        }

        item { Text("Partner", style = MaterialTheme.typography.titleMedium) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Score heute: ${formatPercent(partnerToday?.scorePercent)}")
                    if (days.fromCache) {
                        Text("Offline. Angezeigt wird der letzte bekannte Stand.", style = MaterialTheme.typography.bodySmall)
                    }
                    partnerToday?.snapshots?.let { snapshots ->
                        Spacer(Modifier.height(8.dp))
                        Text(snapshotLine(snapshots), style = MaterialTheme.typography.bodySmall)
                    } ?: Text("Noch keine Bewertung.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item { Text("Offene Aufgaben", style = MaterialTheme.typography.titleMedium) }
        if (openTasks.isEmpty()) {
            item { Text("Keine offenen Aufgaben.") }
        }
        items(openTasks, key = { it.id }) { task ->
            TaskCard(vm, task, catalog)
        }
    }
}

@Composable
private fun WaitingForPartnerCard(vm: AppViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Warte auf deinen Partner", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            vm.inviteCode?.let { Text("Einladungscode: $it", style = MaterialTheme.typography.titleLarge) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { vm.createInvite() }, enabled = !vm.busy) {
                Text(if (vm.inviteCode == null) "Code erstellen" else "Neuen Code erstellen")
            }
        }
    }
}

@Composable
private fun CategoryRow(category: Category, value: Int, onChange: (Int) -> Unit) {
    Column {
        Text(category.name, style = MaterialTheme.typography.bodyLarge)
        Row {
            for (n in 1..5) {
                Text(
                    if (n <= value) "★" else "☆",
                    fontSize = 28.sp,
                    modifier = Modifier
                        .clickable { onChange(if (value == n) 0 else n) }
                        .padding(end = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun TaskCard(vm: AppViewModel, task: Task, catalog: List<CatalogItem>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "${typeLabel(task.type)} für ${formatDay(task.dayKey)}",
                style = MaterialTheme.typography.titleSmall,
            )
            val isMine = task.ownerUid == vm.uid
            when {
                isMine && task.title.isEmpty() -> {
                    Text("Wähle eine Aktion aus deinem Katalog:")
                    val options = catalog.filter { it.type == task.type }
                    if (options.isEmpty()) {
                        Text("Dein Katalog hat keine Einträge dieser Art. Lege sie unter Einstellungen an.")
                    }
                    options.forEach { item ->
                        OutlinedButton(
                            onClick = { vm.chooseTaskTitle(task.id, item.title) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(item.title) }
                    }
                }
                isMine -> Text("${task.title}. Wartet auf Bestätigung durch deinen Partner.")
                task.title.isEmpty() -> Text("Dein Partner wählt noch eine Aktion.")
                else -> {
                    Text("Partner: ${task.title}")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.confirmTask(task.id) }) { Text("Erledigt bestätigen") }
                }
            }
        }
    }
}

private fun snapshotLine(snapshots: List<CategorySnapshot>): String =
    snapshots.joinToString(" · ") { "${it.name} ${it.stars}★" }
