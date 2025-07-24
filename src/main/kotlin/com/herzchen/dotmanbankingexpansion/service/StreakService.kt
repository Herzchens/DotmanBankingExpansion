package com.herzchen.dotmanbankingexpansion.service

import com.herzchen.dotmanbankingexpansion.model.StreakData
import com.herzchen.dotmanbankingexpansion.model.StreakState
import com.herzchen.dotmanbankingexpansion.repository.StreakRepository

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class StreakService(
    val repo: StreakRepository,
    cycleHours: Long
) {
    val cycleSeconds = Duration.ofHours(cycleHours).seconds
    private val zoneId = ZoneId.systemDefault()

    fun updateStreak(uuid: UUID): StreakData {
        val now = Instant.now()
        val today = LocalDate.now(zoneId)

        val data = repo.find(uuid)?.takeIf { it.state != StreakState.EXPIRED }
            ?: StreakData(uuid, now, now).apply {
                currentStreak = 1
            }

        val lastUpdateDate = data.lastUpdate.atZone(zoneId).toLocalDate()
        val yesterday = today.minusDays(1)

        if (lastUpdateDate == yesterday) {
            data.currentStreak++
        }
        else if (lastUpdateDate.isBefore(yesterday)) {
            data.previousStreak = data.currentStreak
            data.currentStreak = 1
        }

        data.lastUpdate = now
        data.state = StreakState.ACTIVE
        repo.save(data)
        return data
    }

    fun useFreeze(uuid: UUID): Boolean {
        val data = repo.find(uuid) ?: return false
        if (data.freezeTokens <= 0) return false
        data.freezeTokens--
        data.state = StreakState.FROZEN
        repo.save(data)
        return true
    }

    fun useRestore(uuid: UUID): Boolean {
        val data = repo.find(uuid) ?: return false
        if (data.state != StreakState.EXPIRED || data.restoreTokens <= 0) return false

        data.restoreTokens -= 1
        data.state = StreakState.ACTIVE
        data.currentStreak = data.previousStreak
        data.lastUpdate = Instant.now()
        repo.save(data)
        return true
    }

    fun checkExpirations() {
        val now = Instant.now()
        repo.findAll().forEach { data ->
            if (data.state == StreakState.ACTIVE) {
                val elapsed = Duration.between(data.lastUpdate, now).seconds
                if (elapsed >= cycleSeconds) {
                    data.state = StreakState.EXPIRED
                    repo.save(data)
                }
            }
        }
    }

    fun useRevert(uuid: UUID): Boolean {
        val data = repo.find(uuid) ?: return false
        if (data.revertTokens <= 0) return false

        data.currentStreak = data.longestStreak
        data.revertTokens--
        data.state = StreakState.ACTIVE
        data.lastUpdate = Instant.now()
        repo.save(data)
        return true
    }

    fun setStreak(uuid: UUID, amount: Int): Boolean {
        val data = repo.find(uuid) ?: return false
        data.currentStreak = amount
        if (amount > data.longestStreak) {
            data.longestStreak = amount
        }
        repo.save(data)
        return true
    }

    fun resetAllStreaks() {
        repo.findAll().forEach { data ->
            data.currentStreak = 0
            data.state = StreakState.EXPIRED
            data.lastUpdate = Instant.now()
            repo.save(data)
        }
    }

    fun getProgress(uuid: UUID): Double =
        repo.find(uuid)?.progress(cycleSeconds) ?: 0.0
}
