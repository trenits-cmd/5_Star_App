package com.trenits.fivestar.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.trenits.fivestar.domain.DEMI
import com.trenits.fivestar.domain.GLASSES
import com.trenits.fivestar.domain.LITERS
import com.trenits.fivestar.domain.MEAL_BREAKFAST
import com.trenits.fivestar.domain.MEAL_DINNER
import com.trenits.fivestar.domain.MEAL_LUNCH
import com.trenits.fivestar.domain.PAGES
import com.trenits.fivestar.domain.PINT
import com.trenits.fivestar.domain.SPORT_MINUTES
import com.trenits.fivestar.ui.theme.FiveStarTheme
import org.junit.Rule
import org.junit.Test

/** UI-Tests der gemeinsamen Zähler-Zusammenfassung (Dashboard-Karten und Archiv). */
class CounterSummaryTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(values: Map<String, Double>, name: String = "Wasser", showName: Boolean = true) {
        compose.setContent {
            FiveStarTheme {
                CounterSummary(name = name, values = values, showName = showName)
            }
        }
    }

    @Test
    fun wasser_zeigt_glaeser_und_summe() {
        show(mapOf(GLASSES to 8.0))
        compose.onNodeWithText("Glasses (0.25 L each): 8").assertIsDisplayed()
        compose.onNodeWithText("Total today: 2.00 liters").assertIsDisplayed()
    }

    @Test
    fun wasser_blendet_zusatzliter_bei_null_aus() {
        show(mapOf(GLASSES to 4.0))
        compose.onNodeWithText("Additional amount in liters (decimal): 0.5").assertDoesNotExist()
        compose.onNodeWithText("Total today: 1.00 liters").assertIsDisplayed()
    }

    @Test
    fun wasser_zeigt_zusatzliter_und_summe() {
        show(mapOf(GLASSES to 8.0, LITERS to 0.5))
        compose.onNodeWithText("Additional amount in liters (decimal): 0.5").assertIsDisplayed()
        compose.onNodeWithText("Total today: 2.50 liters").assertIsDisplayed()
    }

    @Test
    fun alkohol_zeigt_pinte_demi_und_summe() {
        show(mapOf(PINT to 1.0, DEMI to 2.0), name = "Alkohol")
        compose.onNodeWithText("Pint (0.5 L): 1").assertIsDisplayed()
        compose.onNodeWithText("Demi (0.25 L): 2").assertIsDisplayed()
        compose.onNodeWithText("Total today: 1.00 liters").assertIsDisplayed()
    }

    @Test
    fun essen_zeigt_die_drei_stufen() {
        show(
            mapOf(MEAL_BREAKFAST to 3.0, MEAL_LUNCH to 2.0, MEAL_DINNER to 1.0),
            name = "Essen",
        )
        compose.onNodeWithText("Breakfast: healthy · Lunch: medium · Dinner: little").assertIsDisplayed()
    }

    @Test
    fun lesen_zeigt_die_seitenzahl() {
        show(mapOf(PAGES to 42.0), name = "Lesen")
        compose.onNodeWithText("42 pages read").assertIsDisplayed()
    }

    @Test
    fun sport_zeigt_die_minuten() {
        show(mapOf(SPORT_MINUTES to 45.0), name = "Sport")
        compose.onNodeWithText("45 min today").assertIsDisplayed()
    }

    @Test
    fun kategorie_name_wird_mit_showName_false_ausgeblendet() {
        show(mapOf(GLASSES to 2.0), name = "Wasser", showName = false)
        compose.onNodeWithText("Wasser").assertDoesNotExist()
    }
}
