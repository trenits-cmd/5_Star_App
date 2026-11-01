package com.trenits.fivestar.domain

import kotlin.random.Random

const val INVITE_CODE_LENGTH = 6
private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

/** Ohne leicht verwechselbare Zeichen (0/O, 1/I/L). */
fun generateInviteCode(random: Random = Random.Default): String =
    (1..INVITE_CODE_LENGTH).map { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }.joinToString("")

fun normalizeInviteCode(input: String): String = input.trim().uppercase()
