package com.trenits.fivestar.domain

import java.time.Instant
import java.time.ZoneId

data class Couple(val id: String, val members: List<String>)

data class Category(
    val id: String,
    val ownerUid: String,
    val name: String,
    val weight: Int,
    val archived: Boolean = false,
)

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
 * Erstellt den Tageseintrag mit Snapshot. Name und Gewicht werden zum Bewertungszeitpunkt
 * übernommen, damit spätere Änderungen an Kategorien alte Scores nicht verändern.
 * Archivierte Kategorien fließen nicht in neue Einträge ein.
 */
fun buildDayRecord(
    dayKey: String,
    ownerUid: String,
    categories: List<Category>,
    stars: Map<String, Int>,
): DayRecord = DayRecord(
    dayKey = dayKey,
    ownerUid = ownerUid,
    snapshots = categories
        .filter { it.ownerUid == ownerUid && !it.archived }
        .map {
            CategorySnapshot(
                categoryId = it.id,
                name = it.name,
                weight = it.weight,
                stars = (stars[it.id] ?: 0).coerceIn(0, MAX_STARS),
            )
        },
)
