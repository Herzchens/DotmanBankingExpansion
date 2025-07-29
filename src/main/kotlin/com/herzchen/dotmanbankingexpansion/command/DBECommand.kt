package com.herzchen.dotmanbankingexpansion.command

import com.herzchen.dotmanbankingexpansion.DotmanBankingExpansion
import com.herzchen.dotmanbankingexpansion.model.StreakState
import com.herzchen.dotmanbankingexpansion.model.StreakData

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
            "streakinfo" -> plugin.streakInfoCommand.onCommand(sender, cmd, label, args.copyOfRange(1, args.size))
            "streak" -> handleStreak(sender, args)
            "frozen", "restore", "revert" -> handleTokenManagement(sender, args)
            "help" -> showHelp(sender)
            "confirm" -> handleConfirm(sender, args)
            "discord" -> handleDiscord(sender, args)
            else -> showHelp(sender)
        }

        return true
    }

    private fun getPlayerAndData(
        sender: CommandSender,
        args: Array<out String>
    ): Pair<Player, StreakData>? {
        val target = if (args.size > 2 && sender.hasPermission("dbe.admin")) {
            Bukkit.getPlayer(args[2])
        } else {
            sender as? Player
        }
        if (target == null) {
            sender.sendMessage(Component.text("Không tìm thấy người chơi!", NamedTextColor.RED))
            return null
        }
        val data = plugin.streakService.repo.find(target.uniqueId)
        if (data == null) {
            sender.sendMessage(Component.text("Không có dữ liệu streak!", NamedTextColor.RED))
            return null
        }
        return target to data
    }

    private fun showHelp(sender: CommandSender) {
        sender.sendMessage(Component.text("----Member Command------", NamedTextColor.AQUA))
        sender.sendMessage(Component.text("/dbe help - Hiển thị trang trợ giúp này", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/dbe streakinfo - Xem thông tin streak của bạn", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/dbe streak restore - Khôi phục streak", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/dbe streak revert - Chuyển thành chuỗi dài nhất bạn từng có", NamedTextColor.YELLOW))

        sender.sendMessage(Component.text("----Discord Command--------", NamedTextColor.AQUA))
        sender.sendMessage(Component.text("/dbe discord link <id | username> - Liên kết discord", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/dbe discord unlink - Huỷ liên kết discord", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/dbe discord confirm link <code> - Xác nhân liên kết discord", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/dbe discord confirm unlink <code> - Xác nhận huỷ liên kết discord", NamedTextColor.YELLOW))

        if (sender.hasPermission("dbe.admin")) {
            sender.sendMessage(Component.text("----Admin Command-------", NamedTextColor.AQUA))
            sender.sendMessage(Component.text("/dbe reload - Tải lại cấu hình plugin", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/dbe streakinfo <player> - Xem thông tin streak của player", NamedTextColor.YELLOW))

            sender.sendMessage(Component.text("----------Frozen Management----------", NamedTextColor.GOLD))
            sender.sendMessage(Component.text("/dbe frozen give <player> <amount> - Trao frozen token", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/dbe frozen take <player> <amount> - Lấy frozen token", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/dbe frozen takeall <player> - Lấy toàn bộ frozen token", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/dbe streak frozen <player> <days> - Đóng băng streak", NamedTextColor.YELLOW))

            sender.sendMessage(Component.text("----------Restore Management----------", NamedTextColor.GOLD))
            sender.sendMessage(Component.text("/dbe restore give <player> <amount> - Trao restore token", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/dbe restore take <player> <amount> - Lấy restore token", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/dbe restore takeall <player> - Lấy toàn bộ restore token", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/dbe streak restore <player> - Khôi phục streak", NamedTextColor.YELLOW))

            sender.sendMessage(Component.text("----------Revert Management----------", NamedTextColor.GOLD))
            sender.sendMessage(Component.text("/dbe revert give <player> <amount> - Trao revert token", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/dbe revert take <player> <amount> - Lấy revert token", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/dbe revert takeall <player> - Lấy toàn bộ revert token", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/dbe streak revert <player> - Chuyển thành chuỗi dài nhất", NamedTextColor.YELLOW))

            sender.sendMessage(Component.text("----------Debug Command----------", NamedTextColor.GOLD))
            sender.sendMessage(Component.text("/dbe streak timeset <player> <time> - Set thời gian bossbar", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/dbe streak status <player> <status> - Set trạng thái streak", NamedTextColor.YELLOW))

            sender.sendMessage(Component.text("----------Other Command----------", NamedTextColor.GOLD))
            sender.sendMessage(Component.text("/dbe streak resetall - Reset toàn bộ streak", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/dbe confirm <code> - Xác nhận reset", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/dbe streak set <player> <amount> - Set streak", NamedTextColor.YELLOW))

        }
    }

    private fun handleReload(sender: CommandSender) {
        plugin.reload()
        sender.sendMessage(Component.text("Đã tải lại file cấu hình!", NamedTextColor.GREEN))
    }

    private fun handleStreak(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            showHelp(sender)
            return
        }

        when (args[1].lowercase()) {
            "resetall" -> handleResetAll(sender)
            "frozen" -> handleFreezePlayer(sender, args)
            "restore" -> handleRestore(sender, args)
            "revert" -> handleRevert(sender, args)
            "set" -> handleSetStreak(sender, args)
            "timeset" -> handleTimeSet(sender, args)
            "status" -> handleStatusSet(sender, args)
            else -> sender.sendMessage(Component.text("Lệnh không hợp lệ!", NamedTextColor.RED))
        }
    }

    private fun handleRestore(sender: CommandSender, args: Array<out String>) {
        val (target, data) = getPlayerAndData(sender, args) ?: return

        if (data.state != StreakState.EXPIRED) {
            sender.sendMessage(Component.text("Chỉ có thể khôi phục khi streak đã hết hạn!", NamedTextColor.RED))
            return
        }

        if (plugin.streakService.useRestore(target.uniqueId)) {
            sender.sendMessage(Component.text("Đã khôi phục streak cho ${target.name}", NamedTextColor.GREEN))
        } else {
            sender.sendMessage(Component.text("Không đủ token restore!", NamedTextColor.RED))
        }
    }

    private fun handleTokenManagement(sender: CommandSender, args: Array<out String>) {
        val tokenType = args[0].lowercase()
        val requiredPermission = "dbe.$tokenType"

        if (!sender.hasPermission(requiredPermission) && !sender.hasPermission("dbe.admin")) {
            sender.sendMessage(Component.text("Bạn không có quyền sử dụng lệnh này!", NamedTextColor.RED))
            return
        }

        if (args.size < 2) {
            showTokenHelp(sender, args.getOrNull(0) ?: "frozen|restore|revert")
            return
        }
        val action = args[1].lowercase()

        if (action == "takeall") {
            if (args.size < 3) {
                sender.sendMessage(Component.text("Usage: /dbe $tokenType takeall <player>", NamedTextColor.RED))
                return
            }

            val target = Bukkit.getPlayer(args[2]) ?: run {
                sender.sendMessage(Component.text("Không tìm thấy người chơi!", NamedTextColor.RED))
                return
            }

            val result = when (tokenType) {
                "frozen" -> plugin.streakTokenManager.takeAllFreezes(target)
                "restore" -> plugin.streakTokenManager.takeAllRestores(target)
                "revert" -> plugin.streakTokenManager.takeAllReverts(target)
                else -> false
            }

            val tokenName = when (tokenType) {
                "frozen" -> "đóng băng"
                "restore" -> "khôi phục"
                "revert" -> "revert"
                else -> "không xác định"
            }
            sender.sendMessage(if (result) {
                Component.text("Đã thu hồi toàn bộ token $tokenName của ${target.name}", NamedTextColor.GREEN)
            } else {
                Component.text("Thao tác thất bại!", NamedTextColor.RED)
            })
            return
        }

        if (args.size < 4) {
            sender.sendMessage(Component.text("Usage: /dbe $tokenType $action <player> <amount>", NamedTextColor.RED))
            return
        }

        val target = Bukkit.getPlayer(args[2]) ?: run {
            sender.sendMessage(Component.text("Không tìm thấy người chơi!", NamedTextColor.RED))
            return
        }

        val amount = args[3].toIntOrNull() ?: run {
            sender.sendMessage(Component.text("Số lượng phải là số nguyên!", NamedTextColor.RED))
            return
        }

        val result = when (action) {
            "give" -> {
                if (amount <= 0) {
                    sender.sendMessage(Component.text("Số lượng phải lớn hơn 0!", NamedTextColor.RED))
                    return
                }
                when (tokenType) {
                    "frozen" -> plugin.streakTokenManager.addFreezes(target, amount)
                    "restore" -> plugin.streakTokenManager.addRestores(target, amount)
                    "revert" -> plugin.streakTokenManager.addReverts(target, amount)
                    else -> {}
                }
                true
            }
            "take" -> {
                if (amount <= 0) {
                    sender.sendMessage(Component.text("Số lượng phải lớn hơn 0!", NamedTextColor.RED))
                    return
                }
                when (tokenType) {
                    "frozen" -> plugin.streakTokenManager.takeFreezes(target, amount)
                    "restore" -> plugin.streakTokenManager.takeRestores(target, amount)
                    "revert" -> plugin.streakTokenManager.takeReverts(target, amount)
                    else -> false
                }
            }
            else -> false
        }

        if (result) {
            sender.sendMessage(Component.text("Thao tác thành công!", NamedTextColor.GREEN))
        } else {
            val errorMsg = "Thao tác thất bại!"
            sender.sendMessage(Component.text(errorMsg, NamedTextColor.RED))
        }
    }

    private fun showTokenHelp(sender: CommandSender, tokenType: String) {
        sender.sendMessage(Component.text("Sử dụng lệnh:", NamedTextColor.RED))
        sender.sendMessage(Component.text("/dbe $tokenType give <player> <amount>", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/dbe $tokenType take <player> <amount>", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/dbe $tokenType takeall <player>", NamedTextColor.YELLOW))
    }

    private fun handleResetAll(sender: CommandSender) {
        val confirmationCode = generateConfirmationCode()
        sender.sendMessage(Component.text("Bạn có chắc muốn reset toàn bộ    streak?", NamedTextColor.RED))
        sender.sendMessage(Component.text("Nhập mã xác nhận sau trong 10 giây: $confirmationCode", NamedTextColor.YELLOW))

        sender.sendMessage(Component.text("Sử dụng: /dbe confirm $confirmationCode", NamedTextColor.YELLOW))

        plugin.confirmationManager.setConfirmation(sender, confirmationCode) {
            plugin.streakService.resetAllStreaks()
            sender.sendMessage(Component.text("Đã reset toàn bộ streak!", NamedTextColor.GREEN))
        }
    }

    private fun handleConfirm(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(Component.text("Usage: /dbe confirm <mã-xác-nhận>", NamedTextColor.RED))
            return
        }

        val code = args[1]
        if (plugin.confirmationManager.checkConfirmation(sender, code)) {
            sender.sendMessage(Component.text("Xác nhận thành công!", NamedTextColor.GREEN))
        } else {
            sender.sendMessage(Component.text("Mã xác nhận không đúng hoặc đã hết hạn!", NamedTextColor.RED))
        }
    }

    private fun handleSetStreak(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("dbe.set") && !sender.hasPermission("dbe.admin")) {
            sender.sendMessage(Component.text("Bạn không có quyền sử dụng lệnh này!", NamedTextColor.RED))
            return
        }
        if (args.size < 4) {
            sender.sendMessage(Component.text("Usage: /dbe streak set <player> <amount>", NamedTextColor.RED))
            return
        }

        val target = Bukkit.getPlayer(args[2])
        val amount = args[3].toIntOrNull()

        if (target == null || amount == null || amount < 0) {
            sender.sendMessage(Component.text("Tham số không hợp lệ! Số streak phải là số nguyên dương", NamedTextColor.RED))
            return
        }

        if (plugin.streakService.setStreak(target.uniqueId, amount)) {
            sender.sendMessage(Component.text("Đã set streak cho ${target.name} thành $amount", NamedTextColor.GREEN))
        } else {
            sender.sendMessage(Component.text("Thao tác thất bại!", NamedTextColor.RED))
        }
    }

    private fun handleRevert(sender: CommandSender, args: Array<out String>) {
        val (target, data) = getPlayerAndData(sender, args) ?: return

        if (data.currentStreak >= data.longestStreak) {
            sender.sendMessage(Component.text("Chuỗi hiện tại đã là dài nhất!", NamedTextColor.YELLOW))
            return
        }

        if (plugin.streakService.useRevert(target.uniqueId)) {
            sender.sendMessage(Component.text("Đã chuyển chuỗi của ${target.name} thành chuỗi dài nhất", NamedTextColor.GREEN))
        } else {
            sender.sendMessage(Component.text("Không đủ token revert!", NamedTextColor.RED))
        }
    }

    private fun handleFreezePlayer(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("dbe.admin")) {
            sender.sendMessage(Component.text("Bạn không có quyền sử dụng lệnh này!", NamedTextColor.RED))
            return
        }
        if (args.size < 4) {
            sender.sendMessage(Component.text("Usage: /dbe streak frozen <player> <days>", NamedTextColor.RED))
            return
        }

        val target = Bukkit.getPlayer(args[2])
        val days = args[3].toIntOrNull()

        if (target == null || days == null || days <= 0) {
            sender.sendMessage(Component.text("Tham số không hợp lệ!", NamedTextColor.RED))
            return
        }

        if (plugin.streakService.freezePlayer(target.uniqueId, days)) {
            sender.sendMessage(Component.text("Đã đóng băng streak của ${target.name} trong $days ngày", NamedTextColor.GREEN))
        } else {
            sender.sendMessage(Component.text("Thao tác thất bại!", NamedTextColor.RED))
        }
    }

    private fun handleTimeSet(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("dbe.timeset") && !sender.hasPermission("dbe.admin")) {
            sender.sendMessage(Component.text("Bạn không có quyền!", NamedTextColor.RED))
            return
        }

        if (args.size < 4) {
            sender.sendMessage(Component.text("Usage: /dbe streak timeset <player> <time>", NamedTextColor.RED))
            sender.sendMessage(Component.text("Ví dụ: /dbe streak timeset Player1 12h30m", NamedTextColor.YELLOW))
            return
        }

        val target = Bukkit.getPlayer(args[2])
        if (target == null) {
            sender.sendMessage(Component.text("Không tìm thấy người chơi!", NamedTextColor.RED))
            return
        }

        val timeInput = args[3].lowercase()
        var totalSeconds = 0L

        try {
            if (timeInput.contains("h")) {
                val hoursPart = timeInput.substringBefore("h")
                totalSeconds += hoursPart.toLong() * 3600
            }

            if (timeInput.contains("m")) {
                val minutesPart = timeInput.substringAfter("h", "").substringBefore("m")
                if (minutesPart.isNotEmpty()) {
                    totalSeconds += minutesPart.toLong() * 60
                }
            }

            if (!timeInput.contains("h") && !timeInput.contains("m")) {
                totalSeconds = timeInput.toLong()
            }
        } catch (_: Exception) {
            sender.sendMessage(Component.text("Thời gian không hợp lệ!", NamedTextColor.RED))
            return
        }

        if (totalSeconds <= 0 || totalSeconds > 86400) {
            sender.sendMessage(Component.text("Thời gian phải từ 1 giây đến 24 giờ!", NamedTextColor.RED))
            return
        }

        if (plugin.streakService.setRemainingTime(target.uniqueId, totalSeconds)) {
            sender.sendMessage(Component.text("Đã set thời gian bossbar cho ${target.name}: $totalSeconds giây", NamedTextColor.GREEN))
        } else {
            sender.sendMessage(Component.text("Thao tác thất bại!", NamedTextColor.RED))
        }
    }

    private fun handleStatusSet(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("dbe.status") && !sender.hasPermission("dbe.admin")) {
            sender.sendMessage(Component.text("Bạn không có quyền sử dụng lệnh này!", NamedTextColor.RED))
            return
        }
        if (args.size < 4) {
            sender.sendMessage(Component.text("Usage: /dbe streak status <player> <status>", NamedTextColor.RED))
            return
        }

        val target = Bukkit.getPlayer(args[2])
        if (target == null) {
            sender.sendMessage(Component.text("Không tìm thấy người chơi!", NamedTextColor.RED))
            return
        }

        val status = try {
            StreakState.valueOf(args[3].uppercase())
        } catch (_: IllegalArgumentException) {
            null
        }

        if (status == null) {
            sender.sendMessage(Component.text("Trạng thái không hợp lệ! (INACTIVE, ACTIVE, FROZEN, EXPIRED)", NamedTextColor.RED))
            return
        }

        if (plugin.streakService.setStatus(target.uniqueId, status)) {
            sender.sendMessage(Component.text("Đã set trạng thái streak cho ${target.name} thành $status", NamedTextColor.GREEN))
        } else {
            sender.sendMessage(Component.text("Thao tác thất bại!", NamedTextColor.RED))
        }
    }

    private fun handleDiscord(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            showDiscordHelp(sender)
            return
        }
        when (args[1].lowercase()) {
            "link" -> handleDiscordLink(sender, args)
            "unlink" -> handleDiscordUnlink(sender)
            "confirm" -> handleDiscordConfirm(sender, args)
            else -> showDiscordHelp(sender)
        }
    }

    private fun handleDiscordLink(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Chỉ người chơi mới sử dụng được lệnh này", NamedTextColor.RED))
            return
        }

        if (args.size < 3) {
            sender.sendMessage(Component.text("Sử dụng: /dbe discord link <discord-id>", NamedTextColor.RED))
            return
        }

        val discordId = args[2]
        val code = plugin.discordLinkManager.createLinkRequest(sender, discordId)

        sender.sendMessage(Component.text("Mã xác nhận: $code", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("Hãy gửi mã này cho bot trong Discord qua kênh riêng (DM) để xác nhận", NamedTextColor.YELLOW))

        sender.sendMessage(Component.text("Cách xác nhận:", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("1. Mở Discord và nhắn tin riêng với bot", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("2. Gửi nội dung: $code", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("3. Bot sẽ xác nhận liên kết tự động", NamedTextColor.YELLOW))
    }

    private fun handleDiscordUnlink(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Chỉ người chơi mới sử dụng được lệnh này", NamedTextColor.RED))
            return
        }

        val code = plugin.discordLinkManager.createUnlinkRequest(sender)
        sender.sendMessage(Component.text("Mã xác nhận: $code", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("Sử dụng /dbe discord confirm $code để xác nhận", NamedTextColor.YELLOW))
    }

    private fun handleDiscordConfirm(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Chỉ người chơi mới sử dụng được lệnh này", NamedTextColor.RED))
            return
        }

        if (args.size < 3) {
            sender.sendMessage(Component.text("Sử dụng: /dbe discord confirm <mã>", NamedTextColor.RED))
            return
        }

        val code = args[2]
        val success = plugin.discordLinkManager.confirmLink(sender, code) ||
                plugin.discordLinkManager.confirmUnlink(sender, code)

        if (success) {
            sender.sendMessage(Component.text("Xác nhận thành công!", NamedTextColor.GREEN))
        } else {
            sender.sendMessage(Component.text("Mã xác nhận không hợp lệ hoặc đã hết hạn", NamedTextColor.RED))
        }
    }

    private fun showDiscordHelp(sender: CommandSender) {
        sender.sendMessage(Component.text("Sử dụng lệnh Discord:", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/dbe discord link <id | username> - Liên kết discord để nhận thông báo", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/dbe discord unlink - Huỷ liên kết discord", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/dbe discord confirm link <code> - Xác nhân liên kết discord", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/dbe discord confirm unlink <code> - Xác nhận huỷ liên kết discord", NamedTextColor.YELLOW))
    }

    private fun generateConfirmationCode(): String {
        val charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%?&*."
        return (1..7)
            .map { charPool.random() }
            .joinToString("")
    }
}