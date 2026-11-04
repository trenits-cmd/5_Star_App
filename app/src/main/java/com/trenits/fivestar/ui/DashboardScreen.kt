package com.trenits.fivestar.ui

import android.app.TimePickerDialog
import android.widget.Toast
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trenits.fivestar.R
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.CircleShape
import java.util.Locale
import java.time.format.TextStyle
import com.trenits.fivestar.domain.DayOutcome
import java.time.ZoneId
import java.time.Duration
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.produceState
import androidx.compose.foundation.background
import com.trenits.fivestar.data.AppClock
import com.trenits.fivestar.domain.CatalogItem
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.CategoryKind
import com.trenits.fivestar.domain.CategorySnapshot
import com.trenits.fivestar.domain.DEFAULT_ALCOHOL_LIMIT
import com.trenits.fivestar.domain.DEFAULT_WATER_GOAL
import com.trenits.fivestar.domain.DEMI
import com.trenits.fivestar.domain.BEDTIME_HOUR
import com.trenits.fivestar.domain.BEDTIME_MINUTE
import com.trenits.fivestar.domain.DINNER_HOUR
import com.trenits.fivestar.domain.DINNER_MINUTE
import com.trenits.fivestar.domain.DayRecord
import com.trenits.fivestar.domain.GLASSES
import com.trenits.fivestar.domain.LITERS
import com.trenits.fivestar.domain.MAX_STARS
import com.trenits.fivestar.domain.MEAL_BREAKFAST
import com.trenits.fivestar.domain.MEAL_DINNER
import com.trenits.fivestar.domain.MEAL_LUNCH
import com.trenits.fivestar.domain.MealLevel
import com.trenits.fivestar.domain.PAGES
import com.trenits.fivestar.domain.PINT
import com.trenits.fivestar.domain.ROTATION_STARS
import com.trenits.fivestar.domain.ROTATION_TASK_INDEX
import com.trenits.fivestar.domain.RelationshipAspect
import com.trenits.fivestar.domain.RotationTask
import com.trenits.fivestar.domain.SPORT_MINUTES
import com.trenits.fivestar.domain.SPORT_WEEKLY_DAYS_GOAL
import com.trenits.fivestar.domain.REWARD_TIER_BY_STREAK
import com.trenits.fivestar.domain.rewardDaysForTier
import com.trenits.fivestar.data.NotificationTargets
import androidx.compose.foundation.lazy.rememberLazyListState
import com.trenits.fivestar.domain.STREAK_DAYS_FOR_REWARD
import com.trenits.fivestar.domain.THRESHOLD_PERCENT
import com.trenits.fivestar.domain.Task
import com.trenits.fivestar.domain.TaskStatus
import com.trenits.fivestar.domain.TaskType
import com.trenits.fivestar.domain.WeightedCategory
import com.trenits.fivestar.domain.DinnerConfirmation
import com.trenits.fivestar.domain.alcoholLiters
import com.trenits.fivestar.domain.autoConfirmAt
import com.trenits.fivestar.domain.availableRotationTasks
import com.trenits.fivestar.domain.buildDayRecord
import com.trenits.fivestar.domain.counterStars
import com.trenits.fivestar.domain.coupleRelationshipScorePercent
import com.trenits.fivestar.domain.relationshipScorePercent
import com.trenits.fivestar.domain.relationshipStarsOf
import com.trenits.fivestar.domain.rotationCountsInWeek
import com.trenits.fivestar.domain.sportDaysInWeek
import com.trenits.fivestar.domain.suggestedRotationTask
import com.trenits.fivestar.domain.waterLiters
import com.trenits.fivestar.domain.weightSharePercent
import com.trenits.fivestar.ui.theme.Amber
import java.time.Instant
import java.time.LocalDate
import com.trenits.fivestar.domain.missingCategoryIds
import com.trenits.fivestar.domain.missingRelationshipAspects

