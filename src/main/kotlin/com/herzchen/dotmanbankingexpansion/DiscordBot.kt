package com.herzchen.dotmanbankingexpansion

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import org.bukkit.Bukkit

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

        val lines = raw
            .split(Regex("\r?\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

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

        val amountLevels = config.commands.keys.mapNotNull { key ->
            when (key) {
                "10K" -> 10_000
                "20K" -> 20_000
                "50K" -> 50_000
                "100K" -> 100_000
                "200K" -> 200_000
                "500K" -> 500_000
                else   -> null
            }
        }.sorted()

        val toRun = mutableListOf<String>()
        for (level in amountLevels) {
            if (amount >= level) {
                val key = "${level/1000}K"
                config.commands[key]?.let { toRun += it }
            }
        }

        toRun.forEach { cmdTmpl ->
            val cmd = cmdTmpl
                .replace("{player}", player)
                .replace("{amount}", amount.toString())
                .replace("{cluster}", cluster)

            Bukkit.getScheduler().runTask(plugin, Runnable {
                val success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
                if (config.debugEnabled) {
                    val emoji = if (success) "✅" else "❌"
                    sendDebugMessage("$emoji Thi hành lệnh `$cmd`")
                }
            })

        }

        if (toRun.isNotEmpty()) {
            logger.log("Processed donation: $player - $amount VNĐ - $cluster")
            sendDebugMessage("💰 Đã xử lý donate $amount VNĐ từ $player tại $cluster")
        } else {
            logger.log("No commands for donation: $amount VNĐ")
            sendDebugMessage("⚠️ Không tìm thấy lệnh cho donate $amount VNĐ")
        }
    }

    private fun sendDebugMessage(msg: String) {
        if (!config.debugEnabled) return
        debugChannel?.sendMessage(msg)?.queue()
    }

    fun shutdown() {
        try {
            jda.shutdown()
            logger.log("Discord bot shutdown")
        } catch (e: Exception) {
            logger.log("Error shutting down Discord bot: ${e.message}")
        }
    }
}
