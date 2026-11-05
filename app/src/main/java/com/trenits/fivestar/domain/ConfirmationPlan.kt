package com.trenits.fivestar.domain

import java.time.Instant

/** Art einer fälligen Bestätigungs-Aktion, die der ViewModel ausführt. */
enum class ConfirmationActionType {
    /** Erste Erinnerung an den Partner, eine offene Belohnung zu bestätigen. */
    CONFIRM_REMINDER,

    /** Warnung kurz vor Ablauf der Bestätigungsfrist. */
    DEADLINE_WARNING,

    /** Frist abgelaufen: Aufgabe gilt automatisch als bestätigt. */
    TASK_AUTO_CONFIRM,

    /** Abgelehnte Abendessen-Zeit: der Eintragende muss korrigieren. */
    DINNER_REJECTED,

    /** Abendessen-Zeit wartet auf Bestätigung und die Frist läuft bald ab. */
    DINNER_WARNING,

    /** Abendessen-Zeit wartet auf Bestätigung. */
    DINNER_REMINDER,

    /** Frist abgelaufen: Abendessen-Zeit gilt automatisch als bestätigt. */
    DINNER_AUTO_CONFIRM,
}

/**
 * Eine fällige Aktion. [dedupKey] ist der Schlüssel im Reminder-Speicher, damit dieselbe
 * Meldung nicht mehrfach gesendet wird; `null` bedeutet, die Aktion läuft immer.
 */
data class ConfirmationAction(
    val type: ConfirmationActionType,
    val taskId: String? = null,
    val dayKey: String? = null,
    val dedupKey: String? = null,
    val autoConfirmAt: Instant? = null,
)

/**
 * Entscheidet, welche Erinnerungen fällig sind und was automatisch bestätigt wird.
 * Reine Funktion ohne Firebase, damit das Verhalten des ViewModels testbar ist.
 */
fun planConfirmationActions(
    now: Instant,
    uid: String,
    tasks: List<Task>,
    dinners: List<DinnerConfirmation>,
): List<ConfirmationAction> {
    val actions = mutableListOf<ConfirmationAction>()

    tasks.forEach { task ->
        if (task.ownerUid != uid) {
            if (task.needsPartnerConfirmationReminder(now)) {
                actions += ConfirmationAction(
                    type = ConfirmationActionType.CONFIRM_REMINDER,
                    taskId = task.id,
                    dedupKey = "confirm_${task.id}",
                )
            }
            if (task.needsDeadlineWarning(now)) {
                actions += ConfirmationAction(
                    type = ConfirmationActionType.DEADLINE_WARNING,
                    taskId = task.id,
                    dedupKey = "warn_${task.id}_${task.autoConfirmAt.epochSecond}",
                    autoConfirmAt = task.autoConfirmAt,
                )
            }
        }
        if (task.withAutoConfirmation(now).status != task.status) {
            actions += ConfirmationAction(
                type = ConfirmationActionType.TASK_AUTO_CONFIRM,
                taskId = task.id,
                dayKey = task.dayKey,
            )
        }
    }

    dinners.forEach { dinner ->
        // Abgelehnt: nur der Eintragende (Partner) muss korrigieren, keine Auto-Bestätigung.
        if (dinner.isRejected) {
            if (dinner.enteredBy == uid) {
                actions += ConfirmationAction(
                    type = ConfirmationActionType.DINNER_REJECTED,
                    dayKey = dinner.dayKey,
                    dedupKey = "dinner_rejected_${dinner.dayKey}_${(dinner.rejectedAt ?: Instant.EPOCH).epochSecond}",
                )
            }
            return@forEach
        }
        if (dinner.isPending && dinner.enteredBy != uid) {
            if (dinner.withAutoConfirmation(now).confirmedAt != null) {
                actions += ConfirmationAction(
                    type = ConfirmationActionType.DINNER_AUTO_CONFIRM,
                    dayKey = dinner.dayKey,
                )
                return@forEach
            }
            // Schlüssel je Eintrag: eine korrigierte Uhrzeit löst erneut eine Meldung aus.
            val entryKey = dinner.enteredAt.epochSecond
            if (dinner.needsWarning(now)) {
                actions += ConfirmationAction(
                    type = ConfirmationActionType.DINNER_WARNING,
                    dayKey = dinner.dayKey,
                    dedupKey = "dinner_warn_${dinner.dayKey}_$entryKey",
                )
            }
            actions += ConfirmationAction(
                type = ConfirmationActionType.DINNER_REMINDER,
                dayKey = dinner.dayKey,
                dedupKey = "dinner_${dinner.dayKey}_$entryKey",
            )
        }
    }

    return actions
}
