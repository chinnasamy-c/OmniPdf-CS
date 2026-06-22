package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_records ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<HistoryRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: HistoryRecord)

    @Query("DELETE FROM history_records WHERE id = :id")
    suspend fun deleteRecordById(id: Int)

    @Query("DELETE FROM history_records")
    suspend fun deleteAllHistory()
}
