package com.herzchen.dotmanbankingexpansion.command

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
                1 -> listOf("reload", "streakinfo", "streak", "help").filter { it.startsWith(args[0], true) }
                2 -> when (args[0].lowercase()) {
                    "streak" -> {
                        val options = mutableListOf("frozen", "restore", "revert")
                        if (sender.hasPermission("dbe.admin")) {
                            options.addAll(listOf("resetall", "set"))
                        }
                        options.filter { it.startsWith(args[1], true) }
                    }
                    "streakinfo" -> if (sender.hasPermission("dbe.admin")) {
                        Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], true) }
                    } else {
                        emptyList()
                    }
                    else -> emptyList()
                }
                3 -> when (args[0].lowercase()) {
                    "streak" -> when (args[1].lowercase()) {
                        "frozen", "restore" -> listOf("give", "take", "takeall").filter { it.startsWith(args[2], true) }
                        "set", "revert" -> if (sender.hasPermission("dbe.admin")) {
                            Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[2], true) }
                        } else {
                            emptyList()
                        }
                        else -> emptyList()
                    }
                    else -> emptyList()
                }
                4 -> when (args[0].lowercase()) {
                    "streak" -> when (args[1].lowercase()) {
                        "frozen", "restore", "revert" -> if (args[2] != "takeall") {
                            Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[3], true) }
                        } else {
                            emptyList()
                        }
                        "set" -> (1..100).map { it.toString() }.filter { it.startsWith(args[3], true) }
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