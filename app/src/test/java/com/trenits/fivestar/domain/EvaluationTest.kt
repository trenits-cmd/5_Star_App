package com.trenits.fivestar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.Assert.assertTrue
import java.time.LocalDate
import kotlin.random.Random

class EvaluationTest {

    private val today = LocalDate.of(2026, 11, 1)
    private val sport = Category("sport", "alice", "Sport", 3)
    private val essen = Category("essen", "alice", "Essen", 1)

    private fun record(vararg snapshots: CategorySnapshot) =
        DayRecord("2026-10-31", "alice", snapshots.toList())

    @Test
    fun `Zeitraeume beginnen an den richtigen Tagen`() {
        assertEquals(LocalDate.of(2026, 10, 26), Period.SEVEN_DAYS.startDay(today))
        assertEquals(LocalDate.of(2026, 10, 3), Period.THIRTY_DAYS.startDay(today))
        assertEquals(LocalDate.of(2026, 1, 1), Period.THIS_YEAR.startDay(today))
        assertNull(Period.ALL.startDay(today))
    }

    @Test
    fun `Eintrag mit Sternen ueber 80 Prozent ist Belohnung`() {
        val r = record(
            CategorySnapshot("sport", "Sport", 3, 5),
            CategorySnapshot("essen", "Essen", 1, 5),
        )
        assertEquals(DayOutcome.REWARD, dayOutcomeFor(r, listOf(sport, essen)))
    }

    @Test
    fun `Eintrag unter 80 Prozent ist Strafe`() {
        val r = record(
            CategorySnapshot("sport", "Sport", 3, 2),
            CategorySnapshot("essen", "Essen", 1, 5),
        )
        assertEquals(DayOutcome.PENALTY, dayOutcomeFor(r, listOf(sport, essen)))
    }

    @Test
    fun `fehlender Eintrag mit aktiven Kategorien ist Strafe`() {
        assertEquals(DayOutcome.PENALTY, dayOutcomeFor(null, listOf(sport, essen)))
    }

    @Test
    fun `ohne Kategorien entsteht keine Aufgabe`() {
        assertNull(dayOutcomeFor(null, emptyList()))
        assertNull(dayOutcomeFor(null, listOf(sport.copy(archived = true))))
    }

    @Test
    fun `leerer Eintrag zaehlt nicht als Belohnung`() {
        assertEquals(DayOutcome.PENALTY, dayOutcomeFor(record(), listOf(sport)))
    }

    @Test
    fun `Einladungscode hat erwartete Laenge und Zeichen`() {
        val code = generateInviteCode(Random(42))
        assertEquals(INVITE_CODE_LENGTH, code.length)
        assertTrue(code.all { it in "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" })
    }

    @Test
    fun `Eingabe des Einladungscodes wird normalisiert`() {
        assertEquals("ABC123", normalizeInviteCode("  abc123 "))
    }
}
