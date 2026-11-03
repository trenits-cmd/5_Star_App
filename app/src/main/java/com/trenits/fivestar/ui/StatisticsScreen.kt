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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.trenits.fivestar.R
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.DayRecord
import com.trenits.fivestar.domain.StatsRange
import com.trenits.fivestar.domain.THRESHOLD_PERCENT
import com.trenits.fivestar.domain.categoryPercentOf
import com.trenits.fivestar.domain.hasCategoryOfKind
import com.trenits.fivestar.domain.overallPercentOf
import com.trenits.fivestar.domain.statsDays
import com.trenits.fivestar.domain.statsPoints
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

private data class ChartSeries(val name: String, val color: Color, val values: List<Double>)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatisticsScreen(vm: AppViewModel) {
    val days by vm.days.collectAsState()
    val targeted by vm.targeted.collectAsState()
    val partnerUid by vm.partnerUid.collectAsState()
    val myName by vm.myName.collectAsState()
    val partnerName by vm.partnerName.collectAsState()
    val myLabel = myName ?: stringResource(R.string.who_you)
    val partnerLabel = partnerName ?: stringResource(R.string.who_partner)

    var range by rememberSaveable { mutableStateOf(StatsRange.WEEK) }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }

    val today = LocalDate.now()
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
    val category = targeted.firstOrNull { it.id == categoryId }

    val myValues = periodDays.map { d ->
        val r = myRecords[d]
        if (category == null) overallPercentOf(r) else categoryPercentOf(r, category)
    }
    val partnerAvailable = partnerUid != null &&
        (category == null || hasCategoryOfKind(partnerRecords.values, category))
    val partnerValues = periodDays.map { d ->
        val r = partnerRecords[d]
        if (category == null) overallPercentOf(r) else categoryPercentOf(r, category)
    }

    val myPoints = statsPoints(range, periodDays, myValues)
    val partnerPoints = statsPoints(range, periodDays, partnerValues)
    val labels = myPoints.map { chartLabel(range, it.start) }

    val series = buildList {
        add(ChartSeries(myLabel, MaterialTheme.colorScheme.primary, myPoints.map { it.value }))
        if (partnerAvailable) {
            add(ChartSeries(partnerLabel, MaterialTheme.colorScheme.tertiary, partnerPoints.map { it.value }))
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                stringResource(R.string.stats_title),
                style = MaterialTheme.typography.headlineMedium,
            )
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
                        FilterChip(
                            selected = category == null,
                            onClick = {
                                categoryId = null
                                selected = null
                            },
                            label = { Text(stringResource(R.string.stats_category_all)) },
                        )
                        targeted.forEach { cat ->
                            FilterChip(
                                selected = category?.id == cat.id,
                                onClick = {
                                    categoryId = cat.id
                                    selected = null
                                },
                                label = { Text(localizedCategoryName(cat)) },
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        category?.let { stringResource(R.string.stats_chart_title_category, localizedCategoryName(it)) }
                            ?: stringResource(R.string.stats_chart_title_overall),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (myRecords.isEmpty() && partnerRecords.isEmpty()) {
                        Text(
                            stringResource(R.string.stats_no_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatsLineChart(
                        labels = labels,
                        series = series,
                        selected = selected,
                        onSelect = { selected = it },
                        maxLabels = if (range == StatsRange.WEEK) 7 else 6,
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        series.forEach { s ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(s.color))
                                Spacer(Modifier.width(6.dp))
                                Text(s.name, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    val idx = selected?.takeIf { it in labels.indices }
                    if (idx != null) {
                        val parts = series.mapIndexed { i, s ->
                            "${s.name} ${formatPercent(s.values.getOrNull(idx))}"
                        }
                        Text(
                            stringResource(
                                R.string.stats_point_detail,
                                formatDay(myPoints[idx].start.toString()),
                                parts.joinToString(" · "),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (partnerUid != null && category != null && !partnerAvailable) {
                        Text(
                            stringResource(R.string.stats_partner_missing, partnerLabel, localizedCategoryName(category)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            StatsSummaryCard(
                name = myLabel,
                color = MaterialTheme.colorScheme.primary,
                values = periodDays.filter { myRecords.containsKey(it) }.map { d ->
                    if (category == null) overallPercentOf(myRecords[d]) else categoryPercentOf(myRecords[d], category)
                },
            )
        }
        if (partnerAvailable) {
            item {
                StatsSummaryCard(
                    name = partnerLabel,
                    color = MaterialTheme.colorScheme.tertiary,
                    values = periodDays.filter { partnerRecords.containsKey(it) }.map { d ->
                        if (category == null) overallPercentOf(partnerRecords[d]) else categoryPercentOf(partnerRecords[d], category)
                    },
                )
            }
        }
    }
}

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
    val padBottom = with(density) { 28.dp.toPx() }
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
            val layout = textMeasurer.measure(labels[i], labelStyle)
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
private fun StatsSummaryCard(name: String, color: Color, values: List<Double>) {
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
                    StatBox(
                        stringResource(R.string.stats_goal_days),
                        values.count { it >= THRESHOLD_PERCENT }.toString(),
                    )
                }
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

private fun chartLabel(range: StatsRange, start: LocalDate): String = when (range) {
    StatsRange.WEEK -> start.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
    else -> start.format(DateTimeFormatter.ofPattern("dd.MM", Locale.getDefault()))
}
