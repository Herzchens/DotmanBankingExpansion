package com.herzchen.dotmanbankingexpansion.ui

import com.herzchen.dotmanbankingexpansion.model.StreakState
import com.herzchen.dotmanbankingexpansion.service.StreakService

import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar

import java.time.Duration
import java.util.*

class BossBarManager(private val streakService: StreakService) {
    private val bars = mutableMapOf<UUID, BossBar>()
    private val inactiveBars = mutableMapOf<UUID, BossBar>()

    fun show(uuid: UUID, progress: Double, state: StreakState, cycleSeconds: Long) {
        val player = Bukkit.getPlayer(uuid) ?: return

        if (state == StreakState.INACTIVE) {
            bars[uuid]?.removePlayer(player)

            val inactiveBar = inactiveBars.computeIfAbsent(uuid) {
                Bukkit.createBossBar("§aHãy donate để chúng mình có tiền duy trì server nhé ^^", BarColor.PINK, BarStyle.SOLID)
            }
            inactiveBar.color = BarColor.PINK
            inactiveBar.progress = 1.0
            inactiveBar.setTitle("§aHãy donate để chúng mình có tiền duy trì server nhé ^^")
            if (!inactiveBar.players.contains(player)) {
                inactiveBar.addPlayer(player)
            }
            return
        } else {
            inactiveBars[uuid]?.removePlayer(player)
        }

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
                bar.setTitle("§aThời gian còn lại: ${remainingTime.toHours()}h ${remainingTime.toMinutesPart()}m")
            }
            StreakState.FROZEN -> {
                val data = streakService.repo.find(uuid)
                val freezeTokens = data?.freezeTokens ?: 0

                val color = if (freezeTokens == 1) {
                    when {
                        remainingProgress <= 0.1 -> BarColor.WHITE
                        remainingProgress <= 0.5 -> BarColor.RED
                        else -> BarColor.BLUE
                    }
                } else {
                    BarColor.BLUE
                }
                bar.color = color
                val remainingTime = Duration.ofSeconds((remainingProgress * cycleSeconds).toLong())
                bar.setTitle("§bĐóng băng: ${remainingTime.toHours()}h ${remainingTime.toMinutesPart()}m")
            }
            StreakState.EXPIRED -> {
                bar.color = BarColor.WHITE
                val remainingTime = Duration.ofSeconds((remainingProgress * cycleSeconds).toLong())
                bar.setTitle("§cHết hạn: ${remainingTime.toHours()}h ${remainingTime.toMinutesPart()}m để khôi phục")
            }
            else -> {}
        }

        if (!bar.players.contains(player)) {
            bar.addPlayer(player)
        }
    }

    fun remove(uuid: UUID) {
        bars.remove(uuid)?.removeAll()
        inactiveBars.remove(uuid)?.removeAll()
    }
}