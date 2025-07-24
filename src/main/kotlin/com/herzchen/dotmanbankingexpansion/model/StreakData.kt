package com.herzchen.dotmanbankingexpansion.model

import java.time.Instant
import java.util.UUID

data class StreakData(
    val uuid: UUID,
    var startTime: Instant,
    var lastUpdate: Instant,
    var state: StreakState = StreakState.ACTIVE,
    var freezeTokens: Int = 0,
    var restoreTokens: Int = 0,
    var revertTokens: Int = 0,
    var currentStreak: Int = 1,
    var previousStreak: Int = 0,
    var longestStreak: Int = 1,
    var discordUsername: String? = null
) {
    fun progress(cycleSeconds: Long): Double {
        val elapsed = java.time.Duration.between(lastUpdate, Instant.now()).seconds
        return (elapsed.coerceIn(0, cycleSeconds).toDouble() / cycleSeconds)
    }
}