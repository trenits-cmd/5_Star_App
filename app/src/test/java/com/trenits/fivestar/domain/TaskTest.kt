package com.trenits.fivestar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TaskTest {

    private val created = Instant.parse("2026-11-01T20:00:00Z")

    private fun task(status: TaskStatus = TaskStatus.OPEN) = Task(
        id = "t1",
        ownerUid = "u1",
        type = TaskType.PENALTY,
        title = "Test",
        dayKey = "2026-11-01",
        status = status,
        createdAt = created,
    )

    @Test
    fun `offene Aufgabe vor 48 Stunden bleibt offen`() {
        val result = task().withAutoConfirmation(created.plusSeconds(48 * 3600 - 1))
        assertEquals(TaskStatus.OPEN, result.status)
    }

    @Test
    fun `offene Aufgabe nach genau 48 Stunden wird automatisch bestaetigt`() {
        val result = task().withAutoConfirmation(created.plusSeconds(48 * 3600))
        assertEquals(TaskStatus.AUTO_CONFIRMED, result.status)
    }

    @Test
    fun `vom Partner bestaetigte Aufgabe wird nicht automatisch geaendert`() {
        val result = task(TaskStatus.CONFIRMED).withAutoConfirmation(created.plusSeconds(100 * 3600))
        assertEquals(TaskStatus.CONFIRMED, result.status)
    }

    @Test
    fun `Partner-Bestaetigung setzt offene Aufgabe auf bestaetigt und speichert Einloesedatum`() {
        val open = task()
        assertFalse(open.isArchived)
        val confirmed = open.confirmedByPartner("2026-11-03")
        assertEquals(TaskStatus.CONFIRMED, confirmed.status)
        assertEquals("2026-11-01", confirmed.dayKey)
        assertEquals("2026-11-03", confirmed.redeemedDayKey)
        assertTrue(confirmed.isArchived)
    }

    @Test
    fun `Partner-Bestaetigung ueberschreibt keine automatische Bestaetigung`() {
        assertEquals(
            TaskStatus.AUTO_CONFIRMED,
            task(TaskStatus.AUTO_CONFIRMED).confirmedByPartner("2026-11-03").status,
        )
    }

    @Test
    fun `gewaehlte Strafe verschwindet bis alle Strafen aufgebraucht sind`() {
        val pool = listOf("Abwasch", "Outfit-Diktat", "Rücken kraulen")
        assertEquals(listOf("Abwasch", "Outfit-Diktat", "Rücken kraulen"), availablePoolOptions(pool, emptyList()))
        assertEquals(listOf("Outfit-Diktat", "Rücken kraulen"), availablePoolOptions(pool, listOf("Abwasch")))
        assertEquals(listOf("Rücken kraulen"), availablePoolOptions(pool, listOf("Abwasch", "Outfit-Diktat")))
        // Alle 3 aufgebraucht -> Reset, wieder alle verfügbar!
        assertEquals(
            listOf("Abwasch", "Outfit-Diktat", "Rücken kraulen"),
            availablePoolOptions(pool, listOf("Abwasch", "Outfit-Diktat", "Rücken kraulen")),
        )
        // Im neuen Durchlauf verschwindet die neu gewählte Strafe wieder:
        assertEquals(
            listOf("Abwasch", "Rücken kraulen"),
            availablePoolOptions(pool, listOf("Abwasch", "Outfit-Diktat", "Rücken kraulen", "Outfit-Diktat")),
        )
    }
}
