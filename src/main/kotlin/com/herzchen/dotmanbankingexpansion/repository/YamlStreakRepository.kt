package com.herzchen.dotmanbankingexpansion.repository

import com.herzchen.dotmanbankingexpansion.model.StreakData

import org.bukkit.configuration.file.YamlConfiguration

import java.io.File
import java.time.Instant
import java.util.UUID

class YamlStreakRepository(dataFolder: File) : StreakRepository {
    private val file = File(dataFolder, "streaks.yml")
    private val cfg = YamlConfiguration.loadConfiguration(file)

    override fun find(uuid: UUID): StreakData? {
        val section = cfg.getConfigurationSection(uuid.toString()) ?: return null
        return StreakData(
            uuid = uuid,
            startTime = Instant.parse(section.getString("startTime")!!),
            lastUpdate = Instant.parse(section.getString("lastUpdate")!!),
            state = enumValueOf(section.getString("state")!!),
            freezeTokens = section.getInt("freezeTokens"),
            restoreTokens = section.getInt("restoreTokens"),
            revertTokens = section.getInt("revertTokens"),
            currentStreak = section.getInt("currentStreak", 1),
            previousStreak = section.getInt("previousStreak", 0),
            longestStreak = section.getInt("longestStreak", 1),
            discordUsername = section.getString("discordUsername")
        )
    }

    override fun findAll(): Collection<StreakData> =
        cfg.getKeys(false).mapNotNull { key ->
            runCatching { find(UUID.fromString(key)) }.getOrNull()
        }

    override fun save(data: StreakData) {
        cfg.createSection(data.uuid.toString(), mapOf(
            "startTime" to data.startTime.toString(),
            "lastUpdate" to data.lastUpdate.toString(),
            "state" to data.state.name,
            "freezeTokens" to data.freezeTokens,
            "restoreTokens" to data.restoreTokens,
            "revertTokens" to data.revertTokens,
            "currentStreak" to data.currentStreak,
            "previousStreak" to data.previousStreak,
            "longestStreak" to data.longestStreak,
            "discordUsername" to data.discordUsername
        ))
        cfg.save(file)
    }
}
