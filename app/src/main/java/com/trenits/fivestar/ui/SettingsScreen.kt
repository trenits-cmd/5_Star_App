package com.trenits.fivestar.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trenits.fivestar.BuildConfig
import com.trenits.fivestar.R
import com.trenits.fivestar.domain.CatalogItem
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.CategoryIcon
import com.trenits.fivestar.domain.CategoryKind
import com.trenits.fivestar.domain.TaskType
import com.trenits.fivestar.reminder.ReminderPrefs
import com.trenits.fivestar.reminder.ReminderScheduler
import java.time.LocalTime

private enum class Audience { ME, PARTNER, BOTH }

@OptIn(ExperimentalLayoutApi::class)
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
    var categoryIcon by remember { mutableStateOf(CategoryIcon.STAR) }
    var goalText by rememberSaveable { mutableStateOf("") }
    var audience by remember { mutableStateOf(Audience.ME) }
    var newCategoryNotifications by remember { mutableStateOf(listOf<String>()) }
    var newNotificationInput by rememberSaveable { mutableStateOf("") }

    var editCategory by remember { mutableStateOf<Category?>(null) }
    var editCatalogItem by remember { mutableStateOf<CatalogItem?>(null) }

    var displayName by rememberSaveable { mutableStateOf("") }
    var catalogTitle by rememberSaveable { mutableStateOf("") }
    var catalogType by remember { mutableStateOf(TaskType.REWARD) }
    var catalogAudience by remember { mutableStateOf(Audience.ME) }

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
                CategoryImage(category = category, size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("${category.name} (${stringResource(R.string.weight_label, category.weight)})")
                    Text(
                        stringResource(
                            R.string.settings_target_label,
                            kindLabel(category.kind),
                            targetLabel(category.targets, vm.uid, partnerUid),
                        ) + " · " + stringResource(R.string.settings_notifications_count, category.notifications.size),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = { editCategory = category }) {
                    Text(stringResource(R.string.settings_edit_target))
                }
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
            Text(stringResource(R.string.settings_kind))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CategoryKind.entries.forEach { kind ->
                    FilterChip(
                        selected = categoryKind == kind,
                        onClick = {
                            categoryKind = kind
                            goalText = defaultGoal(kind)
                            if (kind == CategoryKind.WATER) categoryIcon = CategoryIcon.WATER
                            if (kind == CategoryKind.ALCOHOL) categoryIcon = CategoryIcon.ALCOHOL
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
            Text(stringResource(R.string.settings_category_image))
            IconSelector(selected = categoryIcon, onSelect = { categoryIcon = it })
        }
        item {
            Text(stringResource(R.string.settings_category_notifications))
            newCategoryNotifications.forEachIndexed { idx, msg ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("• $msg", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(
                        onClick = {
                            newCategoryNotifications = newCategoryNotifications.filterIndexed { i, _ -> i != idx }
                        },
                    ) {
                        Text(stringResource(R.string.settings_delete))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newNotificationInput,
                    onValueChange = { newNotificationInput = it },
                    label = { Text(stringResource(R.string.settings_new_notification)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        if (newNotificationInput.isNotBlank()) {
                            newCategoryNotifications = newCategoryNotifications + newNotificationInput.trim()
                            newNotificationInput = ""
                        }
                    },
                    enabled = newNotificationInput.isNotBlank(),
                ) {
                    Text(stringResource(R.string.settings_add_notification))
                }
            }
        }
        item {
            Text(stringResource(R.string.settings_targets))
            AudienceSelector(
                audience = audience,
                hasPartner = partnerUid != null,
                onSelect = { audience = it },
            )
        }
        item {
            Button(
                onClick = {
                    val partner = partnerUid
                    val targets = targetsForAudience(audience, vm.uid, partner)
                    val goal = goalText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val pendingList = if (newNotificationInput.isNotBlank()) {
                        newCategoryNotifications + newNotificationInput.trim()
                    } else {
                        newCategoryNotifications
                    }
                    vm.saveCategory(
                        name = categoryName,
                        weight = categoryWeight,
                        kind = categoryKind,
                        targets = targets,
                        goal = goal,
                        icon = categoryIcon.name,
                        notifications = pendingList,
                    )
                    categoryName = ""
                    newCategoryNotifications = emptyList()
                    newNotificationInput = ""
                },
                enabled = categoryName.isNotBlank() && (audience == Audience.ME || partnerUid != null),
            ) { Text(stringResource(R.string.settings_add_category)) }
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

        item { Text(stringResource(R.string.settings_rewards), style = MaterialTheme.typography.titleMedium) }
        items(catalog, key = { it.id }) { item ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(
                        R.string.catalog_item_label,
                        typeLabel(item.type),
                        item.title,
                        targetLabel(item.targets, vm.uid, partnerUid),
                    ),
                    modifier = Modifier.weight(1f),
                )
                if (partnerUid != null) {
                    TextButton(onClick = { editCatalogItem = item }) {
                        Text(stringResource(R.string.settings_edit_target))
                    }
                }
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
            Text(stringResource(R.string.settings_targets))
            AudienceSelector(
                audience = catalogAudience,
                hasPartner = partnerUid != null,
                onSelect = { catalogAudience = it },
            )
        }
        item {
            Button(
                onClick = {
                    val targets = targetsForAudience(catalogAudience, vm.uid, partnerUid)
                    vm.saveCatalogItem(catalogType, catalogTitle, targets)
                    catalogTitle = ""
                },
                enabled = catalogTitle.isNotBlank() && (catalogAudience == Audience.ME || partnerUid != null),
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    TimePickerDialog(context, { _, hour, minute ->
                        val time = LocalTime.of(hour, minute)
                        reminder = time
                        ReminderPrefs(context).save(time)
                        ReminderScheduler.schedule(context, time)
                    }, reminder.hour, reminder.minute, true).show()
                }) { Text(stringResource(R.string.settings_change_time)) }

                OutlinedButton(onClick = { vm.sendTestPushNotifications() }) {
                    Text(stringResource(R.string.settings_test_push))
                }
            }
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
            Spacer(Modifier.height(12.dp))
        }

        // App-Version als allerletzter Punkt in den Einstellungen
        item {
            Text(
                stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    // Bestehende Kategorie bearbeiten (Zielgruppe, Bild und mehrere Push-Nachrichten)
    editCategory?.let { category ->
        var dialogAudience by remember(category.id) {
            mutableStateOf(audienceFromTargets(category.targets, vm.uid))
        }
        var dialogIcon by remember(category.id) {
            mutableStateOf(category.resolvedIcon())
        }
        var dialogNotifications by remember(category.id) {
            mutableStateOf(category.notifications)
        }
        var dialogNewMessage by remember(category.id) {
            mutableStateOf("")
        }

        AlertDialog(
            onDismissRequest = { editCategory = null },
            title = { Text(stringResource(R.string.edit_target_for, category.name)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.settings_targets), style = MaterialTheme.typography.labelLarge)
                    AudienceSelector(
                        audience = dialogAudience,
                        hasPartner = partnerUid != null,
                        onSelect = { dialogAudience = it },
                    )

                    Text(stringResource(R.string.settings_category_image), style = MaterialTheme.typography.labelLarge)
                    IconSelector(selected = dialogIcon, onSelect = { dialogIcon = it })

                    Text(stringResource(R.string.settings_category_notifications), style = MaterialTheme.typography.labelLarge)
                    dialogNotifications.forEachIndexed { idx, msg ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("• $msg", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            TextButton(
                                onClick = {
                                    dialogNotifications = dialogNotifications.filterIndexed { i, _ -> i != idx }
                                },
                            ) {
                                Text(stringResource(R.string.settings_delete))
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = dialogNewMessage,
                            onValueChange = { dialogNewMessage = it },
                            label = { Text(stringResource(R.string.settings_new_notification)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(6.dp))
                        OutlinedButton(
                            onClick = {
                                if (dialogNewMessage.isNotBlank()) {
                                    dialogNotifications = dialogNotifications + dialogNewMessage.trim()
                                    dialogNewMessage = ""
                                }
                            },
                            enabled = dialogNewMessage.isNotBlank(),
                        ) {
                            Text("+")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targets = targetsForAudience(dialogAudience, vm.uid, partnerUid)
                        val finalMessages = if (dialogNewMessage.isNotBlank()) {
                            dialogNotifications + dialogNewMessage.trim()
                        } else {
                            dialogNotifications
                        }
                        vm.saveCategory(
                            name = category.name,
                            weight = category.weight,
                            kind = category.kind,
                            targets = targets,
                            goal = category.goal,
                            icon = dialogIcon.name,
                            notifications = finalMessages,
                            id = category.id,
                        )
                        editCategory = null
                    },
                ) { Text(stringResource(R.string.settings_save)) }
            },
            dismissButton = {
                TextButton(onClick = { editCategory = null }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }

    // Bestehenden Belohnungs-/Strafen-Eintrag auf Person(en) anpassen
    editCatalogItem?.let { item ->
        var dialogAudience by remember(item.id) {
            mutableStateOf(audienceFromTargets(item.targets, vm.uid))
        }
        AlertDialog(
            onDismissRequest = { editCatalogItem = null },
            title = { Text(stringResource(R.string.edit_target_for, item.title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_targets), style = MaterialTheme.typography.labelLarge)
                    AudienceSelector(
                        audience = dialogAudience,
                        hasPartner = partnerUid != null,
                        onSelect = { dialogAudience = it },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targets = targetsForAudience(dialogAudience, vm.uid, partnerUid)
                        vm.saveCatalogItem(item.type, item.title, targets, item.id)
                        editCatalogItem = null
                    },
                ) { Text(stringResource(R.string.settings_save)) }
            },
            dismissButton = {
                TextButton(onClick = { editCatalogItem = null }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconSelector(selected: CategoryIcon, onSelect: (CategoryIcon) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CategoryIcon.entries.forEach { icon ->
            FilterChip(
                selected = selected == icon,
                onClick = { onSelect(icon) },
                leadingIcon = {
                    Image(
                        painter = painterResource(categoryIconRes(icon)),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                label = { Text(stringResource(categoryIconLabelRes(icon))) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudienceSelector(
    audience: Audience,
    hasPartner: Boolean,
    onSelect: (Audience) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
            selected = audience == Audience.ME,
            onClick = { onSelect(Audience.ME) },
            label = { Text(stringResource(R.string.target_me)) },
        )
        if (hasPartner) {
            FilterChip(
                selected = audience == Audience.PARTNER,
                onClick = { onSelect(Audience.PARTNER) },
                label = { Text(stringResource(R.string.target_partner)) },
            )
            FilterChip(
                selected = audience == Audience.BOTH,
                onClick = { onSelect(Audience.BOTH) },
                label = { Text(stringResource(R.string.target_both)) },
            )
        }
    }
}

private fun audienceFromTargets(targets: List<String>, uid: String): Audience = when {
    targets.size > 1 -> Audience.BOTH
    targets.firstOrNull() == uid -> Audience.ME
    else -> Audience.PARTNER
}

private fun targetsForAudience(audience: Audience, uid: String, partnerUid: String?): List<String> =
    when (audience) {
        Audience.ME -> listOf(uid)
        Audience.PARTNER -> listOfNotNull(partnerUid).ifEmpty { listOf(uid) }
        Audience.BOTH -> listOfNotNull(uid, partnerUid)
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
private fun targetLabel(targets: List<String>, uid: String, partnerUid: String?): String = stringResource(
    when {
        targets.size > 1 -> R.string.target_both
        targets.firstOrNull() == uid -> R.string.target_me
        partnerUid != null && targets.firstOrNull() == partnerUid -> R.string.target_partner
        else -> R.string.target_partner
    },
)
