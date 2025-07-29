package com.herzchen.dotmanbankingexpansion.model

import java.time.Instant
import java.util.UUID

data class StreakData(
    val uuid: UUID,
    var startTime: Instant = Instant.now(),
    var lastUpdate: Instant = Instant.now(),
    var state: StreakState = StreakState.INACTIVE,
    var freezeTokens: Int = 0,
    var restoreTokens: Int = 0,
    var revertTokens: Int = 0,
    var currentStreak: Int = 0,
    var previousStreak: Int = 0,
    var longestStreak: Int = 0,
    var discordUsername: String? = null,
    var discordId: String? = null
) {
    fun progress(cycleSeconds: Long): Double {
        val elapsed = java.time.Duration.between(lastUpdate, Instant.now()).seconds
        return (elapsed.coerceIn(0, cycleSeconds).toDouble() / cycleSeconds)
    }
}