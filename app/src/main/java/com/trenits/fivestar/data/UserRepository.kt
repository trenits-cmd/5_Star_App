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
    /** Einladung, die ich als Einlader erstellt habe und auf deine Bestätigung wartet. */
    val activeInviteCode: String? = null,
    /** Einladung, bei der ich als Partner um Beitritt gebeten habe. */
    val pendingInviteCode: String? = null,
)

/** Zustand einer Einladung. status: open, requested, confirmed oder declined. */
data class InviteState(
    val code: String,
    val status: String,
    val coupleId: String,
    val requestedBy: String?,
    val requestedEmail: String?,
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
                    activeInviteCode = snapshot?.getString("activeInviteCode")?.takeIf { it.isNotBlank() },
                    pendingInviteCode = snapshot?.getString("pendingInviteCode")?.takeIf { it.isNotBlank() },
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

    /** Erstellt eine neue Einladung. Der Partner muss sie danach anfordern und du bestätigst sie. */
    suspend fun createInvite(coupleId: String, invitedEmail: String? = null): String {
        val code = generateInviteCode()
        val data = mutableMapOf<String, Any>(
            "coupleId" to coupleId,
            "createdBy" to uid,
            "status" to "open",
        )
        if (!invitedEmail.isNullOrBlank()) {
            data["invitedEmail"] = invitedEmail.trim()
        }
        Firebase.firestore.collection("invites").document(code).set(data).await()
        user.set(mapOf("activeInviteCode" to code), SetOptions.merge()).await()
        return code
    }

    /** Beobachtet eine Einladung. Emittiert null, sobald sie auf dem Server nicht mehr existiert. */
    fun inviteFlow(code: String): Flow<InviteState?> = callbackFlow {
        val registration = Firebase.firestore.collection("invites").document(code)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    // Berechtigungsfehler: der letzte bekannte Stand bleibt erhalten, die App stürzt nicht ab.
                    return@addSnapshotListener
                }
                if (snapshot.metadata.isFromCache && !snapshot.exists()) return@addSnapshotListener
                val coupleId = snapshot.getString("coupleId")
                trySend(
                    if (!snapshot.exists() || coupleId == null) {
                        null
                    } else {
                        InviteState(
                            code = code,
                            status = snapshot.getString("status") ?: "open",
                            coupleId = coupleId,
                            requestedBy = snapshot.getString("requestedBy"),
                            requestedEmail = snapshot.getString("requestedEmail"),
                        )
                    },
                )
            }
        awaitClose { registration.remove() }
    }

    /** Partner fordert den Beitritt an. Die Regeln prüfen, ob die E-Mail zur Einladung passt. */
    suspend fun requestJoin(input: String, email: String) {
        val code = normalizeInviteCode(input)
        val invite = Firebase.firestore.collection("invites").document(code).get().await()
        if (!invite.exists() || invite.getString("status") != "open") throw InvalidInviteException()
        val invitedEmail = invite.getString("invitedEmail")
        if (!invitedEmail.isNullOrBlank() && !invitedEmail.equals(email, ignoreCase = true)) {
            throw InvalidInviteException()
        }
        invite.reference.update(
            mapOf("status" to "requested", "requestedBy" to uid, "requestedEmail" to email),
        ).await()
        user.set(mapOf("pendingInviteCode" to code), SetOptions.merge()).await()
    }

    /** Einlader bestätigt die Anfrage: Partner wird Mitglied des Paares, die Einladung wird bestätigt. */
    suspend fun confirmRequest(invite: InviteState) {
        val requestedBy = invite.requestedBy ?: throw InvalidInviteException()
        Firebase.firestore.collection("couples").document(invite.coupleId)
            .update("members", FieldValue.arrayUnion(requestedBy)).await()
        Firebase.firestore.collection("invites").document(invite.code)
            .update("status", "confirmed").await()
        user.update(mapOf("activeInviteCode" to FieldValue.delete())).await()
    }

    /** Einlader lehnt die Anfrage ab. */
    suspend fun declineRequest(invite: InviteState) {
        Firebase.firestore.collection("invites").document(invite.code)
            .update("status", "declined").await()
        user.update(mapOf("activeInviteCode" to FieldValue.delete())).await()
    }

    /** Partner übernimmt die bestätigte Einladung und löscht sie danach. */
    suspend fun finishJoin(invite: InviteState) {
        user.set(mapOf("coupleId" to invite.coupleId, "pendingInviteCode" to FieldValue.delete()), SetOptions.merge())
            .await()
        Firebase.firestore.collection("invites").document(invite.code).delete().await()
    }

    /** Partner zieht die Anfrage zurück oder räumt eine abgelehnte Einladung auf. */
    suspend fun cancelJoin(code: String) {
        user.update(mapOf("pendingInviteCode" to FieldValue.delete())).await()
        try {
            Firebase.firestore.collection("invites").document(code).delete().await()
        } catch (_: Exception) {
            // Die Einladung existiert nicht mehr; der lokale Zustand ist bereits bereinigt.
        }
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
