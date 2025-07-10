package com.herzchen.dotmanbankingexpansion

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PluginLogger(private val plugin: DotmanBankingExpansion) {
    private lateinit var logFile: File
    private lateinit var dateFormat: SimpleDateFormat
    private var logFormat: String = "[{date}] {message}"

    init {
        reloadConfig()
    }

    fun reloadConfig() {
        val logConfigFile = File(plugin.dataFolder, "log.yml")
        if (!logConfigFile.exists()) {
            plugin.saveResource("log.yml", true)
        }

        val logConfig = YamlConfiguration.loadConfiguration(logConfigFile)

        logFormat = logConfig.getString("log-format") ?: "[{date}] {message}"
        val datePattern = logConfig.getString("date-format") ?: "dd/MM/yyyy - HH:mm:ss.SSS"
        val logPath = logConfig.getString("log-file") ?: "logs/dotman-banking.log"

        dateFormat = SimpleDateFormat(datePattern)
        logFile = File(plugin.dataFolder, logPath).apply {
            parentFile.mkdirs()
        }
    }

    fun log(message: String) {
        val date = dateFormat.format(Date())
        val formattedMessage = logFormat
            .replace("{date}", date)
            .replace("{message}", message)

        logFile.appendText("$formattedMessage\n")
        plugin.server.consoleSender.sendMessage(formattedMessage)
    }
}