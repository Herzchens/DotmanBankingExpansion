package com.herzchen.dotmanbankingexpansion.command

import com.herzchen.dotmanbankingexpansion.model.StreakState

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class TabComplete : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (command.name.equals("dbe", ignoreCase = true)) {
            return when (args.size) {
                1 -> {
                    val base = listOf("reload", "streakinfo", "streak", "help", "confirm", "discord")
                    val admin = if (sender.hasPermission("dbe.admin")) {
                        listOf("frozen", "restore", "revert")
                    } else {
                        emptyList()
                    }
                    (base + admin).filter { it.startsWith(args[0], true) }
                }
                2 -> when (args[0].lowercase()) {
                    "streak" -> {
                        val playerOptions = listOf("restore", "revert")
                        val adminOptions = if (sender.hasPermission("dbe.admin")) {
                            listOf("resetall", "frozen", "set", "timeset", "status")
                        } else {
                            emptyList()
                        }
                        (playerOptions + adminOptions).filter { it.startsWith(args[1], true) }
                    }
                    "frozen", "restore", "revert" ->
                        listOf("give", "take", "takeall").filter { it.startsWith(args[1], true) }
                    "confirm" -> emptyList()
                    "streakinfo" -> if (sender.hasPermission("dbe.admin")) {
                        Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], true) }
                    } else {
                        emptyList()
                    }
                    "discord" -> listOf("link", "unlink", "confirm").filter { it.startsWith(args[1], true) }
                    else -> emptyList()
                }
                3 -> when (args[0].lowercase()) {
                    "frozen", "restore", "revert" ->
                        Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[2], true) }
                    "streak" -> when (args[1].lowercase()) {
                        "frozen", "set", "timeset", "status" ->
                            Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[2], true) }
                        else -> emptyList()
                    }
                    "discord" -> when (args[1].lowercase()) {
                        "confirm" -> listOf("link", "unlink").filter { it.startsWith(args[2], true) }
                        else -> emptyList()
                    }
                    else -> emptyList()
                }
                4 -> when (args[0].lowercase()) {
                    "frozen", "restore", "revert" ->
                        (1..64).map { it.toString() }.filter { it.startsWith(args[3], true) }
                    "streak" -> when (args[1].lowercase()) {
                        "frozen" -> (1..30).map { it.toString() }.filter { it.startsWith(args[3], true) }
                        "set" -> (1..100).map { it.toString() }.filter { it.startsWith(args[3], true) }
                        "status" -> StreakState.entries.map { it.name }.filter { it.startsWith(args[3], true) }
                        else -> emptyList()
                    }
                    else -> emptyList()
                }
                else -> emptyList()
            }
        }
        return emptyList()
    }
}