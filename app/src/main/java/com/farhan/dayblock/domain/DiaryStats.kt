package com.farhan.dayblock.domain

import com.farhan.dayblock.data.DiaryEntry
import java.time.LocalDate

data class DiaryStats(
    val totalEntries: Int = 0,
    val currentStreak: Int = 0,
    val entriesThisWeek: Int = 0,
    val totalWords: Int = 0,
    val moodCounts: Map<String, Int> = emptyMap()
)

object DiaryStatsCalculator {

    fun calculate(entries: List<DiaryEntry>, today: LocalDate): DiaryStats {
        if (entries.isEmpty()) return DiaryStats()

        val dates: Set<LocalDate> = entries.mapNotNull { entry ->
            runCatching { LocalDate.parse(entry.date) }.getOrNull()
        }.toSet()

        val weekStart = today.minusDays(6)
        return DiaryStats(
            totalEntries = entries.size,
            currentStreak = currentStreak(dates, today),
            entriesThisWeek = entries.count { entry ->
                val parsed = runCatching { LocalDate.parse(entry.date) }.getOrNull()
                parsed != null && !parsed.isBefore(weekStart) && !parsed.isAfter(today)
            },
            totalWords = entries.sumOf { it.wordCount },
            moodCounts = entries.groupingBy { it.mood }.eachCount()
        )
    }

    /**
     * Consecutive written days ending today. If today has no entry yet but
     * yesterday does, the streak is still alive, so it is counted from
     * yesterday instead of reporting zero all day.
     */
    fun currentStreak(dates: Set<LocalDate>, today: LocalDate): Int {
        var cursor = when {
            dates.contains(today) -> today
            dates.contains(today.minusDays(1)) -> today.minusDays(1)
            else -> return 0
        }
        var streak = 0
        while (dates.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}
