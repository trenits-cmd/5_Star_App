package com.trenits.fivestar.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class DayRecordTest {

    private val alice = "alice"
    private val sport = Category("sport", alice, "Sport", 3)
    private val essen = Category("essen", alice, "Essen", 1)

    @Test
    fun `Score bleibt nach Gewichtsaenderung unveraendert`() {
        val record = buildDayRecord("2026-11-01", alice, listOf(sport, essen), mapOf("sport" to 5, "essen" to 3))
        val before = record.scorePercent

        // Gewicht später ändern: der gespeicherte Eintrag darf sich nicht bewegen.
        val changed = sport.copy(weight = 10)
        assertEquals(before, record.scorePercent, 0.0001)
        assertEquals(3, record.snapshots.first { it.categoryId == "sport" }.weight)
        assertEquals(10, changed.weight)
    }

    @Test
    fun `geloeschte Kategorie bleibt im Snapshot mit Namen erhalten`() {
        val record = buildDayRecord("2026-11-01", alice, listOf(sport), mapOf("sport" to 4))
        val archivedSport = sport.copy(archived = true)

        val snapshot = record.snapshots.single()
        assertEquals("Sport", snapshot.name)
        assertEquals(4, snapshot.stars)
        // Archiviert zählt nicht mehr in neuen Einträgen.
        assertEquals(0, buildDayRecord("2026-11-02", alice, listOf(archivedSport), emptyMap()).snapshots.size)
    }

    @Test
    fun `nur eigene Kategorien fliessen ein`() {
        val bobCat = Category("bob-sport", "bob", "Joggen", 2)
        val record = buildDayRecord("2026-11-01", alice, listOf(sport, bobCat), mapOf("sport" to 5))
        assertEquals(listOf("sport"), record.snapshots.map { it.categoryId })
    }

    @Test
    fun `Tagesschluessel richtet sich nach der Zeitzone`() {
        val berlin = ZoneId.of("Europe/Berlin")
        // 2026-10-31 23:30 UTC ist in Berlin bereits der 1. November.
        assertEquals("2026-11-01", dayKeyOf(Instant.parse("2026-10-31T23:30:00Z"), berlin))
        assertEquals("2026-10-31", dayKeyOf(Instant.parse("2026-10-31T23:30:00Z"), ZoneId.of("UTC")))
    }
}
