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
import com.trenits.fivestar.data.AuthLinks
import com.trenits.fivestar.data.AuthRepository
import com.trenits.fivestar.data.InvalidInviteException
import com.trenits.fivestar.data.LocalProfileStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository()
    private val store = LocalProfileStore(application)

    val user: StateFlow<FirebaseUser?> = repository.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, Firebase.auth.currentUser)

    var busy by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    /** Adresse, an die der Anmeldelink gesendet wurde. */
    var linkSentTo by mutableStateOf<String?>(null)
        private set

    /** Ein Link kam an, aber die Mailadresse ist auf diesem Gerät nicht bekannt. */
    var linkNeedsEmail by mutableStateOf(false)
        private set

    private var incomingLink: String? = null

    init {
        viewModelScope.launch {
            AuthLinks.incoming.collect { link ->
                if (link != null) {
                    AuthLinks.incoming.value = null
                    onIncomingLink(link)
                }
            }
        }
    }

    /** Sendet den Anmeldelink. Der Einladungscode wird gemerkt und nach der Anmeldung eingelöst. */
    fun sendLink(email: String, inviteCode: String) = run {
        val clean = email.trim()
        if (inviteCode.isNotBlank()) store.savePendingRedeemCode(inviteCode)
        repository.sendSignInLink(clean)
        store.savePendingEmail(clean)
        linkSentTo = clean
    }

    /** Ein Link wurde geöffnet: mit gespeicherter Adresse direkt anmelden, sonst nachfragen. */
    fun onIncomingLink(link: String) {
        if (!repository.isSignInLink(link)) return
        incomingLink = link
        val email = store.pendingEmail()
        if (email == null) {
            linkNeedsEmail = true
        } else {
            finishSignIn(email, link)
        }
    }

    /** Mailadresse nachtragen, wenn der Link auf einem anderen Gerät geöffnet wurde. */
    fun confirmEmailForLink(email: String) {
        val link = incomingLink ?: return
        finishSignIn(email.trim(), link)
    }

    private fun finishSignIn(email: String, link: String) = run {
        repository.completeSignIn(email, link)
        store.clearPendingEmail()
        incomingLink = null
        linkNeedsEmail = false
        linkSentTo = null
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