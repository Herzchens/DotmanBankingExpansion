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
    cycleHours: Long,
    private val discordNotifier: ((UUID, String, Array<Pair<String, String>>) -> Unit)? = null
) {
    val cycleSeconds = Duration.ofHours(cycleHours).seconds
    private val zoneId = ZoneId.systemDefault()

    fun updateStreak(uuid: UUID): StreakData {
        val now = Instant.now()
        val today = LocalDate.now(zoneId)

        val data = repo.find(uuid) ?: StreakData(uuid).apply {
            state = StreakState.INACTIVE
            repo.save(this)
        }

        if (data.state == StreakState.INACTIVE) {
            data.state = StreakState.ACTIVE
            data.currentStreak = 1
            data.longestStreak = 1
            data.lastUpdate = now
            repo.save(data)

            sendDiscordNotification(uuid, "streak-start", "streak" to "1")

            return data
        }

        if (data.state == StreakState.EXPIRED) {
            data.state = StreakState.ACTIVE
            data.currentStreak = 1
            data.lastUpdate = now
            repo.save(data)

            sendDiscordNotification(uuid, "streak-restart", "streak" to "1")

            return data
        }

        if (data.state == StreakState.FROZEN) {
            return data
        }

        val lastUpdateDate = data.lastUpdate.atZone(zoneId).toLocalDate()
        val yesterday = today.minusDays(1)

        if (lastUpdateDate == yesterday) {
            val previousStreak = data.currentStreak
            data.currentStreak++
            if (data.currentStreak > data.longestStreak) {
                data.longestStreak = data.currentStreak
            }

            sendDiscordNotification(uuid, "streak-increase",
                "current" to data.currentStreak.toString(),
                "previous" to previousStreak.toString())

        } else if (lastUpdateDate.isBefore(yesterday)) {
            data.previousStreak = data.currentStreak
            data.currentStreak = 1

            sendDiscordNotification(uuid, "streak-reset",
                "previous" to data.previousStreak.toString(),
                "current" to "1")
        }

        data.lastUpdate = now
        data.state = StreakState.ACTIVE
        repo.save(data)
        return data
    }

    fun useRestore(uuid: UUID): Boolean {
        val data = repo.find(uuid) ?: return false
        if (data.state != StreakState.EXPIRED || data.restoreTokens <= 0) return false

        data.restoreTokens--
        data.state = StreakState.ACTIVE
        data.currentStreak = data.previousStreak
        data.lastUpdate = Instant.now()
        repo.save(data)

        sendDiscordNotification(uuid, "streak-restored",
            "streak" to data.currentStreak.toString(),
            "tokens" to data.restoreTokens.toString())

        return true
    }

    fun checkExpirations() {
        val now = Instant.now()
        repo.findAll().forEach { data ->
            when (data.state) {
                StreakState.ACTIVE -> {
                    val elapsed = Duration.between(data.lastUpdate, now).seconds

                    if (elapsed >= cycleSeconds - 7200 && elapsed < cycleSeconds - 7140) {
                        sendDiscordNotification(data.uuid, "streak-warning", "time" to "2 giờ")
                    }

                    if (elapsed >= cycleSeconds - 3600 && elapsed < cycleSeconds - 3540) {
                        sendDiscordNotification(data.uuid, "streak-warning", "time" to "1 giờ")
                    }

                    if (elapsed >= cycleSeconds - 1800 && elapsed < cycleSeconds - 1740) {
                        sendDiscordNotification(data.uuid, "streak-warning", "time" to "30 phút")
                    }

                    if (elapsed >= cycleSeconds) {
                        if (data.freezeTokens > 0) {
                            data.freezeTokens--
                            data.state = StreakState.FROZEN
                            data.lastUpdate = now

                            sendDiscordNotification(data.uuid, "streak-frozen",
                                "tokens" to data.freezeTokens.toString())
                        } else {
                            data.state = StreakState.EXPIRED
                            data.lastUpdate = now

                            sendDiscordNotification(data.uuid, "streak-expired",
                                "streak" to data.currentStreak.toString())
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

                            sendDiscordNotification(data.uuid, "streak-continue-frozen",
                                "tokens" to data.freezeTokens.toString())
                        } else {
                            data.state = StreakState.EXPIRED

                            sendDiscordNotification(data.uuid, "streak-freeze-expired")
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

                        sendDiscordNotification(data.uuid, "streak-inactive")
                    }
                }
                else -> {}
            }
        }
    }

    fun useRevert(uuid: UUID): Boolean {
        val data = repo.find(uuid) ?: return false
        if (data.revertTokens <= 0) return false

        val previousCurrent = data.currentStreak
        data.currentStreak = data.longestStreak
        data.revertTokens--
        data.state = StreakState.ACTIVE
        data.lastUpdate = Instant.now()
        repo.save(data)

        sendDiscordNotification(uuid, "streak-reverted",
            "previous" to previousCurrent.toString(),
            "current" to data.currentStreak.toString(),
            "tokens" to data.revertTokens.toString())

        return true
    }

    fun setStreak(uuid: UUID, amount: Int): Boolean {
        val data = repo.find(uuid) ?: return false
        val previousStreak = data.currentStreak
        data.currentStreak = amount

        if (amount > data.longestStreak) {
            data.longestStreak = amount
        }

        repo.save(data)

        sendDiscordNotification(uuid, "streak-admin-set",
            "previous" to previousStreak.toString(),
            "current" to amount.toString())

        return true
    }

    fun resetAllStreaks() {
        repo.findAll().forEach { data ->
            data.currentStreak = 0
            data.previousStreak = 0
            data.longestStreak = 0
            data.state = StreakState.INACTIVE
            data.lastUpdate = Instant.now()
            repo.save(data)

            sendDiscordNotification(data.uuid, "streak-global-reset")
        }
    }

    fun getProgress(uuid: UUID): Double =
        repo.find(uuid)?.progress(cycleSeconds) ?: 0.0

    fun freezePlayer(uuid: UUID, days: Int): Boolean {
        val data = repo.find(uuid) ?: StreakData(uuid).also {
            repo.save(it)
        }

        days * 24 * 3600L
        data.state = StreakState.FROZEN
        data.lastUpdate = Instant.now()
        repo.save(data)

        sendDiscordNotification(uuid, "streak-admin-frozen",
            "days" to days.toString())

        return true
    }

    fun setRemainingTime(uuid: UUID, seconds: Long): Boolean {
        if (seconds < 0 || seconds > cycleSeconds) return false

        val data = repo.find(uuid) ?: return false
        data.lastUpdate = Instant.now().minusSeconds(cycleSeconds - seconds)
        repo.save(data)

        sendDiscordNotification(uuid, "streak-admin-time-set",
            "time" to "${seconds / 3600} giờ ${(seconds % 3600) / 60} phút")

        return true
    }

    fun setStatus(uuid: UUID, status: StreakState): Boolean {
        val data = repo.find(uuid) ?: return false
        val previousStatus = data.state
        data.state = status
        repo.save(data)

        sendDiscordNotification(uuid, "streak-admin-status-set",
            "previous" to previousStatus.name,
            "current" to status.name)

        return true
    }

    private fun sendDiscordNotification(uuid: UUID, messageKey: String, vararg params: Pair<String, String>) {
        val data = repo.find(uuid) ?: return
        data.discordId?.let { discordId ->
            try {
                val paramsArray = arrayOf(*params)
                discordNotifier?.invoke(uuid, messageKey, paramsArray)
            } catch (e: Exception) {
                println("Failed to send Discord notification: ${e.message}")
            }
        }
    }
}