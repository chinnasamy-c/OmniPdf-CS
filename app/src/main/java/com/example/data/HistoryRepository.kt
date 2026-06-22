package com.example.data

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<HistoryRecord>> = historyDao.getAllHistory()

    suspend fun insertRecord(record: HistoryRecord) {
        historyDao.insertRecord(record)
    }

    suspend fun deleteRecordById(id: Int) {
        historyDao.deleteRecordById(id)
    }

    suspend fun deleteAllHistory() {
        historyDao.deleteAllHistory()
    }
}
