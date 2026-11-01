package com.trenits.fivestar.domain

import kotlin.math.roundToInt

const val MAX_STARS = 5
const val THRESHOLD_PERCENT = 80

/** Gewichtungsstufen: Essen hat die höchste Priorität (5), kleinere Aufgaben entsprechend weniger. */
const val WEIGHT_CRITICAL = 5
const val WEIGHT_VERY_HIGH = 4
const val WEIGHT_IMPORTANT = 3
const val WEIGHT_DAILY = 2
const val WEIGHT_MINOR = 1

data class WeightedCategory(val id: String, val weight: Int)

/**
 * Berechnet den prozentualen Anteil (0..100), den eine Kategorie mit [weight]
 * am gesamten Tages-Score ausmacht.
 */
fun weightSharePercent(weight: Int, categories: List<WeightedCategory>): Int {
    val totalWeight = categories.filter { it.weight > 0 }.sumOf { it.weight }
    if (totalWeight <= 0 || weight <= 0) return 0
    return (weight * 100.0 / totalWeight).roundToInt()
}

/**
 * Score = Σ(Gewicht × Sterne) / Σ(Gewicht × 5) in Prozent.
 * Fehlende Bewertungen zählen als 0 Sterne, behalten aber ihr Gewicht im Nenner.
 * Liefert 0, wenn es keine Kategorien mit positivem Gewicht gibt.
 */
fun dayScorePercent(categories: List<WeightedCategory>, stars: Map<String, Int>): Double {
    val (earned, possible) = scoreParts(categories, stars)
    if (possible == 0) return 0.0
    return earned * 100.0 / possible
}

/** Zähler und Nenner in ganzzahligen Punkten, damit die Schwelle ohne Rundungsfehler geprüft wird. */
fun scoreParts(categories: List<WeightedCategory>, stars: Map<String, Int>): Pair<Int, Int> {
    val weights = categories.filter { it.weight > 0 }
    val possible = weights.sumOf { it.weight * MAX_STARS }
    val earned = weights.sumOf { it.weight * (stars[it.id] ?: 0).coerceIn(0, MAX_STARS) }
    return earned to possible
}

enum class DayOutcome { REWARD, PENALTY }

/** Score ≥ 80 % ergibt ein erreichtes Tagesziel (REWARD), darunter eine Strafe (PENALTY). */
fun dayOutcome(categories: List<WeightedCategory>, stars: Map<String, Int>): DayOutcome {
    val (earned, possible) = scoreParts(categories, stars)
    return if (earned * 100 >= THRESHOLD_PERCENT * possible) DayOutcome.REWARD else DayOutcome.PENALTY
}
