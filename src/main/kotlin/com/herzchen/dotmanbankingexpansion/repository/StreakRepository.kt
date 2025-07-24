package com.herzchen.dotmanbankingexpansion.repository

import com.herzchen.dotmanbankingexpansion.model.StreakData

import java.util.UUID

interface StreakRepository {
    fun find(uuid: UUID): StreakData?
    fun findAll(): Collection<StreakData>
    fun save(data: StreakData)
}
