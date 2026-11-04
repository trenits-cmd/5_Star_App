package com.trenits.fivestar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.trenits.fivestar.R
import com.trenits.fivestar.data.AppClock
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.DayRecord
import com.trenits.fivestar.domain.RelationshipAspect
import com.trenits.fivestar.domain.StatsRange
import com.trenits.fivestar.domain.THRESHOLD_PERCENT
import com.trenits.fivestar.domain.Task
import com.trenits.fivestar.domain.TaskType
import com.trenits.fivestar.domain.categoryPercentOf
import com.trenits.fivestar.domain.hasCategoryOfKind
import com.trenits.fivestar.domain.overallPercentOf
import com.trenits.fivestar.domain.relationshipAspectPercentOf
import com.trenits.fivestar.domain.relationshipPercentOf
import com.trenits.fivestar.domain.relationshipStarsSum
import com.trenits.fivestar.domain.statsDays
import com.trenits.fivestar.domain.statsPoints
import com.trenits.fivestar.domain.statsSums
import com.trenits.fivestar.domain.weightedStarsOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

private data class ChartSeries(val name: String, val color: Color, val values: List<Double>)

private enum class StatsGroup { OVERALL, CATEGORIES, RELATIONSHIP, REWARDS }

private enum class PersonFilter { ME, PARTNER, BOTH }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatisticsScreen(vm: AppViewModel) {
    val days by vm.days.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val targeted by vm.targeted.collectAsState()
    val partnerUid by vm.partnerUid.collectAsState()
    val myName by vm.myName.collectAsState()
    val partnerName by vm.partnerName.collectAsState()
    val myLabel = myName ?: stringResource(R.string.who_you)
    val partnerLabel = partnerName ?: stringResource(R.string.who_partner)

