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
    /** Vorlage für Kategorien: "main_user" (Einlader) oder "partner" (Eingeladener). */
    val presetRole: String? = null,
    /** Tag, ab dem die Serie für Belohnungen zählt (nach jeder Auswahl neu). */
    val rewardBaseline: String? = null,
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
                    presetRole = snapshot?.getString("presetRole")?.takeIf { it.isNotBlank() },
                    rewardBaseline = snapshot?.getString("rewardBaseline")?.takeIf { it.isNotBlank() },
                ),
            )
        }
        awaitClose { registration.remove() }
    }

    suspend fun createCouple(): String {
        val ref = Firebase.firestore.collection("couples").document()
        ref.set(mapOf("members" to listOf(uid))).await()
        user.set(mapOf("coupleId" to ref.id, "presetRole" to "main_user"), SetOptions.merge()).await()
        return ref.id
    }

    /** Erstellt eine neue Einladung oder verwendet einen bereits offenen Code wieder. */
    suspend fun createInvite(coupleId: String, invitedEmail: String? = null, forceNew: Boolean = false): String {
        val userSnap = runCatching { user.get().await() }.getOrNull()
        val existingCode = userSnap?.getString("activeInviteCode")?.trim()?.takeIf { it.isNotEmpty() }
        if (forceNew && existingCode != null) {
            runCatching { Firebase.firestore.collection("invites").document(existingCode).delete().await() }
            runCatching { user.update(mapOf("activeInviteCode" to FieldValue.delete())).await() }
        }
        if (!forceNew && existingCode != null) {
            val existingDoc = runCatching {
                Firebase.firestore.collection("invites").document(existingCode).get().await()
            }.getOrNull()
            if (existingDoc != null && existingDoc.exists() && existingDoc.getString("status") == "open") {
                if (!invitedEmail.isNullOrBlank()) {
                    runCatching {
                        existingDoc.reference.update("invitedEmail", invitedEmail.trim()).await()
                    }
                }
                return existingCode
            }
        }
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

    /**
     * Partner löst den Einladungscode ein:
     * Validiert gegen /invites/{code}, fügt den Partner automatisch zu couples/{coupleId}.members hinzu
     * und setzt den Status der Einladung auf "confirmed".
     */
    suspend fun requestJoin(input: String, email: String) {
        val code = normalizeInviteCode(input)
        val invite = Firebase.firestore.collection("invites").document(code).get().await()
        if (!invite.exists()) {
            val existingUser = runCatching { user.get().await() }.getOrNull()
            if (!existingUser?.getString("coupleId").isNullOrBlank()) return
            throw InvalidInviteException()
        }
        val status = invite.getString("status")
        val invitedEmail = invite.getString("invitedEmail")
        if (!invitedEmail.isNullOrBlank() && email.isNotBlank() && !invitedEmail.equals(email, ignoreCase = true)) {
            throw InvalidInviteException()
        }
        val coupleId = invite.getString("coupleId") ?: throw InvalidInviteException()
        if (status == "confirmed") {
            runCatching {
                Firebase.firestore.collection("couples").document(coupleId)
                    .update("members", FieldValue.arrayUnion(uid)).await()
            }
            user.set(mapOf("coupleId" to coupleId, "presetRole" to "partner", "pendingInviteCode" to FieldValue.delete()), SetOptions.merge()).await()
            return
        }
        if (status != "open" && status != "requested") throw InvalidInviteException()
        runCatching {
            Firebase.firestore.collection("couples").document(coupleId)
                .update("members", FieldValue.arrayUnion(uid)).await()
        }
        invite.reference.update(
            mapOf("status" to "confirmed", "requestedBy" to uid, "requestedEmail" to email),
        ).await()
        user.set(mapOf("coupleId" to coupleId, "presetRole" to "partner", "pendingInviteCode" to FieldValue.delete()), SetOptions.merge()).await()
    }

    /** Einlader räumt activeInviteCode nach Erhalt der Benachrichtigung auf (lässt den bestätigten Code für spätere Logins bestehen). */
    suspend fun acknowledgeConfirmedInvite(code: String) {
        runCatching { user.update(mapOf("activeInviteCode" to FieldValue.delete())).await() }
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
        user.set(mapOf("coupleId" to invite.coupleId, "presetRole" to "partner", "pendingInviteCode" to FieldValue.delete()), SetOptions.merge())
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

    fun setRewardBaseline(dayKey: String) {
        user.set(mapOf("rewardBaseline" to dayKey), SetOptions.merge())
    }

    fun setPresetRole(role: String) {
        user.set(mapOf("presetRole" to role), SetOptions.merge())
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

    /** Mail-Adresse einer Person im selben Paar (zur eindeutigen Zuordnung). */
    fun emailOf(otherUid: String): Flow<String?> = callbackFlow {
        val registration = Firebase.firestore.collection("users").document(otherUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.getString("email")?.takeIf { it.isNotBlank() })
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

    /**
     * Löscht alle mit diesem Nutzer verknüpften Firestore-Daten (eigene Kategorien, Katalog-Einträge,
     * Tageseinträge, Aufgaben, offene Einladungen, Paar-Mitgliedschaft sowie das /users/{uid}-Dokument).
     * Jeder Einzelschritt ist in runCatching gekapselt, damit keinerlei Exception zum Absturz führt.
     */
    suspend fun deleteUserData(
        coupleId: String?,
        activeInviteCode: String? = null,
        pendingInviteCode: String? = null,
    ) {
        val db = Firebase.firestore

        if (!activeInviteCode.isNullOrBlank()) {
            runCatching { db.collection("invites").document(activeInviteCode).delete().await() }
        }
        if (!pendingInviteCode.isNullOrBlank()) {
            runCatching { db.collection("invites").document(pendingInviteCode).delete().await() }
        }
        runCatching {
            val createdInvites = db.collection("invites").whereEqualTo("createdBy", uid).get().await()
            for (doc in createdInvites.documents) {
                runCatching { doc.reference.delete().await() }
            }
        }
        runCatching {
            val requestedInvites = db.collection("invites").whereEqualTo("requestedBy", uid).get().await()
            for (doc in requestedInvites.documents) {
                runCatching { doc.reference.delete().await() }
            }
        }

        if (!coupleId.isNullOrBlank()) {
            val coupleRef = db.collection("couples").document(coupleId)
            for (subcollection in listOf("categories", "catalog", "days", "tasks")) {
                runCatching {
                    val snap = coupleRef.collection(subcollection)
                        .whereEqualTo("ownerUid", uid)
                        .get()
                        .await()
                    for (doc in snap.documents) {
                        runCatching { doc.reference.delete().await() }
                    }
                }
            }
            runCatching {
                coupleRef.update("members", FieldValue.arrayRemove(uid)).await()
            }
        }

        runCatching {
            user.delete().await()
        }
    }
}
