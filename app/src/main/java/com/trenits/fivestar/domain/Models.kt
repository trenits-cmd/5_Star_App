package com.trenits.fivestar.domain

import java.time.Instant
import java.time.ZoneId

data class Couple(val id: String, val members: List<String>)

enum class CategoryKind { STARS, ALCOHOL, WATER }

data class Category(
    val id: String,
    val ownerUid: String,
    val name: String,
    val weight: Int,
    val archived: Boolean = false,
    val kind: CategoryKind = CategoryKind.STARS,
    val targets: List<String> = listOf(ownerUid),
) {
    /** Gilt die Kategorie für diese Person? */
    fun appliesTo(uid: String): Boolean = !archived && uid in targets
}

const val PINT = "pint"
const val DEMI = "demi"
const val GLASSES = "glasses"
const val LITERS = "liters"

data class CatalogItem(
    val id: String,
    val ownerUid: String,
    val type: TaskType,
    val title: String,
)

/** Tagesschlüssel in der Zeitzone des Nutzers, z. B. "2026-11-01". */
fun dayKeyOf(instant: Instant, zone: ZoneId): String =
    instant.atZone(zone).toLocalDate().toString()

/**
 * Erstellt den Tageseintrag der Person mit Snapshot. Übernommen werden nur Sternkategorien,
 * die für diese Person gelten. Name und Gewicht werden zum Bewertungszeitpunkt festgehalten.
 * Zähler werden unverändert übernommen.
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
        .filter { it.kind == CategoryKind.STARS && it.appliesTo(ownerUid) }
        .map {
            CategorySnapshot(
                categoryId = it.id,
                name = it.name,
                weight = it.weight,
                stars = (stars[it.id] ?: 0).coerceIn(0, MAX_STARS),
            )
        },
    counters = counters,
)
