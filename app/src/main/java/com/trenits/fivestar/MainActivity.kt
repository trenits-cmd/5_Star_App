package com.trenits.fivestar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trenits.fivestar.ui.AppRoot
import com.trenits.fivestar.ui.AppViewModel
import com.trenits.fivestar.ui.AuthViewModel
import com.trenits.fivestar.ui.LoginScreen
import com.trenits.fivestar.ui.theme.FiveStarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FiveStarTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
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
