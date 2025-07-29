package com.herzchen.dotmanbankingexpansion.repository

import com.herzchen.dotmanbankingexpansion.model.StreakData

import org.bukkit.configuration.file.YamlConfiguration

import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class YamlStreakRepository(dataFolder: File) : StreakRepository {
    private val file = File(dataFolder, "streaks.yml")
    private val cfg = YamlConfiguration.loadConfiguration(file)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy - HH:mm:ss:SSS")

    override fun find(uuid: UUID): StreakData? {
        val section = cfg.getConfigurationSection(uuid.toString()) ?: return null

        return StreakData(
            uuid = uuid,
            startTime = parseDateTime(section.getString("startTime")!!),
            lastUpdate = parseDateTime(section.getString("lastUpdate")!!),
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
        val section = cfg.createSection(data.uuid.toString())
        section.set("startTime", formatDateTime(data.startTime))
        section.set("lastUpdate", formatDateTime(data.lastUpdate))
        section.set("state", data.state.name)
        section.set("freezeTokens", data.freezeTokens)
        section.set("restoreTokens", data.restoreTokens)
        section.set("revertTokens", data.revertTokens)
        section.set("currentStreak", data.currentStreak)
        section.set("previousStreak", data.previousStreak)
        section.set("longestStreak", data.longestStreak)
        section.set("discordUsername", data.discordUsername)
        cfg.save(file)
    }

    private fun formatDateTime(instant: Instant): String {
        return dateFormatter.format(instant.atZone(ZoneId.systemDefault()))
    }

    private fun parseDateTime(dateString: String): Instant {
        val localDateTime = LocalDateTime.parse(dateString, dateFormatter)
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant()
    }
}