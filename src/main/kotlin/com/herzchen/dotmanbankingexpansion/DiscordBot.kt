package com.herzchen.dotmanbankingexpansion

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import org.bukkit.Bukkit
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.Duration

class DiscordBot(private val plugin: DotmanBankingExpansion) : ListenerAdapter() {
    private lateinit var jda: JDA
    private val config get() = plugin.configManager
    private val logger get() = plugin.pluginLogger
    private var debugChannel: TextChannel? = null

    fun start() {
        try {
            jda = JDABuilder.createDefault(config.token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(this)
                .build()
                .awaitReady()

            logger.log("Discord bot started successfully")

            if (config.debugEnabled) {
                jda.getGuildById(config.debugGuildId)
                    ?.getTextChannelById(config.debugChannelId)
                    ?.let { debugChannel = it }
                    ?: logger.log("Debug channel/guild not found")
            }
        } catch (e: Exception) {
            logger.log("Failed to start Discord bot: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.author.isBot && !config.acceptUserMessages) return
        if (event.author.isBot && event.author.id == jda.selfUser.id) return

        if (event.guild.idLong != config.guildId || event.channel.idLong != config.channelId) return

        val raw = event.message.contentRaw.trim()
        if (!raw.startsWith("## Nạp thẻ")) {
            if (config.debugEnabled) sendDebugMessage("❗ Message không khớp header: $raw")
            return
        }

        val lines = raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        val line2 = lines.getOrNull(1) ?: run {
            if (config.debugEnabled) sendDebugMessage("❗ Thiếu dòng 2: $raw")
            return
        }
        val player = line2.substringAfter("**").substringBefore("**")
        val amountStr = line2
            .substringAfter("nạp **")
            .substringBefore(" VNĐ**")
            .replace("\\D".toRegex(), "")
        val amount = amountStr.toIntOrNull() ?: 0

        val method = lines
            .firstOrNull { it.startsWith("Hình thức nạp:") }
            ?.substringAfter("Hình thức nạp:")
            ?.trim() ?: ""
        val cluster = lines
            .firstOrNull { it.startsWith("Cụm:") }
            ?.substringAfter("Cụm:")
            ?.trim() ?: ""

        if (method !in config.acceptedMethods) {
            sendDebugMessage("⚠️ Phương thức không được chấp nhận: $method")
            return
        }
        if (cluster !in config.clusters) {
            sendDebugMessage("⚠️ Cụm không hợp lệ: $cluster")
            return
        }

        val toRun = config.commands.entries
            .flatMap { (key, cmds) ->
                val currentLevel = parseLevel(key.toString())
                if (amount >= currentLevel) cmds else emptyList()
            }

        toRun.forEach { cmdTmpl ->
            val cmd = cmdTmpl
                .replace("{player}", player)
                .replace("{amount}", amount.toString())
                .replace("{cluster}", cluster)

            Bukkit.getScheduler().runTask(plugin, Runnable {
                try {
                    val result = executeCommandAndCapture(cmd)

                    plugin.pluginLogger.logCommandExecution(cmd, result.success, result.response)

                    if (config.debugEnabled) {
                        val emoji = if (result.success) "✅" else "❌"
                        sendDebugMessage("$emoji Thi hành lệnh `$cmd`")
                    }
                } catch (e: Exception) {
                    plugin.pluginLogger.logCommandExecution(cmd, false, "Exception: ${e.message}")
                    if (config.debugEnabled) {
                        sendDebugMessage("❌ Lỗi khi thi hành lệnh `$cmd`: ${e.message}")
                    }
                }
            })
        }

        if (toRun.isNotEmpty()) {
            logger.log("Processed donation: $player - $amount VNĐ - $cluster")
            sendDebugMessage("## 💰 ĐÃ XỬ LÝ DONATE ${amount}VNĐ ##\n• Người chơi: $player\n• Cụm: $cluster")
        } else {
            logger.log("No commands for donation: $amount VNĐ")
            sendDebugMessage("⚠️ Không tìm thấy lệnh cho donate $amount VNĐ")
        }
    }

    private fun parseLevel(key: String): Int {
        return when {
            key.endsWith("K", true) -> key.removeSuffix("K").removeSuffix("k").toDoubleOrNull()?.times(1000)?.toInt() ?: 0
            key.endsWith("M", true) -> key.removeSuffix("M").removeSuffix("m").toDoubleOrNull()?.times(1000000)?.toInt() ?: 0
            key.endsWith("B", true) -> key.removeSuffix("B").removeSuffix("b").toDoubleOrNull()?.times(1000000000)?.toInt() ?: 0
            else -> key.toIntOrNull() ?: 0
        }
    }

    private fun executeCommandAndCapture(cmd: String): CommandResult {
        val outputCapture = ByteArrayOutputStream()
        val originalOut = System.out
        val originalErr = System.err

        try {
            System.setOut(PrintStream(outputCapture))
            System.setErr(PrintStream(outputCapture))

            val success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
            return CommandResult(success, outputCapture.toString().trim())
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
        }
    }

    private data class CommandResult(val success: Boolean, val response: String)

    private fun sendDebugMessage(msg: String) {
        if (!config.debugEnabled) return
        debugChannel?.sendMessage(msg)?.queue()
    }

    fun shutdown() {
        if (!::jda.isInitialized) return

        try {
            jda.shutdown()
            if (!jda.awaitShutdown(Duration.ofSeconds(10))) {
                logger.log("Warning: Discord bot shutdown timed out")
            }
            logger.log("Discord bot shutdown")
        } catch (e: Exception) {
            logger.log("Error shutting down Discord bot: ${e.message}")
        }
    }
}