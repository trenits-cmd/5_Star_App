package com.trenits.fivestar.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.WeekFields
import kotlin.math.floor

data class Couple(val id: String, val members: List<String>)

enum class CategoryKind {
    STARS,
    ALCOHOL,
    WATER,
    ROTATION,
    DINNER_TIME,
    MEALS,
    READING,
}

/**
 * Bewertung einer Mahlzeit (Frühstück, Mittagessen, Abendessen):
 * nichts (0), wenig (1), mittel (2), gesund (3).
 * Wenn bei allen 3 Mahlzeiten "gesund" herauskommt (9/9 Punkte), gibt es 5 Sterne,
 * ansonsten entsprechend weniger.
 */
enum class MealLevel(val points: Int) {
    NONE(0),
    LITTLE(1),
    MEDIUM(2),
    HEALTHY(3);

    companion object {
        fun fromPoints(points: Int): MealLevel =
            entries.firstOrNull { it.points == points } ?: NONE
    }
}

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
 * Wechsel-Aufgaben für die Woche:
 * Unterricht planen passiert am häufigsten (3×/Woche),
 * Wohnung aufräumen, Müll runterbringen, Kochen und Geschirr spülen je 1×/Woche.
 */
enum class RotationTask(val index: Int, val weeklyTarget: Int) {
    LESSON_PLANNING(0, weeklyTarget = 3),
    CLEAN_APARTMENT(1, weeklyTarget = 1),
    TAKE_OUT_TRASH(2, weeklyTarget = 1),
    COOKING(3, weeklyTarget = 1),
    WASH_DISHES(4, weeklyTarget = 1);

    companion object {
        fun fromIndex(index: Int): RotationTask =
            entries.firstOrNull { it.index == index } ?: LESSON_PLANNING
    }
}

