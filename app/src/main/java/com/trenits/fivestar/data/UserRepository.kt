package com.trenits.fivestar.data

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.trenits.fivestar.domain.generateInviteCode
import com.trenits.fivestar.domain.normalizeInviteCode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val coupleId: String?,
    val catalogSeeded: Boolean,
    val displayName: String? = null,
    val photoBase64: String? = null,
)

class InvalidInviteException : Exception()

class UserRepository(private val uid: String) {

    private val user get() = Firebase.firestore.collection("users").document(uid)

    fun profile(): Flow<UserProfile> = callbackFlow {
        val registration = user.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Berechtigungsfehler: der letzte bekannte Stand bleibt erhalten, die App stürzt nicht ab.
                return@addSnapshotListener
            }
            trySend(
                UserProfile(
                    coupleId = snapshot?.getString("coupleId"),
                    catalogSeeded = snapshot?.getBoolean("catalogSeeded") ?: false,
                    displayName = snapshot?.getString("displayName")?.takeIf { it.isNotBlank() },
                    photoBase64 = snapshot?.getString("photoBase64")?.takeIf { it.isNotBlank() },
                ),
            )
        }
        awaitClose { registration.remove() }
    }

    suspend fun createCouple(): String {
        val ref = Firebase.firestore.collection("couples").document()
        ref.set(mapOf("members" to listOf(uid))).await()
        user.set(mapOf("coupleId" to ref.id), SetOptions.merge()).await()
        return ref.id
    }

    suspend fun createInvite(coupleId: String, invitedEmail: String? = null): String {
        val code = generateInviteCode()
        val data = mutableMapOf<String, Any>(
            "coupleId" to coupleId,
            "createdBy" to uid,
        )
        if (!invitedEmail.isNullOrBlank()) {
            data["invitedEmail"] = invitedEmail.trim()
        }
        Firebase.firestore.collection("invites").document(code).set(data).await()
        return code
    }

    /** Tritt dem Paar bei. Die Regeln erlauben das nur, solange noch ein Platz frei ist. */
    suspend fun redeemInvite(input: String) {
        val invite = Firebase.firestore.collection("invites").document(normalizeInviteCode(input)).get().await()
        val coupleId = invite.getString("coupleId") ?: throw InvalidInviteException()
        Firebase.firestore.collection("couples").document(coupleId)
            .update("members", FieldValue.arrayUnion(uid)).await()
        user.set(mapOf("coupleId" to coupleId), SetOptions.merge()).await()
        invite.reference.delete().await()
    }

    /**
     * Entfernt den Partner zu Testzwecken aus dem Paar (bzw. legt ein neues Einzel-Paar an,
     * falls die Server-Regeln das Entfernen aus dem bestehenden Dokument blockieren).
     */
    suspend fun removePartner(currentCoupleId: String?) {
        if (currentCoupleId != null) {
            try {
                Firebase.firestore.collection("couples").document(currentCoupleId)
                    .update("members", listOf(uid)).await()
                return
            } catch (_: Exception) {
                // Fallback: neues Einzel-Paar für diesen Nutzer anlegen
            }
        }
        createCouple()
    }

    fun markCatalogSeeded() {
        user.set(mapOf("catalogSeeded" to true), SetOptions.merge())
    }

    fun setDisplayName(name: String) {
        user.set(mapOf("displayName" to name.trim()), SetOptions.merge())
    }

    fun setPhotoBase64(base64: String?) {
        user.set(mapOf("photoBase64" to (base64 ?: "")), SetOptions.merge())
    }

    /** Anzeigename einer anderen Person, sonst deren E-Mail. Nur im selben Paar lesbar. */
    fun nameOf(otherUid: String): Flow<String?> = callbackFlow {
        val registration = Firebase.firestore.collection("users").document(otherUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Berechtigungsfehler: der letzte bekannte Stand bleibt erhalten, die App stürzt nicht ab.
                    return@addSnapshotListener
                }
                val name = snapshot?.getString("displayName")?.takeIf { it.isNotBlank() }
                    ?: snapshot?.getString("email")
                trySend(name)
            }
        awaitClose { registration.remove() }
    }

    /** Profilfoto (Base64) einer Person im selben Paar. */
    fun photoOf(otherUid: String): Flow<String?> = callbackFlow {
        val registration = Firebase.firestore.collection("users").document(otherUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Berechtigungsfehler: der letzte bekannte Stand bleibt erhalten, die App stürzt nicht ab.
                    return@addSnapshotListener
                }
                val photo = snapshot?.getString("photoBase64")?.takeIf { it.isNotBlank() }
                trySend(photo)
            }
        awaitClose { registration.remove() }
    }
}
