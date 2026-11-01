package com.trenits.fivestar.ui

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.trenits.fivestar.data.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    val user: StateFlow<FirebaseUser?> = repository.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, Firebase.auth.currentUser)

    var busy by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun signIn(email: String, password: String) = run { repository.signInWithEmail(email, password) }

    fun createAccount(email: String, password: String) = run { repository.createAccount(email, password) }

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
            } catch (e: Exception) {
                error = "Anmeldung fehlgeschlagen. Bitte prüfe deine Eingaben."
            } finally {
                busy = false
            }
        }
    }
}
