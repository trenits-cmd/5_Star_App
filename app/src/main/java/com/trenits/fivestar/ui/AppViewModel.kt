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
import com.trenits.fivestar.domain.CategoryIcon
import com.trenits.fivestar.domain.CategoryKind
import com.trenits.fivestar.domain.DEFAULT_ALCOHOL_LIMIT
import com.trenits.fivestar.domain.DEFAULT_WATER_GOAL
import com.trenits.fivestar.domain.DINNER_HOUR
import com.trenits.fivestar.domain.DINNER_MINUTE
import com.trenits.fivestar.domain.DayOutcome
import com.trenits.fivestar.domain.DayRecord
import com.trenits.fivestar.domain.MAX_STARS
import com.trenits.fivestar.domain.MealLevel
import com.trenits.fivestar.domain.RELATIONSHIP_COUNTER_KEY
import com.trenits.fivestar.domain.ROTATION_STARS
import com.trenits.fivestar.domain.ROTATION_TASK_INDEX
import com.trenits.fivestar.domain.RelationshipAspect
import com.trenits.fivestar.domain.RotationTask
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
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val starterCatalog = listOf(
        R.string.seed_reward_1 to TaskType.REWARD,
        R.string.seed_reward_2 to TaskType.REWARD,
        R.string.seed_reward_3 to TaskType.REWARD,
        R.string.seed_reward_4 to TaskType.REWARD,
        R.string.seed_penalty_1 to TaskType.PENALTY,
        R.string.seed_penalty_2 to TaskType.PENALTY,
        R.string.seed_penalty_3 to TaskType.PENALTY,
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

    val myName: StateFlow<String?> = if (uid.isEmpty()) {
        flowOf(null).share(null)
    } else {
        userRepository.nameOf(uid).share(null)
    }

    val myPhoto: StateFlow<String?> = profile.map { it?.photoBase64 }.share(null)

    val partnerName: StateFlow<String?> = partnerUid
        .flatMapLatest { id -> if (id == null) flowOf(null) else userRepository.nameOf(id) }
        .share(null)

    val partnerPhoto: StateFlow<String?> = partnerUid
        .flatMapLatest { id -> if (id == null) flowOf(null) else userRepository.photoOf(id) }
        .share(null)

    /** Katalogeinträge, die ich selbst angelegt habe. */
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

    /** Tageseinträge der aktuellen ISO-Woche (ohne heute) zur Berechnung der noch offenen Wechsel-Aufgaben. */
    val isoWeekRecordsBeforeToday: StateFlow<List<DayRecord>> = combine(days, clock) { list, now ->
        val today = now.atZone(zone).toLocalDate()
        val monday = today.with(DayOfWeek.MONDAY)
        val keysBeforeToday = (0L until 7L)
            .map { monday.plusDays(it) }
            .filter { it.isBefore(today) }
            .map { it.toString() }
            .toSet()
        list.items.filter { it.ownerUid == uid && it.dayKey in keysBeforeToday }
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
                syncSharedDinnerTimeForToday()
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

    /** Speichert die Bewertung einer Mahlzeit (Frühstück, Mittagessen, Abendessen: nichts, wenig, mittel, gesund). */
    fun setMealLevel(categoryId: String, mealKey: String, level: MealLevel) {
        setCounter(categoryId, mealKey, level.points.toDouble())
    }

    /** Speichert die heute ausgewählte Wechsel-Aufgabe und deren Sterne-Bewertung (0..5). */
    fun setRotation(categoryId: String, rotationTask: RotationTask, stars: Int) {
        val repo = repo() ?: return
        val stored = myToday.value?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
        val counters = myToday.value?.counters.orEmpty().toMutableMap()
        counters[categoryId] = counters[categoryId].orEmpty() + mapOf(
            ROTATION_TASK_INDEX to rotationTask.index.toDouble(),
            ROTATION_STARS to stars.coerceIn(0, MAX_STARS).toDouble(),
        )
        repo.saveDay(buildDayRecord(todayKey.value, uid, categories.value.items, stored, counters))
    }

    /** Speichert die Abendessen-Uhrzeit (wird von Andi eingegeben und in beiden Apps synchronisiert). */
    fun setDinnerTime(categoryId: String, hour: Int, minute: Int) {
        val repo = repo() ?: return
        val stored = myToday.value?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
        val counters = myToday.value?.counters.orEmpty().toMutableMap()
        counters[categoryId] = counters[categoryId].orEmpty() + mapOf(
            DINNER_HOUR to hour.coerceIn(0, 23).toDouble(),
            DINNER_MINUTE to minute.coerceIn(0, 59).toDouble(),
        )
        repo.saveDay(buildDayRecord(todayKey.value, uid, categories.value.items, stored, counters))
    }

    /**
     * Speichert die Sterne-Bewertung (0..5) für eine der 5 Dimensionen des Beziehungstrackers.
     */
    fun setRelationshipStars(aspect: RelationshipAspect, stars: Int) {
        val repo = repo() ?: return
        val stored = myToday.value?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
        val counters = myToday.value?.counters.orEmpty().toMutableMap()
        val currentRel = counters[RELATIONSHIP_COUNTER_KEY].orEmpty()
        counters[RELATIONSHIP_COUNTER_KEY] = currentRel + (aspect.key to stars.coerceIn(0, MAX_STARS).toDouble())
        repo.saveDay(buildDayRecord(todayKey.value, uid, categories.value.items, stored, counters))
    }

    /**
     * Synchronisiert die vom Partner (Andi) eingetragene Abendessen-Uhrzeit automatisch
     * in den eigenen Tageseintrag, damit sie in beiden Apps identisch getrackt wird.
     */
    private fun syncSharedDinnerTimeForToday() {
        val repo = repo() ?: return
        val partner = partnerToday.value ?: return
        val dinnerCategories = targeted.value.filter { it.kind == CategoryKind.DINNER_TIME }
        if (dinnerCategories.isEmpty()) return

        val myCurrent = myToday.value
        val myCounters = myCurrent?.counters.orEmpty().toMutableMap()
        var changed = false

        for (cat in dinnerCategories) {
            val partnerDinner = partner.counters[cat.id]
                ?: partner.counters.values.firstOrNull { it.containsKey(DINNER_HOUR) }
                ?: continue
            val pHour = partnerDinner[DINNER_HOUR] ?: continue
            val pMinute = partnerDinner[DINNER_MINUTE] ?: 0.0
            val mine = myCounters[cat.id].orEmpty()
            if (mine[DINNER_HOUR] != pHour || mine[DINNER_MINUTE] != pMinute) {
                myCounters[cat.id] = mine + mapOf(DINNER_HOUR to pHour, DINNER_MINUTE to pMinute)
                changed = true
            }
        }

        if (changed) {
            val stored = myCurrent?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
            repo.saveDay(buildDayRecord(todayKey.value, uid, categories.value.items, stored, myCounters))
        }
    }

    /**
     * Legt das Profil, die Kategorien (mit exakten Gewichtungen) sowie die Belohnungen & Strafen für Rosie an:
     * - Wasser trinken (3-fache Wertung, mind. 2,5 L)
     * - Alkohol-Grenze (2-fache Wertung, 0 L = 5 ★)
     * - Weitere Aufgaben im Wechsel (1-fache Wertung: Wohnung, Müll, Geschirr, Kochen, Unterricht)
     * - Abendessen vor 20h (3-fache Wertung)
     * - Essen (3-fache Wertung: Frühstück, Mittagessen, Abendessen: nichts, wenig, mittel, gesund)
     * - Belohnungen: Vinted Budget (10 € von Andi), Blumen, Massage für 30 Minuten, Essen gehen
     * - Strafen: kein Orgasmus, keine Bar
     */
    fun applyRosiePreset() {
        userRepository.setDisplayName("Rosie")
        val repo = repo() ?: return
        val bothTargets = listOfNotNull(uid, partnerUid.value).distinct()
        val myTargets = listOf(uid)

        repo.saveCategory(
            ownerUid = uid,
            name = text(R.string.cat_preset_food),
            weight = 3,
            kind = CategoryKind.MEALS,
            targets = myTargets,
            goal = 0.0,
            icon = CategoryIcon.FOOD.name,
            notifications = listOf("Frühstück, Mittagessen & Abendessen gesund eintragen 🥗"),
            id = "preset_meals_$uid",
        )
        repo.saveCategory(
            ownerUid = uid,
            name = text(R.string.cat_preset_dinner),
            weight = 3,
            kind = CategoryKind.DINNER_TIME,
            targets = bothTargets,
            goal = 20.0,
            icon = CategoryIcon.FOOD.name,
            notifications = listOf("Abendessen vor 20:00 Uhr = 5 Sterne 🍽️"),
            id = "preset_dinner_$uid",
        )
        repo.saveCategory(
            ownerUid = uid,
            name = text(R.string.cat_preset_water),
            weight = 3,
            kind = CategoryKind.WATER,
            targets = myTargets,
            goal = DEFAULT_WATER_GOAL,
            icon = CategoryIcon.WATER.name,
            notifications = listOf(
                "Mindestens 2,5 Liter Wasser heute trinken 💧",
                "Zwischenstand: Gläser oder Liter eintragen!",
            ),
            pushEnabled = true,
            pushIntervalHours = 2,
            id = "preset_water_$uid",
        )
        repo.saveCategory(
            ownerUid = uid,
            name = text(R.string.cat_preset_alcohol),
            weight = 2,
            kind = CategoryKind.ALCOHOL,
            targets = myTargets,
            goal = DEFAULT_ALCOHOL_LIMIT,
            icon = CategoryIcon.ALCOHOL.name,
            notifications = listOf("0 Liter Alkohol = 5 Sterne ⭐"),
            id = "preset_alcohol_$uid",
        )
        repo.saveCategory(
            ownerUid = uid,
            name = text(R.string.cat_preset_rotation),
            weight = 1,
            kind = CategoryKind.ROTATION,
            targets = myTargets,
            goal = 0.0,
            icon = CategoryIcon.HOME.name,
            notifications = listOf("Heutige Wechsel-Aufgabe auswählen (Unterricht / Haushalt) 🏠"),
            id = "preset_rotation_$uid",
        )

        // Rosie Belohnungen & Strafen
        val rosieRewards = listOf(
            text(R.string.seed_reward_1),
            text(R.string.seed_reward_2),
            text(R.string.seed_reward_3),
            text(R.string.seed_reward_4),
        )
        rosieRewards.forEachIndexed { idx, title ->
            repo.saveCatalogItem(CatalogItem("rosie_reward_${idx}_$uid", uid, TaskType.REWARD, title, myTargets))
        }
        val rosiePenalties = listOf(
            text(R.string.seed_penalty_1),
            text(R.string.seed_penalty_2),
        )
        rosiePenalties.forEachIndexed { idx, title ->
            repo.saveCatalogItem(CatalogItem("rosie_penalty_${idx}_$uid", uid, TaskType.PENALTY, title, myTargets))
        }
    }

    /**
     * Legt das Profil, die Kategorien sowie Belohnungen & Strafen für Andi an:
     * - Sport (3-fache Wertung)
     * - Abendessen vor 20h (3-fache Wertung, Andi gibt die Uhrzeit ein)
     * - Strafe: Cooking
     */
    fun applyAndiPreset() {
        userRepository.setDisplayName("Andi")
        val repo = repo() ?: return
        val bothTargets = listOfNotNull(uid, partnerUid.value).distinct()
        val myTargets = listOf(uid)

        repo.saveCategory(
            ownerUid = uid,
            name = text(R.string.cat_preset_sport),
            weight = 3,
            kind = CategoryKind.STARS,
            targets = myTargets,
            goal = 0.0,
            icon = CategoryIcon.FITNESS.name,
            notifications = listOf("Zeit für Sport heute 💪"),
            id = "preset_sport_$uid",
        )
        repo.saveCategory(
            ownerUid = uid,
            name = text(R.string.cat_preset_dinner),
            weight = 3,
            kind = CategoryKind.DINNER_TIME,
            targets = bothTargets,
            goal = 20.0,
            icon = CategoryIcon.FOOD.name,
            notifications = listOf("Abendessen-Uhrzeit eintragen (vor 20h = 5 ★) 🍽️"),
            id = "preset_dinner_$uid",
        )

        repo.saveCatalogItem(
            CatalogItem("andi_penalty_0_$uid", uid, TaskType.PENALTY, text(R.string.seed_penalty_3), myTargets),
        )
    }

    fun saveCategory(
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
        repo()?.saveCategory(
            ownerUid = uid,
            name = name.trim(),
            weight = weight.coerceIn(1, 5),
            kind = kind,
            targets = targets,
            goal = goal.coerceAtLeast(0.0),
            icon = icon,
            notifications = notifications.map { it.trim() }.filter { it.isNotEmpty() },
            pushEnabled = pushEnabled,
            pushIntervalHours = pushIntervalHours.coerceIn(0, 12),
            pushHour = pushHour.coerceIn(0, 23),
            pushMinute = pushMinute.coerceIn(0, 59),
            id = id,
        )
    }

    fun setCategoryPushEnabled(id: String, enabled: Boolean) {
        repo()?.setCategoryPushEnabled(id, enabled)
    }

    fun saveDisplayName(name: String) {
        userRepository.setDisplayName(name)
    }

    fun savePhoto(base64: String?) {
        userRepository.setPhotoBase64(base64)
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

    fun invitePartnerByEmail(email: String, onCodeReady: (String) -> Unit) = action {
        val coupleId = profile.value?.coupleId ?: userRepository.createCouple()
        val code = inviteCode ?: userRepository.createInvite(coupleId, email)
        inviteCode = code
        onCodeReady(code)
    }

    fun removePartnerForTest() = action {
        userRepository.removePartner(profile.value?.coupleId)
        inviteCode = null
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
