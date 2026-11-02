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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
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
import com.trenits.fivestar.domain.STREAK_DAYS_FOR_REWARD
import com.trenits.fivestar.domain.THRESHOLD_PERCENT
import com.trenits.fivestar.domain.Task
import com.trenits.fivestar.domain.TaskStatus
import com.trenits.fivestar.domain.TaskType
import com.trenits.fivestar.domain.WeightedCategory
import com.trenits.fivestar.domain.alcoholLiters
import com.trenits.fivestar.domain.availableRotationTasks
import com.trenits.fivestar.domain.counterStars
import com.trenits.fivestar.domain.coupleRelationshipScorePercent
import com.trenits.fivestar.domain.relationshipScorePercent
import com.trenits.fivestar.domain.relationshipStarsOf
import com.trenits.fivestar.domain.rotationCountsInWeek
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
    val weekRecordsBeforeToday by vm.isoWeekRecordsBeforeToday.collectAsState()
    val streak by vm.weekSuccessfulDays.collectAsState()
    val weekRewardUnlocked by vm.weekRewardUnlocked.collectAsState()

    // Toast-Bestätigung nach erfolgreichem Speichern
    val toast by vm.toastMessage.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(toast) {
        if (toast != null) {
            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
        }
    }

    val myLabel = myName ?: stringResource(R.string.who_you)
    val partnerLabel = partnerName ?: stringResource(R.string.who_partner)

    val stored = myToday?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
    val openTasks = tasks.items
        .filter { it.status == TaskStatus.OPEN }
        .sortedWith(compareByDescending<Task> { it.dayKey }.thenByDescending { it.createdAt })

    LazyColumn(
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
                        dateHeader(LocalDate.now()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (partnerUid != null) {
                        Text(
                            stringResource(R.string.connected_with, partnerLabel),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
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
                score = myToday?.scorePercent,
                title = stringResource(R.string.score_for_name, myLabel),
            )
        }

        // Gesonderte Anzeige 2: Wochenstand (7-Tage-Serie)
        item {
            WeeklyStatusBanner(
                streak = streak,
                rewardUnlocked = weekRewardUnlocked,
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
                        onPagesChanged = { pages ->
                            vm.setCounter(category.id, PAGES, pages.toDouble())
                        },
                    )
                }
            }
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

/**
 * Zeigt eine einzelne Kategorie im Partner-Tab an (nur deren Kategorien, nicht editierbar).
 * Liest den aktuellen Wert aus der gespeicherten Tagesbilanz und zeigt Stern-/Zählerwert an.
 */
@Composable
private fun PartnerCategorySummaryCard(
    category: Category,
    values: Map<String, Double>,
    snapshots: List<CategorySnapshot>,
) {
    val stars = snapshots.firstOrNull { it.categoryId == category.id }?.stars ?: 0
    val counters = values
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryImage(category = category, size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(localizedCategoryName(category), style = MaterialTheme.typography.titleMedium)
                    CategoryWeightSubtitle(category.weight, weightSharePercent(category.weight, listOf(WeightedCategory(category.id, category.weight))))
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
        stringResource(R.string.weight_with_share, weight, priorityLabel(weight), sharePercent),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ScoreHero(score: Double?, title: String) {
    val progress = ((score ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    val reached = (score ?: 0.0) >= THRESHOLD_PERCENT
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
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
                color = Amber,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(if (reached) R.string.status_daily_ok else R.string.status_daily_below),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

/**
 * Gesonderte Anzeige für den Wochenstand (7-Tage-Serie bis zur Wochenbelohnung).
 */
@Composable
private fun WeeklyStatusBanner(streak: Int, rewardUnlocked: Boolean) {
    val cycleStreak = if (streak > 0 && streak % STREAK_DAYS_FOR_REWARD == 0) {
        STREAK_DAYS_FOR_REWARD
    } else {
        streak % STREAK_DAYS_FOR_REWARD
    }
    val remaining = (STREAK_DAYS_FOR_REWARD - cycleStreak).coerceAtLeast(0)
    val progress = (cycleStreak.toFloat() / STREAK_DAYS_FOR_REWARD.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.status_weekly_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    stringResource(R.string.status_weekly_progress, cycleStreak, STREAK_DAYS_FOR_REWARD),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
                color = Amber,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (rewardUnlocked) {
                    stringResource(R.string.week_reward_unlocked)
                } else {
                    stringResource(R.string.week_no_reward, remaining)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
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
            Spacer(Modifier.height(8.dp))
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
                    vm.invitePartnerByEmail(partnerEmail) { code ->
                        launchInviteEmail(context, partnerEmail, code)
                    }
                },
                enabled = !vm.busy && partnerEmail.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.invite_send_email))
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
    onPagesChanged: (Int) -> Unit,
) {
    val pages = (values[PAGES] ?: 0.0).toInt().coerceAtLeast(0)
    val stars = counterStars(category, values)
    var text by remember(category.id, pages) { mutableStateOf(if (pages == 0) "" else pages.toString()) }

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
                step = 5.0,
            ) { newVal ->
                onPagesChanged(newVal.toInt().coerceAtLeast(0))
            }
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    text = input
                    if (input.isBlank()) {
                        onPagesChanged(0)
                    } else {
                        input.trim().toIntOrNull()?.let { onPagesChanged(it.coerceAtLeast(0)) }
                    }
                },
                label = { Text(stringResource(R.string.reading_pages_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
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
                CounterRow(stringResource(R.string.counter_demi), values[DEMI] ?: 0.0, step = 1.0) {
                    onChange(DEMI, it)
                }
                CounterRow(stringResource(R.string.counter_pint), values[PINT] ?: 0.0, step = 1.0) {
                    onChange(PINT, it)
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
                val answered = values.containsKey(ROTATION_STARS)
                FilterChip(
                    selected = answered && stars == MAX_STARS,
                    onClick = { onSelectAndRate(selectedTask, MAX_STARS) },
                    label = { Text(stringResource(R.string.rotation_yes)) },
                )
                Spacer(Modifier.width(6.dp))
                FilterChip(
                    selected = answered && stars == 0,
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
    val myName by vm.myName.collectAsState()
    val partnerName by vm.partnerName.collectAsState()
    val partnerPhoto by vm.partnerPhoto.collectAsState()
    val partnerToday by vm.partnerToday.collectAsState()
    val days by vm.days.collectAsState()
    val partnerUid by vm.partnerUid.collectAsState()
    val allCategories by vm.categories.collectAsState()
    val tasks by vm.tasks.collectAsState()

    val myLabel = myName ?: stringResource(R.string.who_you)
    val partnerLabel = partnerName ?: stringResource(R.string.who_partner)

    val partnerCategories = allCategories.items
        .filter { it.appliesTo(partnerUid ?: vm.uid) }
        .sortedByDescending { it.weight }
    val partnerOpenTasks = tasks.items.filter {
        it.status == TaskStatus.OPEN && (it.ownerUid == partnerUid || (partnerUid != null && it.appliesTo(partnerUid!!)))
    }
    val partnerStored = partnerToday?.snapshots.orEmpty()
    val partnerCounters = partnerToday?.counters.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                score = partnerToday?.scorePercent,
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
                values = partnerCounters[category.id].orEmpty(),
                snapshots = partnerStored,
            )
        }
        if (partnerOpenTasks.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.section_open_tasks)) }
            items(partnerOpenTasks, key = { it.id }) { task ->
                TaskCard(
                    vm = vm,
                    task = task,
                    partnerUid = partnerUid,
                    myLabel = myLabel,
                    partnerLabel = partnerLabel,
                )
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
private fun TaskCard(
    vm: AppViewModel,
    task: Task,
    partnerUid: String?,
    myLabel: String,
    partnerLabel: String,
) {
    val isMine = task.ownerUid == vm.uid
    val (availableOptions, totalPoolSize) = vm.availableChoicesFor(task)
    var dropdownExpanded by remember(task.id) { mutableStateOf(false) }
    var selectedOption by remember(task.id, availableOptions) {
        mutableStateOf(availableOptions.firstOrNull().orEmpty())
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
                stringResource(
                    if (task.type == TaskType.PENALTY) R.string.archive_penalty_date else R.string.archive_reward_date,
                    formatDay(task.dayKey),
                ),
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
                        if (task.type == TaskType.PENALTY) {
                            Text(
                                stringResource(R.string.penalty_pool_reset_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
                isMine -> Text(stringResource(R.string.task_waiting_confirm, task.title, partnerLabel))
                task.title.isEmpty() -> Text(stringResource(R.string.task_partner_chooses, partnerLabel))
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
