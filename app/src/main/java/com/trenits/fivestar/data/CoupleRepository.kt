package com.trenits.fivestar.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.trenits.fivestar.domain.CatalogItem
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.CategorySnapshot
import com.trenits.fivestar.domain.DayRecord
import com.trenits.fivestar.domain.Task
import com.trenits.fivestar.domain.TaskStatus
import com.trenits.fivestar.domain.TaskType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Instant

/**
 * Zugriff auf die Daten eines Paares. Schreibvorgänge laufen lokal sofort
 * und werden synchronisiert, sobald eine Verbindung besteht.
 */
class CoupleRepository(private val coupleId: String) {

    private val couple get() = Firebase.firestore.collection("couples").document(coupleId)

    fun categories(): Flow<List<Category>> = listen(couple.collection("categories")) { doc ->
        Category(
            id = doc.id,
            ownerUid = doc.getString("ownerUid") ?: return@listen null,
            name = doc.getString("name") ?: "",
            weight = doc.getLong("weight")?.toInt() ?: 1,
            archived = doc.getBoolean("archived") ?: false,
        )
    }

    fun saveCategory(ownerUid: String, name: String, weight: Int, id: String? = null) {
        val ref = couple.collection("categories").document(id ?: couple.collection("categories").document().id)
        ref.set(mapOf("ownerUid" to ownerUid, "name" to name, "weight" to weight, "archived" to false))
    }

    fun archiveCategory(id: String) {
        couple.collection("categories").document(id).update("archived", true)
    }

    fun catalog(): Flow<List<CatalogItem>> = listen(couple.collection("catalog")) { doc ->
        CatalogItem(
            id = doc.id,
            ownerUid = doc.getString("ownerUid") ?: return@listen null,
            type = doc.getString("type")?.let { TaskType.valueOf(it) } ?: return@listen null,
            title = doc.getString("title") ?: "",
        )
    }

    fun saveCatalogItem(item: CatalogItem) {
        couple.collection("catalog").document(item.id.ifEmpty { couple.collection("catalog").document().id })
            .set(mapOf("ownerUid" to item.ownerUid, "type" to item.type.name, "title" to item.title))
    }

    fun deleteCatalogItem(id: String) {
        couple.collection("catalog").document(id).delete()
    }

    fun days(): Flow<List<DayRecord>> = listen(couple.collection("days")) { doc ->
        @Suppress("UNCHECKED_CAST")
        val snapshots = (doc.get("snapshots") as? List<Map<String, Any>>)?.map { m ->
            CategorySnapshot(
                categoryId = m["categoryId"] as? String ?: "",
                name = m["name"] as? String ?: "",
                weight = (m["weight"] as? Number)?.toInt() ?: 0,
                stars = (m["stars"] as? Number)?.toInt() ?: 0,
            )
        } ?: emptyList()
        DayRecord(
            dayKey = doc.getString("dayKey") ?: return@listen null,
            ownerUid = doc.getString("ownerUid") ?: return@listen null,
            snapshots = snapshots,
        )
    }

    fun saveDay(record: DayRecord) {
        couple.collection("days").document("${record.dayKey}_${record.ownerUid}").set(
            mapOf(
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
            ),
        )
    }

    fun tasks(): Flow<List<Task>> = listen(couple.collection("tasks")) { doc ->
        Task(
            id = doc.id,
            ownerUid = doc.getString("ownerUid") ?: return@listen null,
            type = doc.getString("type")?.let { TaskType.valueOf(it) } ?: return@listen null,
            title = doc.getString("title") ?: "",
            dayKey = doc.getString("dayKey") ?: "",
            status = doc.getString("status")?.let { TaskStatus.valueOf(it) } ?: TaskStatus.OPEN,
            createdAt = doc.getTimestamp("createdAt")?.toInstant() ?: return@listen null,
        )
    }

    fun createTask(task: Task) {
        val ref = couple.collection("tasks").document(task.id.ifEmpty { couple.collection("tasks").document().id })
        ref.set(
            mapOf(
                "ownerUid" to task.ownerUid,
                "type" to task.type.name,
                "title" to task.title,
                "dayKey" to task.dayKey,
                "status" to task.status.name,
                "createdAt" to Timestamp(task.createdAt.epochSecond, task.createdAt.nano),
            ),
        )
    }

    /** Partner bestätigt. Die Regeln lassen nur OPEN → CONFIRMED durch den Partner zu. */
    fun confirmTask(id: String) {
        couple.collection("tasks").document(id).update("status", TaskStatus.CONFIRMED.name)
    }

    /** Nach 48 Stunden. Die Regeln prüfen die Frist serverseitig. */
    fun autoConfirmTask(id: String) {
        couple.collection("tasks").document(id).update("status", TaskStatus.AUTO_CONFIRMED.name)
    }

    private fun <T> listen(query: Query, map: (DocumentSnapshot) -> T?): Flow<List<T>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents?.mapNotNull(map).orEmpty())
        }
        awaitClose { registration.remove() }
    }
}

private fun Timestamp.toInstant(): Instant = Instant.ofEpochSecond(seconds, nanoseconds.toLong())
