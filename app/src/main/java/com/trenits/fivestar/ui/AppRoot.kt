package com.trenits.fivestar.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.trenits.fivestar.R
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

private enum class Tab(val labelRes: Int, val icon: ImageVector) {
    TODAY(R.string.tab_today, Icons.Filled.Home),
    RELATIONSHIP(R.string.tab_relationship, Icons.Filled.Favorite),
    WEEK(R.string.tab_week, Icons.Filled.DateRange),
    PARTNER(R.string.tab_partner, Icons.Filled.Person),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings),
}

@Composable
private fun MainTabs(viewModel: AppViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val partnerUid by viewModel.partnerUid.collectAsState()

    LaunchedEffect(Unit) {
        ReminderScheduler.schedule(context, ReminderPrefs(context).time())
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val visibleTabs = if (partnerUid == null) {
        listOf(Tab.TODAY, Tab.RELATIONSHIP, Tab.WEEK, Tab.SETTINGS)
    } else {
        listOf(Tab.TODAY, Tab.RELATIONSHIP, Tab.WEEK, Tab.PARTNER, Tab.SETTINGS)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                visibleTabs.forEach { item ->
                    val label = stringResource(item.labelRes)
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.TODAY -> DashboardScreen(viewModel)
                Tab.RELATIONSHIP -> RelationshipScreen(viewModel)
                Tab.WEEK -> WeeklyScreen(viewModel)
                Tab.PARTNER -> PartnerTodayScreen(viewModel)
                Tab.SETTINGS -> SettingsScreen(viewModel)
            }
        }
    }
}
