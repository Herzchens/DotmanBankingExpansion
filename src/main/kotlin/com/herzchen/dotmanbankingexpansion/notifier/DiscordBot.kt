package com.herzchen.dotmanbankingexpansion.notifier

import com.herzchen.dotmanbankingexpansion.DotmanBankingExpansion

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.EmbedBuilder

import org.bukkit.Bukkit

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.Duration
import java.awt.Color

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

            logger.log("Bot đã khởi động lại !")

            if (config.debugEnabled) {
                jda.getGuildById(config.debugGuildId)
                    ?.getTextChannelById(config.debugChannelId)
                    ?.let { debugChannel = it }
                    ?: logger.log("Không tìm thấy máy chủ/kênh debug !")
            }
        } catch (e: Exception) {
            logger.log("Khởi động bot discord thất bại: ${e.message}")
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

        val amountRaw = line2
            .substringAfter("nạp **")
            .substringBefore(" VNĐ**")
            .replace(" ", "")
        val amount = parseAmountWithSuffix(amountRaw)

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
        if (config.debugEnabled) {
            sendDebugMessage("💰 Tổng donate: $amount VNĐ")
            sendDebugMessage("🔍 Các mốc nạp tiền: ${config.commands.keys.sorted().joinToString()}")
        }

        var bonusTimes = 0
        if (config.milestoneBonusEnabled && config.milestoneAmount > 0 && amount > 0) {
            bonusTimes = amount / config.milestoneAmount
            if (bonusTimes > 0) {
                config.milestoneCommands.forEach { cmdTemplate ->
                    val cmd = cmdTemplate
                        .replace("{player}", player)
                        .replace("{bonus_times}", bonusTimes.toString())
                        .replace("{amount}", amount.toString())
                        .replace("{cluster}", cluster)

                    runCommand(cmd, bonusTimes)
                }
                if (config.debugEnabled) {
                    sendDebugMessage("🎁 Milestone bonus được trao $bonusTimes lần cho $amount VNĐ")
                }
            }
        }

        if (config.streakEnabled) {
            val bukkitPlayer = Bukkit.getPlayerExact(player)
            if (bukkitPlayer != null) {
                val streakData = plugin.streakService.updateStreak(bukkitPlayer.uniqueId)
                val streak = streakData.currentStreak

                config.streakCommands.forEach { (requiredStreak, commands) ->
                    if (streak >= requiredStreak) {
                        commands.forEach { cmdTemplate ->
                            val cmd = cmdTemplate
                                .replace("{player}", player)
                                .replace("{amount}", amount.toString())
                                .replace("{streak}", streak.toString())

                            runCommand(cmd, 1)
                        }
                    }
                }

                if (config.debugEnabled) {
                    sendDebugMessage("🔥 Cập nhật chuỗi cho $player: $streak ngày")
                }
            } else {
                if (config.debugEnabled) {
                    sendDebugMessage("⚠️ Người chơi $player chưa online, chuỗi chưa được cập nhật.")
                }
            }
        }

        val toRun = config.commands.entries
            .map { (level, cmds) -> level to cmds }
            .filter { (level, _) -> amount >= level }
            .sortedBy { (level, _) -> level }
            .flatMap { (_, cmds) -> cmds }


        toRun.forEach { cmdTmpl ->
            val cmd = cmdTmpl
                .replace("{player}", player)
                .replace("{amount}", amount.toString())
                .replace("{cluster}", cluster)

            runCommand(cmd, 1)
        }

        if (toRun.isNotEmpty() || bonusTimes > 0) {
            logger.log("Xử lý thanh toán: $player - $amount VNĐ - $cluster")
            val milestoneText = if (bonusTimes > 0) " (Milestone x$bonusTimes)" else ""

            sendDebugEmbed(
                "💰 BIẾT ÔNG HOÀN KHÔNG ?",
                "**Tổng:** $amount VNĐ$milestoneText\n" +
                        "**Người chơi:** $player\n" +
                        "**Cụm:** $cluster\n" +
                        "**Số lệnh thực hiện:** ${toRun.size + (if (bonusTimes > 0) config.milestoneCommands.size else 0)}",
                Color(0x00FF00)
            )
        } else {
            logger.log("Không tìm thấy lệnh cho mốc: $amount VNĐ")
            sendDebugEmbed(
                "⚠️ GIAO DỊCH THẤT BẠI",
                "Không tìm thấy lệnh cho mốc: $amount VNĐ",
                Color(0xFF0000)
            )
        }
    }

    private fun parseAmountWithSuffix(input: String): Int {
        val cleanInput = input.replace("VNĐ", "")
            .replace("đ", "")
            .replace(",", "")
            .replace(".", "")
            .replace(" ", "")
            .trim()
        return cleanInput.toIntOrNull() ?: 0
    }

    private fun runCommand(cmd: String, times: Int = 1) {
        Bukkit.getScheduler().runTask(plugin, Runnable {
            try {
                val result = executeCommandAndCapture(cmd)
                val timesText = if (times > 1) " Tổng thi hành [x$times]" else ""

                plugin.pluginLogger.logCommandExecution(cmd, result.success, result.response)

                if (config.debugEnabled) {
                    val emoji = if (result.success) "✅" else "❌"
                    sendDebugMessage("$emoji Thi hành lệnh `$cmd`$timesText")

                }

            } catch (e: Exception) {
                plugin.pluginLogger.logCommandExecution(cmd, false, "Exception: ${e.message}")
                if (config.debugEnabled) {
                    sendDebugMessage("❌ Lỗi thi hành lệnh `$cmd`: ${e.message}")
                }
            }
        })
    }
    //TODO Init Discord Hook to send messages to player in order to remind player

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

    private fun sendDebugEmbed(title: String, description: String, color: Color = Color(0x00FF00)) {
        if (!config.debugEnabled) return
        try {
            val embed = EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .build()
            debugChannel?.sendMessageEmbeds(embed)?.queue()
        } catch (e: Exception) {
            logger.log("Lỗi gửi tin nhắn debug: ${e.message}")
        }
    }

    fun shutdown() {
        if (!::jda.isInitialized) return

        try {
            jda.shutdown()
            if (!jda.awaitShutdown(Duration.ofSeconds(10))) {
                logger.log("Cảnh báo: Quá hạn thời gian shutdown bot!")
            }
            logger.log("Bot đã ngừng hoạt động")
        } catch (e: Exception) {
            logger.log("Có lỗi xảy ra khi tắt bot: ${e.message}")
        }
    }
}