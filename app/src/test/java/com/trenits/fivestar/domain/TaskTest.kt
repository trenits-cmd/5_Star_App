package com.trenits.fivestar.domain

import org.junit.Assert.assertEquals
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
    fun `Partner-Bestaetigung setzt offene Aufgabe auf bestaetigt`() {
        assertEquals(TaskStatus.CONFIRMED, task().confirmedByPartner().status)
    }

    @Test
    fun `Partner-Bestaetigung ueberschreibt keine automatische Bestaetigung`() {
        assertEquals(
            TaskStatus.AUTO_CONFIRMED,
            task(TaskStatus.AUTO_CONFIRMED).confirmedByPartner().status,
        )
    }
}
