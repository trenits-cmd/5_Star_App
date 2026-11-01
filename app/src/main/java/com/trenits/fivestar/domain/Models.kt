package com.trenits.fivestar.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import kotlin.math.floor

data class Couple(val id: String, val members: List<String>)

enum class CategoryKind { STARS, ALCOHOL, WATER }

/**
 * goal: bei ALCOHOL die Obergrenze in Litern pro Tag, bei WATER das Tagesziel in Litern.
 * Bei STARS ohne Bedeutung.
 */
data class Category(
    val id: String,
    val ownerUid: String,
    val name: String,
    val weight: Int,
    val archived: Boolean = false,
    val kind: CategoryKind = CategoryKind.STARS,
    val targets: List<String> = listOf(ownerUid),
    val goal: Double = 0.0,
) {
    /** Gilt die Kategorie für diese Person? */
    fun appliesTo(uid: String): Boolean = !archived && uid in targets
}

const val PINT = "pint"
const val DEMI = "demi"
const val GLASSES = "glasses"
const val LITERS = "liters"

const val PINT_LITERS = 0.5
const val DEMI_LITERS = 0.25

data class CatalogItem(
    val id: String,
    val ownerUid: String,
    val type: TaskType,
    val title: String,
)

/** Tagesschlüssel in der Zeitzone des Nutzers, z. B. "2026-11-01". */
fun dayKeyOf(instant: Instant, zone: ZoneId): String =
    instant.atZone(zone).toLocalDate().toString()

fun alcoholLiters(counters: Map<String, Double>): Double =
    (counters[PINT] ?: 0.0) * PINT_LITERS + (counters[DEMI] ?: 0.0) * DEMI_LITERS

/** Innerhalb der Obergrenze volle Sterne. Darüber anteilig weniger: Obergrenze / Menge. */
fun alcoholStars(liters: Double, limit: Double): Int {
    if (liters <= limit) return MAX_STARS
    return floor(MAX_STARS * limit / liters + EPSILON).toInt().coerceIn(0, MAX_STARS)
}

/** Erreicht das Tagesziel, gibt es volle Sterne. Darunter anteilig. */
fun waterStars(liters: Double, goal: Double): Int {
    if (goal <= 0.0) return MAX_STARS
    return floor(MAX_STARS * liters / goal + EPSILON).toInt().coerceIn(0, MAX_STARS)
}

/** Schlüssel der ISO-Woche in der Zeitzone des Nutzers, z. B. "2026-W44". */
fun weekKeyOf(instant: Instant, zone: ZoneId): String {
    val date = instant.atZone(zone).toLocalDate()
    // ISO-Woche beginnt am Montag.
    val monday = date.with(DayOfWeek.MONDAY)
    return "${monday.year}-W${String("%02d").format(monday.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASE_YEAR))}"
}

/** Ergebnis einer Woche: erfolgreich, wenn genug Tage das Tagesziel erreichten. */
enum class WeekOutcome { SUCCESS, FAILED }

/**
 * Prüft, ob die Woche erfolgreich war.
 * [dayOutcomes] enthält die Tages-Ergebnisse der Woche (Tag → DayOutcome).
 * [minSuccessfulDays] ist die Mindestzahl erfolgreicher Tage (z. B. 5 von 7).
 */
fun weekOutcomeFor(dayOutcomes: Map<String, DayOutcome>, minSuccessfulDays: Int): WeekOutcome {
    val successful = dayOutcomes.values.count { it == DayOutcome.REWARD }
    return if (successful >= minSuccessfulDays) WeekOutcome.SUCCESS else WeekOutcome.FAILED
}

private const val EPSILON = 1e-9

/** Sterne einer Zählerkategorie aus ihren Zählerwerten. */
fun counterStars(category: Category, values: Map<String, Double>): Int = when (category.kind) {
    CategoryKind.STARS -> 0
    CategoryKind.ALCOHOL -> alcoholStars(alcoholLiters(values), category.goal)
    CategoryKind.WATER -> waterStars(values[LITERS] ?: 0.0, category.goal)
}

/**
 * Erstellt den Tageseintrag der Person mit Snapshot. Übernommen werden alle Kategorien,
 * die für diese Person gelten. Sternkategorien nehmen die Sterne aus [stars], Zählerkategorien
 * berechnen sie aus den Zählern. Name und Gewicht werden zum Bewertungszeitpunkt festgehalten.
 */
fun buildDayRecord(
    dayKey: String,
    ownerUid: String,
    categories: List<Category>,
    stars: Map<String, Int>,
    counters: Counters = emptyMap(),
): DayRecord = DayRecord(
    dayKey = dayKey,
    ownerUid = ownerUid,
    snapshots = categories
        .filter { it.appliesTo(ownerUid) }
        .map {
            CategorySnapshot(
                categoryId = it.id,
                name = it.name,
                weight = it.weight,
                stars = when (it.kind) {
                    CategoryKind.STARS -> (stars[it.id] ?: 0).coerceIn(0, MAX_STARS)
                    else -> counterStars(it, counters[it.id].orEmpty())
                },
            )
        },
    counters = counters,
)
