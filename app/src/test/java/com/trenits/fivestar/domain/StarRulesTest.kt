package com.trenits.fivestar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class StarRulesTest {

    @Test
    fun `Schlafenszeit vor Mitternacht 5 Sterne, danach abnehmend, ohne Eintrag 0`() {
        assertEquals(5, bedtimeStars(23))
        assertEquals(5, bedtimeStars(12))
        assertEquals(4, bedtimeStars(0))
        assertEquals(3, bedtimeStars(1))
        assertEquals(2, bedtimeStars(2))
        assertEquals(0, bedtimeStars(3))
        assertEquals(0, bedtimeStars(-1))
    }

    @Test
    fun `Schlafenszeit-Kategorie ohne Uhrzeit ergibt 0 und mit Uhrzeit die Sterne`() {
        val bedtime = Category("b", "andi", "Schlaf", 2, kind = CategoryKind.BEDTIME)
        assertEquals(0, counterStars(bedtime, emptyMap()))
        assertEquals(4, counterStars(bedtime, mapOf(BEDTIME_HOUR to 0.0)))
    }

    @Test
    fun `Abendessen vor 20h 5 Sterne, 20h-Stunde 3 Sterne, ab 21h 0`() {
        assertEquals(5, dinnerStars(19, 30))
        assertEquals(3, dinnerStars(20, 0))
        assertEquals(3, dinnerStars(20, 59))
        assertEquals(0, dinnerStars(21, 0))
        assertEquals(0, dinnerStars(-1))
    }

    @Test
    fun `Essen mit allen drei gesund gibt 5, mit nichts gibt 0, Mischung anteilig`() {
        assertEquals(5, mealStars(MealLevel.HEALTHY, MealLevel.HEALTHY, MealLevel.HEALTHY))
        assertEquals(0, mealStars(MealLevel.NONE, MealLevel.NONE, MealLevel.NONE))
        assertEquals(3, mealStars(MealLevel.HEALTHY, MealLevel.HEALTHY, MealLevel.NONE))
    }

    @Test
    fun `Lesen Grenzen 5 Seiten 0 Sterne, 6 Seiten 1 Stern, 50 Seiten 4, 51 Seiten 5`() {
        assertEquals(0, readingStars(5))
        assertEquals(1, readingStars(6))
        assertEquals(4, readingStars(50))
        assertEquals(5, readingStars(51))
    }

    @Test
    fun `Sport ab 45 Minuten 5 Sterne, darunter anteilig mit Abrunden`() {
        assertEquals(0, sportStars(0))
        assertEquals(2, sportStars(22))
        assertEquals(5, sportStars(45))
        assertEquals(5, sportStars(60))
    }

    @Test
    fun `Sport-Ruhetag mit Freigabe gibt 5 Sterne, ohne Eintrag 0`() {
        val sport = Category("s", "andi", "Sport", 2, kind = CategoryKind.SPORT)
        assertEquals(5, counterStars(sport, mapOf(SPORT_MINUTES to 0.0, SPORT_REST_DAY_OK to 1.0)))
        assertEquals(0, counterStars(sport, emptyMap()))
        assertEquals(0, counterStars(sport, mapOf(SPORT_MINUTES to 0.0)))
    }

    @Test
    fun `ISO-Wochenschluessel Jahreswechsel richtig zugeordnet`() {
        assertEquals("2026-W01", weekKeyOf(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC))
        assertEquals("2025-W52", weekKeyOf(Instant.parse("2025-12-28T12:00:00Z"), ZoneOffset.UTC))
    }

    @Test
    fun `Belohnungsstufen je Serienlaenge`() {
        assertEquals(3, rewardTierForStreak(3))
        assertEquals(2, rewardTierForStreak(5))
        assertEquals(1, rewardTierForStreak(7))
        assertNull(rewardTierForStreak(4))
        assertEquals(5, rewardDaysForTier(2))
        assertEquals(7, rewardDaysForTier(1))
        assertEquals(0, rewardDaysForTier(9))
    }
}
