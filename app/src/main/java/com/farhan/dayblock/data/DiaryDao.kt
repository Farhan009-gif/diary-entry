package com.farhan.dayblock.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {

    @Query("SELECT * FROM diary_entries ORDER BY date DESC")
    fun observeAll(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<DiaryEntry?>

    @Query("SELECT * FROM diary_entries WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DiaryEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DiaryEntry): Long

    @Delete
    suspend fun delete(entry: DiaryEntry)

    @Query("UPDATE diary_entries SET starred = :starred, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setStarred(id: Long, starred: Boolean, updatedAt: Long)
}
