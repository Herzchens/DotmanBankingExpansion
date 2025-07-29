package com.herzchen.dotmanbankingexpansion.manager

import com.herzchen.dotmanbankingexpansion.DotmanBankingExpansion

import org.bukkit.Bukkit
import org.bukkit.entity.Player

import java.util.*
import java.util.concurrent.ConcurrentHashMap

class DiscordLinkManager(private val plugin: DotmanBankingExpansion) {
    private val pendingLinks = ConcurrentHashMap<UUID, PendingLink>()
    private val pendingLinksByCode = ConcurrentHashMap<String, PendingLink>()
    private val pendingUnlinks = ConcurrentHashMap<UUID, String>()

    data class PendingLink(
        val playerUuid: UUID,
        val discordId: String,
        val code: String,
        val expireTime: Long
    )

    fun createLinkRequest(player: Player, discordId: String): String {
        val code = generateCode()
        val expireTime = System.currentTimeMillis() + plugin.configManager.discordLinkExpireMinutes * 60 * 1000L
        val pending = PendingLink(player.uniqueId, discordId, code, expireTime)
        pendingLinks[player.uniqueId] = pending
        pendingLinksByCode[code] = pending
        return code
    }

    fun confirmLinkByCode(code: String, discordId: String): Boolean {
        val pending = pendingLinksByCode[code] ?: return false

        if (System.currentTimeMillis() > pending.expireTime || pending.discordId != discordId) {
            pendingLinksByCode.remove(code)
            pendingLinks.remove(pending.playerUuid)
            return false
        }

        plugin.streakDataManager.linkDiscord(pending.playerUuid, pending.discordId)
        pendingLinks.remove(pending.playerUuid)
        pendingLinksByCode.remove(code)

        val playerName = Bukkit.getOfflinePlayer(pending.playerUuid).name ?: "Unknown"
        plugin.pluginLogger.logDiscordAction(
            "LINK_CONFIRMED",
            playerName,
            discordId
        )

        return true
    }

    fun createUnlinkRequest(player: Player): String {
        val code = generateCode()
        pendingUnlinks[player.uniqueId] = code
        return code
    }

    fun confirmLink(player: Player, code: String): Boolean {
        val pending = pendingLinks[player.uniqueId] ?: return false

        if (System.currentTimeMillis() > pending.expireTime) {
            pendingLinks.remove(player.uniqueId)
            pendingLinksByCode.remove(pending.code)
            return false
        }

        if (pending.code == code) {
            plugin.streakDataManager.linkDiscord(player.uniqueId, pending.discordId)
            pendingLinks.remove(player.uniqueId)
            pendingLinksByCode.remove(pending.code)
            return true
        }
        return false
    }

    fun confirmUnlink(player: Player, code: String): Boolean {
        val pendingCode = pendingUnlinks[player.uniqueId] ?: return false

        if (pendingCode == code) {
            plugin.streakDataManager.unlinkDiscord(player.uniqueId)
            pendingUnlinks.remove(player.uniqueId)
            return true
        }
        return false
    }

    private fun generateCode(): String {
        val length = plugin.configManager.discordLinkCodeLength
        return (1..length).joinToString("") { (0..9).random().toString() }
    }
}