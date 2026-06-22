package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_records")
data class HistoryRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val toolType: String, // "MERGE" or "SPLIT"
    val fileName: String,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val fileSize: Long,
    val pagesCount: Int
)
