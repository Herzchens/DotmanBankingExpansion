package com.herzchen.dotmanbankingexpansion.command

import com.herzchen.dotmanbankingexpansion.DotmanBankingExpansion
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class ReloadCommand(private val plugin: DotmanBankingExpansion) : CommandExecutor {
    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        plugin.reload()
        sender.sendMessage("§aDotmanBankingExpansion reloaded successfully!")
        return true
    }
}