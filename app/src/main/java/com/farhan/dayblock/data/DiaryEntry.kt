package com.farhan.dayblock.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Mood choices shown in the editor and used for filters / stats. */
object Moods {
    const val GREAT = "Great"
    const val GOOD = "Good"
    const val OKAY = "Okay"
    const val LOW = "Low"
    const val ROUGH = "Rough"

    val all: List<String> = listOf(GREAT, GOOD, OKAY, LOW, ROUGH)
}

/**
 * One diary entry per calendar day.
 *
 * [date] is an ISO local date string (yyyy-MM-dd), which keeps ordering,
 * calendar lookups and streak maths simple and timezone-safe.
 */
@Entity(
    tableName = "diary_entries",
    indices = [Index(value = ["date"], unique = true)]
)
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val title: String,
    val body: String,
    val mood: String = Moods.OKAY,
    val tags: String = "",
    val starred: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** Tags as a clean list, parsed from the comma-separated storage form. */
    val tagList: List<String>
        get() = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val wordCount: Int
        get() = body.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
}
