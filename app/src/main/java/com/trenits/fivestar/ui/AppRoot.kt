package com.trenits.fivestar.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.trenits.fivestar.R
import com.trenits.fivestar.data.NotificationTargets
import com.trenits.fivestar.reminder.ReminderPrefs
import com.trenits.fivestar.reminder.ReminderScheduler

@Composable
fun AppRoot(viewModel: AppViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    when {
        profile == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        profile?.coupleId == null -> PairingScreen(viewModel)
        else -> MainTabs(viewModel)
    }
}

private val StatisticsIcon: ImageVector = ImageVector.Builder(
    name = "Statistics",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).path(fill = SolidColor(Color.Black)) {
    moveTo(4f, 20f); lineTo(4f, 12f); lineTo(8f, 12f); lineTo(8f, 20f); close()
    moveTo(10f, 20f); lineTo(10f, 4f); lineTo(14f, 4f); lineTo(14f, 20f); close()
    moveTo(16f, 20f); lineTo(16f, 9f); lineTo(20f, 9f); lineTo(20f, 20f); close()
}.build()

private enum class Tab(val labelRes: Int, val icon: ImageVector) {
    TODAY(R.string.tab_today, Icons.Filled.Home),
    WEEK(R.string.tab_week, Icons.Filled.DateRange),
    PARTNER(R.string.tab_partner, Icons.Filled.Person),
    STATISTICS(R.string.tab_statistics, StatisticsIcon),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTabs(viewModel: AppViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val partnerUid by viewModel.partnerUid.collectAsStateWithLifecycle()
    val myName by viewModel.myName.collectAsStateWithLifecycle()
    val myPhoto by viewModel.myPhoto.collectAsStateWithLifecycle()
    val partnerName by viewModel.partnerName.collectAsStateWithLifecycle()
    val partnerPhoto by viewModel.partnerPhoto.collectAsStateWithLifecycle()
    val toast by viewModel.toastMessage.collectAsStateWithLifecycle()
    val dinnerDay by NotificationTargets.dinnerDay.collectAsStateWithLifecycle()

    LaunchedEffect(dinnerDay) {
        val day = dinnerDay ?: return@LaunchedEffect
        viewModel.focusDay(day)
        tab = Tab.TODAY
    }

    LaunchedEffect(toast) {
        val msg = toast
        if (msg != null) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.toastMessage.value = null
        }
    }

    LaunchedEffect(Unit) {
        ReminderScheduler.schedule(context, ReminderPrefs(context).time())
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val visibleTabs = listOf(Tab.TODAY, Tab.PARTNER, Tab.WEEK, Tab.STATISTICS, Tab.SETTINGS)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "5 Star App", style = MaterialTheme.typography.titleMedium) },
            )
        },
        bottomBar = {
            NavigationBar {
                visibleTabs.forEach { item ->
                    val label = when (item) {
                        Tab.TODAY -> myName ?: stringResource(item.labelRes)
                        Tab.PARTNER -> partnerName ?: stringResource(item.labelRes)
                        else -> stringResource(item.labelRes)
                    }
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = {
                            when (item) {
                                Tab.TODAY -> if (!myPhoto.isNullOrBlank()) {
                                    UserAvatar(photoBase64 = myPhoto, name = myName, size = 24.dp)
                                } else {
                                    Icon(item.icon, contentDescription = label)
                                }
                                Tab.PARTNER -> if (!partnerPhoto.isNullOrBlank()) {
                                    UserAvatar(photoBase64 = partnerPhoto, name = partnerName, size = 24.dp)
                                } else {
                                    Icon(item.icon, contentDescription = label)
                                }
                                else -> Icon(item.icon, contentDescription = label)
                            }
                        },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.TODAY -> DashboardScreen(viewModel)
                Tab.WEEK -> WeeklyScreen(viewModel)
                Tab.PARTNER -> PartnerTodayScreen(viewModel)
                Tab.STATISTICS -> StatisticsScreen(viewModel)
                Tab.SETTINGS -> SettingsScreen(viewModel)
            }
        }
    }
}
