package com.herzchen.dotmanbankingexpansion.manager

import com.herzchen.dotmanbankingexpansion.model.StreakData
import com.herzchen.dotmanbankingexpansion.repository.StreakRepository

import org.bukkit.entity.Player

import java.time.Instant

class StreakTokenManager(private val repo: StreakRepository) {
    fun addFreezes(player: Player, amount: Int) {
        updateTokens(player) { it.freezeTokens += amount }
    }

    fun addRestores(player: Player, amount: Int) {
        updateTokens(player) { it.restoreTokens += amount }
    }

    fun addReverts(player: Player, amount: Int) {
        updateTokens(player) { it.revertTokens += amount }
    }

    fun takeFreezes(player: Player, amount: Int): Boolean {
        return updateTokens(player) {
            if (it.freezeTokens >= amount) {
                it.freezeTokens -= amount
                true
            } else {
                false
            }
        }
    }

    fun takeRestores(player: Player, amount: Int): Boolean {
        return updateTokens(player) {
            if (it.restoreTokens >= amount) {
                it.restoreTokens -= amount
                true
            } else {
                false
            }
        }
    }

    fun takeReverts(player: Player, amount: Int): Boolean {
        return updateTokens(player) {
            if (it.revertTokens >= amount) {
                it.revertTokens -= amount
                true
            } else {
                false
            }
        }
    }

    fun takeAllFreezes(player: Player): Boolean {
        return updateTokens(player) {
            if (it.freezeTokens > 0) {
                it.freezeTokens = 0
                true
            } else {
                false
            }
        }
    }

    fun takeAllRestores(player: Player): Boolean {
        return updateTokens(player) {
            if (it.restoreTokens > 0) {
                it.restoreTokens = 0
                true
            } else {
                false
            }
        }
    }

    fun takeAllReverts(player: Player): Boolean {
        return updateTokens(player) {
            if (it.revertTokens > 0) {
                it.revertTokens = 0
                true
            } else {
                false
            }
        }
    }

    private fun <T> updateTokens(player: Player, action: (StreakData) -> T): T {
        val data = repo.find(player.uniqueId) ?: StreakData(
            player.uniqueId,
            Instant.now(),
            Instant.now()
        ).apply {
            currentStreak = 0
            repo.save(this)
        }

        val result = action(data)
        repo.save(data)
        return result
    }
}