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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.CategorySnapshot
import com.trenits.fivestar.domain.CatalogItem
import com.trenits.fivestar.domain.Task
import com.trenits.fivestar.domain.TaskStatus
import com.trenits.fivestar.domain.TaskType
import com.trenits.fivestar.ui.theme.Amber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val TARGET_PERCENT = 80.0

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
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text(
                    "Heute",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMANY)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!members.fromCache && partnerUid == null) {
            item { WaitingForPartnerCard(vm) }
        }

        item { ScoreHero(myToday?.scorePercent) }

        item { SectionTitle("Kategorien") }
        if (categories.isEmpty()) {
            item { HintCard("Lege unter Einstellungen zuerst Kategorien an.") }
        }
        items(categories, key = { it.id }) { category ->
            CategoryCard(category, stored[category.id] ?: 0) { vm.setStars(category.id, it) }
        }

        item { SectionTitle("Partner") }
        item {
            PartnerCard(
                score = partnerToday?.scorePercent,
                snapshots = partnerToday?.snapshots,
                offline = days.fromCache,
            )
        }

        item { SectionTitle("Offene Aufgaben") }
        if (openTasks.isEmpty()) {
            item { HintCard("Keine offenen Aufgaben. Gut gemacht!") }
        }
        items(openTasks, key = { it.id }) { task ->
            TaskCard(vm, task, catalog)
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
private fun ScoreHero(score: Double?) {
    val progress = ((score ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Dein Score",
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
                "Ab $TARGET_PERCENT % gibt es eine Belohnung, darunter eine Strafe.",
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
            Text("Warte auf deinen Partner", style = MaterialTheme.typography.titleMedium)
            vm.inviteCode?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text("Gib diesen Code an deinen Partner weiter.", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { vm.createInvite() }, enabled = !vm.busy) {
                Text(if (vm.inviteCode == null) "Code erstellen" else "Neuen Code erstellen")
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
            Column(Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Gewicht ${category.weight}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StarRating(value, onChange)
        }
    }
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
private fun PartnerCard(score: Double?, snapshots: List<CategorySnapshot>?, offline: Boolean) {
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
                    if (snapshots == null) "noch keine Bewertung" else "heute",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (offline) {
                Text(
                    "Offline. Angezeigt wird der letzte bekannte Stand.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            snapshots?.takeIf { it.isNotEmpty() }?.let { list ->
                Spacer(Modifier.height(8.dp))
                list.forEach { s ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(s.name, modifier = Modifier.weight(1f))
                        Text(
                            "★".repeat(s.stars) + "☆".repeat(5 - s.stars),
                            color = Amber,
                        )
                    }
                }
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
private fun TaskCard(vm: AppViewModel, task: Task, catalog: List<CatalogItem>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "${typeLabel(task.type)} für ${formatDay(task.dayKey)}",
                style = MaterialTheme.typography.titleSmall,
                color = if (task.type == TaskType.REWARD) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            Spacer(Modifier.height(6.dp))
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
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
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
