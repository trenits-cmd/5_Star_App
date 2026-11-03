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
import com.trenits.fivestar.domain.normalizeInviteCode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class AuthRepository {

    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val auth = Firebase.auth
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun derivePassword(email: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("5Star_Auth_${email.trim().lowercase()}".toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }.take(20)
        return "5S!$hex"
    }

    /**
     * Meldet den Nutzer allein mit E-Mail-Adresse und Einladungscode an (ohne separates Passwort):
     * Erstellt bei Bedarf automatisch das Firebase-Konto, validiert den Einladungscode gegen /invites/{code}
     * und verknüpft das Paar automatisch.
     */
    suspend fun signInWithEmailAndCode(email: String, code: String) {
        val cleanEmail = email.trim()
        val cleanCode = normalizeInviteCode(code)
        if (cleanEmail.isBlank() || cleanCode.isBlank()) throw InvalidInviteException()

        val derivedPwd = derivePassword(cleanEmail)
        val codePwd = "${cleanCode}_5Star!"
        val user = runCatching {
            Firebase.auth.createUserWithEmailAndPassword(cleanEmail, derivedPwd).await().user
        }.getOrElse {
            runCatching {
                Firebase.auth.signInWithEmailAndPassword(cleanEmail, derivedPwd).await().user
            }.getOrElse {
                Firebase.auth.signInWithEmailAndPassword(cleanEmail, codePwd).await().user
            }
        } ?: throw InvalidInviteException()

        saveProfile(user)
        try {
            UserRepository(user.uid).requestJoin(cleanCode, cleanEmail)
        } catch (e: InvalidInviteException) {
            val snap = runCatching {
                Firebase.firestore.collection("users").document(user.uid).get().await()
            }.getOrNull()
            if (snap?.getString("coupleId").isNullOrBlank()) {
                Firebase.auth.signOut()
                throw e
            }
        }
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