/**
 * goal: bei ALCOHOL die Obergrenze in Litern (0 L = 5 ★, ab goal = 0 ★),
 * bei WATER das Tagesziel in Litern (Standard: 2,5 L = 5 ★).
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
    val pushEnabled: Boolean = true,
    val pushIntervalHours: Int = 0,
    val pushHour: Int = 20,
    val pushMinute: Int = 0,
) {
    /** Gilt die Kategorie für diese Person? */
    fun appliesTo(uid: String): Boolean = !archived && uid in targets

    /** Liefert das gewählte Bild oder erkennt automatisch ein passendes Bild anhand von Art und Name. */
    fun resolvedIcon(): CategoryIcon {
        CategoryIcon.entries.firstOrNull { it.name.equals(icon, ignoreCase = true) }?.let { return it }
        if (kind == CategoryKind.WATER) return CategoryIcon.WATER
        if (kind == CategoryKind.ALCOHOL) return CategoryIcon.ALCOHOL
        if (kind == CategoryKind.DINNER_TIME || kind == CategoryKind.MEALS) return CategoryIcon.FOOD
        if (kind == CategoryKind.ROTATION) return CategoryIcon.HOME
        if (kind == CategoryKind.READING) return CategoryIcon.MIND
        val lower = name.lowercase()
        return when {
            listOf("essen", "abendessen", "dinner", "food", "mahlzeit", "kochen", "repas", "dîner", "meal").any { it in lower } ->
                CategoryIcon.FOOD
            listOf("wasser", "water", "eau", "trinken", "drink").any { it in lower } ->
                CategoryIcon.WATER
            listOf("alkohol", "alcohol", "alcool", "bier", "wein", "pinte").any { it in lower } ->
                CategoryIcon.ALCOHOL
            listOf("sport", "bewegung", "fitness", "joggen", "workout", "gym", "laufen").any { it in lower } ->
                CategoryIcon.FITNESS
            listOf("wohnung", "ordnung", "haushalt", "putzen", "müll", "geschirr", "wechsel", "home", "clean").any { it in lower } ->
                CategoryIcon.HOME
            listOf("unterricht", "mental", "me-time", "lesen", "musik", "hobby", "meditation", "mind").any { it in lower } ->
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
const val ROTATION_TASK_INDEX = "rotationTask"
const val ROTATION_STARS = "rotationStars"
const val DINNER_HOUR = "dinnerHour"
const val DINNER_MINUTE = "dinnerMinute"
const val MEAL_BREAKFAST = "mealBreakfast"
const val MEAL_LUNCH = "mealLunch"
const val MEAL_DINNER = "mealDinner"
const val PAGES = "pages"
const val RELATIONSHIP_COUNTER_KEY = "_relationship"

/**
 * Die 5 Dimensionen des Beziehungstrackers, die beide Partner täglich mit 1–5 Sternen bewerten.
 */
enum class RelationshipAspect(val key: String) {
    MOOD_GENERAL("mood_general"),
    MOOD_RELATIONSHIP("mood_relationship"),
    COMMUNICATION("communication"),
    INTIMACY("intimacy"),
    SEX("sex"),
}

/** Liest die abgegebenen Beziehungs-Sterne (0..5) einer Person für einen Tag aus. */
fun relationshipStarsOf(record: DayRecord?): Map<RelationshipAspect, Int> {
    val map = record?.counters?.get(RELATIONSHIP_COUNTER_KEY).orEmpty()
    return RelationshipAspect.entries.associateWith { aspect ->
        (map[aspect.key]?.toInt() ?: 0).coerceIn(0, MAX_STARS)
    }
}

/**
 * Berechnet den Beziehungs-Score (0..100 %) einer Person aus den 5 Dimensionen.
 * Liefert null, wenn heute noch keine Dimension (> 0 Sterne) bewertet wurde.
 */
fun relationshipScorePercent(stars: Map<RelationshipAspect, Int>): Double? {
    val rated = RelationshipAspect.entries.map { (stars[it] ?: 0).coerceIn(0, MAX_STARS) }
    if (rated.all { it == 0 }) return null
    val totalEarned = rated.sum()
    val totalPossible = RelationshipAspect.entries.size * MAX_STARS
    return totalEarned * 100.0 / totalPossible
}

/**
 * Berechnet den gemeinsamen Beziehungs-Score beider Partner (Mittelwert der vorliegenden Bewertungen).
 */
fun coupleRelationshipScorePercent(
    myStars: Map<RelationshipAspect, Int>,
    partnerStars: Map<RelationshipAspect, Int>,
): Double? {
    val scores = listOfNotNull(
        relationshipScorePercent(myStars),
        relationshipScorePercent(partnerStars),
    )
    if (scores.isEmpty()) return null
    return scores.average()
}

const val PINT_LITERS = 0.5
const val DEMI_LITERS = 0.25
const val GLASS_LITERS = 0.25
const val DEFAULT_WATER_GOAL = 2.5
const val DEFAULT_ALCOHOL_LIMIT = 1.25

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

/**
 * Prüft, ob [time] innerhalb der Ruhepause liegt (Standard: 23:00 bis 09:00 Uhr).
 * Während der Ruhepause werden keine Push-Benachrichtigungen gesendet.
 */
fun isWithinQuietHours(
    time: LocalTime,
    quietStart: LocalTime = LocalTime.of(23, 0),
    quietEnd: LocalTime = LocalTime.of(9, 0),
): Boolean {
    if (quietStart == quietEnd) return false
    return if (quietStart.isAfter(quietEnd)) {
        // Über Mitternacht (z. B. 23:00 bis 09:00 Uhr)
        !time.isBefore(quietStart) || time.isBefore(quietEnd)
    } else {
        !time.isBefore(quietStart) && time.isBefore(quietEnd)
    }
}

/**
 * Gesamte Alkoholmenge in Litern aus Pinte (0,5 L), Demi (0,25 L)
 * und optionaler zusätzlicher Dezimal-Menge in Litern.
 */
fun alcoholLiters(counters: Map<String, Double>): Double =
    (counters[PINT] ?: 0.0) * PINT_LITERS +
        (counters[DEMI] ?: 0.0) * DEMI_LITERS +
        (counters[LITERS] ?: 0.0)

/**
 * Gesamte Wassermenge in Litern aus Gläsern (à 0,25 L) und Dezimal-Menge in Litern.
 */
fun waterLiters(counters: Map<String, Double>): Double =
    (counters[GLASSES] ?: 0.0) * GLASS_LITERS + (counters[LITERS] ?: 0.0)

/**
 * Alkohol-Grenze:
 * Bei 0 Litern gibt es 5 Sterne. Darüber gibt es prozentual weniger Sterne (mit Abrunden)
 * bis zur Obergrenze [limit] (ab [limit] gibt es 0 Sterne).
 */
fun alcoholStars(liters: Double, limit: Double = DEFAULT_ALCOHOL_LIMIT): Int {
    if (liters <= 0.0) return MAX_STARS
    if (limit <= 0.0) return 0
    if (liters >= limit) return 0
    return floor(MAX_STARS * (1.0 - liters / limit) + EPSILON).toInt().coerceIn(0, MAX_STARS)
}

/**
 * Wasser trinken:
 * Ab dem Tagesziel (z. B. 2,5 Liter) gibt es 5 Sterne, darunter prozentual weniger
 * mit Abrunden auf ganze Sterne.
 */
fun waterStars(liters: Double, goal: Double = DEFAULT_WATER_GOAL): Int {
    if (goal <= 0.0) return MAX_STARS
    return floor(MAX_STARS * liters / goal + EPSILON).toInt().coerceIn(0, MAX_STARS)
}

/**
 * Abendessen vor 20h:
 * - Vor 20:00 Uhr (< 20h) = 5 Sterne
 * - Vor 21:00 Uhr (20:00–20:59) = 1 Stern
 * - Ab 21:00 Uhr = 0 Sterne
 */
fun dinnerStars(hour: Int, minute: Int = 0): Int {
    if (hour < 0 || minute < 0) return 0
    return when {
        hour < 20 -> MAX_STARS
        hour < 21 -> 1
        else -> 0
    }
}

/**
 * Essen (Frühstück, Mittagessen, Abendessen):
 * Jede Mahlzeit wird mit nichts (0), wenig (1), mittel (2) oder gesund (3) bewertet.
 * Sind alle 3 gesund (9 Punkte), gibt es 5 Sterne, sonst entsprechend anteilig weniger.
 */
fun mealStars(breakfast: MealLevel, lunch: MealLevel, dinner: MealLevel): Int {
    val total = breakfast.points + lunch.points + dinner.points
    val maxTotal = MealLevel.HEALTHY.points * 3.0
    return floor(MAX_STARS * total / maxTotal + EPSILON).toInt().coerceIn(0, MAX_STARS)
}

/**
 * Lesen (Seitenanzahl):
 * - 0 bis 5 Seiten = 0 Sterne
 * - Mehr als 50 Seiten = 5 Sterne
 * - 6 bis 50 Seiten = 1 bis 4 Sterne in Relation
 */
fun readingStars(pages: Int): Int = when {
    pages <= 5 -> 0
    pages > 50 -> MAX_STARS
    else -> (1 + ((pages - 6) * 4) / 45).coerceIn(1, 4)
}

/**
 * Zählt für eine Wechsel-Kategorie [categoryId], wie oft jede [RotationTask]
 * in den übergebenen Tageseinträgen der Woche bereits erledigt wurde (Sterne > 0).
 */
fun rotationCountsInWeek(categoryId: String, weekRecords: List<DayRecord>): Map<RotationTask, Int> {
    val counts = mutableMapOf<RotationTask, Int>()
    for (record in weekRecords) {
        val catCounters = record.counters[categoryId].orEmpty()
        val stars = catCounters[ROTATION_STARS]?.toInt()
            ?: record.snapshots.firstOrNull { it.categoryId == categoryId }?.stars
            ?: 0
        if (stars > 0) {
            val taskIndex = catCounters[ROTATION_TASK_INDEX]?.toInt() ?: 0
            val task = RotationTask.fromIndex(taskIndex)
            counts[task] = (counts[task] ?: 0) + 1
        }
    }
    return counts
}

/**
 * Liefert die Wechsel-Aufgaben, deren Wochen-Soll in dieser Woche noch nicht ausgeschöpft ist.
 * Sind alle Wochen-Ziele erfüllt, stehen wieder alle zur Auswahl.
 */
fun availableRotationTasks(categoryId: String, weekRecords: List<DayRecord>): List<RotationTask> {
    val counts = rotationCountsInWeek(categoryId, weekRecords)
    val remaining = RotationTask.entries.filter { task ->
        (counts[task] ?: 0) < task.weeklyTarget
    }
    return remaining.ifEmpty { RotationTask.entries }
}

/**
 * Schlägt im Wechsel passend zum Wochentag die heutige Aufgabe vor:
 * Unterricht planen im Wechsel mit Haushalt (Wohnung, Müll, Kochen, Geschirr).
 */
fun suggestedRotationTask(dayOfWeek: DayOfWeek, available: List<RotationTask>): RotationTask {
    val preferred = when (dayOfWeek) {
        DayOfWeek.MONDAY -> RotationTask.LESSON_PLANNING
        DayOfWeek.TUESDAY -> RotationTask.CLEAN_APARTMENT
        DayOfWeek.WEDNESDAY -> RotationTask.LESSON_PLANNING
        DayOfWeek.THURSDAY -> RotationTask.TAKE_OUT_TRASH
        DayOfWeek.FRIDAY -> RotationTask.LESSON_PLANNING
        DayOfWeek.SATURDAY -> RotationTask.COOKING
        DayOfWeek.SUNDAY -> RotationTask.WASH_DISHES
    }
    return if (preferred in available) preferred else available.firstOrNull() ?: RotationTask.LESSON_PLANNING
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

/** Sterne einer Zähler-/Spezialkategorie aus ihren Zählerwerten. */
fun counterStars(category: Category, values: Map<String, Double>): Int = when (category.kind) {
    CategoryKind.STARS -> 0
    CategoryKind.ALCOHOL -> {
        val limit = if (category.goal > 0.0) category.goal else DEFAULT_ALCOHOL_LIMIT
        alcoholStars(alcoholLiters(values), limit)
    }
    CategoryKind.WATER -> {
        val goal = if (category.goal > 0.0) category.goal else DEFAULT_WATER_GOAL
        waterStars(waterLiters(values), goal)
    }
    CategoryKind.ROTATION -> (values[ROTATION_STARS]?.toInt() ?: 0).coerceIn(0, MAX_STARS)
    CategoryKind.DINNER_TIME -> {
        val hour = values[DINNER_HOUR]?.toInt() ?: -1
        val minute = values[DINNER_MINUTE]?.toInt() ?: 0
        if (hour < 0) 0 else dinnerStars(hour, minute)
    }
    CategoryKind.MEALS -> {
        val b = MealLevel.fromPoints(values[MEAL_BREAKFAST]?.toInt() ?: 0)
        val l = MealLevel.fromPoints(values[MEAL_LUNCH]?.toInt() ?: 0)
        val d = MealLevel.fromPoints(values[MEAL_DINNER]?.toInt() ?: 0)
        mealStars(b, l, d)
    }
    CategoryKind.READING -> readingStars((values[PAGES] ?: 0.0).toInt())
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
