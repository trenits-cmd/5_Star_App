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

data class UserProfile(val coupleId: String?, val catalogSeeded: Boolean)

class InvalidInviteException : Exception()

class UserRepository(private val uid: String) {

    private val user get() = Firebase.firestore.collection("users").document(uid)

    fun profile(): Flow<UserProfile> = callbackFlow {
        val registration = user.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(UserProfile(snapshot?.getString("coupleId"), snapshot?.getBoolean("catalogSeeded") ?: false))
        }
        awaitClose { registration.remove() }
    }

    suspend fun createCouple(): String {
        val ref = Firebase.firestore.collection("couples").document()
        ref.set(mapOf("members" to listOf(uid))).await()
        user.set(mapOf("coupleId" to ref.id), SetOptions.merge()).await()
        return ref.id
    }

    suspend fun createInvite(coupleId: String): String {
        val code = generateInviteCode()
        Firebase.firestore.collection("invites").document(code)
            .set(mapOf("coupleId" to coupleId, "createdBy" to uid)).await()
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

    fun markCatalogSeeded() {
        user.set(mapOf("catalogSeeded" to true), SetOptions.merge())
    }

    fun setDisplayName(name: String) {
        user.set(mapOf("displayName" to name.trim()), SetOptions.merge())
    }

    /** Anzeigename einer anderen Person, sonst deren E-Mail. Nur im selben Paar lesbar. */
    fun nameOf(otherUid: String): Flow<String?> = callbackFlow {
        val registration = Firebase.firestore.collection("users").document(otherUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val name = snapshot?.getString("displayName")?.takeIf { it.isNotBlank() }
                    ?: snapshot?.getString("email")
                trySend(name)
            }
        awaitClose { registration.remove() }
    }
}
