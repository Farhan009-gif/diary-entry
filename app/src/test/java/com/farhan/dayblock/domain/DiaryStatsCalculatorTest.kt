package com.farhan.dayblock.domain

import com.farhan.dayblock.data.DiaryEntry
import com.farhan.dayblock.data.Moods
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DiaryStatsCalculatorTest {

    private fun entry(date: LocalDate, body: String = "one two three", mood: String = Moods.GOOD) =
        DiaryEntry(date = date.toString(), title = "Day", body = body, mood = mood)

    @Test
    fun streakCountsConsecutiveDaysEndingToday() {
        val today = LocalDate.of(2026, 11, 2)
        val entries = listOf(
            entry(today),
            entry(today.minusDays(1)),
            entry(today.minusDays(2)),
            entry(today.minusDays(5))
        )
        assertEquals(3, DiaryStatsCalculator.calculate(entries, today).currentStreak)
    }

    @Test
    fun streakStaysAliveFromYesterday() {
        val today = LocalDate.of(2026, 11, 2)
        val entries = listOf(entry(today.minusDays(1)), entry(today.minusDays(2)))
        assertEquals(2, DiaryStatsCalculator.calculate(entries, today).currentStreak)
    }

    @Test
    fun totalsAndWordsAreCounted() {
        val today = LocalDate.of(2026, 11, 2)
        val entries = listOf(
            entry(today, body = "hello world"),
            entry(today.minusDays(10), body = "one")
        )
        val stats = DiaryStatsCalculator.calculate(entries, today)
        assertEquals(2, stats.totalEntries)
        assertEquals(1, stats.entriesThisWeek)
        assertEquals(3, stats.totalWords)
    }

    @Test
    fun emptyDiaryHasZeroStats() {
        val stats = DiaryStatsCalculator.calculate(emptyList(), LocalDate.of(2026, 11, 2))
        assertEquals(0, stats.totalEntries)
        assertEquals(0, stats.currentStreak)
    }
}
