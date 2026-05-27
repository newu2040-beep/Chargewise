package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BatteryRepository(private val batteryDao: BatteryDao) {
    val allLogs: Flow<List<BatteryLog>> = batteryDao.getAllLogs()
    
    val stats: Flow<BatteryStats> = batteryDao.getStatsFlow().map { 
        it ?: BatteryStats()
    }

    suspend fun insertLog(log: BatteryLog) {
        batteryDao.insertLog(log)
    }

    suspend fun clearLogs() {
        batteryDao.clearLogs()
    }

    suspend fun getStatsDirect(): BatteryStats {
        return batteryDao.getStatsDirect() ?: BatteryStats()
    }

    suspend fun updateStats(newStats: BatteryStats) {
        batteryDao.insertOrUpdateStats(newStats)
    }

    suspend fun getRecentLogs(limit: Int): List<BatteryLog> {
        return batteryDao.getRecentLogs(limit)
    }
}
