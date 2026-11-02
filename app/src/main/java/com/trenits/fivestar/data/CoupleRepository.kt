package com.trenits.fivestar.data

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.trenits.fivestar.domain.CatalogItem
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.CategoryKind
import com.trenits.fivestar.domain.CategorySnapshot
import com.trenits.fivestar.domain.Counters
import com.trenits.fivestar.domain.DayRecord
import com.trenits.fivestar.domain.Task
import com.trenits.fivestar.domain.TaskStatus
import com.trenits.fivestar.domain.TaskType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Instant

/** Liste aus einem Listener. fromCache ist true, solange der Stand nur lokal bekannt ist. */
data class Listed<T>(val items: List<T>, val fromCache: Boolean)

/**
 * Zugriff auf die Daten eines Paares. Schreibvorgänge laufen lokal sofort
 * und werden synchronisiert, sobald eine Verbindung besteht.
 */
class CoupleRepository(private val coupleId: String) {

    private val couple get() = Firebase.firestore.collection("couples").document(coupleId)

    fun members(): Flow<Listed<String>> = callbackFlow {
        val registration = couple.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val members = (snapshot?.get("members") as? List<*>)?.filterIsInstance<String>().orEmpty()
            trySend(Listed(members, snapshot?.metadata?.isFromCache ?: true))
        }
        awaitClose { registration.remove() }
    }

    fun categories(): Flow<Listed<Category>> = listen(couple.collection("categories")) { doc ->
        val owner = doc.getString("ownerUid") ?: return@listen null
        val targets = (doc.get("targets") as? List<*>)?.filterIsInstance<String>()
        val notifications = (doc.get("notifications") as? List<*>)?.filterIsInstance<String>().orEmpty()
        Category(
            id = doc.id,
            ownerUid = owner,
            name = doc.getString("name") ?: "",
            weight = doc.getLong("weight")?.toInt() ?: 1,
            archived = doc.getBoolean("archived") ?: false,
            kind = doc.getString("kind")?.let { runCatching { CategoryKind.valueOf(it) }.getOrNull() } ?: CategoryKind.STARS,
            targets = if (targets.isNullOrEmpty()) listOf(owner) else targets,
            goal = doc.getDouble("goal") ?: 0.0,
            icon = doc.getString("icon") ?: "",
            notifications = notifications,
            pushEnabled = doc.getBoolean("pushEnabled") ?: true,
            pushIntervalHours = doc.getLong("pushIntervalHours")?.toInt() ?: 0,
            pushHour = doc.getLong("pushHour")?.toInt() ?: 20,
            pushMinute = doc.getLong("pushMinute")?.toInt() ?: 0,
        )
    }

    fun saveCategory(
        ownerUid: String,
        name: String,
        weight: Int,
        kind: CategoryKind,
        targets: List<String>,
        goal: Double,
        icon: String = "",
        notifications: List<String> = emptyList(),
        pushEnabled: Boolean = true,
        pushIntervalHours: Int = 0,
        pushHour: Int = 20,
        pushMinute: Int = 0,
        id: String? = null,
    ) {
        val collection = couple.collection("categories")
        collection.document(id ?: collection.document().id).set(
            mapOf(
                "ownerUid" to ownerUid,
                "name" to name,
                "weight" to weight,
                "archived" to false,
                "kind" to kind.name,
                "targets" to targets,
                "goal" to goal,
                "icon" to icon,
                "notifications" to notifications,
                "pushEnabled" to pushEnabled,
                "pushIntervalHours" to pushIntervalHours,
                "pushHour" to pushHour,
                "pushMinute" to pushMinute,
            ),
        )
    }

    fun setCategoryPushEnabled(id: String, enabled: Boolean) {
        couple.collection("categories").document(id).update("pushEnabled", enabled)
    }

    fun archiveCategory(id: String) {
        couple.collection("categories").document(id).update("archived", true)
    }

    fun catalog(): Flow<Listed<CatalogItem>> = listen(couple.collection("catalog")) { doc ->
        val owner = doc.getString("ownerUid") ?: return@listen null
        val targets = (doc.get("targets") as? List<*>)?.filterIsInstance<String>()
        CatalogItem(
            id = doc.id,
            ownerUid = owner,
            type = doc.getString("type")?.let { TaskType.valueOf(it) } ?: return@listen null,
            title = doc.getString("title") ?: "",
            targets = if (targets.isNullOrEmpty()) listOf(owner) else targets,
        )
    }

    fun saveCatalogItem(item: CatalogItem) {
        val collection = couple.collection("catalog")
        collection.document(item.id.ifEmpty { collection.document().id }).set(
            mapOf(
                "ownerUid" to item.ownerUid,
                "type" to item.type.name,
                "title" to item.title,
                "targets" to item.targets,
            ),
        )
    }

    fun deleteCatalogItem(id: String) {
        couple.collection("catalog").document(id).delete()
    }

    fun days(): Flow<Listed<DayRecord>> = listen(couple.collection("days")) { doc ->
        @Suppress("UNCHECKED_CAST")
        val snapshots = (doc.get("snapshots") as? List<Map<String, Any>>)?.map { m ->
            CategorySnapshot(
                categoryId = m["categoryId"] as? String ?: "",
                name = m["name"] as? String ?: "",
                weight = (m["weight"] as? Number)?.toInt() ?: 0,
                stars = (m["stars"] as? Number)?.toInt() ?: 0,
            )
        } ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val rawCounters = doc.get("counters") as? Map<String, Map<String, Any>>
        val counters: Counters = rawCounters.orEmpty().mapValues { (_, values) ->
            values.mapValues { (_, n) -> (n as? Number)?.toDouble() ?: 0.0 }
        }
        DayRecord(
            dayKey = doc.getString("dayKey") ?: return@listen null,
            ownerUid = doc.getString("ownerUid") ?: return@listen null,
            snapshots = snapshots,
            counters = counters,
            confirmedAt = doc.getLong("confirmedAt"),
        )
    }

    /**
     * Speichert den Tageseintrag und synchronisiert ihn (Firestore sendet ihn sofort bzw. nach Verbindung).
     * Mit merge bleibt eine bereits gesetzte Bestätigung erhalten, wenn später weitere Werte ergänzt werden.
     */
    fun saveDay(record: DayRecord) {
        val data = mutableMapOf<String, Any>(
            "dayKey" to record.dayKey,
            "ownerUid" to record.ownerUid,
            "snapshots" to record.snapshots.map {
                mapOf(
                    "categoryId" to it.categoryId,
                    "name" to it.name,
                    "weight" to it.weight,
                    "stars" to it.stars,
                )
            },
            "counters" to record.counters,
        )
        record.confirmedAt?.let { data["confirmedAt"] = it }
        couple.collection("days").document("${record.dayKey}_${record.ownerUid}")
            .set(data, com.google.firebase.firestore.SetOptions.merge())
    }

    fun tasks(): Flow<Listed<Task>> = listen(couple.collection("tasks")) { doc ->
        val owner = doc.getString("ownerUid") ?: return@listen null
        val targets = (doc.get("targets") as? List<*>)?.filterIsInstance<String>()
        Task(
            id = doc.id,
            ownerUid = owner,
            type = doc.getString("type")?.let { TaskType.valueOf(it) } ?: return@listen null,
            title = doc.getString("title") ?: "",
            dayKey = doc.getString("dayKey") ?: "",
            status = doc.getString("status")?.let { TaskStatus.valueOf(it) } ?: TaskStatus.OPEN,
            createdAt = doc.getTimestamp("createdAt")?.let { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) }
                ?: return@listen null,
            targets = if (targets.isNullOrEmpty()) listOf(owner) else targets,
            redeemedDayKey = doc.getString("redeemedDayKey")?.takeIf { it.isNotEmpty() },
        )
    }

    fun createTask(task: Task) {
        val collection = couple.collection("tasks")
        collection.document(task.id.ifEmpty { collection.document().id }).set(
            mapOf(
                "ownerUid" to task.ownerUid,
                "type" to task.type.name,
                "title" to task.title,
                "dayKey" to task.dayKey,
                "status" to task.status.name,
                "createdAt" to Timestamp(task.createdAt.epochSecond, task.createdAt.nano),
                "targets" to task.targets,
                "redeemedDayKey" to (task.redeemedDayKey ?: ""),
            ),
        )
    }

    /** Der Besitzer wählt die Aktion aus dem eigenen Katalog / Dropdown. */
    fun setTaskTitle(id: String, title: String) {
        couple.collection("tasks").document(id).update("title", title)
    }

    /** Partner bestätigt die Einlösung. Speichert Status CONFIRMED und den Tag der Einlösung. */
    fun confirmTask(id: String, redeemedDayKey: String) {
        couple.collection("tasks").document(id).update(
            mapOf(
                "status" to TaskStatus.CONFIRMED.name,
                "redeemedDayKey" to redeemedDayKey,
            ),
        )
    }

    /** Nach 48 Stunden. Die Regeln prüfen die Frist serverseitig. */
    fun autoConfirmTask(id: String) {
        couple.collection("tasks").document(id).update("status", TaskStatus.AUTO_CONFIRMED.name)
    }

    private fun <T> listen(query: Query, map: (DocumentSnapshot) -> T?): Flow<Listed<T>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val items = snapshot?.documents?.mapNotNull(map).orEmpty()
            trySend(Listed(items, snapshot?.metadata?.isFromCache ?: true))
        }
        awaitClose { registration.remove() }
    }
}
