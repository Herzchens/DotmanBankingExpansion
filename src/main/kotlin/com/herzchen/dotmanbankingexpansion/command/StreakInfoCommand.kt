package com.herzchen.dotmanbankingexpansion.command

import com.herzchen.dotmanbankingexpansion.DotmanBankingExpansion

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

import java.time.Duration
import java.time.Instant

class StreakInfoCommand(private val plugin: DotmanBankingExpansion) : CommandExecutor {
    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        val targetPlayer = if (args.isNotEmpty() && sender.hasPermission("dbe.admin")) {
            Bukkit.getPlayer(args[0])
        } else if (sender is Player) {
            sender
        } else {
            sender.sendMessage(Component.text("Chỉ người chơi mới có thể sử dụng lệnh này!", NamedTextColor.RED))
            return true
        }

        val uuid = targetPlayer?.uniqueId ?: run {
            sender.sendMessage(Component.text("Người chơi không tồn tại!", NamedTextColor.RED))
            return true
        }

        val data = plugin.streakDataManager.getPlayerData(uuid) ?: run {
            sender.sendMessage(Component.text("Không có dữ liệu streak!", NamedTextColor.RED))
            return true
        }

        val timeLeft = Duration.between(Instant.now(), data.lastUpdate.plusSeconds(plugin.streakService.cycleSeconds))
        val hours = timeLeft.toHours()
        val minutes = timeLeft.toMinutesPart()

        val info = Component.text()
            .append(Component.text("===== Thông Tin Streak =====", NamedTextColor.GOLD))
            .append(Component.newline())
            .append(Component.text("Chuỗi hiện tại: ", NamedTextColor.YELLOW))
            .append(Component.text(data.currentStreak, NamedTextColor.GREEN))
            .append(Component.newline())
            .append(Component.text("Chuỗi dài nhất: ", NamedTextColor.YELLOW))
            .append(Component.text(data.longestStreak, NamedTextColor.GREEN))
            .append(Component.newline())
            .append(Component.text("Token đóng băng: ", NamedTextColor.YELLOW))
            .append(Component.text(data.freezeTokens, NamedTextColor.AQUA))
            .append(Component.newline())
            .append(Component.text("Token khôi phục: ", NamedTextColor.YELLOW))
            .append(Component.text(data.restoreTokens, NamedTextColor.AQUA))
            .append(Component.newline())
            .append(Component.text("Token revert: ", NamedTextColor.YELLOW))
            .append(Component.text(data.revertTokens, NamedTextColor.AQUA))
            .append(Component.newline())
            .append(Component.text("Thời gian còn lại: ", NamedTextColor.YELLOW))
            .append(Component.text("${hours}h${minutes}m", NamedTextColor.GREEN))
            .build()

        sender.sendMessage(info)
        return true
    }
}