package com.farhan.dayblock.data

import kotlinx.coroutines.flow.Flow

/** Thin repository over the DAO so the UI never touches Room directly. */
class DiaryRepository(private val dao: DiaryDao) {

    val entries: Flow<List<DiaryEntry>> = dao.observeAll()

    fun entryForDate(date: String): Flow<DiaryEntry?> = dao.observeByDate(date)

    suspend fun getByDate(date: String): DiaryEntry? = dao.getByDate(date)

    suspend fun upsert(entry: DiaryEntry) {
        dao.upsert(entry)
    }

    suspend fun delete(entry: DiaryEntry) {
        dao.delete(entry)
    }

    suspend fun setStarred(entry: DiaryEntry, starred: Boolean) {
        dao.setStarred(entry.id, starred, System.currentTimeMillis())
    }
}
