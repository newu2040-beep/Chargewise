package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_logs")
data class BatteryLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val level: Int,
    val temperature: Float, // in Celsius
    val voltage: Int, // in mV
    val isCharging: Boolean,
    val status: String
)

@Entity(tableName = "battery_stats")
data class BatteryStats(
    @PrimaryKey val id: Int = 1,
    val chargingSessionsCount: Int = 0,
    val cumulativePercentageGained: Float = 0f,
    val cumulativeChargeCycles: Int = 0
)
