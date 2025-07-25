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

        var data = repo.find(uuid) ?: StreakData(uuid).apply {
            state = StreakState.INACTIVE
            repo.save(this)
        }

        if (data.state == StreakState.INACTIVE) {
            data.state = StreakState.ACTIVE
            data.currentStreak = 1
            data.longestStreak = 1
            data.lastUpdate = now
            repo.save(data)
            return data
        }

        if (data.state == StreakState.EXPIRED) {
            data.state = StreakState.ACTIVE
            data.currentStreak = 1
            data.lastUpdate = now
            repo.save(data)
            return data
        }

        if (data.state == StreakState.FROZEN) {
            return data
        }

        val lastUpdateDate = data.lastUpdate.atZone(zoneId).toLocalDate()
        val yesterday = today.minusDays(1)

        if (lastUpdateDate == yesterday) {
            data.currentStreak++
            if (data.currentStreak > data.longestStreak) {
                data.longestStreak = data.currentStreak
            }
        } else if (lastUpdateDate.isBefore(yesterday)) {
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
        if (data.freezeTokens <= 0 || data.state != StreakState.ACTIVE) return false

        data.freezeTokens--
        data.state = StreakState.FROZEN
        data.lastUpdate = Instant.now() 
        repo.save(data)
        return true
    }

    fun useRestore(uuid: UUID): Boolean {
        val data = repo.find(uuid) ?: return false
        if (data.state != StreakState.EXPIRED || data.restoreTokens <= 0) return false

        data.restoreTokens--
        data.state = StreakState.ACTIVE
        data.currentStreak = data.previousStreak
        data.lastUpdate = Instant.now()
        repo.save(data)
        return true
    }

    fun checkExpirations() {
        val now = Instant.now()
        repo.findAll().forEach { data ->
            when (data.state) {
                StreakState.ACTIVE -> {
                    val elapsed = Duration.between(data.lastUpdate, now).seconds
                    if (elapsed >= cycleSeconds) {
                        if (data.freezeTokens > 0) {
                            data.freezeTokens--
                            data.state = StreakState.FROZEN
                            data.lastUpdate = now
                        } else {
                            data.state = StreakState.EXPIRED
                            data.lastUpdate = now
                        }
                        repo.save(data)
                    }
                }
                StreakState.FROZEN -> {
                    val elapsed = Duration.between(data.lastUpdate, now).seconds
                    if (elapsed >= cycleSeconds) {
                        if (data.freezeTokens > 0) {
                            data.freezeTokens--
                            data.lastUpdate = now
                        } else {
                            data.state = StreakState.EXPIRED
                        }
                        repo.save(data)
                    }
                }
                StreakState.EXPIRED -> {
                    val elapsed = Duration.between(data.lastUpdate, now).seconds
                    if (elapsed >= cycleSeconds) {
                        data.state = StreakState.INACTIVE
                        data.currentStreak = 0
                        repo.save(data)
                    }
                }
                else -> {}
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
