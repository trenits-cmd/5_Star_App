package com.trenits.fivestar.ui

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
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

private val INTERVAL_OPTIONS = listOf(1, 2, 3, 4)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val prefs = remember { ReminderPrefs(context) }

    val created by vm.created.collectAsState()
    val catalog by vm.myCatalog.collectAsState()
    val days by vm.days.collectAsState()
    val lastSync by vm.lastSync.collectAsState()
    val myName by vm.myName.collectAsState()
    val myPhoto by vm.myPhoto.collectAsState()
    val partnerName by vm.partnerName.collectAsState()
    val partnerUid by vm.partnerUid.collectAsState()

    var globalPushEnabled by remember { mutableStateOf(prefs.enabled()) }
    var reminder by remember { mutableStateOf(prefs.time()) }
    var globalIntervalHours by remember { mutableStateOf(prefs.intervalHours()) }
    var quietEnabled by remember { mutableStateOf(prefs.quietHoursEnabled()) }
    var quietStart by remember { mutableStateOf(prefs.quietStart()) }
    var quietEnd by remember { mutableStateOf(prefs.quietEnd()) }

    val myLabel = myName ?: stringResource(R.string.who_you)
    val partnerLabel = partnerName ?: stringResource(R.string.who_partner)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            encodeAvatarUri(context, uri)?.let { base64 ->
                vm.savePhoto(base64)
            }
        }
    }

    var categoryName by rememberSaveable { mutableStateOf("") }
    var categoryWeight by rememberSaveable { mutableStateOf(3) }
    var categoryKind by remember { mutableStateOf(CategoryKind.STARS) }
    var categoryIcon by remember { mutableStateOf(CategoryIcon.STAR) }
    var goalText by rememberSaveable { mutableStateOf("") }
    var audience by remember { mutableStateOf(Audience.ME) }
    var newCategoryPushEnabled by rememberSaveable { mutableStateOf(true) }
    var newCategoryIntervalHours by rememberSaveable { mutableStateOf(0) }
    var newCategoryPushHour by rememberSaveable { mutableStateOf(reminder.hour) }
    var newCategoryPushMinute by rememberSaveable { mutableStateOf(reminder.minute) }
    var newCategoryNotifications by remember { mutableStateOf(listOf<String>()) }
    var newNotificationInput by rememberSaveable { mutableStateOf("") }

    var editCategory by remember { mutableStateOf<Category?>(null) }
    var editCatalogItem by remember { mutableStateOf<CatalogItem?>(null) }

    var displayName by rememberSaveable(myName) { mutableStateOf(myName.orEmpty()) }
    var partnerEmail by rememberSaveable { mutableStateOf("") }
    var catalogTitle by rememberSaveable { mutableStateOf("") }
    var catalogType by remember { mutableStateOf(TaskType.REWARD) }
    var catalogAudience by remember { mutableStateOf(Audience.ME) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(28.dp)) }

        // 1. Eigenes Profilbild (Icon) & eigener Name
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(stringResource(R.string.profile_photo_title), style = MaterialTheme.typography.titleMedium)
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        UserAvatar(photoBase64 = myPhoto, name = myLabel, size = 56.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            OutlinedButton(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.profile_photo_upload))
                            }
                            if (!myPhoto.isNullOrBlank()) {
                                TextButton(onClick = { vm.savePhoto(null) }) {
                                    Text(stringResource(R.string.profile_photo_remove))
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    Text(stringResource(R.string.settings_display_name), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.settings_display_name_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { vm.saveDisplayName(displayName) },
                        enabled = displayName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.settings_save)) }
                }
            }
        }

        // 2. Partner per E-Mail einladen & Partner zu Testzwecken löschen
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.pairing_title), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = partnerEmail,
                        onValueChange = { partnerEmail = it },
                        label = { Text(stringResource(R.string.invite_email_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            vm.invitePartnerByEmail(partnerEmail) { code ->
                                launchInviteEmail(context, partnerEmail, code)
                            }
                        },
                        enabled = !vm.busy && partnerEmail.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.invite_send_email))
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text(
                        stringResource(R.string.partner_remove_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { vm.removePartnerForTest() },
                        enabled = !vm.busy,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.partner_remove_button))
                    }
                }
            }
        }

        // 3. Schnellstart-Vorlagen für Rosie und Andi
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.settings_presets_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.settings_presets_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { vm.applyRosiePreset() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_preset_rosie))
                    }
                    OutlinedButton(
                        onClick = { vm.applyAndiPreset() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_preset_andi))
                    }
                }
            }
        }

        // 4. Globale Push-Benachrichtigungen, Intervall & Ruhepause (23:00–09:00 Uhr)
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(stringResource(R.string.settings_reminder), style = MaterialTheme.typography.titleMedium)

                    // Globaler Kippschalter für Push-Benachrichtigungen
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.push_global_enable),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = globalPushEnabled,
                            onCheckedChange = { enabled ->
                                globalPushEnabled = enabled
                                prefs.saveEnabled(enabled)
                                ReminderScheduler.schedule(context, reminder)
                            },
                        )
                    }

                    if (globalPushEnabled) {
                        Text(
                            stringResource(
                                R.string.settings_reminder_text,
                                "%02d:%02d".format(reminder.hour, reminder.minute),
                                "%02d:%02d".format(quietStart.hour, quietStart.minute),
                                "%02d:%02d".format(quietEnd.hour, quietEnd.minute),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        // Auswahl: Tägliche Uhrzeit vs. Globales Stunden-Intervall
                        Text(
                            stringResource(R.string.push_mode_label),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        PushScheduleSelector(
                            intervalHours = globalIntervalHours,
                            hour = reminder.hour,
                            minute = reminder.minute,
                            onDailySelected = {
                                globalIntervalHours = 0
                                prefs.saveIntervalHours(0)
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        val t = LocalTime.of(h, m)
                                        reminder = t
                                        prefs.save(t)
                                        ReminderScheduler.schedule(context, t)
                                    },
                                    reminder.hour,
                                    reminder.minute,
                                    true,
                                ).show()
                            },
                            onIntervalSelected = { hours ->
                                globalIntervalHours = hours
                                prefs.saveIntervalHours(hours)
                                ReminderScheduler.schedule(context, reminder)
                            },
                        )

                        HorizontalDivider(Modifier.padding(vertical = 2.dp))

                        // Ruhepause (Standard 23:00 – 09:00 Uhr)
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.quiet_hours_enable),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    stringResource(
                                        R.string.quiet_hours_range,
                                        "%02d:%02d".format(quietStart.hour, quietStart.minute),
                                        "%02d:%02d".format(quietEnd.hour, quietEnd.minute),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = quietEnabled,
                                onCheckedChange = { enabled ->
                                    quietEnabled = enabled
                                    prefs.saveQuietHoursEnabled(enabled)
                                },
                            )
                        }

                        if (quietEnabled) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        TimePickerDialog(
                                            context,
                                            { _, h, m ->
                                                val t = LocalTime.of(h, m)
                                                quietStart = t
                                                prefs.saveQuietStart(t)
                                            },
                                            quietStart.hour,
                                            quietStart.minute,
                                            true,
                                        ).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.quiet_hours_start,
                                            "%02d:%02d".format(quietStart.hour, quietStart.minute),
                                        ),
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        TimePickerDialog(
                                            context,
                                            { _, h, m ->
                                                val t = LocalTime.of(h, m)
                                                quietEnd = t
                                                prefs.saveQuietEnd(t)
                                            },
                                            quietEnd.hour,
                                            quietEnd.minute,
                                            true,
                                        ).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.quiet_hours_end,
                                            "%02d:%02d".format(quietEnd.hour, quietEnd.minute),
                                        ),
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { vm.sendTestPushNotifications() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.settings_test_push))
                        }
                    }
                }
            }
        }

        // 5. Bestehende Kategorien (handyfreundliche Karten mit eigenem Push-Kippschalter)
        item {
            Text(stringResource(R.string.settings_categories), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_categories_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        items(created, key = { it.id }) { category ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryImage(category = category, size = 38.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                localizedCategoryName(category),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "${stringResource(R.string.weight_label, category.weight)} · ${priorityLabel(category.weight)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = category.pushEnabled,
                            onCheckedChange = { enabled ->
                                vm.setCategoryPushEnabled(category.id, enabled)
                            },
                        )
                    }

                    val pushSummary = when {
                        !category.pushEnabled -> stringResource(R.string.push_summary_off)
                        category.pushIntervalHours > 0 ->
                            stringResource(R.string.push_summary_interval, category.pushIntervalHours)
                        else -> stringResource(
                            R.string.push_Summary_daily,
                            "%02d:%02d".format(category.pushHour, category.pushMinute),
                        )
                    }
                    Text(
                        stringResource(
                            R.string.settings_target_label,
                            kindLabel(category.kind),
                            targetLabel(category.targets, vm.uid, partnerUid, myLabel, partnerLabel),
                        ) + " · $pushSummary · " +
                            stringResource(R.string.settings_notifications_count, category.notifications.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { editCategory = category }) {
                            Text(stringResource(R.string.settings_edit_target))
                        }
                        TextButton(onClick = { vm.archiveCategory(category.id) }) {
                            Text(stringResource(R.string.settings_archive))
                        }
                    }
                }
            }
        }

        // 6. Neue Kategorie anlegen (in eigener Card)
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(stringResource(R.string.settings_new_category), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text(stringResource(R.string.settings_new_category)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Gewichtungs-Steuerung (sauber zweizeilig für schmale Handys)
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        OutlinedButton(onClick = { categoryWeight = (categoryWeight - 1).coerceAtLeast(1) }) {
                            Text("−")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.weight_label, categoryWeight),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                priorityLabel(categoryWeight),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(onClick = { categoryWeight = (categoryWeight + 1).coerceAtMost(5) }) {
                            Text("+")
                        }
                    }

                    Text(stringResource(R.string.settings_kind), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CategoryKind.entries.forEach { kind ->
                            FilterChip(
                                selected = categoryKind == kind,
                                onClick = {
                                    categoryKind = kind
                                    goalText = defaultGoal(kind)
                                    when (kind) {
                                        CategoryKind.WATER -> {
                                            categoryIcon = CategoryIcon.WATER
                                            categoryWeight = 3
                                            newCategoryIntervalHours = 2
                                        }
                                        CategoryKind.ALCOHOL -> {
                                            categoryIcon = CategoryIcon.ALCOHOL
                                            categoryWeight = 2
                                            newCategoryIntervalHours = 0
                                        }
                                        CategoryKind.ROTATION -> {
                                            categoryIcon = CategoryIcon.HOME
                                            categoryWeight = 1
                                            newCategoryIntervalHours = 0
                                        }
                                        CategoryKind.DINNER_TIME -> {
                                            categoryIcon = CategoryIcon.FOOD
                                            categoryWeight = 3
                                            newCategoryIntervalHours = 0
                                        }
                                        CategoryKind.MEALS -> {
                                            categoryIcon = CategoryIcon.FOOD
                                            categoryWeight = 3
                                            newCategoryIntervalHours = 0
                                        }
                                        CategoryKind.READING -> {
                                            categoryIcon = CategoryIcon.MIND
                                            categoryWeight = 2
                                            newCategoryIntervalHours = 0
                                        }
                                        CategoryKind.STARS -> {}
                                    }
                                },
                                label = { Text(kindLabel(kind)) },
                            )
                        }
                    }

                    if (categoryKind == CategoryKind.ALCOHOL || categoryKind == CategoryKind.WATER) {
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

                    Text(stringResource(R.string.settings_category_image), style = MaterialTheme.typography.labelLarge)
                    IconSelector(selected = categoryIcon, onSelect = { categoryIcon = it })

                    HorizontalDivider()

                    // Push-Kippschalter & Rhythmus für diese Kategorie
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.push_category_enable),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = newCategoryPushEnabled,
                            onCheckedChange = { newCategoryPushEnabled = it },
                        )
                    }

                    if (newCategoryPushEnabled) {
                        Text(stringResource(R.string.push_mode_label), style = MaterialTheme.typography.labelLarge)
                        PushScheduleSelector(
                            intervalHours = newCategoryIntervalHours,
                            hour = newCategoryPushHour,
                            minute = newCategoryPushMinute,
                            onDailySelected = {
                                newCategoryIntervalHours = 0
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        newCategoryPushHour = h
                                        newCategoryPushMinute = m
                                    },
                                    newCategoryPushHour,
                                    newCategoryPushMinute,
                                    true,
                                ).show()
                            },
                            onIntervalSelected = { hours ->
                                newCategoryIntervalHours = hours
                            },
                        )

                        Text(
                            stringResource(R.string.settings_category_notifications),
                            style = MaterialTheme.typography.labelLarge,
                        )
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
                                Text("+")
                            }
                        }
                    }

                    Text(stringResource(R.string.settings_targets), style = MaterialTheme.typography.labelLarge)
                    AudienceSelector(
                        audience = audience,
                        hasPartner = partnerUid != null,
                        myLabel = myLabel,
                        partnerLabel = partnerLabel,
                        onSelect = { audience = it },
                    )

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
                                pushEnabled = newCategoryPushEnabled,
                                pushIntervalHours = newCategoryIntervalHours,
                                pushHour = newCategoryPushHour,
                                pushMinute = newCategoryPushMinute,
                            )
                            categoryName = ""
                            newCategoryNotifications = emptyList()
                            newNotificationInput = ""
                        },
                        enabled = categoryName.isNotBlank() && (audience == Audience.ME || partnerUid != null),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.settings_add_category)) }
                }
            }
        }

        // 7. Belohnungen und Strafen Katalog
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.settings_rewards), style = MaterialTheme.typography.titleMedium)
                    catalog.forEach { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    stringResource(
                                        R.string.catalog_item_label,
                                        typeLabel(item.type),
                                        item.title,
                                        targetLabel(item.targets, vm.uid, partnerUid, myLabel, partnerLabel),
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
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
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 4.dp))

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaskType.entries.forEach { type ->
                            FilterChip(
                                selected = catalogType == type,
                                onClick = { catalogType = type },
                                label = { Text(typeLabel(type)) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = catalogTitle,
                        onValueChange = { catalogTitle = it },
                        label = { Text(stringResource(R.string.settings_new_entry)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(stringResource(R.string.settings_targets), style = MaterialTheme.typography.labelLarge)
                    AudienceSelector(
                        audience = catalogAudience,
                        hasPartner = partnerUid != null,
                        myLabel = myLabel,
                        partnerLabel = partnerLabel,
                        onSelect = { catalogAudience = it },
                    )
                    Button(
                        onClick = {
                            val targets = targetsForAudience(catalogAudience, vm.uid, partnerUid)
                            vm.saveCatalogItem(catalogType, catalogTitle, targets)
                            catalogTitle = ""
                        },
                        enabled = catalogTitle.isNotBlank() && (catalogAudience == Audience.ME || partnerUid != null),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.settings_add_entry)) }
                }
            }
        }

        // 8. Sync & Abmelden
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.settings_sync), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(if (days.fromCache) R.string.sync_offline else R.string.sync_online),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        stringResource(
                            R.string.sync_last,
                            lastSync?.let { formatTime(it) } ?: stringResource(R.string.sync_never),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { vm.syncNow() },
                        enabled = !vm.busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.sync_now))
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Button(
                        onClick = { vm.signOut() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_sign_out))
                    }
                }
            }
        }

        // 9. App-Version als allerletzter Punkt in den Einstellungen
        item {
            Text(
                stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
        }
    }

    // Bestehende Kategorie bearbeiten (Gewicht, Zielgruppe, Bild, Kippschalter, Uhrzeit/Intervall & Push-Nachrichten)
    editCategory?.let { category ->
        var dialogWeight by remember(category.id) {
            mutableStateOf(category.weight)
        }
        var dialogAudience by remember(category.id) {
            mutableStateOf(audienceFromTargets(category.targets, vm.uid))
        }
        var dialogIcon by remember(category.id) {
            mutableStateOf(category.resolvedIcon())
        }
        var dialogPushEnabled by remember(category.id) {
            mutableStateOf(category.pushEnabled)
        }
        var dialogIntervalHours by remember(category.id) {
            mutableStateOf(category.pushIntervalHours)
        }
        var dialogPushHour by remember(category.id) {
            mutableStateOf(category.pushHour)
        }
        var dialogPushMinute by remember(category.id) {
            mutableStateOf(category.pushMinute)
        }
        var dialogNotifications by remember(category.id) {
            mutableStateOf(category.notifications)
        }
        var dialogNewMessage by remember(category.id) {
            mutableStateOf("")
        }

        AlertDialog(
            onDismissRequest = { editCategory = null },
            title = { Text(stringResource(R.string.edit_target_for, localizedCategoryName(category))) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        OutlinedButton(onClick = { dialogWeight = (dialogWeight - 1).coerceAtLeast(1) }) { Text("−") }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.weight_label, dialogWeight),
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                priorityLabel(dialogWeight),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        OutlinedButton(onClick = { dialogWeight = (dialogWeight + 1).coerceAtMost(5) }) { Text("+") }
                    }

                    Text(stringResource(R.string.settings_targets), style = MaterialTheme.typography.labelLarge)
                    AudienceSelector(
                        audience = dialogAudience,
                        hasPartner = partnerUid != null,
                        myLabel = myLabel,
                        partnerLabel = partnerLabel,
                        onSelect = { dialogAudience = it },
                    )

                    Text(stringResource(R.string.settings_category_image), style = MaterialTheme.typography.labelLarge)
                    IconSelector(selected = dialogIcon, onSelect = { dialogIcon = it })

                    HorizontalDivider()

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.push_category_enable),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = dialogPushEnabled,
                            onCheckedChange = { dialogPushEnabled = it },
                        )
                    }

                    if (dialogPushEnabled) {
                        Text(stringResource(R.string.push_mode_label), style = MaterialTheme.typography.labelLarge)
                        PushScheduleSelector(
                            intervalHours = dialogIntervalHours,
                            hour = dialogPushHour,
                            minute = dialogPushMinute,
                            onDailySelected = {
                                dialogIntervalHours = 0
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        dialogPushHour = h
                                        dialogPushMinute = m
                                    },
                                    dialogPushHour,
                                    dialogPushMinute,
                                    true,
                                ).show()
                            },
                            onIntervalSelected = { hours ->
                                dialogIntervalHours = hours
                            },
                        )

                        Text(
                            stringResource(R.string.settings_category_notifications),
                            style = MaterialTheme.typography.labelLarge,
                        )
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
                            weight = dialogWeight,
                            kind = category.kind,
                            targets = targets,
                            goal = category.goal,
                            icon = dialogIcon.name,
                            notifications = finalMessages,
                            pushEnabled = dialogPushEnabled,
                            pushIntervalHours = dialogIntervalHours,
                            pushHour = dialogPushHour,
                            pushMinute = dialogPushMinute,
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
                        myLabel = myLabel,
                        partnerLabel = partnerLabel,
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
private fun PushScheduleSelector(
    intervalHours: Int,
    hour: Int,
    minute: Int,
    onDailySelected: () -> Unit,
    onIntervalSelected: (Int) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
            selected = intervalHours == 0,
            onClick = onDailySelected,
            label = {
                Text(stringResource(R.string.push_mode_daily, "%02d:%02d".format(hour, minute)))
            },
        )
        INTERVAL_OPTIONS.forEach { h ->
            FilterChip(
                selected = intervalHours == h,
                onClick = { onIntervalSelected(h) },
                label = { Text(stringResource(R.string.push_mode_interval, h)) },
            )
        }
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
    myLabel: String,
    partnerLabel: String,
    onSelect: (Audience) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
            selected = audience == Audience.ME,
            onClick = { onSelect(Audience.ME) },
            label = { Text(stringResource(R.string.target_me, myLabel)) },
        )
        if (hasPartner) {
            FilterChip(
                selected = audience == Audience.PARTNER,
                onClick = { onSelect(Audience.PARTNER) },
                label = { Text(stringResource(R.string.target_partner, partnerLabel)) },
            )
            FilterChip(
                selected = audience == Audience.BOTH,
                onClick = { onSelect(Audience.BOTH) },
                label = { Text(stringResource(R.string.target_both, myLabel, partnerLabel)) },
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
    CategoryKind.ALCOHOL -> "1.25"
    CategoryKind.WATER -> "2.5"
    CategoryKind.ROTATION -> ""
    CategoryKind.DINNER_TIME -> "20"
    CategoryKind.MEALS -> ""
    CategoryKind.READING -> "50"
}

@Composable
private fun kindLabel(kind: CategoryKind): String = stringResource(
    when (kind) {
        CategoryKind.STARS -> R.string.kind_stars
        CategoryKind.ALCOHOL -> R.string.kind_alcohol
        CategoryKind.WATER -> R.string.kind_water
        CategoryKind.ROTATION -> R.string.kind_rotation
        CategoryKind.DINNER_TIME -> R.string.kind_dinner
        CategoryKind.MEALS -> R.string.kind_meals
        CategoryKind.READING -> R.string.kind_reading
    },
)

@Composable
private fun targetLabel(
    targets: List<String>,
    uid: String,
    partnerUid: String?,
    myLabel: String,
    partnerLabel: String,
): String = when {
    targets.size > 1 -> stringResource(R.string.target_both, myLabel, partnerLabel)
    targets.firstOrNull() == uid -> stringResource(R.string.target_me, myLabel)
    partnerUid != null && targets.firstOrNull() == partnerUid -> stringResource(R.string.target_partner, partnerLabel)
    else -> stringResource(R.string.target_partner, partnerLabel)
}
