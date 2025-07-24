package com.herzchen.dotmanbankingexpansion.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.plugin.java.JavaPlugin

import java.io.File

class ConfigManager(private val plugin: JavaPlugin) {
    var token: String = ""
    var guildId = 0L
    var channelId = 0L
    val clusters = mutableListOf<String>()
    val acceptedMethods = mutableListOf<String>()
    val commands = mutableMapOf<Int, List<String>>()
    var acceptUserMessages = false

    var debugEnabled = false
    var debugGuildId = 0L
    var debugChannelId = 0L

    var milestoneBonusEnabled = false
    var milestoneAmount = 0
    val milestoneCommands = mutableListOf<String>()

    var streakEnabled = false
    var streakCycleHours = 24
    val streakCommands = mutableMapOf<Int, List<String>>()
    var streakFreezeItem: String? = null
    var streakRestoreItem: String? = null

    var discordNotificationsEnabled = false


    fun loadConfig() {
        val configFile = File(plugin.dataFolder, "config.yml")
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false)
        }

        plugin.reloadConfig()
        with(plugin.config) {
            token = getString("discord.token", "")!!.trim()
            guildId = getString("discord.guild-id", "0")!!.toLongOrNull() ?: 0L
            channelId = getString("discord.channel-id", "0")!!.toLongOrNull() ?: 0L

            clusters.clear()
            clusters.addAll(getStringList("clusters"))

            acceptedMethods.clear()
            acceptedMethods.addAll(getStringList("accepted-methods"))

            loadCommands(getConfigurationSection("commands"))

            acceptUserMessages = getBoolean("discord.accept-user-messages", false)

            debugEnabled = getBoolean("debug.enabled", false)
            debugGuildId = getString("debug.guild-id", "0")!!.toLongOrNull() ?: 0L
            debugChannelId = getString("debug.channel-id", "0")!!.toLongOrNull() ?: 0L

            milestoneBonusEnabled = getBoolean("milestone-bonus.enabled", false)
            milestoneAmount = getInt("milestone-bonus.amount", 50000)
            milestoneCommands.clear()
            milestoneCommands.addAll(getStringList("milestone-bonus.commands"))

            streakEnabled = getBoolean("streak.enabled", false)
            streakCycleHours = getInt("streak.cycle-hours", 24)
            streakFreezeItem = getString("streak.freeze-item", null)
            streakRestoreItem = getString("streak.restore-item", null)

            streakCommands.clear()
            val streakSection = getConfigurationSection("streak.commands")
            streakSection?.getKeys(false)?.forEach { key ->
                streakCommands[key.toInt()] = streakSection.getStringList(key)
            }

            discordNotificationsEnabled = getBoolean("discord.notifications-enabled", false)


            plugin.logger.info("Discord token loaded: ${if (token.isNotEmpty()) "***${token.takeLast(4)}" else "EMPTY"}")
            plugin.logger.info("Guild ID: $guildId, Channel ID: $channelId")
            plugin.logger.info("Clusters: $clusters")
            plugin.logger.info("Accepted methods: $acceptedMethods")
            plugin.logger.info("Command keys: ${commands.keys}")
            plugin.logger.info("Accept user messages: $acceptUserMessages")
            plugin.logger.info("Debug enabled: $debugEnabled")
            if (debugEnabled) {
                plugin.logger.info("Debug guild ID: $debugGuildId, channel ID: $debugChannelId")
            }
            plugin.logger.info("Milestone bonus: enabled=$milestoneBonusEnabled, amount=$milestoneAmount, commands=$milestoneCommands")
            plugin.logger.info("Streak: enabled=$streakEnabled, cycle=$streakCycleHours hours, freeze=$streakFreezeItem, restore=$streakRestoreItem")
            plugin.logger.info("Streak commands: ${streakCommands.keys}")
        }
    }

    private fun loadCommands(section: ConfigurationSection?) {
        commands.clear()
        section?.getKeys(false)?.forEach { key ->
            val level = parseLevel(key)
            commands[level] = section.getStringList(key)
        }
    }

    private fun parseLevel(key: String): Int {
        val cleanKey = key.replace(",", "").replace(" ", "")

        return cleanKey.toIntOrNull() ?: 0
    }
}