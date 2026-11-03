package com.trenits.fivestar.domain

import java.time.LocalDate

/** Zeitraum der Statistik. ALL beginnt am ersten erfassten Tag. */
enum class StatsRange { WEEK, MONTH, THREE_MONTHS, ALL }

/** Ein Diagrammpunkt: Startdatum des Abschnitts und Durchschnittswert (0–100 %). */
data class StatsPoint(val start: LocalDate, val value: Double)

/**
 * Alle Kalendertage des Zeitraums bis [today] (inklusive).
 * ALL beginnt am frühesten bekannten Tag: Account-Erstellung oder erster Eintrag.
 */
fun statsDays(
    range: StatsRange,
    today: LocalDate,
    firstRecorded: LocalDate?,
    accountStart: LocalDate? = null,
): List<LocalDate> {
    val start = when (range) {
        StatsRange.WEEK -> today.minusDays(6)
        StatsRange.MONTH -> today.minusDays(29)
        StatsRange.THREE_MONTHS -> today.minusDays(89)
        StatsRange.ALL -> listOfNotNull(accountStart, firstRecorded).minOrNull() ?: today
    }
    val first = if (start.isAfter(today)) today else start
    return generateSequence(first) { it.plusDays(1) }
        .takeWhile { !it.isAfter(today) }
        .toList()
}

/**
 * Fasst Tageswerte für das Diagramm zusammen, damit jeder Zeitraum lesbar bleibt:
 * - Letzte Woche (7 Punkte) und letzter Monat (30 Punkte): je ein Tag pro Punkt
 * - Letzte 3 Monate (~13 Punkte): Wochendurchschnitt
 * - Gesamter Zeitraum: Wochendurchschnitt bei bis zu 180 Tagen, sonst 30-Tage-Abschnitte
 */
fun statsPoints(range: StatsRange, days: List<LocalDate>, values: List<Double>): List<StatsPoint> {
    val bucket = when (range) {
        StatsRange.WEEK, StatsRange.MONTH -> 1
        StatsRange.THREE_MONTHS -> 7
        StatsRange.ALL -> if (days.size <= 180) 7 else 30
    }
    return days.indices.chunked(bucket).map { idx ->
        StatsPoint(days[idx.first()], idx.map { values[it] }.average())
    }
}

/**
 * Prozentwert eines Tages für eine Kategorie: 0–5 Sterne → 0–100 %. Fehlende Einträge zählen als 0.
 * Kategorien werden über die exakte ID oder über den Kategorietyp (Präfix) erkannt,
 * damit Rosie- und Andi-Kategorien zueinander passen.
 */
fun categoryPercentOf(record: DayRecord?, category: Category): Double {
    val prefix = categoryIdPrefix(category.kind)
    val stars = record?.snapshots
        ?.firstOrNull { it.categoryId == category.id || (prefix != null && it.categoryId.startsWith(prefix)) }
        ?.stars ?: 0
    return stars * 100.0 / MAX_STARS
}

/** Gesamtergebnis eines Tages (gewichteter Score), 0 ohne Eintrag. */
fun overallPercentOf(record: DayRecord?): Double = record?.scorePercent ?: 0.0

/** Hat eine Person in irgendeinem Tag eine Kategorie dieses Typs? */
fun hasCategoryOfKind(records: Collection<DayRecord>, category: Category): Boolean {
    val prefix = categoryIdPrefix(category.kind)
    return records.any { r ->
        r.snapshots.any { it.categoryId == category.id || (prefix != null && it.categoryId.startsWith(prefix)) }
    }
}

private fun categoryIdPrefix(kind: CategoryKind): String? = when (kind) {
    CategoryKind.WATER -> "preset_water_"
    CategoryKind.ALCOHOL -> "preset_alcohol_"
    CategoryKind.DINNER_TIME -> "preset_dinner_"
    CategoryKind.MEALS -> "preset_meals_"
    CategoryKind.ROTATION -> "preset_rotation_"
    CategoryKind.SPORT -> "preset_sport_"
    CategoryKind.READING -> "preset_reading_"
    CategoryKind.STARS -> null
}
