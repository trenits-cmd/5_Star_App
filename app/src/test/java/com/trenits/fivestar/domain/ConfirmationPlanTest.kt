package com.trenits.fivestar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * Tests der Entscheidungslogik, die `AppViewModel.checkConfirmationRemindersAndAutoConfirm`
 * pro Minute ausführt (Erinnerungen, Fristwarnungen, automatische Bestätigung).
 */
class ConfirmationPlanTest {

    private val now: Instant = Instant.parse("2026-11-05T20:00:00Z")
    private val me = "main-user"
    private val partner = "partner"

    private fun task(
        id: String = "t1",
        ownerUid: String = partner,
        status: TaskStatus = TaskStatus.OPEN,
        selectedAt: Instant? = now.minus(Duration.ofHours(4)),
        deadlineAt: Instant? = null,
        postponeCount: Int = 0,
    ) = Task(
        id = id,
        ownerUid = ownerUid,
        type = TaskType.REWARD,
        title = "Reward",
        dayKey = "2026-11-05",
        status = status,
        createdAt = now.minus(Duration.ofHours(5)),
        targets = listOf(ownerUid),
        selectedAt = selectedAt,
        deadlineAt = deadlineAt,
        postponeCount = postponeCount,
    )

    private fun dinner(
        enteredAt: Instant = now.minus(Duration.ofHours(1)),
        enteredBy: String = partner,
        confirmedAt: Instant? = null,
        rejectedAt: Instant? = null,
    ) = DinnerConfirmation(
        dayKey = "2026-11-05",
        enteredBy = enteredBy,
        hour = 19,
        minute = 30,
        enteredAt = enteredAt,
        confirmedAt = confirmedAt,
        rejectedAt = rejectedAt,
    )

    private fun plan(tasks: List<Task> = emptyList(), dinners: List<DinnerConfirmation> = emptyList()) =
        planConfirmationActions(now, me, tasks, dinners)

    @Test
    fun `offene Partner-Aufgabe erinnert nach drei Stunden`() {
        val actions = plan(tasks = listOf(task()))
        assertEquals(listOf(ConfirmationActionType.CONFIRM_REMINDER), actions.map { it.type })
        assertEquals("confirm_t1", actions.single().dedupKey)
    }

    @Test
    fun `eigene Aufgabe erzeugt keine Erinnerung an den Partner`() {
        val actions = plan(tasks = listOf(task(ownerUid = me)))
        assertTrue(actions.none { it.type == ConfirmationActionType.CONFIRM_REMINDER })
    }

    @Test
    fun `verschobene Aufgabe erinnert nicht erneut`() {
        val actions = plan(tasks = listOf(task(postponeCount = 1)))
        assertTrue(actions.none { it.type == ConfirmationActionType.CONFIRM_REMINDER })
    }

    @Test
    fun `Fristwarnung traegt den Ablaufzeitpunkt im Schluessel`() {
        val deadline = now.plus(Duration.ofHours(2))
        val actions = plan(tasks = listOf(task(deadlineAt = deadline)))
        val warning = actions.first { it.type == ConfirmationActionType.DEADLINE_WARNING }
        assertEquals("warn_t1_${deadline.epochSecond}", warning.dedupKey)
        assertEquals(deadline, warning.autoConfirmAt)
    }

    @Test
    fun `abgelaufene Frist bestaetigt die Aufgabe automatisch`() {
        val actions = plan(
            tasks = listOf(task(selectedAt = now.minus(Duration.ofHours(49)))),
        )
        assertTrue(actions.any { it.type == ConfirmationActionType.TASK_AUTO_CONFIRM && it.taskId == "t1" })
    }

    @Test
    fun `bereits bestaetigte Aufgabe wird nicht erneut bestaetigt`() {
        val actions = plan(
            tasks = listOf(task(status = TaskStatus.CONFIRMED, selectedAt = now.minus(Duration.ofHours(49)))),
        )
        assertTrue(actions.none { it.type == ConfirmationActionType.TASK_AUTO_CONFIRM })
    }

    @Test
    fun `abgelehnte Abendessen-Zeit meldet nur dem Eintragenden`() {
        val rejected = dinner(rejectedAt = now.minus(Duration.ofMinutes(5)))
        val forEnteringPartner = planConfirmationActions(now, partner, emptyList(), listOf(rejected))
        val rejectedAction = forEnteringPartner.single { it.type == ConfirmationActionType.DINNER_REJECTED }
        assertEquals(
            "dinner_rejected_2026-11-05_${(now.minus(Duration.ofMinutes(5))).epochSecond}",
            rejectedAction.dedupKey,
        )

        val forMainUser = plan(dinners = listOf(rejected))
        assertTrue(forMainUser.none { it.type == ConfirmationActionType.DINNER_REJECTED })
    }

    @Test
    fun `offene Abendessen-Zeit erzeugt Erinnerung mit Eintrags-Schluessel`() {
        val enteredAt = now.minus(Duration.ofHours(1))
        val actions = plan(dinners = listOf(dinner(enteredAt = enteredAt)))
        assertEquals(listOf(ConfirmationActionType.DINNER_REMINDER), actions.map { it.type })
        assertEquals("dinner_2026-11-05_${enteredAt.epochSecond}", actions.single().dedupKey)
    }

    @Test
    fun `kurz vor Ablauf kommt zusaetzlich die Warnung`() {
        val enteredAt = now.minus(Duration.ofHours(43))
        val actions = plan(dinners = listOf(dinner(enteredAt = enteredAt)))
        assertEquals(
            listOf(ConfirmationActionType.DINNER_WARNING, ConfirmationActionType.DINNER_REMINDER),
            actions.map { it.type },
        )
        assertEquals("dinner_warn_2026-11-05_${enteredAt.epochSecond}", actions.first().dedupKey)
    }

    @Test
    fun `abgelaufene Abendessen-Zeit wird automatisch bestaetigt`() {
        val actions = plan(dinners = listOf(dinner(enteredAt = now.minus(Duration.ofHours(49)))))
        assertEquals(listOf(ConfirmationActionType.DINNER_AUTO_CONFIRM), actions.map { it.type })
    }

    @Test
    fun `bereits bestaetigte Abendessen-Zeit loest nichts aus`() {
        val actions = plan(dinners = listOf(dinner(confirmedAt = now.minus(Duration.ofHours(2)))))
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `selbst eingetragene Abendessen-Zeit wartet nicht auf sich selbst`() {
        val actions = planConfirmationActions(now, partner, emptyList(), listOf(dinner(enteredBy = partner)))
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `neue Uhrzeit nach Korrektur erzeugt einen neuen Schluessel`() {
        val first = plan(dinners = listOf(dinner(enteredAt = now.minus(Duration.ofHours(1)))))
        val corrected = plan(dinners = listOf(dinner(enteredAt = now.minus(Duration.ofMinutes(2)))))
        assertTrue(first.single().dedupKey != corrected.single().dedupKey)
    }
}
