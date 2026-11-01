package com.trenits.fivestar.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import kotlin.math.floor

data class Couple(val id: String, val members: List<String>)

enum class CategoryKind { STARS, ALCOHOL, WATER }

enum class CategoryIcon {
    FOOD,
    WATER,
    ALCOHOL,
    FITNESS,
    HOME,
    MIND,
    DETOX,
    STAR,
}

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
    val icon: String = "",
    val notifications: List<String> = emptyList(),
) {
    /** Gilt die Kategorie für diese Person? */
    fun appliesTo(uid: String): Boolean = !archived && uid in targets

    /** Liefert das gewählte Bild oder erkennt automatisch ein passendes Bild anhand von Art und Name. */
    fun resolvedIcon(): CategoryIcon {
        CategoryIcon.entries.firstOrNull { it.name.equals(icon, ignoreCase = true) }?.let { return it }
        if (kind == CategoryKind.WATER) return CategoryIcon.WATER
        if (kind == CategoryKind.ALCOHOL) return CategoryIcon.ALCOHOL
        val lower = name.lowercase()
        return when {
            listOf("essen", "food", "mahlzeit", "kochen", "repas", "meal", "frühstück").any { it in lower } ->
                CategoryIcon.FOOD
            listOf("wasser", "water", "eau", "trinken", "drink").any { it in lower } ->
                CategoryIcon.WATER
            listOf("alkohol", "alcohol", "alcool", "bier", "wein", "pinte").any { it in lower } ->
                CategoryIcon.ALCOHOL
            listOf("sport", "bewegung", "fitness", "joggen", "workout", "gym", "laufen").any { it in lower } ->
                CategoryIcon.FITNESS
            listOf("wohnung", "ordnung", "haushalt", "putzen", "home", "clean", "küche", "abwasch").any { it in lower } ->
                CategoryIcon.HOME
            listOf("mental", "me-time", "lesen", "musik", "hobby", "meditation", "buch", "mind").any { it in lower } ->
                CategoryIcon.MIND
            listOf("detox", "digital", "handy", "smartphone", "screen", "schlaf", "sleep").any { it in lower } ->
                CategoryIcon.DETOX
            else -> CategoryIcon.STAR
        }
    }
}

const val PINT = "pint"
const val DEMI = "demi"
const val GLASSES = "glasses"
const val LITERS = "liters"

const val PINT_LITERS = 0.5
const val DEMI_LITERS = 0.25

const val STREAK_DAYS_FOR_REWARD = 7

data class CatalogItem(
    val id: String,
    val ownerUid: String,
    val type: TaskType,
    val title: String,
    val targets: List<String> = listOf(ownerUid),
) {
    fun appliesTo(uid: String): Boolean = uid in targets
}

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
    val monday = date.with(DayOfWeek.MONDAY)
    val weekYear = monday.get(WeekFields.ISO.weekBasedYear())
    val weekNum = monday.get(WeekFields.ISO.weekOfWeekBasedYear())
    return "%d-W%02d".format(weekYear, weekNum)
}

/** Ergebnis einer Woche / Serie: erfolgreich, wenn genug Tage das Tagesziel erreichten. */
enum class WeekOutcome { SUCCESS, FAILED }

/**
 * Prüft, ob die Serie erfolgreich war (Standard: 7 von 7 Tagen).
 */
fun weekOutcomeFor(
    dayOutcomes: Map<String, DayOutcome>,
    minSuccessfulDays: Int = STREAK_DAYS_FOR_REWARD,
): WeekOutcome {
    val successful = dayOutcomes.values.count { it == DayOutcome.REWARD }
    return if (successful >= minSuccessfulDays) WeekOutcome.SUCCESS else WeekOutcome.FAILED
}

/**
 * Zählt, wie viele Tage am Stück (rückwärts ab [endDay]) die Person ihr Tagesziel (≥ 80 %)
 * erreicht hat. Sobald ein Tag fehlt oder unter 80 % liegt, endet die Serie.
 */
fun consecutiveSuccessDays(
    endDay: LocalDate,
    recordsByDay: Map<String, DayRecord>,
    categories: List<Category>,
    maxLookbackDays: Int = 365,
): Int {
    val activeCategories = categories.filter { !it.archived && it.weight > 0 }
    if (activeCategories.isEmpty()) return 0
    var count = 0
    for (offset in 0 until maxLookbackDays) {
        val key = endDay.minusDays(offset.toLong()).toString()
        val record = recordsByDay[key] ?: break
        val outcome = dayOutcomeFor(record, activeCategories)
        if (outcome == DayOutcome.REWARD) {
            count++
        } else {
            break
        }
    }
    return count
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
