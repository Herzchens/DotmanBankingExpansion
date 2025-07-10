package com.herzchen.dotmanbankingexpansion

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.regex.Pattern

class ConfigManager(private val plugin: JavaPlugin) {
    var token: String = ""
    var guildId = 0L
    var channelId = 0L
    lateinit var triggerPattern: Pattern
    val clusters = mutableListOf<String>()
    val acceptedMethods = mutableListOf<String>()
    val commands = mutableMapOf<String, List<String>>()

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

            plugin.logger.info("Loaded Discord token: ${if (token.isNotEmpty()) "***${token.takeLast(4)}" else "EMPTY"}")
            plugin.logger.info("Guild ID: $guildId, Channel ID: $channelId")

            val triggerTemplate = getString("discord.trigger-message") ?: ""
            triggerPattern = Pattern.compile(buildRegexPattern(triggerTemplate))

            clusters.clear()
            clusters.addAll(getStringList("clusters"))

            acceptedMethods.clear()
            acceptedMethods.addAll(getStringList("accepted-methods"))

            loadCommands(getConfigurationSection("commands"))

            plugin.logger.info("Trigger pattern: ${triggerPattern.pattern()}")
            plugin.logger.info("Clusters: $clusters")
            plugin.logger.info("Accepted methods: $acceptedMethods")
            plugin.logger.info("Command keys: ${commands.keys}")
        }
    }

    private fun buildRegexPattern(template: String): String {
        return template
            .replace("{player}", "(?<player>[^\"]+)")
            .replace("{amount}", "(?<amount>[\\d.,]+)")
            .replace("{method}", "(?<method>[^\"]+)")
            .replace("{cluster}", "(?<cluster>[^\"]+)")
            .replace("\"", "\"?")
            .replace("\n", "\\s*")
    }

    private fun loadCommands(section: ConfigurationSection?) {
        commands.clear()
        section?.getKeys(false)?.forEach { key ->
            commands[key] = section.getStringList(key)
        }
    }
}