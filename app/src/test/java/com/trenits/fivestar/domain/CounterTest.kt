package com.trenits.fivestar.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CounterTest {

    @Test
    fun `Alkohol in Litern mit Pinte 0,5 und Demi 0,25`() {
        assertEquals(0.0, alcoholLiters(emptyMap()), 0.0)
        assertEquals(1.0, alcoholLiters(mapOf(PINT to 2.0)), 0.0001)
        assertEquals(0.75, alcoholLiters(mapOf(PINT to 1.0, DEMI to 1.0)), 0.0001)
    }

    @Test
    fun `Alkohol innerhalb der Obergrenze gibt volle Sterne`() {
        assertEquals(5, alcoholStars(liters = 0.0, limit = 0.0))
        assertEquals(5, alcoholStars(liters = 1.0, limit = 1.0))
    }

    @Test
    fun `Alkohol ueber der Obergrenze gibt anteilig weniger Sterne`() {
        // Grenze 1,0, Menge 2,0 → 5 * 1 / 2 = 2,5 → 2 Sterne
        assertEquals(2, alcoholStars(liters = 2.0, limit = 1.0))
        // Grenze 0, jede Menge ist zu viel
        assertEquals(0, alcoholStars(liters = 0.25, limit = 0.0))
    }

    @Test
    fun `Wasser erreicht das Ziel mit vollen Sternen`() {
        assertEquals(5, waterStars(liters = 2.0, goal = 2.0))
        assertEquals(5, waterStars(liters = 3.0, goal = 2.0))
    }

    @Test
    fun `Wasser unter dem Ziel gibt anteilig weniger Sterne`() {
        // 1 von 2 Litern → 5 * 1 / 2 = 2,5 → 2 Sterne
        assertEquals(2, waterStars(liters = 1.0, goal = 2.0))
        assertEquals(0, waterStars(liters = 0.0, goal = 2.0))
    }

    @Test
    fun `Ohne Ziel gibt Wasser volle Sterne`() {
        assertEquals(5, waterStars(liters = 0.0, goal = 0.0))
    }

    @Test
    fun `counterStars nutzt die Art der Kategorie`() {
        val water = Category("w", "alice", "Wasser", 1, kind = CategoryKind.WATER, goal = 2.0)
        assertEquals(2, counterStars(water, mapOf(LITERS to 1.0)))
    }
}
