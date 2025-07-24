package com.herzchen.dotmanbankingexpansion.util

import com.herzchen.dotmanbankingexpansion.DotmanBankingExpansion

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class PluginLogger(private val plugin: DotmanBankingExpansion) {
    private val logsDir: File = File(plugin.dataFolder, "logs")
    private val fileNameFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.getDefault())
    private val entryFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS", Locale.getDefault())
    private var writer: BufferedWriter? = null
    private var currentLogFile: File? = null

    init {
        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }
        log("=== Bot starting up ===")
    }

    fun log(message: String) {
        try {
            val today = LocalDate.now()
            val fileName = fileNameFormatter.format(today) + ".log"
            val logFile = File(logsDir, fileName)

            if (writer == null || currentLogFile != logFile) {
                writer?.close()
                writer = FileWriter(logFile, true).buffered()
                currentLogFile = logFile
            }

            val timestamp = entryFormatter.format(LocalDateTime.now())
            val line = "[$timestamp] $message"

            writer?.write(line)
            writer?.newLine()
            writer?.flush()

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