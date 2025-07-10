package com.herzchen.dotmanbankingexpansion

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import org.bukkit.Bukkit
import java.util.regex.Matcher

class DiscordBot(private val plugin: DotmanBankingExpansion) : ListenerAdapter() {
    private lateinit var jda: JDA
    private val config get() = plugin.configManager
    private val logger get() = plugin.pluginLogger

    fun start() {
        try {
            jda = JDABuilder.createDefault(config.token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(this)
                .build()
                .awaitReady()

            logger.log("Discord bot started successfully")
        } catch (e: Exception) {
            logger.log("Failed to start Discord bot: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return

        val guild: Guild? = event.guild
        val channel: TextChannel? = event.channel.asTextChannel()

        if (guild?.idLong != config.guildId || channel?.idLong != config.channelId) {
            return
        }

        val matcher = config.triggerPattern.matcher(event.message.contentRaw)
        if (matcher.find()) {
            processDonation(matcher)
        }
    }

    private fun processDonation(matcher: Matcher) {
        val cluster = matcher.group("cluster")
        val amount = matcher.group("amount").replace(".", "").replace(",", "")
        val player = matcher.group("player")
        val method = matcher.group("method")

        if (method !in config.acceptedMethods) {
            logger.log("Skipped donation: Invalid method $method")
            return
        }

        if (cluster !in config.clusters) {
            logger.log("Skipped donation: Invalid cluster $cluster")
            return
        }

        val amountKey = when (amount.toInt()) {
            in 10000..19999 -> "10K"
            in 20000..49999 -> "20K"
            in 50000..99999 -> "50K"
            in 100000..199999 -> "100K"
            in 200000..499999 -> "200K"
            else -> "500K"
        }

        config.commands[amountKey]?.forEach { command ->
            val formattedCmd = command
                .replace("{player}", player)
                .replace("{amount}", amount)
                .replace("{cluster}", cluster)

            Bukkit.getScheduler().runTask(plugin, Runnable {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCmd)
            })
        }

        logger.log("Processed donation: $player - $amount VNĐ - $cluster")
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