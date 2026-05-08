package com.example.addnevnik.data.repository

import com.example.addnevnik.data.local.AppDao
import com.example.addnevnik.data.local.NoteEntity
import com.example.addnevnik.model.NoteItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotesRepository private constructor(private val appDao: AppDao) {
    val notes: Flow<List<NoteItem>> = appDao.getAllNotes().map { entities ->
        entities.map { entity ->
            val parts = entity.text.split("\n", limit = 2)
            NoteItem(
                id = entity.id,
                title = parts.getOrNull(0) ?: "Заметка",
                content = parts.getOrNull(1) ?: entity.text
            )
        }
    }

    suspend fun addNote(title: String, content: String) {
        appDao.insertNote(
            NoteEntity(
                text = "$title\n$content",
                timestamp_ms = System.currentTimeMillis()
            )
        )
    }

    companion object {
        private var instance: NotesRepository? = null
        fun getInstance(appDao: AppDao): NotesRepository {
            return instance ?: synchronized(this) {
                instance ?: NotesRepository(appDao).also { instance = it }
            }
        }
    }
}
