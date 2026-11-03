package com.trenits.fivestar.domain

import java.time.Duration
import java.time.Instant

/**
 * Bestätigung einer eingetragenen Abendessen-Uhrzeit. Eingetragen von der Partner-Rolle,
 * bestätigt von der main_user-Rolle per Push. Sterne zählen schon vor der Bestätigung.
 */
data class DinnerConfirmation(
    val dayKey: String,
    val enteredBy: String,
    val hour: Int,
    val minute: Int,
    val enteredAt: Instant,
    val confirmedAt: Instant? = null,
    val confirmedBy: String? = null,
    val autoConfirmed: Boolean = false,
) {
    val isPending: Boolean get() = confirmedAt == null
    val deadline: Instant get() = enteredAt.plus(DINNER_CONFIRM_AFTER)
}

/** Ohne Bestätigung gilt die Uhrzeit 48 Stunden nach Eintrag als bestätigt. */
val DINNER_CONFIRM_AFTER: Duration = Duration.ofHours(48)
/** Frühwarnung vor Ablauf der Bestätigungsfrist. */
val DINNER_WARNING_BEFORE: Duration = Duration.ofHours(6)

/** Erinnerung bzw. Frühwarnung fällig? (nur solange offen) */
fun DinnerConfirmation.needsWarning(now: Instant): Boolean =
    isPending && !now.isBefore(deadline.minus(DINNER_WARNING_BEFORE)) && now.isBefore(deadline)

/** Automatische Bestätigung nach Ablauf der Frist. */
fun DinnerConfirmation.withAutoConfirmation(now: Instant): DinnerConfirmation =
    if (isPending && !now.isBefore(deadline)) copy(confirmedAt = now, autoConfirmed = true) else this