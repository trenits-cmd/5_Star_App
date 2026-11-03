package com.trenits.fivestar

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import com.trenits.fivestar.data.AuthLinks
import com.trenits.fivestar.data.NotificationTargets
import com.trenits.fivestar.ui.AppRoot
import com.trenits.fivestar.ui.AppViewModel
import com.trenits.fivestar.ui.AuthViewModel
import com.trenits.fivestar.ui.LoginScreen
import com.trenits.fivestar.ui.theme.FiveStarTheme

class MainActivity : AppCompatActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLink(intent)
    }

    private fun handleLink(intent: Intent?) {
        intent?.getStringExtra("dinner_day_key")?.let { NotificationTargets.dinnerDay.value = it }
        val data = intent?.dataString ?: return
        if (data.startsWith("https://starapp-99d8e.firebaseapp.com/finishSignIn")) AuthLinks.incoming.value = data
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleLink(intent)
        setContent {
            FiveStarTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                        val authViewModel: AuthViewModel = viewModel()
                        val user by authViewModel.user.collectAsState()
                        val signedIn = user

                        if (signedIn == null) {
                            LoginScreen(authViewModel)
                        } else {
                            val appViewModel: AppViewModel = viewModel(key = signedIn.uid)
                            AppRoot(appViewModel)
                        }
                    }
                }
            }
        }
    }
}
