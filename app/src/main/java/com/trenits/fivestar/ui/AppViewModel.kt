package com.trenits.fivestar.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.trenits.fivestar.R
import com.trenits.fivestar.data.CoupleRepository
import com.trenits.fivestar.data.InvalidInviteException
import com.trenits.fivestar.data.Listed
import com.trenits.fivestar.data.UserProfile
import com.trenits.fivestar.data.UserRepository
import com.trenits.fivestar.domain.CatalogItem
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.CategoryKind
import com.trenits.fivestar.domain.DayOutcome
import com.trenits.fivestar.domain.DayRecord
import com.trenits.fivestar.domain.STREAK_DAYS_FOR_REWARD
import com.trenits.fivestar.domain.Task
import com.trenits.fivestar.domain.TaskStatus
import com.trenits.fivestar.domain.TaskType
import com.trenits.fivestar.domain.availablePoolOptions
import com.trenits.fivestar.domain.buildDayRecord
import com.trenits.fivestar.domain.consecutiveSuccessDays
import com.trenits.fivestar.domain.dayKeyOf
import com.trenits.fivestar.domain.dayOutcomeFor
import com.trenits.fivestar.domain.weekKeyOf
import com.trenits.fivestar.reminder.ReminderScheduler
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

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val starterCatalog = listOf(
        R.string.seed_reward_1 to TaskType.REWARD,
        R.string.seed_reward_2 to TaskType.REWARD,
        R.string.seed_reward_3 to TaskType.REWARD,
        R.string.seed_penalty_1 to TaskType.PENALTY,
        R.string.seed_penalty_2 to TaskType.PENALTY,
        R.string.seed_penalty_3 to TaskType.PENALTY,
        R.string.seed_penalty_4 to TaskType.PENALTY,
    )

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

    /** Kategorien, die für mich gelten. */
    val targeted: StateFlow<List<Category>> = categories
        .map { list -> list.items.filter { it.appliesTo(uid) } }
        .share(emptyList())

    /** Kategorien, die ich angelegt habe, unabhängig davon, für wen sie gelten. */
    val created: StateFlow<List<Category>> = categories
        .map { list -> list.items.filter { it.ownerUid == uid && !it.archived } }
        .share(emptyList())

    val partnerName: StateFlow<String?> = partnerUid
        .flatMapLatest { id -> if (id == null) flowOf(null) else userRepository.nameOf(id) }
        .share(null)

    /** Katalogeinträge, die ich selbst verwirklichen/verwalten kann (meine eigenen). */
    val myCatalog: StateFlow<List<CatalogItem>> = catalog
        .map { list -> list.items.filter { it.ownerUid == uid } }
        .share(emptyList())

    val myToday: StateFlow<DayRecord?> = combine(days, todayKey) { list, key ->
        list.items.firstOrNull { it.ownerUid == uid && it.dayKey == key }
    }.share(null)

    val partnerToday: StateFlow<DayRecord?> = combine(days, todayKey, partnerUid) { list, key, partner ->
        list.items.firstOrNull { it.ownerUid == partner && it.dayKey == key }
    }.share(null)

    /** Schlüssel der aktuellen Woche, z. B. "2026-W44". */
    val weekKey: StateFlow<String> = clock.map { weekKeyOf(it, zone) }.share(weekKeyOf(clock.value, zone))

    /** Die letzten 7 Tage (inkl. heute) in chronologischer Reihenfolge. */
    val weekDays: StateFlow<List<String>> = clock.map { now ->
        val today = now.atZone(zone).toLocalDate()
        (6L downTo 0L).map { today.minusDays(it).toString() }
    }.share(emptyList())

    /** Tages-Ergebnisse der letzten 7 Tage (Tag → DayOutcome). */
    val weekOutcomes: StateFlow<Map<String, DayOutcome>> = combine(days, weekDays, targeted) { list, keys, cats ->
        keys.mapNotNull { dayKey ->
            val record = list.items.firstOrNull { it.ownerUid == uid && it.dayKey == dayKey } ?: return@mapNotNull null
            dayOutcomeFor(record, cats)?.let { dayKey to it }
        }.toMap()
    }.share(emptyMap())

    /** Aktuelle Serie aufeinanderfolgender erfolgreicher Tage (≥ 80 %). */
    val weekSuccessfulDays: StateFlow<Int> = combine(days, targeted, clock) { list, cats, now ->
        val today = now.atZone(zone).toLocalDate()
        val byDay = list.items.filter { it.ownerUid == uid }.associateBy { it.dayKey }
        val todayRecord = byDay[today.toString()]
        if (todayRecord != null && dayOutcomeFor(todayRecord, cats) == DayOutcome.REWARD) {
            consecutiveSuccessDays(today, byDay, cats)
        } else {
            consecutiveSuccessDays(today.minusDays(1), byDay, cats)
        }
    }.share(0)

    /** Ob eine 7-Tage-Streak-Belohnung erreicht/freigeschaltet ist. */
    val weekRewardUnlocked: StateFlow<Boolean> = combine(weekSuccessfulDays, tasks) { streak, taskList ->
        streak >= STREAK_DAYS_FOR_REWARD ||
            taskList.items.any { it.appliesTo(uid) && it.type == TaskType.REWARD && it.status == TaskStatus.OPEN }
    }.share(false)

    /** Die aktuell offene oder zuletzt freigeschaltete Belohnung. */
    val currentWeeklyReward: StateFlow<Task?> = tasks.map { list ->
        list.items
            .filter { it.appliesTo(uid) && it.type == TaskType.REWARD }
            .maxByOrNull { it.dayKey }
    }.share(null)

    /** Bestätigte/eingelöste Strafen und Belohnungen im Archiv (neueste zuerst). */
    val archivedTasks: StateFlow<List<Task>> = tasks.map { list ->
        list.items
            .filter { it.isArchived }
            .sortedWith(compareByDescending<Task> { it.redeemedDayKey ?: it.dayKey }.thenByDescending { it.createdAt })
    }.share(emptyList())

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
                starterCatalog.forEach { (res, type) ->
                    repo.saveCatalogItem(CatalogItem("", uid, type, text(res), listOf(uid)))
                }
                userRepository.markCatalogSeeded()
            }
        }
        viewModelScope.launch {
            combine(categories, days, tasks, todayKey) { _, _, _, _ -> Unit }.collect {
                evaluateYesterday()
                evaluateStreakReward()
            }
        }
        viewModelScope.launch {
            while (true) {
                clock.value = Instant.now()
                delay(60_000)
            }
        }
    }

    /**
     * Liefert die für [task] aktuell noch wählbaren Optionen im Dropdown (ohne bereits verbrauchte
     * Strafen/Belohnungen im aktuellen Durchlauf) sowie die Gesamtgröße des Pools.
     */
    fun availableChoicesFor(task: Task): Pair<List<String>, Int> {
        val poolFromCatalog = catalog.value.items
            .filter { it.type == task.type && it.appliesTo(task.ownerUid) }
            .map { it.title.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        val pool = poolFromCatalog.ifEmpty {
            starterCatalog
                .filter { (_, t) -> t == task.type }
                .map { (res, _) -> text(res) }
        }

        val usedChronological = tasks.value.items
            .filter {
                it.ownerUid == task.ownerUid &&
                    it.type == task.type &&
                    it.id != task.id &&
                    it.title.isNotBlank()
            }
            .sortedWith(compareBy<Task> { it.dayKey }.thenBy { it.createdAt })
            .map { it.title }

        return availablePoolOptions(pool, usedChronological) to pool.size
    }

    fun setStars(categoryId: String, stars: Int) {
        val repo = repo() ?: return
        val stored = myToday.value?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
        val counters = myToday.value?.counters.orEmpty()
        repo.saveDay(
            buildDayRecord(todayKey.value, uid, categories.value.items, stored + (categoryId to stars), counters),
        )
    }

    fun setCounter(categoryId: String, key: String, value: Double) {
        val repo = repo() ?: return
        val stored = myToday.value?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
        val counters = myToday.value?.counters.orEmpty().toMutableMap()
        counters[categoryId] = counters[categoryId].orEmpty() + (key to value.coerceAtLeast(0.0))
        repo.saveDay(buildDayRecord(todayKey.value, uid, categories.value.items, stored, counters))
    }

    fun saveCategory(
        name: String,
        weight: Int,
        kind: CategoryKind,
        targets: List<String>,
        goal: Double,
        icon: String = "",
        notifications: List<String> = emptyList(),
        id: String? = null,
    ) {
        repo()?.saveCategory(
            ownerUid = uid,
            name = name.trim(),
            weight = weight.coerceIn(1, 5),
            kind = kind,
            targets = targets,
            goal = goal.coerceAtLeast(0.0),
            icon = icon,
            notifications = notifications.map { it.trim() }.filter { it.isNotEmpty() },
            id = id,
        )
    }

    fun saveDisplayName(name: String) {
        userRepository.setDisplayName(name)
    }

    fun archiveCategory(id: String) {
        repo()?.archiveCategory(id)
    }

    fun saveCatalogItem(type: TaskType, title: String, targets: List<String> = listOf(uid), id: String = "") {
        repo()?.saveCatalogItem(CatalogItem(id, uid, type, title.trim(), targets))
    }

    fun deleteCatalogItem(id: String) {
        repo()?.deleteCatalogItem(id)
    }

    fun chooseTaskTitle(taskId: String, title: String) {
        repo()?.setTaskTitle(taskId, title)
    }

    /** Partner bestätigt die Einlösung; hält das heutige Datum als Tag der Einlösung fest. */
    fun confirmTask(taskId: String) {
        repo()?.confirmTask(taskId, todayKey.value)
    }

    fun sendTestPushNotifications() {
        ReminderScheduler.sendCategoryNotifications(getApplication(), targeted.value)
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

    private fun text(resId: Int): String = getApplication<Application>().getString(resId)

    /**
     * Tagesziel-Prüfung für gestern:
     * Beim Verfehlen des Tagesziels (< 80 %) entsteht eine Strafe (Auswahl per Dropdown durch den Nutzer).
     */
    private fun evaluateYesterday() {
        val repo = repo() ?: return
        if (categories.value.fromCache || days.value.fromCache || tasks.value.fromCache) return
        val yesterday = dayKeyOf(Instant.now().minus(Duration.ofDays(1)), zone)
        if (tasks.value.items.any { it.ownerUid == uid && it.dayKey == yesterday && it.type == TaskType.PENALTY }) return
        val record = days.value.items.firstOrNull { it.ownerUid == uid && it.dayKey == yesterday }
        val outcome = dayOutcomeFor(record, targeted.value) ?: return
        if (outcome == DayOutcome.PENALTY) {
            repo.createTask(
                Task(
                    id = "eval_${yesterday}_$uid",
                    ownerUid = uid,
                    type = TaskType.PENALTY,
                    title = "",
                    dayKey = yesterday,
                    status = TaskStatus.OPEN,
                    createdAt = Instant.now(),
                    targets = listOf(uid),
                ),
            )
        }
    }

    /**
     * Wochenziel / Streak-Prüfung:
     * Beim Erreichen von 7 Tagen am Stück (≥ 80 %) wird eine Belohnung freigeschaltet.
     */
    private fun evaluateStreakReward() {
        val repo = repo() ?: return
        if (categories.value.fromCache || days.value.fromCache || tasks.value.fromCache) return

        val today = Instant.now().atZone(zone).toLocalDate()
        val byDay = days.value.items.filter { it.ownerUid == uid }.associateBy { it.dayKey }
        val cats = targeted.value

        val candidateEndDays = listOf(today.minusDays(1), today)
        for (endDay in candidateEndDays) {
            val streak = consecutiveSuccessDays(endDay, byDay, cats)
            if (streak >= STREAK_DAYS_FOR_REWARD && streak % STREAK_DAYS_FOR_REWARD == 0) {
                val endKey = endDay.toString()
                val alreadyExists = tasks.value.items.any {
                    it.ownerUid == uid && it.dayKey == endKey && it.type == TaskType.REWARD
                }
                if (!alreadyExists) {
                    repo.createTask(
                        Task(
                            id = "streak_${endKey}_$uid",
                            ownerUid = uid,
                            type = TaskType.REWARD,
                            title = "",
                            dayKey = endKey,
                            status = TaskStatus.OPEN,
                            createdAt = Instant.now(),
                            targets = listOf(uid),
                        ),
                    )
                }
            }
        }
    }

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy = true
            error = null
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: InvalidInviteException) {
                error = text(R.string.error_invalid_code)
            } catch (e: Exception) {
                error = text(R.string.error_generic)
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
