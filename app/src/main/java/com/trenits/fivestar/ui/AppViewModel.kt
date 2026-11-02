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
import com.trenits.fivestar.data.InviteState
import com.trenits.fivestar.data.InvalidInviteException
import com.trenits.fivestar.data.Listed
import com.trenits.fivestar.data.LocalProfileStore
import com.trenits.fivestar.data.UserProfile
import com.trenits.fivestar.data.UserRepository
import com.trenits.fivestar.domain.CatalogItem
import com.trenits.fivestar.domain.Category
import com.trenits.fivestar.domain.CategoryIcon
import com.trenits.fivestar.domain.CategoryKind
import com.trenits.fivestar.domain.DEFAULT_ALCOHOL_LIMIT
import com.trenits.fivestar.domain.DEFAULT_SPORT_GOAL_MINUTES
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
import com.trenits.fivestar.domain.SPORT_MINUTES
import com.trenits.fivestar.domain.SPORT_REST_DAY_OK
import com.trenits.fivestar.domain.SPORT_WEEKLY_DAYS_GOAL
import com.trenits.fivestar.domain.STREAK_DAYS_FOR_REWARD
import com.trenits.fivestar.domain.Task
import com.trenits.fivestar.domain.TaskStatus
import com.trenits.fivestar.domain.TaskType
import com.trenits.fivestar.domain.availablePoolOptions
import com.trenits.fivestar.domain.buildDayRecord
import com.trenits.fivestar.domain.consecutiveSuccessDays
import com.trenits.fivestar.domain.dayKeyOf
import com.trenits.fivestar.domain.dayOutcomeFor
import com.trenits.fivestar.domain.missingCategoryIds
import com.trenits.fivestar.domain.missingRelationshipAspects
import com.trenits.fivestar.domain.needsPartnerConfirmationReminder
import com.trenits.fivestar.domain.penaltyCycleStartDayKey
import com.trenits.fivestar.domain.sportDaysInWeek
import com.trenits.fivestar.domain.weekKeyOf
import com.trenits.fivestar.domain.withAutoConfirmation
import com.trenits.fivestar.reminder.ReminderPrefs
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val rosieStarterCatalog = listOf(
        R.string.seed_reward_1 to TaskType.REWARD,
        R.string.seed_reward_2 to TaskType.REWARD,
        R.string.seed_reward_3 to TaskType.REWARD,
        R.string.seed_reward_4 to TaskType.REWARD,
        R.string.seed_reward_5 to TaskType.REWARD,
        R.string.seed_penalty_1 to TaskType.PENALTY,
        R.string.seed_penalty_2 to TaskType.PENALTY,
        R.string.seed_penalty_3 to TaskType.PENALTY,
    )

    private val andiStarterCatalog = listOf(
        R.string.seed_reward_andi_1 to TaskType.REWARD,
        R.string.seed_reward_andi_2 to TaskType.REWARD,
        R.string.seed_reward_andi_3 to TaskType.REWARD,
        R.string.seed_reward_andi_4 to TaskType.REWARD,
        R.string.seed_reward_andi_5 to TaskType.REWARD,
        R.string.seed_reward_andi_6 to TaskType.REWARD,
        R.string.seed_reward_andi_7 to TaskType.REWARD,
        R.string.seed_penalty_andi_1 to TaskType.PENALTY,
    )

    val uid: String = Firebase.auth.currentUser?.uid.orEmpty()
    private val zone = ZoneId.systemDefault()
    private val userRepository = UserRepository(uid)
    private val localProfileStore = LocalProfileStore(getApplication())
    private val clock = MutableStateFlow(Instant.now())

    val profile: StateFlow<UserProfile?> = userRepository.profile().share(null)

    private val coupleIds = profile.map { it?.coupleId }.distinctUntilChanged()

    val members = sharedFor { it.members() }
    val categories = sharedFor { it.categories() }
    val catalog = sharedFor { it.catalog() }
    val days = sharedFor { it.days() }
    val tasks = sharedFor { it.tasks() }

    val todayKey: StateFlow<String> = clock.map { dayKeyOf(it, zone) }.share(dayKeyOf(clock.value, zone))

    val yesterdayKey: StateFlow<String> = clock.map {
        dayKeyOf(it.minus(Duration.ofDays(1)), zone)
    }.share(dayKeyOf(clock.value.minus(Duration.ofDays(1)), zone))

    /** Bis 06:00 Uhr morgens darf die Bewertung des Vortags noch eingegeben werden. */
    val canEditYesterday: StateFlow<Boolean> = clock.map {
        it.atZone(zone).toLocalTime().hour < 6
    }.share(LocalTime.now(zone).hour < 6)

    val editingYesterday = MutableStateFlow(false)

    val activeDayKey: StateFlow<String> = combine(todayKey, yesterdayKey, canEditYesterday, editingYesterday) { today, yesterday, canYesterday, useYesterday ->
        if (canYesterday && useYesterday) yesterday else today
    }.share(todayKey.value)

    val partnerUid: StateFlow<String?> = members
        .map { list -> list.items.firstOrNull { it != uid } }
        .share(null)

    val myName: StateFlow<String?> = if (uid.isEmpty()) {
        flowOf(null).share(null)
    } else {
        localProfileStore.displayName.share(null)
    }

    val myPhoto: StateFlow<String?> = localProfileStore.photoBase64.share(null)

    val partnerName: StateFlow<String?> = localProfileStore.partnerDisplayName.share(null)

    val partnerPhoto: StateFlow<String?> = localProfileStore.partnerPhotoBase64.share(null)

    private fun resolveRole(
        localName: String?,
        partnerLocalName: String?,
        profileName: String?,
        cats: List<Category>,
    ): String? {
        val myLower = localName?.trim()?.lowercase().orEmpty()
        val partnerLower = partnerLocalName?.trim()?.lowercase().orEmpty()
        val profLower = profileName?.trim()?.lowercase().orEmpty()
        val authEmail = Firebase.auth.currentUser?.email?.trim()?.lowercase().orEmpty()
        val authName = Firebase.auth.currentUser?.displayName?.trim()?.lowercase().orEmpty()
        return when {
            myLower.contains("rosie") || myLower.contains("rosa") -> "rosie"
            myLower.contains("andi") || myLower.contains("andreas") -> "andi"
            partnerLower.contains("andi") || partnerLower.contains("andreas") -> "rosie"
            partnerLower.contains("rosie") || partnerLower.contains("rosa") -> "andi"
            profLower.contains("rosie") || profLower.contains("rosa") ||
                authName.contains("rosie") || authEmail.contains("rosie") -> "rosie"
            profLower.contains("andi") || profLower.contains("andreas") ||
                authName.contains("andi") || authEmail.contains("andi") || authEmail.contains("trenits") -> "andi"
            cats.any { it.id == "preset_sport_$uid" && !it.archived } &&
                cats.none { it.id == "preset_meals_$uid" && !it.archived } -> "andi"
            cats.any { it.id == "preset_meals_$uid" && !it.archived } &&
                cats.none { it.id == "preset_sport_$uid" && !it.archived } -> "rosie"
            else -> "andi"
        }
    }

    fun canonicalAndiCategories(ownerUid: String = uid): List<Category> = listOf(
        Category(
            id = "preset_sport_$ownerUid",
            ownerUid = ownerUid,
            name = text(R.string.cat_preset_sport),
            weight = 3,
            kind = CategoryKind.SPORT,
            targets = listOf(ownerUid),
            goal = DEFAULT_SPORT_GOAL_MINUTES,
            icon = CategoryIcon.FITNESS.name,
            notifications = listOf("3× 45 min sport per week – log your minutes today 💪"),
        ),
        Category(
            id = "preset_water_$ownerUid",
            ownerUid = ownerUid,
            name = text(R.string.cat_preset_water),
            weight = 2,
            kind = CategoryKind.WATER,
            targets = listOf(ownerUid),
            goal = DEFAULT_WATER_GOAL,
            icon = CategoryIcon.WATER.name,
            notifications = listOf(
                "Drink at least 2.5 liters of water today 💧",
                "Status: Enter glasses or liters!",
            ),
            pushEnabled = true,
            pushIntervalHours = 2,
        ),
        Category(
            id = "preset_reading_$ownerUid",
            ownerUid = ownerUid,
            name = text(R.string.cat_preset_reading),
            weight = 2,
            kind = CategoryKind.READING,
            targets = listOf(ownerUid),
            goal = 50.0,
            icon = CategoryIcon.MIND.name,
            notifications = listOf("Read a few more pages today 📖"),
        ),
    )

    fun canonicalRosieCategories(ownerUid: String = uid): List<Category> = listOf(
        Category(
            id = "preset_water_$ownerUid",
            ownerUid = ownerUid,
            name = text(R.string.cat_preset_water),
            weight = 3,
            kind = CategoryKind.WATER,
            targets = listOf(ownerUid),
            goal = DEFAULT_WATER_GOAL,
            icon = CategoryIcon.WATER.name,
            notifications = listOf(
                "Drink at least 2.5 liters of water today 💧",
                "Status: Enter glasses or liters!",
            ),
            pushEnabled = true,
            pushIntervalHours = 2,
        ),
        Category(
            id = "preset_dinner_$ownerUid",
            ownerUid = ownerUid,
            name = text(R.string.cat_preset_dinner),
            weight = 3,
            kind = CategoryKind.DINNER_TIME,
            targets = listOf(ownerUid),
            goal = 20.0,
            icon = CategoryIcon.FOOD.name,
            notifications = listOf("Dinner before 20:00 = 5 stars 🍽️"),
        ),
        Category(
            id = "preset_meals_$ownerUid",
            ownerUid = ownerUid,
            name = text(R.string.cat_preset_food),
            weight = 3,
            kind = CategoryKind.MEALS,
            targets = listOf(ownerUid),
            goal = 0.0,
            icon = CategoryIcon.FOOD.name,
            notifications = listOf("Log healthy breakfast, lunch & dinner 🥗"),
        ),
        Category(
            id = "preset_alcohol_$ownerUid",
            ownerUid = ownerUid,
            name = text(R.string.cat_preset_alcohol),
            weight = 2,
            kind = CategoryKind.ALCOHOL,
            targets = listOf(ownerUid),
            goal = DEFAULT_ALCOHOL_LIMIT,
            icon = CategoryIcon.ALCOHOL.name,
            notifications = listOf("0 liters of alcohol = 5 stars ⭐"),
        ),
        Category(
            id = "preset_rotation_$ownerUid",
            ownerUid = ownerUid,
            name = text(R.string.cat_preset_rotation),
            weight = 1,
            kind = CategoryKind.ROTATION,
            targets = listOf(ownerUid),
            goal = 0.0,
            icon = CategoryIcon.HOME.name,
            notifications = listOf("Select today's alternating task (lessons / household) 🏠"),
        ),
    )

    private fun canonicalAndiCatalog(ownerUid: String = uid): List<CatalogItem> {
        val myTargets = listOf(ownerUid)
        val rewards = listOf(
            text(R.string.seed_reward_andi_1),
            text(R.string.seed_reward_andi_2),
            text(R.string.seed_reward_andi_3),
            text(R.string.seed_reward_andi_4),
            text(R.string.seed_reward_andi_5),
            text(R.string.seed_reward_andi_6),
            text(R.string.seed_reward_andi_7),
        ).mapIndexed { idx, title ->
            CatalogItem("andi_reward_${idx}_$ownerUid", ownerUid, TaskType.REWARD, title, myTargets)
        }
        val penalties = listOf(
            CatalogItem("andi_penalty_0_$ownerUid", ownerUid, TaskType.PENALTY, text(R.string.seed_penalty_andi_1), myTargets),
        )
        return rewards + penalties
    }

    private fun canonicalRosieCatalog(ownerUid: String = uid): List<CatalogItem> {
        val myTargets = listOf(ownerUid)
        val rewards = listOf(
            text(R.string.seed_reward_1),
            text(R.string.seed_reward_2),
            text(R.string.seed_reward_3),
            text(R.string.seed_reward_4),
            text(R.string.seed_reward_5),
        ).mapIndexed { idx, title ->
            CatalogItem("rosie_reward_${idx}_$ownerUid", ownerUid, TaskType.REWARD, title, myTargets)
        }
        val penalties = listOf(
            text(R.string.seed_penalty_1),
            text(R.string.seed_penalty_2),
            text(R.string.seed_penalty_3),
        ).mapIndexed { idx, title ->
            CatalogItem("rosie_penalty_${idx}_$ownerUid", ownerUid, TaskType.PENALTY, title, myTargets)
        }
        return rewards + penalties
    }

    /** Kategorien, die für mich gelten (strikt nach Rolle Rosie vs. Andi). */
    val targeted: StateFlow<List<Category>> = combine(categories, myName, partnerName, profile) { list, localName, pName, prof ->
        val role = resolveRole(localName, pName, prof?.displayName, list.items)
        if (role == "rosie") {
            canonicalRosieCategories(uid)
        } else {
            canonicalAndiCategories(uid)
        }
    }.share(emptyList())

    /** Kategorien, die ich angelegt habe, unabhängig davon, für wen sie gelten. */
    val created: StateFlow<List<Category>> = targeted

    /** Einladung, bei der ich als Partner um Beitritt gebeten habe (wartet auf Bestätigung). */
    val pendingInviteCode: StateFlow<String?> = profile.map { it?.pendingInviteCode }.distinctUntilChanged().share(null)

    /** Anfrage eines Partners zu meiner aktiven Einladung; null, wenn keine offen ist. */
    val incomingRequest: StateFlow<InviteState?> = profile.map { it?.activeInviteCode }.distinctUntilChanged()
        .flatMapLatest { code -> if (code == null) flowOf(null) else userRepository.inviteFlow(code) }
        .share(null)

    /** Katalogeinträge (Belohnungen & Strafen) strikt passend zu meiner Rolle (Rosie vs. Andi). */
    val myCatalog: StateFlow<List<CatalogItem>> = combine(catalog, myName, partnerName, profile) { _, localName, pName, prof ->
        val role = resolveRole(localName, pName, prof?.displayName, emptyList())
        if (role == "rosie") {
            canonicalRosieCatalog(uid)
        } else {
            canonicalAndiCatalog(uid)
        }
    }.share(emptyList())

    /** Lokaler Sofort-Cache für Tageseinträge, damit Eingaben (Lesen, Zähler, Sport) sofort synchron im State sind. */
    private val localDayRecords = MutableStateFlow<Map<String, DayRecord>>(emptyMap())

    val myToday: StateFlow<DayRecord?> = combine(days, localDayRecords, activeDayKey, targeted) { list, localMap, key, cats ->
        val remote = list.items.firstOrNull { it.ownerUid == uid && it.dayKey == key }
        val local = localMap[key]
        if (remote == null && local == null) {
            null
        } else {
            val mergedStored = remote?.snapshots?.associate { it.categoryId to it.stars }.orEmpty() +
                local?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
            val mergedCounters = mutableMapOf<String, Map<String, Double>>()
            remote?.counters?.forEach { (catId, map) -> mergedCounters[catId] = map }
            local?.counters?.forEach { (catId, map) ->
                mergedCounters[catId] = mergedCounters[catId].orEmpty() + map
            }
            val confirmedAt = local?.confirmedAt ?: remote?.confirmedAt
            if (cats.isNotEmpty()) {
                buildDayRecord(key, uid, cats, mergedStored, mergedCounters).copy(confirmedAt = confirmedAt)
            } else {
                (local ?: remote)?.copy(confirmedAt = confirmedAt)
            }
        }
    }.share(null)

    val partnerToday: StateFlow<DayRecord?> = combine(days, activeDayKey, partnerUid) { list, key, partner ->
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

    /** Kurze Rückmeldung nach einem erfolgreichen Speichervorgang (Toast-Text). */
    val toastMessage = MutableStateFlow<String?>(null)

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
            var lastSyncedRole: String? = null
            combine(profile, myName, partnerName, categories, catalog) { p, localName, pName, cats, catItems ->
                listOf(p, localName, pName, cats, catItems)
            }.collect { args ->
                val p = args[0] as? UserProfile
                val localName = args[1] as? String
                val pName = args[2] as? String
                @Suppress("UNCHECKED_CAST")
                val cats = args[3] as Listed<Category>
                @Suppress("UNCHECKED_CAST")
                val catItems = args[4] as Listed<CatalogItem>

                val coupleId = p?.coupleId ?: return@collect
                val role = resolveRole(localName, pName, p.displayName, cats.items) ?: return@collect

                val expectedCatIds = if (role == "rosie") rosieCategoryIds(uid) else andiCategoryIds(uid)
                val expectedCatalogIds = if (role == "rosie") rosieCatalogIds() else andiCatalogIds()
                val hasForeignCats = cats.items.any {
                    ((it.ownerUid == uid || uid in it.targets) && !it.archived && it.id !in expectedCatIds) ||
                        (it.ownerUid == uid && it.targets != listOf(uid))
                }
                val hasMissingCats = expectedCatIds.any { expectedId ->
                    cats.items.none { it.id == expectedId && !it.archived }
                }
                val hasOutdatedSportCat = role == "andi" && cats.items.any {
                    it.id == "preset_sport_$uid" && (it.kind != CategoryKind.SPORT || it.goal != DEFAULT_SPORT_GOAL_MINUTES)
                }
                val hasForeignCatalog = catItems.items.any {
                    it.ownerUid == uid && it.id !in expectedCatalogIds
                }
                val hasMissingCatalog = expectedCatalogIds.any { expectedId ->
                    catItems.items.none { it.id == expectedId }
                }

                if (lastSyncedRole != role || hasForeignCats || hasMissingCats || hasOutdatedSportCat || hasForeignCatalog || hasMissingCatalog) {
                    lastSyncedRole = role
                    if (role == "rosie") {
                        applyRosiePreset()
                    } else {
                        applyAndiPreset()
                    }
                    if (!p.catalogSeeded) {
                        userRepository.markCatalogSeeded()
                    }
                }
                val repo = CoupleRepository(coupleId)
                localDayRecords.value.values.forEach { pendingRecord ->
                    repo.saveDay(pendingRecord)
                }
            }
        }
        viewModelScope.launch {
            combine(categories, days, tasks, todayKey) { _, _, _, _ -> Unit }.collect {
                syncSharedDinnerTimeForToday()
                evaluateYesterday()
                evaluateStreakReward()
                checkConfirmationRemindersAndAutoConfirm()
            }
        }
        viewModelScope.launch {
            pendingInviteCode.flatMapLatest { code ->
                if (code == null) flowOf(null) else userRepository.inviteFlow(code).map { code to it }
            }.collect { pair ->
                val (code, invite) = pair ?: return@collect
                try {
                    when {
                        invite?.status == "confirmed" -> userRepository.finishJoin(invite)
                        invite == null || invite.status == "declined" -> {
                            if (invite != null) error = text(R.string.invite_declined)
                            userRepository.cancelJoin(code)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    error = text(R.string.error_generic)
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                clock.value = Instant.now()
                checkConfirmationRemindersAndAutoConfirm()
                delay(60_000)
            }
        }
        fillDeadline()
    }

    /**
     * Liefert die für [task] aktuell noch wählbaren Optionen im Dropdown (ohne bereits verbrauchte
     * Strafen/Belohnungen im aktuellen Durchlauf) sowie die Gesamtgröße des Pools.
     * Für Strafen wird der Pool jeden Sonntagabend nach der Wochenbilanz zurückgesetzt.
     */
    fun availableChoicesFor(task: Task): Pair<List<String>, Int> {
        val myRole = resolveRole(myName.value, partnerName.value, profile.value?.displayName, categories.value.items)
        val ownerIsAndi = if (task.ownerUid == uid) {
            myRole != "rosie"
        } else {
            myRole == "rosie"
        }
        val starter = if (ownerIsAndi) andiStarterCatalog else rosieStarterCatalog
        val pool = starter
            .filter { (_, t) -> t == task.type }
            .map { (res, _) -> text(res) }

        val cycleStartKey = if (task.type == TaskType.PENALTY) {
            penaltyCycleStartDayKey(ZonedDateTime.now(zone))
        } else {
            ""
        }

        val usedChronological = tasks.value.items
            .filter {
                it.ownerUid == task.ownerUid &&
                    it.type == task.type &&
                    it.id != task.id &&
                    it.title.isNotBlank() &&
                    (cycleStartKey.isEmpty() || it.dayKey >= cycleStartKey)
            }
            .sortedWith(compareBy<Task> { it.dayKey }.thenBy { it.createdAt })
            .map { translateCatalogTitle(getApplication(), it.title) }

        return availablePoolOptions(pool, usedChronological) to pool.size
    }

    fun setEditingYesterday(useYesterday: Boolean) {
        editingYesterday.value = useYesterday && canEditYesterday.value
    }

    private fun currentMergedDay(dayKey: String = activeDayKey.value): DayRecord? {
        val remote = days.value.items.firstOrNull { it.ownerUid == uid && it.dayKey == dayKey }
        val local = localDayRecords.value[dayKey]
        if (remote == null && local == null) return null
        val mergedStored = remote?.snapshots?.associate { it.categoryId to it.stars }.orEmpty() +
            local?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
        val mergedCounters = mutableMapOf<String, Map<String, Double>>()
        remote?.counters?.forEach { (catId, map) -> mergedCounters[catId] = map }
        local?.counters?.forEach { (catId, map) ->
            mergedCounters[catId] = mergedCounters[catId].orEmpty() + map
        }
        val confirmedAt = local?.confirmedAt ?: remote?.confirmedAt
        return buildDayRecord(dayKey, uid, targeted.value, mergedStored, mergedCounters).copy(confirmedAt = confirmedAt)
    }

    private fun saveUpdatedDay(record: DayRecord, showToast: Boolean = true) {
        localDayRecords.value = localDayRecords.value + (record.dayKey to record)
        repo()?.saveDay(record)
        if (showToast) {
            toastMessage.value = text(R.string.save_confirmation)
        }
    }

    fun setStars(categoryId: String, stars: Int, showToast: Boolean = true) {
        val dayKey = activeDayKey.value
        val current = currentMergedDay(dayKey)
        val stored = current?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
        val counters = current?.counters.orEmpty()
        val updated = buildDayRecord(dayKey, uid, targeted.value, stored + (categoryId to stars), counters)
            .copy(confirmedAt = current?.confirmedAt)
        saveUpdatedDay(updated, showToast)
    }

    fun setCounter(categoryId: String, key: String, value: Double, showToast: Boolean = true) {
        val dayKey = activeDayKey.value
        val current = currentMergedDay(dayKey)
        val stored = current?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
        val counters = current?.counters.orEmpty().toMutableMap()
        counters[categoryId] = counters[categoryId].orEmpty() + (key to value.coerceAtLeast(0.0))
        val updated = buildDayRecord(dayKey, uid, targeted.value, stored, counters)
            .copy(confirmedAt = current?.confirmedAt)
        saveUpdatedDay(updated, showToast)
    }

    /** Speichert die heute eingetragene Sport-Dauer in Minuten (Andi: 3 Tage/Woche à 45 Min, tägliche Eingabe). */
    fun setSportMinutes(categoryId: String, minutes: Int, showToast: Boolean = true) {
        val dayKey = activeDayKey.value
        val current = currentMergedDay(dayKey)
        val stored = current?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
        val counters = current?.counters.orEmpty().toMutableMap()
        val doneDaysBeforeToday = sportDaysInWeek(categoryId, isoWeekRecordsBeforeToday.value)
        val remainingDaysAfterToday = (7 - LocalDate.now(zone).dayOfWeek.value).coerceAtLeast(0)
        val restDayOk = if (doneDaysBeforeToday + remainingDaysAfterToday >= SPORT_WEEKLY_DAYS_GOAL) 1.0 else 0.0
        counters[categoryId] = counters[categoryId].orEmpty() + mapOf(
            SPORT_MINUTES to minutes.coerceAtLeast(0).toDouble(),
            SPORT_REST_DAY_OK to restDayOk,
        )
        val updated = buildDayRecord(dayKey, uid, targeted.value, stored, counters)
            .copy(confirmedAt = current?.confirmedAt)
        saveUpdatedDay(updated, showToast)
    }

    /** Speichert die Bewertung einer Mahlzeit (Frühstück, Mittagessen, Abendessen: nichts, wenig, mittel, gesund). */
    fun setMealLevel(categoryId: String, mealKey: String, level: MealLevel) {
        setCounter(categoryId, mealKey, level.points.toDouble(), showToast = true)
    }

    /** Speichert die heute ausgewählte Wechsel-Aufgabe und deren Sterne-Bewertung (0..5). */
    fun setRotation(categoryId: String, rotationTask: RotationTask, stars: Int) {
        val dayKey = activeDayKey.value
        val current = currentMergedDay(dayKey)
        val stored = current?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
        val counters = current?.counters.orEmpty().toMutableMap()
        counters[categoryId] = counters[categoryId].orEmpty() + mapOf(
            ROTATION_TASK_INDEX to rotationTask.index.toDouble(),
            ROTATION_STARS to stars.coerceIn(0, MAX_STARS).toDouble(),
        )
        val updated = buildDayRecord(dayKey, uid, targeted.value, stored, counters)
            .copy(confirmedAt = current?.confirmedAt)
        saveUpdatedDay(updated, showToast = true)
    }

    /** Speichert die Abendessen-Uhrzeit (wird von Andi eingegeben und in beiden Apps synchronisiert). */
    fun setDinnerTime(categoryId: String, hour: Int, minute: Int) {
        val dayKey = activeDayKey.value
        val current = currentMergedDay(dayKey)
        val stored = current?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
        val counters = current?.counters.orEmpty().toMutableMap()
        counters[categoryId] = counters[categoryId].orEmpty() + mapOf(
            DINNER_HOUR to hour.coerceIn(0, 23).toDouble(),
            DINNER_MINUTE to minute.coerceIn(0, 59).toDouble(),
        )
        val updated = buildDayRecord(dayKey, uid, targeted.value, stored, counters)
            .copy(confirmedAt = current?.confirmedAt)
        saveUpdatedDay(updated, showToast = true)
    }

    /**
     * Speichert die Sterne-Bewertung (0..5) für eine der 5 Dimensionen des Beziehungstrackers.
     */
    fun setRelationshipStars(aspect: RelationshipAspect, stars: Int) {
        val dayKey = activeDayKey.value
        val current = currentMergedDay(dayKey)
        val stored = current?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
        val counters = current?.counters.orEmpty().toMutableMap()
        val currentRel = counters[RELATIONSHIP_COUNTER_KEY].orEmpty()
        counters[RELATIONSHIP_COUNTER_KEY] = currentRel + (aspect.key to stars.coerceIn(0, MAX_STARS).toDouble())
        val updated = buildDayRecord(dayKey, uid, targeted.value, stored, counters)
            .copy(confirmedAt = current?.confirmedAt)
        saveUpdatedDay(updated, showToast = true)
    }

    /**
     * Speichert und synchronisiert die heutigen Werte. Der Tag gilt nur dann als bestätigt,
     * wenn alle Kategorien und alle Beziehungs-Dimensionen ausgefüllt sind.
     * Liegt das Tagesergebnis unter 80 %, wird außerdem direkt die tägliche Strafe angelegt.
     */
    fun confirmToday() {
        val dayKey = activeDayKey.value
        val record = currentMergedDay(dayKey)
        val stored = record?.snapshots?.associate { it.categoryId to it.stars }.orEmpty()
        val counters = record?.counters.orEmpty()
        val complete = missingCategoryIds(targeted.value, record).isEmpty() &&
            missingRelationshipAspects(record).isEmpty()
        val base = buildDayRecord(dayKey, uid, targeted.value, stored, counters)
            .copy(confirmedAt = if (complete) System.currentTimeMillis() else record?.confirmedAt)
        saveUpdatedDay(base, showToast = true)
        if (dayOutcomeFor(base, targeted.value) == DayOutcome.PENALTY) {
            ensurePenaltyTaskForDay(dayKey)
        }
    }

    /** Legt für [dayKey] eine offene Strafe für den aktuellen Nutzer an, falls noch keine existiert. */
    fun ensurePenaltyTaskForDay(dayKey: String = activeDayKey.value) {
        val repo = repo() ?: return
        if (tasks.value.items.any { it.ownerUid == uid && it.dayKey == dayKey && it.type == TaskType.PENALTY }) return
        repo.createTask(
            Task(
                id = "eval_${dayKey}_$uid",
                ownerUid = uid,
                type = TaskType.PENALTY,
                title = "",
                dayKey = dayKey,
                status = TaskStatus.OPEN,
                createdAt = Instant.now(),
                targets = listOf(uid),
            ),
        )
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
    fun rosieCategoryIds(ownerUid: String = uid): Set<String> = setOf(
        "preset_meals_$ownerUid",
        "preset_dinner_$ownerUid",
        "preset_water_$ownerUid",
        "preset_alcohol_$ownerUid",
        "preset_rotation_$ownerUid",
    )

    fun andiCategoryIds(ownerUid: String = uid): Set<String> = setOf(
        "preset_sport_$ownerUid",
        "preset_water_$ownerUid",
        "preset_reading_$ownerUid",
    )

    private fun rosieCatalogIds(): Set<String> =
        (0..4).map { "rosie_reward_${it}_$uid" }.toSet() +
            (0..2).map { "rosie_penalty_${it}_$uid" }.toSet()

    private fun andiCatalogIds(): Set<String> =
        (0..6).map { "andi_reward_${it}_$uid" }.toSet() +
            setOf("andi_penalty_0_$uid")

    fun applyRosiePreset() {
        if (myName.value.isNullOrBlank()) {
            localProfileStore.saveDisplayName("Rosie")
            userRepository.setDisplayName("Rosie")
        }
        if (partnerName.value.isNullOrBlank()) {
            localProfileStore.savePartnerDisplayName("Andi")
        }
        val repo = repo() ?: return
        val myTargets = listOf(uid)
        val allowedCategories = rosieCategoryIds(uid)
        val allowedCatalog = rosieCatalogIds()

        categories.value.items
            .filter { (it.ownerUid == uid || uid in it.targets) && !it.archived && it.id !in allowedCategories }
            .forEach { repo.archiveCategory(it.id) }

        repo.saveCategory(
            ownerUid = uid,
            name = text(R.string.cat_preset_food),
            weight = 3,
            kind = CategoryKind.MEALS,
            targets = myTargets,
            goal = 0.0,
            icon = CategoryIcon.FOOD.name,
            notifications = listOf("Log healthy breakfast, lunch & dinner 🥗"),
            id = "preset_meals_$uid",
        )
        repo.saveCategory(
            ownerUid = uid,
            name = text(R.string.cat_preset_dinner),
            weight = 3,
            kind = CategoryKind.DINNER_TIME,
            targets = myTargets,
            goal = 20.0,
            icon = CategoryIcon.FOOD.name,
            notifications = listOf("Dinner before 20:00 = 5 stars 🍽️"),
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
                "Drink at least 2.5 liters of water today 💧",
                "Status: Enter glasses or liters!",
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
            notifications = listOf("0 liters of alcohol = 5 stars ⭐"),
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
            notifications = listOf("Select today's alternating task (lessons / household) 🏠"),
            id = "preset_rotation_$uid",
        )

        val rosieRewards = listOf(
            text(R.string.seed_reward_1),
            text(R.string.seed_reward_2),
            text(R.string.seed_reward_3),
            text(R.string.seed_reward_4),
            text(R.string.seed_reward_5),
        )
        rosieRewards.forEachIndexed { idx, title ->
            repo.saveCatalogItem(CatalogItem("rosie_reward_${idx}_$uid", uid, TaskType.REWARD, title, myTargets))
        }
        val rosiePenalties = listOf(
            text(R.string.seed_penalty_1),
            text(R.string.seed_penalty_2),
            text(R.string.seed_penalty_3),
        )
        rosiePenalties.forEachIndexed { idx, title ->
            repo.saveCatalogItem(CatalogItem("rosie_penalty_${idx}_$uid", uid, TaskType.PENALTY, title, myTargets))
        }

        catalog.value.items
            .filter { it.ownerUid == uid && it.id !in allowedCatalog }
            .forEach { repo.deleteCatalogItem(it.id) }
    }

    /**
     * Legt das Profil, die Kategorien sowie Belohnungen & Strafen für Andi an:
     * - Sport (3-fache Wertung, 3 Tage/Woche à 45 Min, tägliche Zeiteingabe)
     * - Wasser trinken (2-fache Wertung, mind. 2,5 L)
     * - Lesen (2-fache Wertung, Seitenanzahl: 0–5 = 0 ★, > 50 = 5 ★)
     * - Belohnungen: Kuscheln, Massage, Edging, Blowjob, Handjob, Video, Lieblingsgericht
     * - Strafe: Handy-Abgabe an Rosie
     */
    fun applyAndiPreset() {
        if (myName.value.isNullOrBlank()) {
            localProfileStore.saveDisplayName("Andi")
            userRepository.setDisplayName("Andi")
        }
        if (partnerName.value.isNullOrBlank()) {
            localProfileStore.savePartnerDisplayName("Rosie")
        }
        val repo = repo() ?: return
        val myTargets = listOf(uid)
        val allowedCategories = andiCategoryIds(uid)
        val allowedCatalog = andiCatalogIds()

        categories.value.items
            .filter { (it.ownerUid == uid || uid in it.targets) && !it.archived && it.id !in allowedCategories }
            .forEach { repo.archiveCategory(it.id) }

        repo.saveCategory(
            ownerUid = uid,
            name = text(R.string.cat_preset_sport),
            weight = 3,
            kind = CategoryKind.SPORT,
            targets = myTargets,
            goal = DEFAULT_SPORT_GOAL_MINUTES,
            icon = CategoryIcon.FITNESS.name,
            notifications = listOf("3× 45 min sport per week – log your minutes today 💪"),
            id = "preset_sport_$uid",
        )
        repo.saveCategory(
            ownerUid = uid,
            name = text(R.string.cat_preset_water),
            weight = 2,
            kind = CategoryKind.WATER,
            targets = myTargets,
            goal = DEFAULT_WATER_GOAL,
            icon = CategoryIcon.WATER.name,
            notifications = listOf(
                "Drink at least 2.5 liters of water today 💧",
                "Status: Enter glasses or liters!",
            ),
            pushEnabled = true,
            pushIntervalHours = 2,
            id = "preset_water_$uid",
        )
        repo.saveCategory(
            ownerUid = uid,
            name = text(R.string.cat_preset_reading),
            weight = 2,
            kind = CategoryKind.READING,
            targets = myTargets,
            goal = 50.0,
            icon = CategoryIcon.MIND.name,
            notifications = listOf("Read a few more pages today 📖"),
            id = "preset_reading_$uid",
        )

        val andiRewards = listOf(
            text(R.string.seed_reward_andi_1),
            text(R.string.seed_reward_andi_2),
            text(R.string.seed_reward_andi_3),
            text(R.string.seed_reward_andi_4),
            text(R.string.seed_reward_andi_5),
            text(R.string.seed_reward_andi_6),
            text(R.string.seed_reward_andi_7),
        )
        andiRewards.forEachIndexed { idx, title ->
            repo.saveCatalogItem(CatalogItem("andi_reward_${idx}_$uid", uid, TaskType.REWARD, title, myTargets))
        }
        repo.saveCatalogItem(
            CatalogItem("andi_penalty_0_$uid", uid, TaskType.PENALTY, text(R.string.seed_penalty_andi_1), myTargets),
        )

        catalog.value.items
            .filter { it.ownerUid == uid && it.id !in allowedCatalog }
            .forEach { repo.deleteCatalogItem(it.id) }
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
        toastMessage.value = text(R.string.save_confirmation)
    }

    fun setCategoryPushEnabled(id: String, enabled: Boolean) {
        repo()?.setCategoryPushEnabled(id, enabled)
    }

    fun saveDisplayName(name: String) {
        val trimmed = name.trim()
        localProfileStore.saveDisplayName(trimmed)
        userRepository.setDisplayName(trimmed)
        when {
            trimmed.contains("rosie", ignoreCase = true) || trimmed.contains("rosa", ignoreCase = true) -> {
                if (partnerName.value.isNullOrBlank() || partnerName.value?.contains("rosie", ignoreCase = true) == true) {
                    localProfileStore.savePartnerDisplayName("Andi")
                }
                applyRosiePreset()
            }
            trimmed.contains("andi", ignoreCase = true) || trimmed.contains("andreas", ignoreCase = true) -> {
                if (partnerName.value.isNullOrBlank() || partnerName.value?.contains("andi", ignoreCase = true) == true) {
                    localProfileStore.savePartnerDisplayName("Rosie")
                }
                applyAndiPreset()
            }
        }
        toastMessage.value = text(R.string.save_confirmation)
    }

    fun savePartnerDisplayName(name: String) {
        localProfileStore.savePartnerDisplayName(name.trim())
        toastMessage.value = text(R.string.save_confirmation)
    }

    fun savePhoto(base64: String?) {
        localProfileStore.savePhotoBase64(base64)
        userRepository.setPhotoBase64(base64)
        toastMessage.value = text(R.string.save_confirmation)
    }

    fun archiveCategory(id: String) {
        repo()?.archiveCategory(id)
    }

    fun saveCatalogItem(type: TaskType, title: String, targets: List<String> = listOf(uid), id: String = "") {
        repo()?.saveCatalogItem(CatalogItem(id, uid, type, title.trim(), targets))
        toastMessage.value = text(R.string.save_confirmation)
    }

    fun deleteCatalogItem(id: String) {
        repo()?.deleteCatalogItem(id)
    }

    fun chooseTaskTitle(taskId: String, title: String) {
        repo()?.setTaskTitle(taskId, title)
        toastMessage.value = text(R.string.save_confirmation)
    }

    /** Partner bestätigt die Einlösung; hält das heutige Datum als Tag der Einlösung fest. */
    fun confirmTask(taskId: String, selectedTitle: String? = null) {
        repo()?.confirmTask(taskId, todayKey.value, selectedTitle)
        toastMessage.value = text(R.string.save_confirmation)
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
        val code = userRepository.createInvite(coupleId, email)
        inviteCode = code
        onCodeReady(code)
    }

    fun removePartnerForTest() = action {
        userRepository.removePartner(profile.value?.coupleId)
        inviteCode = null
    }

    fun requestJoin(code: String) = action {
        userRepository.requestJoin(code, Firebase.auth.currentUser?.email.orEmpty())
    }

    fun confirmIncomingRequest() = action {
        incomingRequest.value?.let { userRepository.confirmRequest(it) }
    }

    fun declineIncomingRequest() = action {
        incomingRequest.value?.let { userRepository.declineRequest(it) }
    }

    fun cancelPendingRequest() = action {
        pendingInviteCode.value?.let { userRepository.cancelJoin(it) }
    }

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
        if (targeted.value.isEmpty()) return
        val nowHour = LocalTime.now(zone).hour
        val yesterday = dayKeyOf(Instant.now().minus(Duration.ofDays(1)), zone)
        if (tasks.value.items.any { it.ownerUid == uid && it.dayKey == yesterday && it.type == TaskType.PENALTY }) return
        val record = days.value.items.firstOrNull { it.ownerUid == uid && it.dayKey == yesterday }
        if (record == null && nowHour < 6) return
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
     * Deadline-Mechanismus:
     * Wenn es nach 06:00 Uhr ist und der eigene Eintrag von gestern fehlt,
     * wird ein Auto-Eintrag mit 0 Sternen für alle Kategorien erstellt.
     */
    private fun fillDeadline() {
        val now = LocalTime.now(ZoneId.systemDefault())
        if (now.hour < 6) return
        val repo = repo() ?: return
        if (days.value.fromCache && days.value.items.isEmpty()) return
        val yesterday = LocalDate.now(zone).minusDays(1).toString()
        val myYesterday = days.value.items.firstOrNull { it.ownerUid == uid && it.dayKey == yesterday }
        if (myYesterday != null) return
        val yesterdayRecord = buildDayRecord(yesterday, uid, targeted.value, emptyMap(), emptyMap())
        repo.saveDay(yesterdayRecord)
    }

    /**
     * Wochenziel / Streak-Prüfung:
     * Beim Erreichen von 7 Tagen am Stück (≥ 80 %) wird eine Belohnung freigeschaltet.
     */
    private fun evaluateStreakReward() {
        val repo = repo() ?: return
        if (targeted.value.isEmpty()) return

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

    /**
     * Prüft offene Aufgaben:
     * 1. Nach 3 Stunden erhält der Partner eine Push-Nachricht („Rosie wartet auf deine Bestätigung!“).
     * 2. Nach 48 Stunden ohne Bestätigung erfolgt ein Auto-Confirm ins Archiv.
     */
    private fun checkConfirmationRemindersAndAutoConfirm() {
        val repo = repo() ?: return
        val now = Instant.now()
        val prefs = ReminderPrefs(getApplication())
        val partnerDisplay = partnerName.value ?: text(R.string.who_partner)

        tasks.value.items.forEach { task ->
            if (task.ownerUid != uid &&
                task.needsPartnerConfirmationReminder(now) &&
                !prefs.hasNotifiedConfirmForTask(task.id)
            ) {
                ReminderScheduler.sendConfirmationReminderNotification(
                    context = getApplication(),
                    partnerDisplayName = partnerDisplay,
                    task = task,
                )
                prefs.markNotifiedConfirmForTask(task.id)
            }
            if (task.withAutoConfirmation(now).status != task.status) {
                repo.autoConfirmTask(task.id, todayKey.value)
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
