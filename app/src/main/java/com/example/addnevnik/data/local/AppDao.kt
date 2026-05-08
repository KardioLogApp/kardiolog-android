package com.example.addnevnik.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Blood Pressure
    @Query("SELECT * FROM blood_pressure ORDER BY timestamp_ms DESC")
    fun getAllBloodPressure(): Flow<List<BloodPressureEntity>>

    @Query("SELECT * FROM blood_pressure ORDER BY timestamp_ms DESC")
    suspend fun getAllBloodPressureOnce(): List<BloodPressureEntity>

    @Query("SELECT * FROM blood_pressure ORDER BY timestamp_ms DESC LIMIT 1")
    fun getLatestBloodPressure(): Flow<BloodPressureEntity?>

    @Query("SELECT * FROM blood_pressure WHERE timestamp_ms >= :since ORDER BY timestamp_ms DESC")
    fun getBloodPressureAfter(since: Long): Flow<List<BloodPressureEntity>>

    @Query("SELECT COUNT(*) FROM blood_pressure")
    fun getBloodPressureCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBloodPressure(entry: BloodPressureEntity)

    @Update
    suspend fun updateBloodPressure(entry: BloodPressureEntity)

    @Delete
    suspend fun deleteBloodPressure(entry: BloodPressureEntity)

    // Notes
    @Query("SELECT * FROM notes ORDER BY timestamp_ms DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)
}
