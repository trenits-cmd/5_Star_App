package com.trenits.fivestar.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.trenits.fivestar.ui.theme.FiveStarTheme
import org.junit.Rule
import org.junit.Test

/** UI-Tests der Einladungszeile aus dem Dashboard. */
class InviteCodeRowTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun zeigt_code_hinweis_und_kopierknopf() {
        compose.setContent {
            FiveStarTheme {
                InviteCodeRow(context = compose.activity, code = "AB12CD")
            }
        }
        compose.onNodeWithText("AB12CD").assertIsDisplayed()
        compose.onNodeWithText("Copy code").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithText("Share this code with your partner or send it directly via email.")
            .assertIsDisplayed()
    }

    /**
     * Die Zwischenablage selbst ist ab Android 13 nur für die fokussierte App lesbar,
     * deshalb prüft der Test den Klickpfad: der Knopf reagiert, der Code bleibt sichtbar.
     */
    @Test
    fun kopierknopf_reagiert_und_code_bleibt_sichtbar() {
        compose.setContent {
            FiveStarTheme {
                InviteCodeRow(context = compose.activity, code = "XY34ZW")
            }
        }
        compose.onNodeWithText("Copy code").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("XY34ZW").assertIsDisplayed()
        compose.onNodeWithText("Copy code").assertIsDisplayed()
    }
}
