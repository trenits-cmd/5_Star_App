package com.trenits.fivestar.data

import kotlinx.coroutines.flow.MutableStateFlow

/** Hält einen über App-Link eingegangenen Anmeldelink, bis die Anmeldung ihn verarbeitet. */
object AuthLinks {
    val incoming = MutableStateFlow<String?>(null)
}