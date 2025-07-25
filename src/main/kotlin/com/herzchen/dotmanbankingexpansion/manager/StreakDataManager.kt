package com.herzchen.dotmanbankingexpansion.manager

import com.herzchen.dotmanbankingexpansion.model.StreakData
import com.herzchen.dotmanbankingexpansion.repository.StreakRepository

import java.util.*
import java.time.Instant

class StreakDataManager(private val repo: StreakRepository) {
    fun getPlayerData(uuid: UUID): StreakData? = repo.find(uuid)

    fun linkDiscord(uuid: UUID, discordUsername: String) {
        val data = repo.find(uuid) ?: StreakData(uuid, Instant.now(), Instant.now()).apply {
            currentStreak = 0
            repo.save(this)
        }
        data.discordUsername = discordUsername
        repo.save(data)
    }
}