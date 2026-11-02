package com.farhan.dayblock.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.farhan.dayblock.data.DayblockDatabase
import com.farhan.dayblock.data.DiaryEntry
import com.farhan.dayblock.data.DiaryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DiaryRepository(
        DayblockDatabase.create(application).diaryDao()
    )

    val entries: StateFlow<List<DiaryEntry>> = repository.entries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    /** Creates or updates the entry for [date] (one entry per day). */
    fun saveEntry(
        date: String,
        title: String,
        body: String,
        mood: String,
        tags: String
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = repository.getByDate(date)
            repository.upsert(
                DiaryEntry(
                    id = existing?.id ?: 0,
                    date = date,
                    title = title.trim(),
                    body = body.trim(),
                    mood = mood,
                    tags = tags.trim(),
                    starred = existing?.starred ?: false,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now
                )
            )
        }
    }

    fun deleteEntry(entry: DiaryEntry) {
        viewModelScope.launch { repository.delete(entry) }
    }

    fun toggleStar(entry: DiaryEntry) {
        viewModelScope.launch { repository.setStarred(entry, !entry.starred) }
    }
}
