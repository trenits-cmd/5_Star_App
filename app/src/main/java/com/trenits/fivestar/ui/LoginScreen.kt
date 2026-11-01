package com.trenits.fivestar.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.trenits.fivestar.ui.theme.Amber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(viewModel: AuthViewModel = viewModel()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val activity = LocalContext.current as Activity
    val canSubmit = !viewModel.busy && email.isNotBlank() && password.isNotBlank()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF4338CA)))),
            contentAlignment = Alignment.Center,
        ) {
            Text("★", fontSize = 44.sp, color = Amber)
        }
        Spacer(Modifier.height(16.dp))
        Text("5 Star", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Gemeinsam Ziele erreichen",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-Mail") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Passwort") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.signIn(email.trim(), password) },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Anmelden") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { viewModel.createAccount(email.trim(), password) },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Konto erstellen") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { viewModel.signInWithGoogle(activity) },
            enabled = !viewModel.busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Mit Google anmelden") }

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
