package com.trenits.fivestar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyCheckTest {

    private val water = Category("w", "rosie", "Wasser", 3, kind = CategoryKind.WATER, goal = 2.5)
    private val stars = Category("s", "rosie", "Sport", 3)

    private fun record(counters: Map<String, Map<String, Double>> = emptyMap(), snapshotStars: Int = 0) =
        DayRecord(
            dayKey = "2026-11-02",
            ownerUid = "rosie",
            snapshots = listOf(CategorySnapshot("s", "Sport", 3, snapshotStars)),
            counters = counters,
        )

    @Test
    fun `leerer Tag meldet alle Kategorien als offen`() {
        assertEquals(listOf("w", "s"), missingCategoryIds(listOf(water, stars), null))
    }

    @Test
    fun `eingetragene Werte gelten als ausgefuellt`() {
        val r = record(
            counters = mapOf("w" to mapOf(GLASSES to 3.0)),
            snapshotStars = 4,
        )
        assertTrue(missingCategoryIds(listOf(water, stars), r).isEmpty())
    }

    @Test
    fun `null-Sterne gelten als offen`() {
        assertEquals(listOf("s"), missingCategoryIds(listOf(stars), record(snapshotStars = 0)))
    }

    @Test
    fun `Beziehung ist offen, bis alle fuenf Dimensionen gespeichert sind`() {
        assertEquals(5, missingRelationshipAspects(null).size)
        val partial = DayRecord(
            dayKey = "2026-11-02",
            ownerUid = "rosie",
            snapshots = emptyList(),
            counters = mapOf(
                RELATIONSHIP_COUNTER_KEY to mapOf(
                    RelationshipAspect.MOOD_GENERAL.key to 0.0,
                    RelationshipAspect.COMMUNICATION.key to 4.0,
                ),
            ),
        )
        assertEquals(3, missingRelationshipAspects(partial).size)
    }
}
