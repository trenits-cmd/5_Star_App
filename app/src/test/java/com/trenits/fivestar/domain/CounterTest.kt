package com.trenits.fivestar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class CounterTest {

    @Test
    fun `Alkohol in Litern mit Pinte 0,5, Demi 0,25 und zusaetzlichen Dezimal-Litern`() {
        assertEquals(0.0, alcoholLiters(emptyMap()), 0.0)
        assertEquals(1.0, alcoholLiters(mapOf(PINT to 2.0)), 0.0001)
        assertEquals(0.75, alcoholLiters(mapOf(PINT to 1.0, DEMI to 1.0)), 0.0001)
        assertEquals(1.10, alcoholLiters(mapOf(PINT to 1.0, DEMI to 1.0, LITERS to 0.35)), 0.0001)
    }

    @Test
    fun `Alkohol bei 0 Litern gibt 5 Sterne, darueber prozentual weniger`() {
        assertEquals(5, alcoholStars(liters = 0.0, limit = 1.25))
        // 1 Demi (0,25 L) bei Grenze 1,25 L -> 5 * (1 - 0,2) = 4 Sterne
        assertEquals(4, alcoholStars(liters = 0.25, limit = 1.25))
        // 1 Pinte (0,50 L) bei Grenze 1,25 L -> 5 * (1 - 0,4) = 3 Sterne
        assertEquals(3, alcoholStars(liters = 0.50, limit = 1.25))
        // Ab der Grenze -> 0 Sterne
        assertEquals(0, alcoholStars(liters = 1.25, limit = 1.25))
        assertEquals(0, alcoholStars(liters = 0.25, limit = 0.0))
    }

    @Test
    fun `Wasser aus Glaesern und Dezimal-Litern mit 2,5 L Ziel`() {
        // 4 Gläser (1,0 L) + 1,5 L Dezimal = 2,5 L -> 5 Sterne
        val values = mapOf(GLASSES to 4.0, LITERS to 1.5)
        assertEquals(2.5, waterLiters(values), 0.0001)
        assertEquals(5, waterStars(liters = waterLiters(values), goal = 2.5))
        assertEquals(5, waterStars(liters = 3.2, goal = 2.5))
    }

    @Test
    fun `Wasser unter 2,5 L gibt prozentual weniger Sterne mit Abrunden`() {
        // 2,0 von 2,5 L -> 5 * 2,0 / 2,5 = 4 Sterne
        assertEquals(4, waterStars(liters = 2.0, goal = 2.5))
        // 1,2 von 2,5 L -> 5 * 1,2 / 2,5 = 2,4 -> abgerundet 2 Sterne
        assertEquals(2, waterStars(liters = 1.2, goal = 2.5))
        // 0,4 von 2,5 L -> 5 * 0,4 / 2,5 = 0,8 -> abgerundet 0 Sterne
        assertEquals(0, waterStars(liters = 0.4, goal = 2.5))
    }

    @Test
    fun `Abendessen vor 20h gibt 5 Sterne, vor 21h 1 Stern, sonst 0 Sterne`() {
        assertEquals(5, dinnerStars(19, 30))
        assertEquals(5, dinnerStars(19, 59))
        assertEquals(1, dinnerStars(20, 0))
        assertEquals(1, dinnerStars(20, 45))
        assertEquals(0, dinnerStars(21, 0))
        assertEquals(0, dinnerStars(22, 15))
    }

    @Test
    fun `Wechsel-Aufgaben rotieren nach Wochen-Soll`() {
        val catId = "rot"
        // Müll (1x/Woche) bereits erledigt:
        val mon = DayRecord(
            dayKey = "2026-11-02",
            ownerUid = "rosie",
            snapshots = emptyList(),
            counters = mapOf(
                catId to mapOf(
                    ROTATION_TASK_INDEX to RotationTask.TAKE_OUT_TRASH.index.toDouble(),
                    ROTATION_STARS to 5.0,
                ),
            ),
        )
        val available = availableRotationTasks(catId, listOf(mon))
        assertFalse(RotationTask.TAKE_OUT_TRASH in available)
        assertTrue(RotationTask.LESSON_PLANNING in available)
        assertEquals(RotationTask.LESSON_PLANNING, suggestedRotationTask(DayOfWeek.WEDNESDAY, available))
    }

    @Test
    fun `counterStars nutzt die Art der Kategorie`() {
        val water = Category("w", "rosie", "Wasser trinken", 3, kind = CategoryKind.WATER, goal = 2.5)
        assertEquals(4, counterStars(water, mapOf(GLASSES to 4.0, LITERS to 1.0)))

        val dinner = Category("d", "andi", "Abendessen vor 20h", 5, kind = CategoryKind.DINNER_TIME)
        assertEquals(5, counterStars(dinner, mapOf(DINNER_HOUR to 19.0, DINNER_MINUTE to 15.0)))
        assertEquals(1, counterStars(dinner, mapOf(DINNER_HOUR to 20.0, DINNER_MINUTE to 30.0)))

        val meals = Category("m", "rosie", "Essen", 3, kind = CategoryKind.MEALS)
        assertEquals(
            5,
            counterStars(
                meals,
                mapOf(
                    MEAL_BREAKFAST to MealLevel.HEALTHY.points.toDouble(),
                    MEAL_LUNCH to MealLevel.HEALTHY.points.toDouble(),
                    MEAL_DINNER to MealLevel.HEALTHY.points.toDouble(),
                ),
            ),
        )
        assertEquals(
            3,
            mealStars(MealLevel.MEDIUM, MealLevel.MEDIUM, MealLevel.MEDIUM),
        )
        assertEquals(
            0,
            mealStars(MealLevel.NONE, MealLevel.NONE, MealLevel.NONE),
        )
    }

    @Test
    fun `Lesen berechnet Sterne nach Seitenanzahl (0 bis 5 ist 0, ueber 50 ist 5)`() {
        assertEquals(0, readingStars(0))
        assertEquals(0, readingStars(5))
        assertEquals(1, readingStars(6))
        assertEquals(2, readingStars(20))
        assertEquals(3, readingStars(30))
        assertEquals(4, readingStars(50))
        assertEquals(5, readingStars(51))
        assertEquals(5, readingStars(120))
    }

    @Test
    fun `Beziehungstracker berechnet Einzel- und Paar-Score ueber alle 5 Dimensionen`() {
        assertEquals(5, RelationshipAspect.entries.size)
        val record = DayRecord(
            dayKey = "2026-11-02",
            ownerUid = "rosie",
            snapshots = emptyList(),
            counters = mapOf(
                RELATIONSHIP_COUNTER_KEY to mapOf(
                    RelationshipAspect.MOOD_GENERAL.key to 4.0,
                    RelationshipAspect.MOOD_RELATIONSHIP.key to 5.0,
                    RelationshipAspect.COMMUNICATION.key to 4.0,
                    RelationshipAspect.INTIMACY.key to 5.0,
                    RelationshipAspect.SEX.key to 2.0,
                ),
            ),
        )
        val stars = relationshipStarsOf(record)
        assertEquals(4, stars[RelationshipAspect.MOOD_GENERAL])
        assertEquals(5, stars[RelationshipAspect.MOOD_RELATIONSHIP])
        assertEquals(4, stars[RelationshipAspect.COMMUNICATION])
        assertEquals(5, stars[RelationshipAspect.INTIMACY])
        assertEquals(2, stars[RelationshipAspect.SEX])
        // Summe = 20 von 25 möglichen Sternen -> 80 %
        assertEquals(80.0, relationshipScorePercent(stars) ?: 0.0, 0.0001)

        val partnerStars = RelationshipAspect.entries.associateWith { 5 }
        // (80 % + 100 %) / 2 = 90 %
        assertEquals(90.0, coupleRelationshipScorePercent(stars, partnerStars) ?: 0.0, 0.0001)
    }
}
