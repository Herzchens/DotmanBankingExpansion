package com.herzchen.dotmanbankingexpansion.manager

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

import kotlin.collections.HashMap

class ConfirmationManager(private val plugin: JavaPlugin) {
    private val pendingConfirmations = HashMap<CommandSender, Pair<String, () -> Unit>>()
    private val tasks = HashMap<CommandSender, BukkitTask>()

    fun setConfirmation(sender: CommandSender, code: String, action: () -> Unit) {
        pendingConfirmations.remove(sender)?.let { tasks.remove(sender)?.cancel() }

        pendingConfirmations[sender] = code to action

        tasks[sender] = Bukkit.getScheduler().runTaskLater(
            plugin,
            Runnable {
                pendingConfirmations.remove(sender)
                sender.sendMessage("Xác nhận đã hết hạn!")
            },
            200L
        )
    }

    fun checkConfirmation(sender: CommandSender, input: String): Boolean {
        val (code, action) = pendingConfirmations[sender] ?: return false

        if (input == code) {
            action()
            tasks.remove(sender)?.cancel()
            pendingConfirmations.remove(sender)
            return true
        }
        return false
    }
}