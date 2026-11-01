package com.trenits.fivestar.domain

const val MAX_STARS = 5
const val THRESHOLD_PERCENT = 80

data class WeightedCategory(val id: String, val weight: Int)

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

/** Score ≥ 80 % ergibt eine Belohnung, darunter eine Strafe. */
fun dayOutcome(categories: List<WeightedCategory>, stars: Map<String, Int>): DayOutcome {
    val (earned, possible) = scoreParts(categories, stars)
    return if (earned * 100 >= THRESHOLD_PERCENT * possible) DayOutcome.REWARD else DayOutcome.PENALTY
}
