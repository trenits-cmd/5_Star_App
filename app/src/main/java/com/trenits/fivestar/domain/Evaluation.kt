package com.trenits.fivestar.domain

import java.time.LocalDate

enum class Period(val label: String) {
    SEVEN_DAYS("7 Tage"),
    THIRTY_DAYS("30 Tage"),
    THIS_YEAR("Dieses Jahr"),
    ALL("Gesamt");

    /** Erster Tag des Zeitraums, bezogen auf heute. Null bedeutet ohne Begrenzung. */
    fun startDay(today: LocalDate): LocalDate? = when (this) {
        SEVEN_DAYS -> today.minusDays(6)
        THIRTY_DAYS -> today.minusDays(29)
        THIS_YEAR -> today.withDayOfYear(1)
        ALL -> null
    }
}

/**
 * Ergebnis eines Tages für die Person. Ein gespeicherter Eintrag mit bewerteten Kategorien
 * hat Vorrang. Ohne Eintrag zählt jede aktive Kategorie als 0 Sterne.
 * Null, wenn es gar keine aktiven Kategorien gibt: dann entsteht keine Aufgabe.
 */
fun dayOutcomeFor(record: DayRecord?, categories: List<Category>): DayOutcome? {
    val rated = record?.snapshots?.filter { it.weight > 0 }.orEmpty()
    if (rated.isNotEmpty()) {
        return dayOutcome(
            rated.map { WeightedCategory(it.categoryId, it.weight) },
            rated.associate { it.categoryId to it.stars },
        )
    }
    if (categories.none { !it.archived && it.weight > 0 }) return null
    return DayOutcome.PENALTY
}
