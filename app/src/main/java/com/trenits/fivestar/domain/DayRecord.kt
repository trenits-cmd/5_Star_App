package com.trenits.fivestar.domain

/** Snapshot einer Kategorie zum Bewertungszeitpunkt, damit spätere Änderungen alte Scores nicht verändern. */
data class CategorySnapshot(
    val categoryId: String,
    val name: String,
    val weight: Int,
    val stars: Int,
)

/** Zählerwerte je Kategorie, z. B. categoryId → (pint → 2.0). */
typealias Counters = Map<String, Map<String, Double>>

data class DayRecord(
    val dayKey: String,
    val ownerUid: String,
    val snapshots: List<CategorySnapshot>,
    val counters: Counters = emptyMap(),
    /** Zeitpunkt (Epoch-Millisekunden), zu dem der Tag vollständig bestätigt wurde. Null = noch nicht bestätigt. */
    val confirmedAt: Long? = null,
) {
    val scorePercent: Double
        get() = dayScorePercent(
            snapshots.map { WeightedCategory(it.categoryId, it.weight) },
            snapshots.associate { it.categoryId to it.stars },
        )
}
