package com.herzchen.dotmanbankingexpansion

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PluginLogger(private val plugin: DotmanBankingExpansion) {
    private val logsDir: File = File(plugin.dataFolder, "logs")
    private val fileNameDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val entryDateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    init {
        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }
        log("=== Bot starting up ===")
    }

    fun log(message: String) {
        try {
            val today = Date()
            val fileName = "${fileNameDateFormat.format(today)}.log"
            val logFile = File(logsDir, fileName)

            if (!logFile.exists()) {
                logFile.createNewFile()
            }

            val timestamp = entryDateFormat.format(today)
            val line = "[$timestamp] $message"

            logFile.appendText(line + System.lineSeparator())

            plugin.server.consoleSender.sendMessage(line)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun logCommandExecution(cmd: String, success: Boolean, response: String? = null) {
        val status = if (success) "SUCCESS" else "FAILED"
        val emoji = if (success) "✅" else "❌"
        val responsePart = if (response != null) " | Response: $response" else ""

        log("$emoji Command: '$cmd' - Status: $status$responsePart")
    }
}