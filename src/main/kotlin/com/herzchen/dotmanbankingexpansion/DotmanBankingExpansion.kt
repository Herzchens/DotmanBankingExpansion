package com.herzchen.dotmanbankingexpansion

import org.bukkit.plugin.java.JavaPlugin
import com.herzchen.dotmanbankingexpansion.command.ReloadCommand
import org.bukkit.Bukkit

class DotmanBankingExpansion : JavaPlugin() {
    lateinit var configManager: ConfigManager
    lateinit var pluginLogger: PluginLogger
    private lateinit var discordBot: DiscordBot

    override fun onEnable() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        saveResource("config.yml", false)
        saveResource("log.yml", false)

        configManager = ConfigManager(this)
        configManager.loadConfig()

        pluginLogger = PluginLogger(this)
        pluginLogger.log("Starting plugin...")

        if (!isJDAAvailable()) {
            pluginLogger.log("CRITICAL ERROR: JDA library not found! Please install JDA 5.0.0+")
            pluginLogger.log("Download: https://github.com/DV8FromTheWorld/JDA/releases")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        if (configManager.token.isEmpty()) {
            pluginLogger.log("ERROR: Discord token is empty! Check config.yml")
        } else {
            try {
                discordBot = DiscordBot(this)
                discordBot.start()
            } catch (e: Exception) {
                pluginLogger.log("Failed to start Discord bot: ${e.message}")
            }
        }

        getCommand("dbankreload")?.setExecutor(ReloadCommand(this))
        pluginLogger.log("Plugin enabled successfully")
    }

    private fun isJDAAvailable(): Boolean {
        return try {
            Class.forName("net.dv8tion.jda.api.JDA")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    override fun onDisable() {
        if (::discordBot.isInitialized) {
            discordBot.shutdown()
        }
        pluginLogger.log("Plugin disabled")
    }

    fun reload() {
        configManager.loadConfig()
        pluginLogger.reloadConfig()

        if (::discordBot.isInitialized) {
            discordBot.shutdown()
            try {
                discordBot = DiscordBot(this)
                discordBot.start()
            } catch (e: Exception) {
                pluginLogger.log("Failed to restart Discord bot: ${e.message}")
            }
        }

        pluginLogger.log("Configuration reloaded")
    }
}