@Composable
fun DashboardScreen(vm: AppViewModel) {
    val targeted by vm.targeted.collectAsState()
    val sortedCategories = remember(targeted) { targeted.sortedByDescending { it.weight } }
    val weightedList = remember(sortedCategories) {
        sortedCategories.map { WeightedCategory(it.id, it.weight) }
    }
    val myName by vm.myName.collectAsState()
    val myPhoto by vm.myPhoto.collectAsState()
    val partnerName by vm.partnerName.collectAsState()
    val partnerPhoto by vm.partnerPhoto.collectAsState()
    val myToday by vm.myToday.collectAsState()
    val partnerToday by vm.partnerToday.collectAsState()
    val partnerUid by vm.partnerUid.collectAsState()
    val members by vm.members.collectAsState()
    val incomingRequest by vm.incomingRequest.collectAsState()
    val days by vm.days.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val catalog by vm.catalog.collectAsState()
    val myCatalog by vm.myCatalog.collectAsState()
    val weekRecordsBeforeToday by vm.isoWeekRecordsBeforeToday.collectAsState()
    val streak by vm.weekSuccessfulDays.collectAsState()
    val weekRewardUnlocked by vm.weekRewardUnlocked.collectAsState()
    val canEditYesterday by vm.canEditYesterday.collectAsState()
    val editingYesterday by vm.editingYesterday.collectAsState()
    val activeDayKey by vm.activeDayKey.collectAsState()
    val activeDinnerConfirmation by vm.activeDinnerConfirmation.collectAsState()
    val allDinnerConfirmations by vm.dinnerConfirmations.collectAsState()
    val pendingDinner = allDinnerConfirmations.items.filter { it.isPending }.maxByOrNull { it.enteredAt }
    val weekDays by vm.weekDays.collectAsState()
    val weekOutcomes by vm.weekOutcomes.collectAsState()
    val currentWeeklyReward by vm.currentWeeklyReward.collectAsState()
    val simulatedToday by vm.simulatedToday.collectAsState()
    val simulatedOffset by vm.simulatedOffset.collectAsState()
    val myPresetRole by vm.myPresetRole.collectAsState()

    val myLabel = myName ?: stringResource(R.string.who_you)
    val partnerLabel = partnerName ?: stringResource(R.string.who_partner)

    val stored = myToday?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
    val myScore = remember(myToday, sortedCategories, stored, activeDayKey, vm.uid) {
        myToday?.scorePercent
            ?: if (sortedCategories.isNotEmpty()) {
                buildDayRecord(activeDayKey, vm.uid, sortedCategories, stored, emptyMap()).scorePercent ?: 0.0
            } else {
                0.0
            }
    }
    val openTasks = tasks.items
        .filter { it.status == TaskStatus.OPEN }
        .sortedWith(compareByDescending<Task> { it.dayKey }.thenByDescending { it.createdAt })

    val listState = rememberLazyListState()
    val dinnerFocus by NotificationTargets.dinnerDay.collectAsState()
    LaunchedEffect(dinnerFocus) {
        if (dinnerFocus == null) return@LaunchedEffect
        val incomingShown = incomingRequest?.takeIf { it.status == "requested" && partnerUid == null } != null
        val waitingShown = !members.fromCache && partnerUid == null
        val emptyPlaceholder = if (sortedCategories.isEmpty()) 1 else 0
        val waitingCardIndex = 5 + (if (incomingShown) 1 else 0) + (if (waitingShown) 1 else 0) + emptyPlaceholder + sortedCategories.size
        listState.animateScrollToItem(waitingCardIndex)
        NotificationTargets.dinnerDay.value = null
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UserAvatar(photoBase64 = myPhoto, name = myLabel, size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${stringResource(R.string.tab_today)} · $myLabel",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        formatDay(activeDayKey),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.shiftSimulatedDays(-1) }) { Text("−") }
                        Text(
                            "TEST ${if (simulatedOffset >= 0) "+" else ""}$simulatedOffset d",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        OutlinedButton(onClick = { vm.shiftSimulatedDays(1) }) { Text("+") }
                    }
                    if (partnerUid != null) {
                        Text(
                            stringResource(R.string.connected_with, partnerLabel),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            if (canEditYesterday) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !editingYesterday,
                        colors = blueChipColors(),
                        onClick = { vm.setEditingYesterday(false) },
                        label = { Text(stringResource(R.string.day_switch_today)) },
                    )
                    FilterChip(
                        selected = editingYesterday,
                        colors = blueChipColors(),
                        onClick = { vm.setEditingYesterday(true) },
                        label = { Text(stringResource(R.string.day_switch_yesterday)) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.day_cutoff_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        incomingRequest?.takeIf { it.status == "requested" && partnerUid == null }?.let { request ->
            item { IncomingRequestCard(vm, request.requestedEmail.orEmpty()) }
        }

        if (!members.fromCache && partnerUid == null) {
            item { WaitingForPartnerCard(vm) }
        }

        // Gesonderte Anzeige 1: Tagesstand
        item {

            ScoreHero(
                score = myScore,
                title = stringResource(R.string.score_for_name, myLabel),
                showCountdown = myPresetRole == "main_user",
            )
        }

        // Relationship as an extra value (not part of the daily status)
        item {
            val myStars = relationshipStarsOf(myToday)
            val myRelScore = relationshipScorePercent(myStars)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.status_relationship_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            formatPercent(myRelScore),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = progressStatusColor(myRelScore ?: 0.0),
                        )
                    }
                    Text(
                        stringResource(R.string.status_relationship_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Gesonderte Anzeige 2: Wochenstand (7-Tage-Serie)
        item {
            WeeklyStatusBanner(
                streak = streak,
                rewardUnlocked = weekRewardUnlocked,
                weekDays = weekDays,
                weekOutcomes = weekOutcomes,
                todayPercent = myScore,
                reward = currentWeeklyReward,
                myUid = vm.uid,
                partnerLabel = partnerLabel,
            )
        }

        // Beziehungs-Tracker an erster Stelle: 5 Dimensionen mit Bewertungen beider Partner
        item { RelationshipTodaySection(vm, editable = true) }

        item { SectionTitle(stringResource(R.string.section_categories, myLabel)) }
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
                        onChange = { key, value, showToast -> vm.setCounter(category.id, key, value, showToast) },
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
                    
                    // Die Partner-Rolle trägt die Abendessen-Uhrzeit ein; main_user kann sie nicht ändern.
                    val canEditTime = myPresetRole == "partner" && activeDinnerConfirmation?.isConfirmed != true
                    DinnerTimeCard(
                        category = category,
                        sharePercent = sharePercent,
                        values = effectiveValues,
                        canEditTime = canEditTime,
                        confirmation = activeDinnerConfirmation,
                        myUid = vm.uid,
                        partnerLabel = partnerLabel,
                        onConfirm = { activeDinnerConfirmation?.let { vm.confirmDinner(it.dayKey) } },
                        onReject = { activeDinnerConfirmation?.let { vm.rejectDinner(it.dayKey) } },
                        onTimeSelected = { h, m -> vm.setDinnerTime(category.id, h, m) },
                    )
                }
                CategoryKind.BEDTIME -> {
                    BedtimeCategoryCard(
                        category = category,
                        sharePercent = sharePercent,
                        values = values,
                        onTimeSelected = { h, m -> vm.setBedtime(category.id, h, m) },
                    )
                }
                CategoryKind.MEALS -> {
                    MealsCategoryCard(
                        category = category,
                        sharePercent = sharePercent,
                        values = values,
                        onMealLevelSelected = { mealKey, level ->
                            vm.setMealLevel(category.id, mealKey, level)
                        },
                    )
                }
                CategoryKind.READING -> {
                    ReadingCategoryCard(
                        category = category,
                        sharePercent = sharePercent,
                        values = values,
                        onPagesChanged = { pages, showToast ->
                            vm.setCounter(category.id, PAGES, pages.toDouble(), showToast)
                        },
                    )
                }
                CategoryKind.SPORT -> {
                    SportCategoryCard(
                        category = category,
                        sharePercent = sharePercent,
                        values = values,
                        weekRecordsBeforeToday = weekRecordsBeforeToday,
                        onMinutesChanged = { minutes, showToast ->
                            vm.setSportMinutes(category.id, minutes, showToast)
                        },
                    )
                }
            }
        }

        item {
            WaitingForConfirmationCard(
                myUid = vm.uid,
                partnerLabel = partnerLabel,
                openTasks = openTasks,
                dinner = pendingDinner,
                onConfirmDinner = { pendingDinner?.let { vm.confirmDinner(it.dayKey) } },
                onRejectDinner = { pendingDinner?.let { vm.rejectDinner(it.dayKey) } },
            )
        }
        item { SectionTitle(stringResource(R.string.section_open_tasks)) }
        if (openTasks.isEmpty()) {
            item { HintCard(stringResource(R.string.no_open_tasks)) }
        }
        items(openTasks, key = { it.id }) { task ->
            @Suppress("UNUSED_EXPRESSION")
            catalog
            TaskCard(
                vm = vm,
                task = task,
                partnerUid = partnerUid,
                myLabel = myLabel,
                partnerLabel = partnerLabel,
            )
        }

        item {
            CatalogOverviewCard(
                ownerLabel = myLabel,
                items = myCatalog,
            )
        }

        item {
            ConfirmTodayCard(
                vm = vm,
                targeted = sortedCategories,
                myToday = myToday,
            )
        }
    }
}

@Composable
private fun ConfirmTodayCard(
    vm: AppViewModel,
    targeted: List<Category>,
    myToday: DayRecord?,
) {
    val missingCategoryNames = missingCategoryIds(targeted, myToday).mapNotNull { id ->
        targeted.firstOrNull { it.id == id }?.let { localizedCategoryName(it) }
    }
    val missingAspects = missingRelationshipAspects(myToday).map { relationshipAspectLabel(it) }
    val missing = missingCategoryNames + missingAspects
    val confirmedAt = myToday?.confirmedAt
    var justSaved by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.confirm_today_title), style = MaterialTheme.typography.titleMedium)
            if (missing.isEmpty()) {
                if (confirmedAt != null) {
                    Text(
                        stringResource(R.string.confirm_today_done, formatTime(Instant.ofEpochMilli(confirmedAt))),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Text(
                    stringResource(R.string.confirm_today_missing_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
                missing.forEach { name ->
                    Text("• $name", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    stringResource(R.string.confirm_today_missing_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (justSaved) {
                Text(
                    stringResource(R.string.confirm_today_saved),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Button(
                onClick = {
                    vm.confirmToday()
                    justSaved = true
                },
                enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (confirmedAt != null) R.string.confirm_today_button_again else R.string.confirm_today_button,
                    ),
                )
            }
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

private fun canonicalPrefixFor(kind: CategoryKind): String = when (kind) {
    CategoryKind.WATER -> "preset_water_"
    CategoryKind.DINNER_TIME -> "preset_dinner_"
    CategoryKind.MEALS -> "preset_meals_"
    CategoryKind.ALCOHOL -> "preset_alcohol_"
    CategoryKind.ROTATION -> "preset_rotation_"
    CategoryKind.SPORT -> "preset_sport_"
    CategoryKind.READING -> "preset_reading_"
    CategoryKind.BEDTIME -> "preset_bedtime_"
    CategoryKind.STARS -> "preset_stars_"
}

/**
 * Zeigt eine einzelne Kategorie im Partner-Tab an (nur deren Kategorien, nicht editierbar).
 * Liest den aktuellen Wert aus der gespeicherten Tagesbilanz und zeigt Stern-/Zählerwert an.
 */
@Composable
private fun PartnerCategorySummaryCard(
    category: Category,
    sharePercent: Int,
    values: Map<String, Double>,
    snapshots: List<CategorySnapshot>,
) {
    val prefix = canonicalPrefixFor(category.kind)
    val stars = when (category.kind) {
        CategoryKind.STARS -> (snapshots.firstOrNull { it.categoryId == category.id }
            ?: snapshots.firstOrNull { it.categoryId.startsWith(prefix) })?.stars ?: 0
        else -> if (values.isNotEmpty()) {
            counterStars(category, values)
        } else {
            (snapshots.firstOrNull { it.categoryId == category.id }
                ?: snapshots.firstOrNull { it.categoryId.startsWith(prefix) })?.stars
                ?: counterStars(category, values)
        }
    }
    val counters = values
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryImage(category = category, size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(localizedCategoryName(category), style = MaterialTheme.typography.titleMedium)
                    CategoryWeightSubtitle(category.weight, sharePercent)
                }
            }
            when (category.kind) {
                CategoryKind.STARS -> StarsRow(stars)
                CategoryKind.WATER, CategoryKind.ALCOHOL -> {
                    if (values.containsKey(LITERS) || values.containsKey(GLASSES) ||
                        values.containsKey(PINT) || values.containsKey(DEMI)
                    ) {
                        CounterSummary(localizedCategoryName(category), values)
                    } else {
                        StarsRow(stars)
                    }
                }
                CategoryKind.DINNER_TIME -> {
                    val hour = counters[DINNER_HOUR]?.toInt() ?: -1
                    val minute = counters[DINNER_MINUTE]?.toInt() ?: 0
                    if (hour >= 0) {
                        Text(
                            stringResource(R.string.dinner_time_current, "%02d:%02d".format(hour, minute)),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text(
                            stringResource(R.string.dinner_time_not_set),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                CategoryKind.BEDTIME -> {
                    val hour = counters[BEDTIME_HOUR]?.toInt() ?: -1
                    val minute = counters[BEDTIME_MINUTE]?.toInt() ?: 0
                    if (hour >= 0) {
                        Text(
                            stringResource(R.string.bedtime_current, "%02d:%02d".format(hour, minute)),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text(
                            stringResource(R.string.dinner_time_not_set),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                CategoryKind.MEALS -> MealsSummaryRow(counters)
                CategoryKind.READING -> {
                    val pages = counters[PAGES]?.toInt() ?: 0
                    if (pages > 0) {
                        Text(
                            stringResource(R.string.reading_pages_summary, pages),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text(
                            stringResource(R.string.dinner_time_not_set),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                CategoryKind.SPORT -> {
                    if (counters.containsKey(SPORT_MINUTES)) {
                        val mins = counters[SPORT_MINUTES]?.toInt() ?: 0
                        Text(
                            stringResource(R.string.sport_minutes_summary, mins),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text(
                            stringResource(R.string.dinner_time_not_set),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                CategoryKind.ROTATION -> RotationSummaryRow(counters)
                else -> StarsRow(stars)
            }
        }
    }
}

@Composable
private fun StarsRow(stars: Int) {
    Text(
        "★".repeat(stars) + "☆".repeat(MAX_STARS - stars),
        color = Amber,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
    )
}

@Composable
private fun MealsSummaryRow(counters: Map<String, Double>) {
    val b = mealLevelLabel(MealLevel.fromPoints(counters[MEAL_BREAKFAST]?.toInt() ?: 0))
    val l = mealLevelLabel(MealLevel.fromPoints(counters[MEAL_LUNCH]?.toInt() ?: 0))
    val d = mealLevelLabel(MealLevel.fromPoints(counters[MEAL_DINNER]?.toInt() ?: 0))
    Text(
        "${stringResource(R.string.meal_breakfast)}: $b · ${stringResource(R.string.meal_lunch)}: $l · ${stringResource(R.string.meal_dinner)}: $d",
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun RotationSummaryRow(counters: Map<String, Double>) {
    val index = counters[ROTATION_TASK_INDEX]?.toInt()
    val stars = counters[ROTATION_STARS]?.toInt() ?: 0
    if (index == null) {
        Text(
            stringResource(R.string.rotation_not_set),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        val task = RotationTask.fromIndex(index)
        val answer = if (stars > 0) stringResource(R.string.rotation_yes) else stringResource(R.string.rotation_no)
        Text(
            "${rotationTaskLabel(task)}: $answer",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CategoryWeightSubtitle(weight: Int, sharePercent: Int) {
    Text(
        stringResource(R.string.weight_with_share, weight, sharePercent),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ScoreHero(score: Double?, title: String, showCountdown: Boolean = false) {
    val effectiveScore = score ?: 0.0
    val progress = (effectiveScore / 100.0).toFloat().coerceIn(0f, 1f)
    val reached = effectiveScore >= THRESHOLD_PERCENT
    val statusColor = progressStatusColor(effectiveScore)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                formatPercent(effectiveScore),
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(if (reached) R.string.status_daily_ok else R.string.status_daily_below),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (showCountdown) {
                DayCountdown(effectiveScore)
            }
        }
    }
}

/**
 * Gesonderte Anzeige für den Wochenstand (7-Tage-Serie bis zur Wochenbelohnung).
 */
@Composable
private fun WeeklyStatusBanner(
    streak: Int,
    rewardUnlocked: Boolean,
    weekDays: List<String>,
    weekOutcomes: Map<String, DayOutcome>,
    todayPercent: Double,
    reward: Task?,
    myUid: String,
    partnerLabel: String,
) {
    val cycleStreak = if (streak > 0 && streak % STREAK_DAYS_FOR_REWARD == 0) {
        STREAK_DAYS_FOR_REWARD
    } else {
        streak % STREAK_DAYS_FOR_REWARD
    }
    val remaining = (STREAK_DAYS_FOR_REWARD - cycleStreak).coerceAtLeast(0)
    val progress = (cycleStreak.toFloat() / STREAK_DAYS_FOR_REWARD.toFloat()).coerceIn(0f, 1f)
    val onCard = MaterialTheme.colorScheme.onSecondaryContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${stringResource(R.string.status_weekly_title)}\n${stringResource(R.string.status_weekly_progress, cycleStreak, STREAK_DAYS_FOR_REWARD)}",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = onCard,
                textAlign = TextAlign.Center,
                softWrap = true,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
                color = Amber,
            )
            Text(
                text = if (rewardUnlocked) {
                    stringResource(R.string.week_reward_unlocked)
                } else {
                    stringResource(R.string.week_no_reward, remaining)
                },
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = onCard,
                textAlign = TextAlign.Center,
                softWrap = true,
            )

            Text(
                stringResource(R.string.week_last_seven),
                style = MaterialTheme.typography.labelLarge,
                color = onCard,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                weekDays.forEach { key ->
                    val outcome = weekOutcomes[key]
                    val dot = when (outcome) {
                        DayOutcome.REWARD -> StatusGreen
                        DayOutcome.MISSED -> StatusRed
                        null -> MaterialTheme.colorScheme.outline
                    }
                    val dayName = runCatching { LocalDate.parse(key).dayOfWeek }.getOrNull()
                        ?.getDisplayName(TextStyle.SHORT, Locale.getDefault())?.take(2).orEmpty()
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(22.dp).clip(CircleShape).background(dot))
                        Spacer(Modifier.height(2.dp))
                        Text(dayName, style = MaterialTheme.typography.labelSmall, color = onCard)
                    }
                }
            }

            val todayText = if (todayPercent >= THRESHOLD_PERCENT) {
                stringResource(R.string.week_today_reached, formatPercent(todayPercent))
            } else {
                stringResource(
                    R.string.week_today_needed,
                    formatPercent(todayPercent),
                    (THRESHOLD_PERCENT - todayPercent).roundToInt(),
                )
            }
            Text(todayText, style = MaterialTheme.typography.bodySmall, color = onCard, textAlign = TextAlign.Center)

            val rewardText = when {
                reward == null -> stringResource(R.string.week_reward_none)
                reward.isArchived -> stringResource(R.string.week_reward_redeemed, formatDay(reward.redeemedDayKey ?: reward.dayKey))
                reward.title.isBlank() -> stringResource(R.string.week_reward_choose)
                reward.ownerUid == myUid -> stringResource(
                    R.string.week_reward_waiting_owner,
                    partnerLabel,
                    localizedCatalogTitle(reward.title),
                    formatTime(reward.autoConfirmAt),
                )
                else -> stringResource(
                    R.string.week_reward_confirm,
                    localizedCatalogTitle(reward.title),
                    formatTime(reward.autoConfirmAt),
                )
            }
            Text(rewardText, style = MaterialTheme.typography.bodySmall, color = onCard, textAlign = TextAlign.Center)
            reward?.takeIf { it.tier > 0 }?.let { r -> Text(stringResource(R.string.reward_tier_label, r.tier, rewardDaysForTier(r.tier)), style = MaterialTheme.typography.labelLarge, color = onCard, textAlign = TextAlign.Center) }
            REWARD_TIER_BY_STREAK.keys.filter { it > streak }.minOrNull()?.let { milestone ->
                Text(
                    stringResource(
                        R.string.week_next_reward,
                        milestone - streak,
                        REWARD_TIER_BY_STREAK.getValue(milestone),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = onCard,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                stringResource(R.string.reward_tiers_hint),
                style = MaterialTheme.typography.labelSmall,
                color = onCard,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.week_evaluation_note),
                style = MaterialTheme.typography.labelSmall,
                color = onCard,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Gesonderte Anzeige der täglichen Beziehungs-Stimmung im Dashboard
 * (alle 5 Punkte gleich gewichtet, 0–5 Sterne).
 */
@Composable
private fun RelationshipMoodBanner(
    myRecord: DayRecord?,
    partnerRecord: DayRecord?,
    myLabel: String,
    partnerLabel: String,
    partnerPhoto: String?,
    hasPartner: Boolean,
) {
    val myStars = relationshipStarsOf(myRecord)
    val partnerStars = relationshipStarsOf(partnerRecord)
    val myScore = relationshipScorePercent(myStars)
    val partnerScore = relationshipScorePercent(partnerStars)
    val coupleScore = coupleRelationshipScorePercent(myStars, partnerStars)
    val avgStars = (coupleScore ?: 0.0) * MAX_STARS / 100.0
    val progress = ((coupleScore ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.status_relationship_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(R.string.status_relationship_summary, avgStars, formatPercent(coupleScore)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
                color = Amber,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.relationship_score_you, myLabel, formatPercent(myScore)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (hasPartner) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(photoBase64 = partnerPhoto, name = partnerLabel, size = 20.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.relationship_score_partner, partnerLabel, formatPercent(partnerScore)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomingRequestCard(vm: AppViewModel, requestedEmail: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.invite_request_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.invite_request_body, requestedEmail.ifBlank { "?" }),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { vm.confirmIncomingRequest() },
                    enabled = !vm.busy,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.invite_confirm)) }
                OutlinedButton(
                    onClick = { vm.declineIncomingRequest() },
                    enabled = !vm.busy,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.invite_decline)) }
            }
        }
    }
}

@Composable
private fun WaitingForPartnerCard(vm: AppViewModel) {
    val context = LocalContext.current
    var partnerEmail by rememberSaveable { mutableStateOf("") }
    val code = vm.inviteCode

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.waiting_partner), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.invite_partner_removed),
                style = MaterialTheme.typography.bodySmall,
            )
            code?.let { InviteCodeRow(context, it) }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = partnerEmail,
                onValueChange = { partnerEmail = it },
                label = { Text(stringResource(R.string.invite_email_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = {
                    vm.createNewInvite(partnerEmail) { newCode ->
                        launchInviteEmail(context, partnerEmail, newCode)
                    }
                },
                enabled = !vm.busy && partnerEmail.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.invite_send_email))
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = { vm.createNewInvite() },
                enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.invite_new_code))
            }
        }
    }
}

/** Einladungscode mit Kopierknopf. */
@Composable
internal fun InviteCodeRow(context: Context, code: String) {
    Spacer(Modifier.height(8.dp))
    Text(
        code,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    Text(stringResource(R.string.invite_share), style = MaterialTheme.typography.bodySmall)
    OutlinedButton(
        onClick = {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            clipboard?.setPrimaryClip(ClipData.newPlainText("invite", code))
            Toast.makeText(context, context.getString(R.string.invite_code_copied), Toast.LENGTH_SHORT).show()
        },
    ) { Text(stringResource(R.string.invite_copy_code)) }
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

/**
 * Karte für die Kategorie "Essen" (Frühstück, Mittagessen, Abendessen: nichts, wenig, mittel, gesund).
 * Wenn bei allen 3 "gesund" herauskommt, gibt es 5 Sterne, ansonsten entsprechend weniger.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealsCategoryCard(
    category: Category,
    sharePercent: Int,
    values: Map<String, Double>,
    onMealLevelSelected: (String, MealLevel) -> Unit,
) {
    val stars = counterStars(category, values)
    val meals = listOf(
        MEAL_BREAKFAST to stringResource(R.string.meal_breakfast),
        MEAL_LUNCH to stringResource(R.string.meal_lunch),
        MEAL_DINNER to stringResource(R.string.meal_dinner),
    )

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
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.meals_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            meals.forEach { (mealKey, mealLabel) ->
                val currentLevel = MealLevel.fromPoints(values[mealKey]?.toInt() ?: 0)
                Text(
                    mealLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MealLevel.entries.forEach { level ->
                        FilterChip(
                            selected = currentLevel == level,
                            colors = blueChipColors(),
                            onClick = { onMealLevelSelected(mealKey, level) },
                            label = { Text(mealLevelLabel(level)) },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(4.dp))
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

/**
 * Karte für die Kategorie "Lesen" (Seitenanzahl):
 * 0–5 Seiten = 0 ★, > 50 Seiten = 5 ★, 6–50 Seiten in Relation (1–4 ★).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadingCategoryCard(
    category: Category,
    sharePercent: Int,
    values: Map<String, Double>,
    onPagesChanged: (Int, Boolean) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val hasEntry = values.containsKey(PAGES)
    val pages = (values[PAGES] ?: 0.0).toInt().coerceAtLeast(0)
    val stars = counterStars(category, values)
    var text by rememberSaveable(category.id) { mutableStateOf(if (!hasEntry && pages == 0) "" else pages.toString()) }
    var isFocused by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }

    LaunchedEffect(pages, hasEntry, isFocused) {
        if (!isFocused) {
            text = if (!hasEntry && pages == 0) "" else pages.toString()
        }
    }

    val commitCurrentText: (Boolean) -> Unit = { showToast ->
        val parsed = text.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
        text = parsed.toString()
        onPagesChanged(parsed, showToast)
    }

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
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.reading_rules_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            CounterRow(
                label = stringResource(R.string.reading_pages_summary, pages),
                value = pages.toDouble(),
                step = 1.0,
            ) { newVal ->
                val updated = newVal.toInt().coerceAtLeast(0)
                text = updated.toString()
                onPagesChanged(updated, true)
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        text = input
                        if (input.isBlank()) {
                            onPagesChanged(0, false)
                        } else {
                            input.trim().toIntOrNull()?.let { onPagesChanged(it.coerceAtLeast(0), false) }
                        }
                    },
                    label = { Text(stringResource(R.string.reading_pages_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            commitCurrentText(true)
                            focusManager.clearFocus()
                        },
                    ),
                    modifier = Modifier.width(120.dp)
                        .onFocusChanged { state ->
                            isFocused = state.isFocused
                            if (state.isFocused) {
                                hadFocus = true
                            } else if (hadFocus) {
                                hadFocus = false
                                if (text.isNotBlank()) {
                                    commitCurrentText(true)
                                }
                            }
                        },
                )
                Button(
                    onClick = {
                        commitCurrentText(true)
                        focusManager.clearFocus()
                    },
                ) {
                    Text(stringResource(R.string.settings_save))
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

/**
 * Karte für die Kategorie "Sport" (Andi: 3×/Woche je mindestens 45 Minuten, tägliche Zeiteingabe).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SportCategoryCard(
    category: Category,
    sharePercent: Int,
    values: Map<String, Double>,
    weekRecordsBeforeToday: List<DayRecord>,
    onMinutesChanged: (Int, Boolean) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val hasEntry = values.containsKey(SPORT_MINUTES)
    val minutes = (values[SPORT_MINUTES] ?: 0.0).toInt().coerceAtLeast(0)
    val stars = counterStars(category, values)
    val doneBeforeToday = remember(category.id, weekRecordsBeforeToday) {
        sportDaysInWeek(category.id, weekRecordsBeforeToday)
    }
    val doneThisWeek = doneBeforeToday + if (minutes >= 45) 1 else 0
    var text by rememberSaveable(category.id) {
        mutableStateOf(if (!hasEntry) "" else minutes.toString())
    }
    var isFocused by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }

    LaunchedEffect(minutes, hasEntry, isFocused) {
        if (!isFocused) {
            text = if (!hasEntry) "" else minutes.toString()
        }
    }

    val commitCurrentMinutes: (Boolean) -> Unit = { showToast ->
        val parsed = text.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
        text = parsed.toString()
        onMinutesChanged(parsed, showToast)
    }

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
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.sport_rules_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.sport_weekly_progress, doneThisWeek, SPORT_WEEKLY_DAYS_GOAL),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = hasEntry && minutes == 0,
                    colors = blueChipColors(),
                    onClick = {
                        text = "0"
                        onMinutesChanged(0, true)
                    },
                    label = { Text(stringResource(R.string.sport_quick_0)) },
                )
                FilterChip(
                    selected = hasEntry && minutes == 30,
                    colors = blueChipColors(),
                    onClick = {
                        text = "30"
                        onMinutesChanged(30, true)
                    },
                    label = { Text(stringResource(R.string.sport_quick_30)) },
                )
                FilterChip(
                    selected = hasEntry && minutes == 45,
                    colors = blueChipColors(),
                    onClick = {
                        text = "45"
                        onMinutesChanged(45, true)
                    },
                    label = { Text(stringResource(R.string.sport_quick_45)) },
                )
                FilterChip(
                    selected = hasEntry && minutes == 60,
                    colors = blueChipColors(),
                    onClick = {
                        text = "60"
                        onMinutesChanged(60, true)
                    },
                    label = { Text(stringResource(R.string.sport_quick_60)) },
                )
            }
            Spacer(Modifier.height(6.dp))
            CounterRow(
                label = stringResource(R.string.sport_minutes_summary, minutes),
                value = minutes.toDouble(),
                step = 1.0,
            ) { newVal ->
                val updated = newVal.toInt().coerceAtLeast(0)
                text = updated.toString()
                onMinutesChanged(updated, true)
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        text = input
                        if (input.isBlank()) {
                            onMinutesChanged(0, false)
                        } else {
                            input.trim().toIntOrNull()?.let { onMinutesChanged(it.coerceAtLeast(0), false) }
                        }
                    },
                    label = { Text(stringResource(R.string.sport_minutes_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            commitCurrentMinutes(true)
                            focusManager.clearFocus()
                        },
                    ),
                    modifier = Modifier.width(120.dp)
                        .onFocusChanged { state ->
                            isFocused = state.isFocused
                            if (state.isFocused) {
                                hadFocus = true
                            } else if (hadFocus) {
                                hadFocus = false
                                if (text.isNotBlank()) {
                                    commitCurrentMinutes(true)
                                }
                            }
                        },
                )
                Button(
                    onClick = {
                        commitCurrentMinutes(true)
                        focusManager.clearFocus()
                    },
                ) {
                    Text(stringResource(R.string.settings_save))
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
private fun CounterCard(
    category: Category,
    sharePercent: Int,
    values: Map<String, Double>,
    onChange: (String, Double, Boolean) -> Unit,
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
                CounterRow(stringResource(R.string.counter_demi), values[DEMI] ?: 0.0, step = 1.0) {
                    onChange(DEMI, it, true)
                }
                CounterRow(stringResource(R.string.counter_pint), values[PINT] ?: 0.0, step = 1.0) {
                    onChange(PINT, it, true)
                }
                LitersField(
                    categoryId = category.id,
                    value = values[LITERS] ?: 0.0,
                    onChange = { liters, showToast -> onChange(LITERS, liters, showToast) },
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
                    onChange(GLASSES, it, true)
                }
                Spacer(Modifier.height(12.dp))
                LitersField(
                    categoryId = category.id,
                    value = values[LITERS] ?: 0.0,
                    onChange = { liters, showToast -> onChange(LITERS, liters, showToast) },
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
        suggestedRotationTask(AppClock.today().dayOfWeek, available)
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
                        colors = blueChipColors(),
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
                val answered = values.containsKey(ROTATION_STARS)
                FilterChip(
                    selected = answered && stars == MAX_STARS,
                    colors = blueChipColors(),
                    onClick = { onSelectAndRate(selectedTask, MAX_STARS) },
                    label = { Text(stringResource(R.string.rotation_yes)) },
                )
                Spacer(Modifier.width(6.dp))
                FilterChip(
                    selected = answered && stars == 0,
                    colors = blueChipColors(),
                    onClick = { onSelectAndRate(selectedTask, 0) },
                    label = { Text(stringResource(R.string.rotation_no)) },
                )
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
    confirmation: DinnerConfirmation?,
    myUid: String,
    partnerLabel: String,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
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
            confirmation?.let { dc ->
                Spacer(Modifier.height(4.dp))
                when {
                    dc.isRejected && dc.enteredBy == myUid -> Text(
                        stringResource(R.string.dinner_rejected_partner, partnerLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    dc.isRejected -> Text(
                        stringResource(R.string.dinner_rejected_you, partnerLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    dc.isPending && dc.enteredBy != myUid -> {
                        Text(
                            stringResource(R.string.task_deadline_dinner, formatTime(dc.deadline)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.dinner_confirm_button))
                        }
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(onClick = onReject, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.dinner_reject_button))
                        }
                    }
                    dc.isPending -> Text(
                        stringResource(R.string.dinner_waiting, partnerLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    dc.autoConfirmed -> Text(
                        stringResource(R.string.dinner_auto_confirmed),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    else -> Text(
                        stringResource(R.string.dinner_confirmed_by, partnerLabel),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
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

/** Schlafenszeit: Uhrzeit eintragen. Vor Mitternacht 5 Sterne, bis 3 Uhr abnehmend, danach 0. */
/** Ausgewählte Auswahl-Buttons in den Kategorien: blau hinterlegt. */
internal val SelectedChipBlue = Color(0xFF1565C0)

@Composable
internal fun blueChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = SelectedChipBlue,
    selectedLabelColor = Color.White,
)

@Composable
private fun BedtimeCategoryCard(
    category: Category,
    sharePercent: Int,
    values: Map<String, Double>,
    onTimeSelected: (Int, Int) -> Unit,
) {
    val context = LocalContext.current
    val hour = values[BEDTIME_HOUR]?.toInt() ?: -1
    val minute = values[BEDTIME_MINUTE]?.toInt() ?: 0
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
            Spacer(Modifier.height(8.dp))
            var selectedBedtime by remember { mutableStateOf<Pair<Int, Int>?>(null) }
            LaunchedEffect(hour, minute) {
                if (hour >= 0) selectedBedtime = Pair(hour, minute)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedBedtime == Pair(23, 30),
                    colors = blueChipColors(),
                    onClick = { onTimeSelected(23, 30); selectedBedtime = Pair(23, 30) },
                    label = { Text(stringResource(R.string.bedtime_quick_2330)) },
                )
                FilterChip(
                    selected = selectedBedtime == Pair(0, 30),
                    colors = blueChipColors(),
                    onClick = { onTimeSelected(0, 30); selectedBedtime = Pair(0, 30) },
                    label = { Text(stringResource(R.string.bedtime_quick_0030)) },
                )
                FilterChip(
                    selected = selectedBedtime == Pair(1, 30),
                    colors = blueChipColors(),
                    onClick = { onTimeSelected(1, 30); selectedBedtime = Pair(1, 30) },
                    label = { Text(stringResource(R.string.bedtime_quick_0130)) },
                )
                FilterChip(
                    selected = selectedBedtime == Pair(2, 30),
                    colors = blueChipColors(),
                    onClick = { onTimeSelected(2, 30); selectedBedtime = Pair(2, 30) },
                    label = { Text(stringResource(R.string.bedtime_quick_0230)) },
                )
                FilterChip(
                    selected = selectedBedtime == Pair(3, 30),
                    colors = blueChipColors(),
                    onClick = { onTimeSelected(3, 30); selectedBedtime = Pair(3, 30) },
                    label = { Text(stringResource(R.string.bedtime_quick_after3)) },
                )
                Button(
                    onClick = {
                        val initH = if (hour >= 0) hour else 23
                        val initM = if (hour >= 0) minute else 0
                        TimePickerDialog(context, { _, h, m -> onTimeSelected(h, m) }, initH, initM, true).show()
                    },
                ) { Text(stringResource(R.string.dinner_pick_time)) }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.counter_rating, "★".repeat(stars) + "☆".repeat(MAX_STARS - stars), stars),
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
private fun LitersField(categoryId: String, value: Double, onChange: (Double, Boolean) -> Unit) {
    val focusManager = LocalFocusManager.current
    var text by rememberSaveable(categoryId) { mutableStateOf(if (value == 0.0) "" else value.toString()) }
    var isFocused by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }

    LaunchedEffect(value, isFocused) {
        if (!isFocused) {
            text = if (value == 0.0) "" else value.toString()
        }
    }

    val commitLiters: (Boolean) -> Unit = { showToast ->
        val parsed = text.trim().replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        text = if (parsed == 0.0) "" else parsed.toString()
        onChange(parsed, showToast)
    }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                text = input
                if (input.isBlank()) {
                    onChange(0.0, false)
                } else {
                    input.trim().replace(',', '.').toDoubleOrNull()?.let { onChange(it.coerceAtLeast(0.0), false) }
                }
            },
            label = { Text(stringResource(R.string.counter_liters)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    commitLiters(true)
                    focusManager.clearFocus()
                },
            ),
            modifier = Modifier.width(120.dp)
                .onFocusChanged { state ->
                    isFocused = state.isFocused
                    if (state.isFocused) {
                        hadFocus = true
                    } else if (hadFocus) {
                        hadFocus = false
                        if (text.isNotBlank()) {
                            commitLiters(true)
                        }
                    }
                },
        )
        Button(
            onClick = {
                commitLiters(true)
                focusManager.clearFocus()
            },
        ) {
            Text(stringResource(R.string.settings_save))
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
private fun PartnerCard(
    partnerLabel: String,
    partnerPhoto: String?,
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
                UserAvatar(photoBase64 = partnerPhoto, name = partnerLabel, size = 36.dp)
                Spacer(Modifier.width(10.dp))
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
    val hintText = stringResource(R.string.readonly_hint)
    val hintContext = LocalContext.current
    val myName by vm.myName.collectAsState()
    val partnerName by vm.partnerName.collectAsState()
    val partnerPhoto by vm.partnerPhoto.collectAsState()
    val myToday by vm.myToday.collectAsState()
    val partnerToday by vm.partnerToday.collectAsState()
    val days by vm.days.collectAsState()
    val partnerUid by vm.partnerUid.collectAsState()
    val allCategories by vm.categories.collectAsState()
    val myPresetRole by vm.myPresetRole.collectAsState()
    val tasks by vm.tasks.collectAsState()

    val myLabel = myName ?: stringResource(R.string.who_you)
    val partnerLabel = partnerName ?: stringResource(R.string.who_partner)

    val pUid = partnerUid ?: partnerToday?.ownerUid ?: vm.uid
    val partnerIsPartnerSet = myPresetRole == "main_user"
    val allCategoriesState by vm.categories.collectAsState()
    val partnerCategories = remember(pUid, partnerIsPartnerSet, allCategoriesState) {
        vm.categoriesFor(pUid, partnerIsPartnerSet)
    }
    val partnerWeightedList = remember(partnerCategories) {
        partnerCategories.map { WeightedCategory(it.id, it.weight) }
    }
    val partnerCatalog = remember(pUid, partnerIsPartnerSet) {
        vm.canonicalCatalogFor(pUid, partnerIsPartnerSet)
    }
    val partnerOpenTasks = tasks.items
        .filter { it.status == TaskStatus.OPEN }
        .sortedWith(compareByDescending<Task> { it.dayKey }.thenByDescending { it.createdAt })
    // Nur der Tageseintrag des Partners zählt. Ohne Eintrag bleibt die Anzeige bei 0 % (nie die eigenen Werte).
    val partnerStored = partnerToday?.snapshots.orEmpty()
    val rawPartnerCounters = partnerToday?.counters.orEmpty()
    val partnerCounters = remember(rawPartnerCounters, partnerCategories) {
        val mutable = mutableMapOf<String, Map<String, Double>>()
        partnerCategories.forEach { cat ->
            val prefix = canonicalPrefixFor(cat.kind)
            val fromPartner = rawPartnerCounters[cat.id]
                ?: rawPartnerCounters.entries.firstOrNull { it.key.startsWith(prefix) }?.value
            if (fromPartner != null) mutable[cat.id] = fromPartner
        }
        mutable
    }
    val partnerScore = remember(partnerToday, partnerStored, partnerCategories, partnerCounters, pUid) {
        if (partnerToday == null || partnerCategories.isEmpty()) {
            0.0
        } else {
            val storedMap = partnerCategories.associate { cat ->
                val prefix = canonicalPrefixFor(cat.kind)
                val snap = partnerStored.firstOrNull { it.categoryId == cat.id }
                    ?: partnerStored.firstOrNull { it.categoryId.startsWith(prefix) }
                cat.id to (snap?.stars ?: 0)
            }
            buildDayRecord(
                dayKey = partnerToday?.dayKey ?: "",
                ownerUid = pUid,
                categories = partnerCategories,
                stars = storedMap,
                counters = partnerCounters,
            ).scorePercent
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().readOnlyHint { Toast.makeText(hintContext, hintText, Toast.LENGTH_SHORT).show() },
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(photoBase64 = partnerPhoto, name = partnerLabel, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(partnerLabel, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        stringResource(R.string.connected_with, partnerLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        item {
            ScoreHero(
                score = partnerScore,
                title = stringResource(R.string.score_for_name, partnerLabel),
            )
        }
        // Beziehungs-Tracker (Read-Only, mit 7-Tage-Verlauf) oben im Partner-Tab
        item { RelationshipTodaySection(vm, editable = false) }
        item { SectionTitle(stringResource(R.string.section_categories, partnerLabel)) }
        if (partnerCategories.isEmpty()) {
            item { HintCard(stringResource(R.string.categories_empty)) }
        }
        items(partnerCategories, key = { it.id }) { category ->
            PartnerCategorySummaryCard(
                category = category,
                sharePercent = weightSharePercent(category.weight, partnerWeightedList),
                values = partnerCounters[category.id].orEmpty(),
                snapshots = partnerStored,
            )
        }
        if (partnerOpenTasks.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.section_open_tasks)) }
            items(partnerOpenTasks, key = { it.id }) { task ->
                ReadOnlyTaskSummary(task = task, who = if (task.ownerUid == vm.uid) myLabel else partnerLabel)
            }
        }
        item {
            CatalogOverviewCard(
                ownerLabel = partnerLabel,
                items = partnerCatalog,
            )
        }
    }
}

@Composable
internal fun CatalogOverviewCard(
    ownerLabel: String,
    items: List<CatalogItem>,
) {
    var expanded by rememberSaveable(ownerLabel) { mutableStateOf(true) }
    val rewards = remember(items) { items.filter { it.type == TaskType.REWARD } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${stringResource(R.string.settings_rewards)} ($ownerLabel)",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.archive_tap_details),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (expanded) {
                Text(
                    stringResource(R.string.week_reward_pool),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (rewards.isEmpty()) {
                    Text(stringResource(R.string.catalog_item_empty_rewards), style = MaterialTheme.typography.bodySmall)
                } else {
                    rewards.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("• ", style = MaterialTheme.typography.bodySmall)
                            Text(localizedCatalogTitle(item.title) + if (item.tier > 0) " · " + stringResource(R.string.reward_tier_label, item.tier, rewardDaysForTier(item.tier)) else "", style = MaterialTheme.typography.bodySmall)
                        }
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
internal fun TaskCard(
    vm: AppViewModel,
    task: Task,
    partnerUid: String?,
    myLabel: String,
    partnerLabel: String,
) {
    val isMine = task.ownerUid == vm.uid
    val (availableOptions, totalPoolSize) = vm.availableChoicesFor(task)
    var dropdownExpanded by remember(task.id) { mutableStateOf(false) }
    var selectedOption by remember(task.id, task.title, availableOptions) {
        mutableStateOf(
            if (task.title.isNotBlank()) {
                task.title
            } else {
                availableOptions.firstOrNull().orEmpty()
            },
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            val whoText = if (isMine) myLabel else partnerLabel
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
                stringResource(R.string.archive_reward_date, formatDay(task.dayKey)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                when {
                    task.targets.size > 1 -> stringResource(R.string.target_both, myLabel, partnerLabel)
                    task.targets.firstOrNull() == vm.uid -> stringResource(R.string.target_me, myLabel)
                    partnerUid != null && task.targets.firstOrNull() == partnerUid ->
                        stringResource(R.string.target_partner, partnerLabel)
                    else -> stringResource(R.string.target_partner, partnerLabel)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (task.tier > 0) {
                Text(
                    stringResource(R.string.reward_tier_label, task.tier, rewardDaysForTier(task.tier)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(8.dp))

            if (task.title.isEmpty() && !isMine) {
                Text(
                    stringResource(R.string.task_partner_chooses, partnerLabel),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else if (task.title.isEmpty()) {
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
            } else {
                Text(
                    stringResource(R.string.task_partner_label, localizedCatalogTitle(task.title)),
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.task_deadline, formatTime(task.autoConfirmAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isMine) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.task_waiting_confirm, localizedCatalogTitle(task.title), partnerLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { vm.confirmTask(task.id, task.title) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.task_confirm))
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { vm.postponeTask(task.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.task_not_yet, task.postponeCount))
                    }
                }
            }
        }
    }
}

private val StatusRed = Color(0xFFC62828)
private val StatusYellow = Color(0xFFF9A825)
private val StatusGreen = Color(0xFF2E7D32)

/** Rot 0–69 %, gelb 70–99 %, grün 100 % (wie im Tagesstatus). */
internal fun statusGradeColor(percent: Double): Color = when {
    percent >= THRESHOLD_PERCENT -> StatusGreen
    else -> lerp(StatusRed, StatusYellow, (percent / THRESHOLD_PERCENT).toFloat().coerceIn(0f, 1f))
}

private fun progressStatusColor(percent: Double): Color = statusGradeColor(percent)

/** Sekunden bis 0 Uhr (nächste Mitternacht in der Gerätezeitzone), basierend auf der App-Uhr. */
private fun secondsUntilMidnight(): Long {
    val zone = ZoneId.systemDefault()
    val now = AppClock.now().atZone(zone)
    val midnight = now.toLocalDate().plusDays(1).atStartOfDay(zone)
    return Duration.between(now, midnight).seconds.coerceAtLeast(0)
}

private fun formatCountdown(total: Long): String =
    "%02d:%02d:%02d".format(total / 3600, (total % 3600) / 60, total % 60)

/** Countdown bis 0 Uhr mit Warnfarbe je nach Tagesstand: unter 70 % rot, 70–99 % gelb, 100 % grün. */
@Composable
private fun DayCountdown(score: Double) {
    val seconds by produceState(initialValue = secondsUntilMidnight()) {
        while (true) {
            value = secondsUntilMidnight()
            delay(1000)
        }
    }
    val background: Color
    val content: Color
    val message: String
    when {
        score >= 100.0 -> {
            background = StatusGreen
            content = Color.White
            message = stringResource(R.string.countdown_perfect)
        }
        score >= 70.0 -> {
            background = StatusYellow
            content = Color.Black
            message = stringResource(R.string.countdown_almost)
        }
        else -> {
            background = StatusRed
            content = Color.White
            message = stringResource(R.string.countdown_behind)
        }
    }
    Spacer(Modifier.height(12.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.countdown_label), style = MaterialTheme.typography.bodySmall, color = content)
            Text(
                formatCountdown(seconds),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = content,
            )
        }
        Text(message, style = MaterialTheme.typography.bodySmall, color = content)
    }
}

/** Tipp auf eine Stelle ohne Aktion: zeigt den Hinweis "nur Ansicht". Blockiert keine Eingaben. */
private fun Modifier.readOnlyHint(onTap: () -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
        if (up != null && (up.position - down.position).getDistance() < 24f) onTap()
    }
}

/** Nur-Lese-Zusammenfassung einer offenen Strafe/Belohnung (ohne Aktionen). */
@Composable
private fun ReadOnlyTaskSummary(task: Task, who: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "$who · ${typeLabel(task.type)}",
                style = MaterialTheme.typography.titleSmall,
                color = if (task.type == TaskType.REWARD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Text(
                if (task.title.isBlank()) stringResource(R.string.no_action_chosen) else localizedCatalogTitle(task.title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                statusLabel(task.status),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Was wartet auf eine Bestätigung: in beide Richtungen, mit Frist. */
@Composable
private fun WaitingForConfirmationCard(
    myUid: String,
    partnerLabel: String,
    openTasks: List<Task>,
    dinner: DinnerConfirmation?,
    onConfirmDinner: () -> Unit,
    onRejectDinner: () -> Unit,
) {
    val rows = buildList {
        dinner?.takeIf { it.isPending }?.let { dc ->
            val time = "%02d:%02d".format(dc.hour, dc.minute)
            add(
                if (dc.enteredBy == myUid) {
                    stringResource(R.string.waiting_dinner_partner, partnerLabel, time, formatTime(dc.deadline))
                } else {
                    stringResource(R.string.waiting_dinner_you, time, formatTime(dc.deadline))
                },
            )
        }
        openTasks.filter { it.title.isNotBlank() }.forEach { task ->
            val action = localizedCatalogTitle(task.title)
            add(
                if (task.ownerUid == myUid) {
                    stringResource(R.string.waiting_task_partner, partnerLabel, action, formatTime(task.autoConfirmAt))
                } else {
                    stringResource(R.string.waiting_task_you, action, formatTime(task.autoConfirmAt))
                },
            )
        }
    }
    if (rows.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.waiting_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            rows.forEach { row ->
                Text(row, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            if (dinner != null && dinner.isPending && dinner.enteredBy != myUid) {
                Button(onClick = onConfirmDinner, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.dinner_confirm_button))
                }
                OutlinedButton(onClick = onRejectDinner, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.dinner_reject_button))
                }
            }
        }
    }
}
