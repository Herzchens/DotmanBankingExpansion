package com.herzchen.dotmanbankingexpansion

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class ConfigManager(private val plugin: JavaPlugin) {
    var token: String = ""
    var guildId = 0L
    var channelId = 0L
    val clusters = mutableListOf<String>()
    val acceptedMethods = mutableListOf<String>()
    val commands = mutableMapOf<String, List<String>>()
    var acceptUserMessages = false

    var debugEnabled = false
    var debugGuildId = 0L
    var debugChannelId = 0L

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
        }
    }

    private fun loadCommands(section: ConfigurationSection?) {
        commands.clear()
        section?.getKeys(false)?.forEach { key ->
            commands[key] = section.getStringList(key)
        }
    }
}
