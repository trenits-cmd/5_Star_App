package com.trenits.fivestar.data

import kotlinx.coroutines.flow.MutableStateFlow

/** Ziel aus einer Push-Benachrichtigung (z. B. Abendessen-Bestätigung), bis die App es verarbeitet. */
object NotificationTargets {
    val dinnerDay = MutableStateFlow<String?>(null)
}
