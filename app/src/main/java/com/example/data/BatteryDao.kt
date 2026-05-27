package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryDao {
    @Query("SELECT * FROM battery_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<BatteryLog>>

    @Query("SELECT * FROM battery_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int): List<BatteryLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BatteryLog)

    @Query("DELETE FROM battery_logs")
    suspend fun clearLogs()

    @Query("SELECT * FROM battery_stats WHERE id = 1")
    fun getStatsFlow(): Flow<BatteryStats?>

    @Query("SELECT * FROM battery_stats WHERE id = 1")
    suspend fun getStatsDirect(): BatteryStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStats(stats: BatteryStats)
}
