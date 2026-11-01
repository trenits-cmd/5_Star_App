package com.trenits.fivestar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PairingScreen(viewModel: AppViewModel) {
    var code by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Paar verbinden", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "Ihr seht gegenseitig alle Kategorien, Sterne, Scores und Aufgaben. " +
                "Verbinde dich nur mit einer Person, der du das zutraust.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.createInvite() },
            enabled = !viewModel.busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Einladungscode erstellen") }

        Spacer(Modifier.height(24.dp))
        Text("oder Code eines Partners eingeben")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.uppercase() },
            label = { Text("Einladungscode") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { viewModel.redeemInvite(code) },
            enabled = !viewModel.busy && code.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Beitreten") }

        viewModel.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (viewModel.busy) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}
