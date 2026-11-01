package com.trenits.fivestar.data

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
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

class AuthRepository {

    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val auth = Firebase.auth
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithEmail(email: String, password: String) {
        val user = Firebase.auth.signInWithEmailAndPassword(email, password).await().user
        user?.let { saveProfile(it) }
    }

    suspend fun createAccount(email: String, password: String) {
        val user = Firebase.auth.createUserWithEmailAndPassword(email, password).await().user
        user?.let { saveProfile(it) }
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

    /** Ohne await: Firestore schreibt lokal sofort und synchronisiert, sobald eine Verbindung besteht. */
    private fun saveProfile(user: FirebaseUser) {
        Firebase.firestore.collection("users").document(user.uid)
            .set(mapOf("email" to user.email, "displayName" to user.displayName), SetOptions.merge())
    }
}
