package com.herzchen.dotmanbankingexpansion.command

import com.herzchen.dotmanbankingexpansion.DotmanBankingExpansion

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class DBECommand(private val plugin: DotmanBankingExpansion) : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> handleReload(sender)

            "streakinfo" -> {
                plugin.streakInfoCommand.onCommand(
                    sender,
                    cmd,
                    label,
                    args.copyOfRange(1, args.size)
                )
            }

            "streak" -> handleStreak(sender, args)
            "help"   -> showHelp(sender)
            else     -> showHelp(sender)
        }

        return true
    }

    private fun showHelp(sender: CommandSender) {
        sender.sendMessage(Component.text("===== Dotman Banking Expansion Help =====", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("/dbe reload - Reload plugin configuration", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/dbe streakinfo [player] - Xem thông tin streak", NamedTextColor.YELLOW))

        if (sender.hasPermission("dbe.admin")) {
            sender.sendMessage(Component.text("/dbe streak frozen|restore|revert give|take|takeall <player> [amount] - Quản lý token", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/dbe streak resetall - Reset toàn bộ streak", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/dbe streak frozen <player> <days> - Đóng băng streak", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/dbe streak revert [player] - Revert streak", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/dbe streak set <player> <amount> - Set streak", NamedTextColor.YELLOW))
        } else {
            sender.sendMessage(Component.text("/dbe streak revert - Revert streak của bạn", NamedTextColor.YELLOW))
        }
    }

    private fun handleReload(sender: CommandSender) {
        plugin.reload()
        sender.sendMessage(Component.text("Plugin reloaded successfully!", NamedTextColor.GREEN))
    }

    private fun handleStreak(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("dbe.admin") && args.size < 2) {
            sender.sendMessage(Component.text("Bạn không có quyền sử dụng lệnh này!", NamedTextColor.RED))
            return
        }

        when (args.getOrNull(1)?.lowercase()) {
            "frozen", "restore" -> handleTokenManagement(sender, args)
            "resetall"                   -> handleResetAll(sender)
            "set"                        -> handleSetStreak(sender, args)
            "revert"                     -> handleRevert(sender, args)
            else                         -> sender.sendMessage(Component.text("lệnh không hợp lệ!", NamedTextColor.RED))
        }
    }

    private fun handleTokenManagement(sender: CommandSender, args: Array<out String>) {
        if (args.size < 4) {
            sender.sendMessage(Component.text("Usage: /dbe streak <type> <action> <player> [amount]", NamedTextColor.RED))
            return
        }

        val tokenType = args[1].lowercase()
        val action    = args[2].lowercase()
        val target    = Bukkit.getPlayer(args[3])
        val amount    = args.getOrNull(4)?.toIntOrNull() ?: 1

        if (target == null) {
            sender.sendMessage(Component.text("Không tìm thấy người chơi!", NamedTextColor.RED))
            return
        }

        val result: Boolean = when (action) {
            "give" -> when (tokenType) {
                "frozen"  -> { plugin.streakTokenManager.addFreezes(target, amount); true }
                "restore" -> { plugin.streakTokenManager.addRestores(target, amount); true }
                "revert"  -> { plugin.streakTokenManager.addReverts(target, amount); true }
                else      -> false
            }
            "take" -> when (tokenType) {
                "frozen"  -> { plugin.streakTokenManager.takeFreezes(target, amount); true }
                "restore" -> { plugin.streakTokenManager.takeRestores(target, amount); true }
                "revert"  -> { plugin.streakTokenManager.takeReverts(target, amount); true }
                else      -> false
            }
            "takeall" -> when (tokenType) {
                "frozen"  -> { plugin.streakTokenManager.takeAllFreezes(target); true }
                "restore" -> { plugin.streakTokenManager.takeAllRestores(target); true }
                "revert"  -> { plugin.streakTokenManager.takeAllReverts(target); true }
                else      -> false
            }
            else -> false
        }

        if (result) {
            sender.sendMessage(Component.text("Thao tác thành công!", NamedTextColor.GREEN))
        } else {
            sender.sendMessage(Component.text("Thao tác thất bại!", NamedTextColor.RED))
        }
    }

    private fun handleResetAll(sender: CommandSender) {
        val confirmationCode = generateConfirmationCode()
        sender.sendMessage(Component.text("Bạn có chắc muốn reset toàn bộ streak?", NamedTextColor.RED))
        sender.sendMessage(Component.text("Nhập mã xác nhận sau trong 10 giây: $confirmationCode", NamedTextColor.YELLOW))

        plugin.confirmationManager.setConfirmation(sender, confirmationCode) {
            plugin.streakService.resetAllStreaks()
            sender.sendMessage(Component.text("Đã reset toàn bộ streak!", NamedTextColor.GREEN))
        }
    }

    private fun handleSetStreak(sender: CommandSender, args: Array<out String>) {
        if (args.size < 4) {
            sender.sendMessage(Component.text("Usage: /dbe streak set <player> <amount>", NamedTextColor.RED))
            return
        }

        val target = Bukkit.getPlayer(args[2])
        val amount = args[3].toIntOrNull()

        if (target == null || amount == null) {
            sender.sendMessage(Component.text("Tham số không hợp lệ!", NamedTextColor.RED))
            return
        }

        if (plugin.streakService.setStreak(target.uniqueId, amount)) {
            sender.sendMessage(Component.text("Đã set streak cho ${target.name} thành $amount", NamedTextColor.GREEN))
        } else {
            sender.sendMessage(Component.text("Thao tác thất bại!", NamedTextColor.RED))
        }
    }

    private fun handleRevert(sender: CommandSender, args: Array<out String>) {
        val target = if (args.size > 2 && sender.hasPermission("dbe.admin")) {
            Bukkit.getPlayer(args[2])
        } else sender as? Player

        if (target == null) {
            sender.sendMessage(Component.text("Không tìm thấy người chơi!", NamedTextColor.RED))
            return
        }

        if (plugin.streakService.useRevert(target.uniqueId)) {
            sender.sendMessage(Component.text("Đã revert streak cho ${target.name}", NamedTextColor.GREEN))
        } else {
            sender.sendMessage(Component.text("Không đủ token revert!", NamedTextColor.RED))
        }
    }

    private fun generateConfirmationCode(): String {
        val charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%?&*."
        return (1..7)
            .map { charPool.random() }
            .joinToString("")
    }
}
