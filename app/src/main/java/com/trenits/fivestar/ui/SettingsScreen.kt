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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
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

    var partnerNameInput by rememberSaveable { mutableStateOf(partnerName.orEmpty()) }

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
                    val account = Firebase.auth.currentUser
                    Text(
                        stringResource(
                            R.string.account_signed_in_as,
                            account?.email ?: account?.uid?.take(8).orEmpty(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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

        // 1b. Partner name (chosen by the user, shown as variable everywhere)
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(stringResource(R.string.settings_partner_name), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.settings_partner_name_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = partnerNameInput,
                        onValueChange = { partnerNameInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { vm.savePartnerDisplayName(partnerNameInput) },
                        enabled = partnerNameInput.isNotBlank() && !vm.busy,
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

        // 7. Rewards & penalties overview (read-only; managed in the Partner tab)
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(stringResource(R.string.settings_rewards), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.settings_rewards_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val myRewards = catalog.filter { it.type == TaskType.REWARD && it.appliesTo(vm.uid) }
                    if (myRewards.isEmpty()) {
                        Text(stringResource(R.string.catalog_item_empty_rewards), style = MaterialTheme.typography.bodySmall)
                    } else {
                        myRewards.forEach { item ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("• ", style = MaterialTheme.typography.bodySmall)
                                Text(localizedCatalogTitle(item.title), style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    targetLabel(item.targets, vm.uid, partnerUid, myLabel, partnerLabel),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_penalties), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.settings_penalties_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val myPenalties = catalog.filter { it.type == TaskType.PENALTY && it.appliesTo(vm.uid) }
                    if (myPenalties.isEmpty()) {
                        Text(stringResource(R.string.catalog_item_empty_penalties), style = MaterialTheme.typography.bodySmall)
                    } else {
                        myPenalties.forEach { item ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("• ", style = MaterialTheme.typography.bodySmall)
                                Text(localizedCatalogTitle(item.title), style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    targetLabel(item.targets, vm.uid, partnerUid, myLabel, partnerLabel),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
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
    CategoryKind.ALCOHOL -> "2.5"
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
