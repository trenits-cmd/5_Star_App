package com.trenits.fivestar.ui

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.trenits.fivestar.R
import com.trenits.fivestar.data.AuthRepository
import com.trenits.fivestar.data.InvalidInviteException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository()

    val user: StateFlow<FirebaseUser?> = repository.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, Firebase.auth.currentUser)

    var busy by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun signInWithEmailAndCode(email: String, code: String) = run {
        repository.signInWithEmailAndCode(email, code)
    }

    fun signInWithGoogle(activity: Activity) = run { repository.signInWithGoogle(activity) }

    fun signOut() = repository.signOut()

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy = true
            error = null
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: GetCredentialCancellationException) {
                // Der Nutzer hat den Google-Dialog geschlossen. Das ist kein Fehler.
            } catch (e: InvalidInviteException) {
                error = getApplication<Application>().getString(R.string.error_invalid_code)
            } catch (e: Exception) {
                error = getApplication<Application>().getString(R.string.error_auth_failed)
            } finally {
                busy = false
            }
        }
    }
}