    var range by rememberSaveable { mutableStateOf(StatsRange.WEEK) }
    var group by rememberSaveable { mutableStateOf(StatsGroup.OVERALL) }
    var sub by rememberSaveable { mutableStateOf<String?>(null) }
    var person by rememberSaveable { mutableStateOf(PersonFilter.ME) }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }

    val today = AppClock.today()
    val myRecords = remember(days.items, vm.uid) { recordsByDate(days.items.filter { it.ownerUid == vm.uid }) }
    val partnerRecords = remember(days.items, partnerUid) {
        recordsByDate(days.items.filter { partnerUid != null && it.ownerUid == partnerUid })
    }
    val firstRecorded = (myRecords.keys + partnerRecords.keys).minOrNull()
    val accountStart = remember {
        Firebase.auth.currentUser?.metadata?.creationTimestamp?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }
    val periodDays = statsDays(range, today, firstRecorded, accountStart)
    val periodKeys = periodDays.map { it.toString() }.toSet()
    val points = statsPoints(range, periodDays, List(periodDays.size) { 0.0 })
    val labels = points.map { chartLabel(range, it.start) }

    val category = if (group == StatsGroup.CATEGORIES) targeted.firstOrNull { it.id == sub } else null
    val aspect = if (group == StatsGroup.RELATIONSHIP) RelationshipAspect.entries.firstOrNull { it.name == sub } else null
    val taskType = TaskType.REWARD
    val isTaskGroup = group == StatsGroup.REWARDS

    val showMe = person != PersonFilter.PARTNER
    val showPartner = partnerUid != null && person != PersonFilter.ME
    val partnerHasCategory = category == null || hasCategoryOfKind(partnerRecords.values, category)

    fun percentOf(record: DayRecord?): Double = when (group) {
        StatsGroup.RELATIONSHIP -> if (aspect == null) relationshipPercentOf(record) else relationshipAspectPercentOf(record, aspect)
        StatsGroup.CATEGORIES -> if (category == null) overallPercentOf(record) else categoryPercentOf(record, category)
        else -> overallPercentOf(record)
    }

    val myTasksByDay = remember(tasks.items, vm.uid) { tasks.items.filter { it.ownerUid == vm.uid }.groupBy { it.dayKey } }
    val partnerTasksByDay = remember(tasks.items, partnerUid) {
        tasks.items.filter { partnerUid != null && it.ownerUid == partnerUid }.groupBy { it.dayKey }
    }
    fun matchesTask(t: Task) = t.type == taskType && (sub == null || t.title == sub)
    val meCounts = periodDays.map { d -> myTasksByDay[d.toString()].orEmpty().count { matchesTask(it) }.toDouble() }
    val partnerCounts = periodDays.map { d -> partnerTasksByDay[d.toString()].orEmpty().count { matchesTask(it) }.toDouble() }

    val taskTitles = tasks.items
        .filter { it.type == taskType && it.dayKey in periodKeys }
        .map { it.title }
        .distinct()

    val lineSeries = buildList {
        if (showMe) {
            add(ChartSeries(myLabel, MaterialTheme.colorScheme.primary,
                statsPoints(range, periodDays, periodDays.map { percentOf(myRecords[it]) }).map { it.value }))
        }
        if (showPartner && partnerHasCategory) {
            add(ChartSeries(partnerLabel, MaterialTheme.colorScheme.tertiary,
                statsPoints(range, periodDays, periodDays.map { percentOf(partnerRecords[it]) }).map { it.value }))
        }
    }
    val barSeries = buildList {
        if (showMe) {
            add(ChartSeries(myLabel, MaterialTheme.colorScheme.primary, statsSums(range, periodDays, meCounts).map { it.value }))
        }
        if (showPartner) {
            add(ChartSeries(partnerLabel, MaterialTheme.colorScheme.tertiary, statsSums(range, periodDays, partnerCounts).map { it.value }))
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineMedium)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatsRange.entries.forEach { r ->
                            FilterChip(
                                selected = range == r,
                                onClick = {
                                    range = r
                                    selected = null
                                },
                                label = { Text(stringResource(rangeLabelRes(r))) },
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        StatsGroup.entries.forEach { g ->
                            FilterChip(
                                selected = group == g,
                                onClick = {
                                    group = g
                                    sub = null
                                    selected = null
                                },
                                label = { Text(stringResource(groupLabelRes(g))) },
                            )
                        }
                    }
                    if (group != StatsGroup.OVERALL) {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            FilterChip(
                                selected = sub == null,
                                onClick = {
                                    sub = null
                                    selected = null
                                },
                                label = { Text(stringResource(R.string.stats_all)) },
                            )
                            when (group) {
                                StatsGroup.CATEGORIES -> targeted.forEach { cat ->
                                    FilterChip(
                                        selected = sub == cat.id,
                                        onClick = {
                                            sub = cat.id
                                            selected = null
                                        },
                                        label = { Text(localizedCategoryName(cat)) },
                                    )
                                }
                                StatsGroup.RELATIONSHIP -> RelationshipAspect.entries.forEach { a ->
                                    FilterChip(
                                        selected = sub == a.name,
                                        onClick = {
                                            sub = a.name
                                            selected = null
                                        },
                                        label = { Text(relationshipAspectLabel(a)) },
                                    )
                                }
                                else -> taskTitles.forEach { title ->
                                    FilterChip(
                                        selected = sub == title,
                                        onClick = {
                                            sub = title
                                            selected = null
                                        },
                                        label = { Text(taskTitleLabel(title)) },
                                    )
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = person == PersonFilter.ME,
                            onClick = { person = PersonFilter.ME },
                            label = { Text(myLabel) },
                        )
                        FilterChip(
                            selected = person == PersonFilter.PARTNER,
                            onClick = { person = PersonFilter.PARTNER },
                            enabled = partnerUid != null,
                            label = { Text(partnerLabel) },
                        )
                        FilterChip(
                            selected = person == PersonFilter.BOTH,
                            onClick = { person = PersonFilter.BOTH },
                            enabled = partnerUid != null,
                            label = { Text(stringResource(R.string.stats_person_both)) },
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        chartTitle(group, category, aspect),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (myRecords.isEmpty() && partnerRecords.isEmpty() && days.items.isEmpty() && tasks.items.isEmpty()) {
                        Text(
                            stringResource(R.string.stats_no_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (isTaskGroup) {
                        StatsBarChart(labels = labels, series = barSeries, maxLabels = if (range == StatsRange.WEEK) 7 else 6)
                        Legend(barSeries.map { it.name to it.color })
                    } else {
                        StatsLineChart(
                            labels = labels,
                            series = lineSeries,
                            selected = selected,
                            onSelect = { selected = it },
                            maxLabels = if (range == StatsRange.WEEK) 7 else 6,
                        )
                        Legend(lineSeries.map { it.name to it.color })
                        val idx = selected?.takeIf { it in labels.indices }
                        if (idx != null) {
                            val parts = lineSeries.map { s -> "${s.name} ${formatPercent(s.values.getOrNull(idx))}" }
                            Text(
                                stringResource(
                                    R.string.stats_point_detail,
                                    formatDay(points[idx].start.toString()),
                                    parts.joinToString(" · "),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (partnerUid == null && person != PersonFilter.ME) {
                        Text(
                            stringResource(R.string.stats_no_partner),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (showPartner && !partnerHasCategory && category != null) {
                        Text(
                            stringResource(R.string.stats_partner_missing, partnerLabel, localizedCategoryName(category)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Gesamtsummen je Person
        if (isTaskGroup) {
            if (showMe) {
                item {
                    StatsTotalCard(
                        name = myLabel,
                        color = MaterialTheme.colorScheme.primary,
                        total = meCounts.sum().toInt(),
                        totalLabel = stringResource(R.string.stats_total_count),
                    )
                }
            }
            if (showPartner) {
                item {
                    StatsTotalCard(
                        name = partnerLabel,
                        color = MaterialTheme.colorScheme.tertiary,
                        total = partnerCounts.sum().toInt(),
                        totalLabel = stringResource(R.string.stats_total_count),
                    )
                }
            }
        } else {
            if (showMe) {
                item {
                    StatsSummaryCard(
                        name = myLabel,
                        color = MaterialTheme.colorScheme.primary,
                        values = periodDays.filter { myRecords.containsKey(it) }.map { percentOf(myRecords[it]) },
                        total = periodDays.sumOf { totalOf(group, category, aspect, myRecords[it]) },
                        totalLabel = totalLabel(group),
                    )
                }
            }
            if (showPartner && partnerHasCategory) {
                item {
                    StatsSummaryCard(
                        name = partnerLabel,
                        color = MaterialTheme.colorScheme.tertiary,
                        values = periodDays.filter { partnerRecords.containsKey(it) }.map { percentOf(partnerRecords[it]) },
                        total = periodDays.sumOf { totalOf(group, category, aspect, partnerRecords[it]) },
                        totalLabel = totalLabel(group),
                    )
                }
            }
        }

        // Details unter der Grafik – nur bei den Rewards
        if (isTaskGroup) {
            item {
                Text(stringResource(R.string.stats_details_title), style = MaterialTheme.typography.titleMedium)
            }
            val rows = buildList {
                if (showMe) addAll(myTasksByDay.values.flatten())
                if (showPartner) addAll(partnerTasksByDay.values.flatten())
            }
                .filter { it.dayKey in periodKeys && matchesTask(it) }
                .sortedWith(compareByDescending<Task> { it.dayKey }.thenByDescending { it.createdAt })
            if (rows.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.stats_no_tasks),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(rows, key = { it.id }) { task ->
                val who = if (task.ownerUid == vm.uid) myLabel else partnerLabel
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "${formatDay(task.dayKey)} · $who · ${taskTitleLabel(task.title)}",
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
        }
    }
}

private fun partnerCategoryPresent(has: Boolean): Boolean = has

@Composable
private fun chartTitle(group: StatsGroup, category: Category?, aspect: RelationshipAspect?): String = when (group) {
    StatsGroup.OVERALL -> stringResource(R.string.stats_chart_title_overall)
    StatsGroup.CATEGORIES -> category?.let { stringResource(R.string.stats_chart_title_category, localizedCategoryName(it)) }
        ?: stringResource(R.string.stats_chart_title_categories)
    StatsGroup.RELATIONSHIP -> aspect?.let { stringResource(R.string.stats_chart_title_aspect, relationshipAspectLabel(it)) }
        ?: stringResource(R.string.stats_chart_title_relationship)
    StatsGroup.REWARDS -> stringResource(R.string.stats_chart_title_rewards)
}

private fun totalOf(group: StatsGroup, category: Category?, aspect: RelationshipAspect?, record: DayRecord?): Int = when (group) {
    StatsGroup.RELATIONSHIP -> relationshipStarsSum(record, aspect)
    StatsGroup.CATEGORIES -> weightedStarsOf(record, category)
    else -> weightedStarsOf(record, null)
}

@Composable
private fun totalLabel(group: StatsGroup): String = when (group) {
    StatsGroup.RELATIONSHIP -> stringResource(R.string.stats_total_stars)
    else -> stringResource(R.string.stats_total_weighted)
}

@Composable
private fun detailExtra(group: StatsGroup, category: Category?, aspect: RelationshipAspect?, record: DayRecord?): String {
    val stars = totalOf(group, category, aspect, record)
    return " · ${stringResource(R.string.stats_total_short, stars)}"
}

@Composable
private fun taskTitleLabel(title: String): String =
    if (title.isBlank()) stringResource(R.string.no_action_chosen) else localizedCatalogTitle(title)

@Composable
private fun StatsLineChart(
    labels: List<String>,
    series: List<ChartSeries>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    maxLabels: Int,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val cursorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val n = labels.size
    val padLeft = with(density) { 36.dp.toPx() }
    val padRight = with(density) { 12.dp.toPx() }
    val padTop = with(density) { 12.dp.toPx() }
    val padBottom = with(density) { 40.dp.toPx() }
    val pointRadius = with(density) { 3.dp.toPx() }
    val strokeWidth = with(density) { 2.dp.toPx() }
    val hairline = with(density) { 1.dp.toPx() }
    var chartWidth by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .onSizeChanged { chartWidth = it.width.toFloat() }
            .pointerInput(n, chartWidth) {
                detectTapGestures { offset ->
                    val plotW = chartWidth - padLeft - padRight
                    if (n > 0 && plotW > 0f) {
                        val idx = if (n == 1) 0 else (((offset.x - padLeft) / plotW) * (n - 1))
                            .roundToInt()
                            .coerceIn(0, n - 1)
                        onSelect(if (selected == idx) null else idx)
                    }
                }
            },
    ) {
        val w = size.width
        val h = size.height
        val plotLeft = padLeft
        val plotRight = w - padRight
        val plotTop = padTop
        val plotBottom = h - padBottom
        val plotW = plotRight - plotLeft
        val plotH = plotBottom - plotTop

        fun yOf(v: Double): Float = (plotBottom - (v.coerceIn(0.0, 100.0) / 100.0).toFloat() * plotH)
        fun xOf(i: Int): Float = if (n <= 1) plotLeft + plotW / 2f else plotLeft + i * plotW / (n - 1)

        listOf(0, 25, 50, 75, 100).forEach { v ->
            val y = yOf(v.toDouble())
            drawLine(gridColor, Offset(plotLeft, y), Offset(plotRight, y), strokeWidth = hairline)
            val layout = textMeasurer.measure("$v%", labelStyle)
            drawText(layout, topLeft = Offset(0f, y - layout.size.height / 2f))
        }

        val step = if (n <= maxLabels) 1 else ceil(n.toDouble() / maxLabels).toInt()
        labels.indices.filter { it % step == 0 }.forEach { i ->
            val layout = axisLabelLayout(textMeasurer, labels[i], labelStyle)
            val x = (xOf(i) - layout.size.width / 2f).coerceIn(0f, (w - layout.size.width).coerceAtLeast(0f))
            drawText(layout, topLeft = Offset(x, plotBottom + 6.dp.toPx()))
        }

        selected?.takeIf { it in 0 until n }?.let { i ->
            drawLine(cursorColor, Offset(xOf(i), plotTop), Offset(xOf(i), plotBottom), strokeWidth = hairline)
        }

        series.forEach { s ->
            if (s.values.isEmpty()) return@forEach
            val path = Path()
            s.values.forEachIndexed { i, v ->
                if (i == 0) path.moveTo(xOf(i), yOf(v)) else path.lineTo(xOf(i), yOf(v))
            }
            drawPath(
                path,
                color = s.color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            s.values.forEachIndexed { i, v ->
                drawCircle(
                    color = s.color,
                    radius = if (selected == i) pointRadius * 2f else pointRadius,
                    center = Offset(xOf(i), yOf(v)),
                )
            }
        }
    }
}

@Composable
private fun StatsBarChart(
    labels: List<String>,
    series: List<ChartSeries>,
    maxLabels: Int,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val n = labels.size
    val padLeft = with(density) { 36.dp.toPx() }
    val padRight = with(density) { 12.dp.toPx() }
    val padTop = with(density) { 12.dp.toPx() }
    val padBottom = with(density) { 40.dp.toPx() }
    val hairline = with(density) { 1.dp.toPx() }
    val yMax = ceil(series.maxOfOrNull { s -> s.values.maxOrNull() ?: 0.0 } ?: 0.0).toInt().coerceAtLeast(1)

    Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
        val w = size.width
        val h = size.height
        val plotLeft = padLeft
        val plotRight = w - padRight
        val plotTop = padTop
        val plotBottom = h - padBottom
        val plotW = plotRight - plotLeft
        val plotH = plotBottom - plotTop

        listOf(0, yMax).distinct().forEach { v ->
            val y = plotBottom - (v.toFloat() / yMax) * plotH
            drawLine(gridColor, Offset(plotLeft, y), Offset(plotRight, y), strokeWidth = hairline)
            val layout = textMeasurer.measure("$v", labelStyle)
            drawText(layout, topLeft = Offset(0f, y - layout.size.height / 2f))
        }

        if (n > 0) {
            val groupW = plotW / n
            val barW = groupW * 0.8f / series.size.coerceAtLeast(1)
            labels.indices.forEach { i ->
                val groupLeft = plotLeft + i * groupW + groupW * 0.1f
                series.forEachIndexed { k, s ->
                    val v = s.values.getOrElse(i) { 0.0 }
                    if (v > 0.0) {
                        val barH = (v / yMax).toFloat() * plotH
                        drawRect(
                            color = s.color,
                            topLeft = Offset(groupLeft + k * barW, plotBottom - barH),
                            size = Size(barW * 0.9f, barH),
                        )
                    }
                }
            }

            val step = if (n <= maxLabels) 1 else ceil(n.toDouble() / maxLabels).toInt()
            labels.indices.filter { it % step == 0 }.forEach { i ->
                val layout = axisLabelLayout(textMeasurer, labels[i], labelStyle)
                val center = plotLeft + i * groupW + groupW / 2f
                val x = (center - layout.size.width / 2f).coerceIn(0f, (w - layout.size.width).coerceAtLeast(0f))
                drawText(layout, topLeft = Offset(x, plotBottom + 6.dp.toPx()))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(items: List<Pair<String, Color>>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items.forEach { (name, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(6.dp))
                Text(name, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StatsSummaryCard(name: String, color: Color, values: List<Double>, total: Int, totalLabel: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(name, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
            if (values.isEmpty()) {
                Text(
                    stringResource(R.string.stats_no_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatBox(stringResource(R.string.stats_average), formatPercent(values.average()))
                    StatBox(stringResource(R.string.stats_best), formatPercent(values.max()))
                    StatBox(stringResource(R.string.stats_goal_days), values.count { it >= THRESHOLD_PERCENT }.toString())
                    StatBox(totalLabel, total.toString())
                }
            }
        }
    }
}

@Composable
private fun StatsTotalCard(name: String, color: Color, total: Int, totalLabel: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(name, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatBox(totalLabel, total.toString())
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun recordsByDate(records: List<DayRecord>): Map<LocalDate, DayRecord> =
    records.mapNotNull { r -> runCatching { LocalDate.parse(r.dayKey) to r }.getOrNull() }.toMap()

private fun rangeLabelRes(range: StatsRange): Int = when (range) {
    StatsRange.WEEK -> R.string.stats_range_week
    StatsRange.MONTH -> R.string.stats_range_month
    StatsRange.THREE_MONTHS -> R.string.stats_range_three_months
    StatsRange.ALL -> R.string.stats_range_all
}

private fun groupLabelRes(group: StatsGroup): Int = when (group) {
    StatsGroup.OVERALL -> R.string.stats_group_overall
    StatsGroup.CATEGORIES -> R.string.stats_group_categories
    StatsGroup.RELATIONSHIP -> R.string.stats_group_relationship
    StatsGroup.REWARDS -> R.string.stats_group_rewards
}

private fun chartLabel(range: StatsRange, start: LocalDate): String = when (range) {
    StatsRange.WEEK -> start.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())) +
        "\n" + start.format(DateTimeFormatter.ofPattern("dd.MM", Locale.getDefault()))
    else -> start.format(DateTimeFormatter.ofPattern("dd.MM", Locale.getDefault()))
}

/**
 * Achsen-Label: der Wochentag in kleiner Schrift, darunter das Datum (dd.MM) kleiner.
 * Andere Zeiträume zeigen nur das Datum (eine Zeile).
 */
private fun axisLabelLayout(
    measurer: TextMeasurer,
    label: String,
    style: TextStyle,
): TextLayoutResult {
    val lineBreak = label.indexOf('\n')
    if (lineBreak < 0) return measurer.measure(label, style)
    val text = buildAnnotatedString {
        append(label.substring(0, lineBreak))
        append('\n')
        withStyle(SpanStyle(fontSize = 8.sp, color = style.color)) {
            append(label.substring(lineBreak + 1))
        }
    }
    return measurer.measure(text, style)
}
