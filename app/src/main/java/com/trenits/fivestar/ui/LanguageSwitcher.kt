package com.trenits.fivestar.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.trenits.fivestar.R

enum class AppLanguage(val tag: String, val flag: Int, val label: Int) {
    GERMAN("de", R.drawable.flag_de, R.string.language_de),
    ENGLISH("en", R.drawable.flag_gb, R.string.language_en),
    FRENCH("fr", R.drawable.flag_fr, R.string.language_fr),
}

@Composable
fun LanguageSwitcher(modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val currentCode = configuration.locales[0].language
    val current = AppLanguage.entries.firstOrNull { it.tag == currentCode } ?: AppLanguage.GERMAN
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }, modifier = modifier) {
        Image(
            painter = painterResource(current.flag),
            contentDescription = stringResource(R.string.language_switch),
            modifier = Modifier.size(width = 28.dp, height = 20.dp),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(language.flag),
                                contentDescription = null,
                                modifier = Modifier.size(width = 28.dp, height = 20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(language.label))
                        }
                    },
                    onClick = {
                        expanded = false
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
                    },
                )
            }
        }
    }
}
