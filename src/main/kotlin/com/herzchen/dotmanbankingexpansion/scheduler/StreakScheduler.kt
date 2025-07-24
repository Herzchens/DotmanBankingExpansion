package com.herzchen.dotmanbankingexpansion.scheduler

import com.herzchen.dotmanbankingexpansion.service.StreakService
import com.herzchen.dotmanbankingexpansion.ui.BossBarManager

import org.bukkit.plugin.java.JavaPlugin

class StreakScheduler(
    private val plugin: JavaPlugin,
    private val service: StreakService,
    private val ui: BossBarManager
) {
    fun start() {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            service.checkExpirations()
            plugin.server.onlinePlayers.forEach { player ->
                val data = service.repo.find(player.uniqueId)
                if (data != null) {
                    val prog = service.getProgress(player.uniqueId)
                    ui.show(
                        player.uniqueId,
                        prog,
                        data.state,
                        service.cycleSeconds
                    )
                } else {
                    ui.remove(player.uniqueId)
                }
            }
        }, 20L, 20L)
    }
}