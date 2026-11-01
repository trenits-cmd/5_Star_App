package com.trenits.fivestar.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.trenits.fivestar.data.CoupleRepository
import com.trenits.fivestar.data.Listed
import com.trenits.fivestar.data.UserProfile
import com.trenits.fivestar.data.UserRepository
import com.trenits.fivestar.domain.CatalogItem
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.DayOutcome
import com.trenits.fivestar.domain.DayRecord
import com.trenits.fivestar.domain.START_CATALOG
import com.trenits.fivestar.domain.Task
import com.trenits.fivestar.domain.TaskStatus
import com.trenits.fivestar.domain.TaskType
import com.trenits.fivestar.domain.buildDayRecord
import com.trenits.fivestar.domain.dayKeyOf
import com.trenits.fivestar.domain.dayOutcomeFor
import com.trenits.fivestar.domain.withAutoConfirmation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class AppViewModel : ViewModel() {

    val uid: String = Firebase.auth.currentUser?.uid.orEmpty()
    private val zone = ZoneId.systemDefault()
    private val userRepository = UserRepository(uid)
    private val clock = MutableStateFlow(Instant.now())

    val profile: StateFlow<UserProfile?> = userRepository.profile().share(null)

    private val coupleIds = profile.map { it?.coupleId }.distinctUntilChanged()

    val members = sharedFor { it.members() }
    val categories = sharedFor { it.categories() }
    val catalog = sharedFor { it.catalog() }
    val days = sharedFor { it.days() }
    val tasks = sharedFor { it.tasks() }

    val todayKey: StateFlow<String> = clock.map { dayKeyOf(it, zone) }.share(dayKeyOf(clock.value, zone))

    val partnerUid: StateFlow<String?> = members
        .map { list -> list.items.firstOrNull { it != uid } }
        .share(null)

    val myCategories: StateFlow<List<Category>> = categories
        .map { list -> list.items.filter { it.ownerUid == uid && !it.archived } }
        .share(emptyList())

    val myCatalog: StateFlow<List<CatalogItem>> = catalog
        .map { list -> list.items.filter { it.ownerUid == uid } }
        .share(emptyList())

    val myToday: StateFlow<DayRecord?> = combine(days, todayKey) { list, key ->
        list.items.firstOrNull { it.ownerUid == uid && it.dayKey == key }
    }.share(null)

    val partnerToday: StateFlow<DayRecord?> = combine(days, todayKey, partnerUid) { list, key, partner ->
        list.items.firstOrNull { it.ownerUid == partner && it.dayKey == key }
    }.share(null)

    val lastSync = MutableStateFlow<Instant?>(null)

    var busy by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var inviteCode by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            days.collect { if (!it.fromCache) lastSync.value = Instant.now() }
        }
        viewModelScope.launch {
            var seeded = false
            profile.collect { p ->
                val coupleId = p?.coupleId ?: return@collect
                if (p.catalogSeeded || seeded) return@collect
                seeded = true
                val repo = CoupleRepository(coupleId)
                START_CATALOG.forEach { repo.saveCatalogItem(CatalogItem("", uid, it.type, it.title)) }
                userRepository.markCatalogSeeded()
            }
        }
        viewModelScope.launch {
            combine(categories, days, tasks, todayKey) { _, _, _, _ -> Unit }.collect { evaluateYesterday() }
        }
        viewModelScope.launch {
            while (true) {
                clock.value = Instant.now()
                autoConfirmDue()
                delay(60_000)
            }
        }
    }

    fun setStars(categoryId: String, stars: Int) {
        val repo = repo() ?: return
        val stored = myToday.value?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
        repo.saveDay(buildDayRecord(todayKey.value, uid, myCategories.value, stored + (categoryId to stars)))
    }

    fun saveCategory(name: String, weight: Int, id: String? = null) {
        repo()?.saveCategory(uid, name.trim(), weight.coerceIn(1, 5), id)
    }

    fun archiveCategory(id: String) {
        repo()?.archiveCategory(id)
    }

    fun saveCatalogItem(type: TaskType, title: String) {
        repo()?.saveCatalogItem(CatalogItem("", uid, type, title.trim()))
    }

    fun deleteCatalogItem(id: String) {
        repo()?.deleteCatalogItem(id)
    }

    fun chooseTaskTitle(taskId: String, title: String) {
        repo()?.setTaskTitle(taskId, title)
    }

    fun confirmTask(taskId: String) {
        repo()?.confirmTask(taskId)
    }

    fun createInvite() = action {
        val coupleId = profile.value?.coupleId ?: userRepository.createCouple()
        inviteCode = userRepository.createInvite(coupleId)
    }

    fun redeemInvite(code: String) = action { userRepository.redeemInvite(code) }

    fun syncNow() = action {
        val db = Firebase.firestore
        db.disableNetwork().await()
        db.enableNetwork().await()
    }

    fun signOut() {
        Firebase.auth.signOut()
    }

    private fun repo(): CoupleRepository? = profile.value?.coupleId?.let { CoupleRepository(it) }

    /** Aufgabe für gestern, wenn die eigene Bewertung vorliegt und noch keine Aufgabe existiert. */
    private fun evaluateYesterday() {
        val repo = repo() ?: return
        if (categories.value.fromCache || days.value.fromCache || tasks.value.fromCache) return
        val yesterday = dayKeyOf(Instant.now().minus(Duration.ofDays(1)), zone)
        if (tasks.value.items.any { it.ownerUid == uid && it.dayKey == yesterday }) return
        val record = days.value.items.firstOrNull { it.ownerUid == uid && it.dayKey == yesterday }
        val outcome = dayOutcomeFor(record, myCategories.value) ?: return
        repo.createTask(
            Task(
                id = "eval_${yesterday}_$uid",
                ownerUid = uid,
                type = if (outcome == DayOutcome.REWARD) TaskType.REWARD else TaskType.PENALTY,
                title = "",
                dayKey = yesterday,
                status = TaskStatus.OPEN,
                createdAt = Instant.now(),
            ),
        )
    }

    private fun autoConfirmDue() {
        val repo = repo() ?: return
        val now = Instant.now()
        tasks.value.items
            .filter { it.withAutoConfirmation(now).status != it.status }
            .forEach { repo.autoConfirmTask(it.id) }
    }

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy = true
            error = null
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                error = e.message
            } catch (e: Exception) {
                error = "Das hat nicht geklappt. Bitte prüfe deine Verbindung und versuche es erneut."
            } finally {
                busy = false
            }
        }
    }

    private fun <T> sharedFor(block: (CoupleRepository) -> Flow<Listed<T>>): StateFlow<Listed<T>> =
        coupleIds.flatMapLatest { id ->
            if (id == null) flowOf(Listed(emptyList<T>(), true)) else block(CoupleRepository(id))
        }.share(Listed(emptyList<T>(), true))

    private fun <T> Flow<T>.share(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.Eagerly, initial)
}
