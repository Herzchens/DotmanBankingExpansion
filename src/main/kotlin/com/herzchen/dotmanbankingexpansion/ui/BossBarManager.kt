package com.herzchen.dotmanbankingexpansion.ui

import com.herzchen.dotmanbankingexpansion.model.StreakState

import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar

import java.time.Duration
import java.util.UUID

class BossBarManager {
    private val bars = mutableMapOf<UUID, BossBar>()

    fun show(uuid: UUID, progress: Double, state: StreakState, cycleSeconds: Long) {
        val player = Bukkit.getPlayer(uuid) ?: return
        val bar = bars.computeIfAbsent(uuid) {
            Bukkit.createBossBar("Streak", BarColor.BLUE, BarStyle.SEGMENTED_10)
        }

        val remainingProgress = 1.0 - progress.coerceIn(0.0, 1.0)
        bar.progress = remainingProgress

        when (state) {
            StreakState.ACTIVE -> {
                val color = when {
                    remainingProgress >= 0.5 -> BarColor.GREEN
                    remainingProgress >= 0.2 -> BarColor.YELLOW
                    else -> BarColor.RED
                }
                bar.color = color

                val remainingTime = Duration.ofSeconds((remainingProgress * cycleSeconds).toLong())
                bar.setTitle("Time left: ${remainingTime.toHours()}h ${remainingTime.toMinutesPart()}m")
            }
            StreakState.FROZEN -> {
                bar.color = BarColor.BLUE
                bar.setTitle("Streak is Frozen")
            }
            StreakState.EXPIRED -> {
                bar.color = BarColor.WHITE
                val remainingTime = Duration.ofSeconds((remainingProgress * cycleSeconds).toLong())
                bar.setTitle("Expired: ${remainingTime.toHours()}h ${remainingTime.toMinutesPart()}m left to restore")
            }
        }

        if (!bar.players.contains(player)) bar.addPlayer(player)
    }

    fun remove(uuid: UUID) {
        bars.remove(uuid)?.removeAll()
    }
}