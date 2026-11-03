package com.trenits.fivestar.data

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.trenits.fivestar.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Domain für den Anmeldelink. Android öffnet Links auf dieser Domain direkt in der App (App Links). */
private const val SIGN_IN_LINK_URL = "https://starapp-99d8e.firebaseapp.com/finishSignIn"
private const val APP_PACKAGE = "com.trenits.fivestar"

class AuthRepository {

    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val auth = Firebase.auth
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Schickt einen Anmeldelink an die Mail-Adresse. Ohne Zugriff auf das Postfach kann niemand
     * sich anmelden. Der Einladungscode wird vorher lokal gemerkt und nach der Anmeldung eingelöst.
     */
    suspend fun sendSignInLink(email: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) throw InvalidInviteException()
        val settings = ActionCodeSettings.newBuilder()
            .setUrl(SIGN_IN_LINK_URL)
            .setHandleCodeInApp(true)
            .setAndroidPackageName(APP_PACKAGE, true, "26")
            .build()
        Firebase.auth.sendSignInLinkToEmail(cleanEmail, settings).await()
    }

    fun isSignInLink(link: String): Boolean = Firebase.auth.isSignInWithEmailLink(link)

    /** Meldet mit Mail-Adresse und Link an. Lege Konten bei Bedarf an, speichert das Profil. */
    suspend fun completeSignIn(email: String, link: String) {
        val user = Firebase.auth.signInWithEmailLink(email.trim(), link).await().user
            ?: throw InvalidInviteException()
        saveProfile(user)
    }

    suspend fun signInWithGoogle(activity: Activity) {
        val option = GetSignInWithGoogleOption.Builder(activity.getString(R.string.default_web_client_id)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = CredentialManager.create(activity).getCredential(activity, request)
        val idToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
        val user = Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().user
        user?.let { saveProfile(it) }
    }

    fun signOut() {
        Firebase.auth.signOut()
    }

    /**
     * Löscht das aktuell angemeldete Firebase-Auth-Konto. Schlägt das fehl, weil die Anmeldung zu alt ist,
     * wird abgemeldet, damit sich der Nutzer neu per Mail-Link anmelden kann.
     */
    suspend fun deleteCurrentUserAndSignOut() {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            runCatching { currentUser.delete().await() }
        }
        runCatching { Firebase.auth.signOut() }
    }

    /** Ohne await: Firestore schreibt lokal sofort und synchronisiert, sobald eine Verbindung besteht. */
    private fun saveProfile(user: FirebaseUser) {
        Firebase.firestore.collection("users").document(user.uid)
            .set(mapOf("email" to user.email, "displayName" to user.displayName), SetOptions.merge())
    }
}