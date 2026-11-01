package com.trenits.fivestar.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import com.trenits.fivestar.reminder.ReminderPrefs
import com.trenits.fivestar.reminder.ReminderScheduler

@Composable
fun AppRoot(viewModel: AppViewModel) {
    val profile by viewModel.profile.collectAsState()
    when {
        profile == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        profile?.coupleId == null -> PairingScreen(viewModel)
        else -> MainTabs(viewModel)
    }
}

private enum class Tab(val label: String) {
    TODAY("Heute"),
    HISTORY("Verlauf"),
    SETTINGS("Einstellungen"),
}

@Composable
private fun MainTabs(viewModel: AppViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        ReminderScheduler.schedule(context, ReminderPrefs(context).time())
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Text(item.label.take(1)) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.TODAY -> DashboardScreen(viewModel)
                Tab.HISTORY -> HistoryScreen(viewModel)
                Tab.SETTINGS -> SettingsScreen(viewModel)
            }
        }
    }
}
