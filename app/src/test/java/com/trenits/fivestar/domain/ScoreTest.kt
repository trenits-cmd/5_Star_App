package com.trenits.fivestar.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreTest {

    private val categories = listOf(
        WeightedCategory("sport", 3),
        WeightedCategory("essen", 1),
    )

    @Test
    fun `alle Kategorien voll bewertet ergibt 100 Prozent`() {
        val stars = mapOf("sport" to 5, "essen" to 5)
        assertEquals(100.0, dayScorePercent(categories, stars), 0.0001)
    }

    @Test
    fun `unbewertete Kategorie zaehlt als 0 Sterne und behaelt ihr Gewicht`() {
        val stars = mapOf("sport" to 5)
        // (3*5 + 1*0) / (3*5 + 1*5) = 15 / 20 = 75 %
        assertEquals(75.0, dayScorePercent(categories, stars), 0.0001)
    }

    @Test
    fun `gewichteter Durchschnitt`() {
        val stars = mapOf("sport" to 4, "essen" to 2)
        // (3*4 + 1*2) / 20 = 14 / 20 = 70 %
        assertEquals(70.0, dayScorePercent(categories, stars), 0.0001)
    }

    @Test
    fun `keine Kategorien ergibt 0 Prozent`() {
        assertEquals(0.0, dayScorePercent(emptyList(), emptyMap()), 0.0)
    }

    @Test
    fun `Sterne werden auf 0 bis 5 begrenzt`() {
        val stars = mapOf("sport" to 9, "essen" to -2)
        // (3*5 + 1*0) / 20 = 75 %
        assertEquals(75.0, dayScorePercent(categories, stars), 0.0001)
    }

    @Test
    fun `genau 80 Prozent ist Belohnung`() {
        val single = listOf(WeightedCategory("a", 1), WeightedCategory("b", 1))
        // (5 + 3) / 10 = 80 %
        val stars = mapOf("a" to 5, "b" to 3)
        assertEquals(DayOutcome.REWARD, dayOutcome(single, stars))
    }

    @Test
    fun `knapp unter 80 Prozent ist Strafe`() {
        val single = listOf(WeightedCategory("a", 1), WeightedCategory("b", 1))
        // (5 + 2) / 10 = 70 %
        val stars = mapOf("a" to 5, "b" to 2)
        assertEquals(DayOutcome.PENALTY, dayOutcome(single, stars))
    }

    @Test
    fun `Schwelle ist exakt ohne Rundungsfehler`() {
        val cats = listOf(WeightedCategory("a", 7), WeightedCategory("b", 3))
        // 7*4 + 3*4 = 40 von 50 = 80 % exakt
        val stars = mapOf("a" to 4, "b" to 4)
        assertEquals(DayOutcome.REWARD, dayOutcome(cats, stars))
    }
}
