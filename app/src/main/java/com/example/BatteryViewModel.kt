package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.theme.PastelTheme
import com.example.util.NotificationHelper
import com.example.util.PdfReporter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class BatteryViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("battery_app_prefs", Context.MODE_PRIVATE)

    // Configurable System Settings
    var isDarkTheme by mutableStateOf(false)
        private set

    var currentPastelTheme by mutableStateOf(PastelTheme.MINT)
        private set

    var tempLimitThreshold by mutableStateOf(40f)
        private set

    // Real-time battery status parameters
    var batteryLevel by mutableStateOf(0)
    var batteryTemperature by mutableStateOf(0f)
    var batteryVoltage by mutableStateOf(0)
    var batteryStatus by mutableStateOf("Unknown")
    var isCharging by mutableStateOf(false)
    var powerSource by mutableStateOf("Unknown")
    var batteryHealth by mutableStateOf("Unknown")
    var batteryTechnology by mutableStateOf("Unknown")
    var currentNowmA by mutableStateOf(0)
    var chargeCountermAh by mutableStateOf(0)

    // Room Database instances & Flows
    private val database = AppDatabase.getDatabase(application)
    val repository = BatteryRepository(database.batteryDao())

    val logsHistory: StateFlow<List<BatteryLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val statsState: StateFlow<BatteryStats> = repository.stats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BatteryStats()
        )

    // Last values state to monitor transitions
    private var lastStatus: Int = -1
    private var previousLevel: Int = -1
    private var lastLoggedLevel = -1
    private var lastLoggedStatus = ""
    private var lastLoggedTime = 0L

    init {
        // Load persistent system configurations
        val themeStr = prefs.getString("pastel_theme", PastelTheme.MINT.name) ?: PastelTheme.MINT.name
        currentPastelTheme = try { PastelTheme.valueOf(themeStr) } catch (e: Exception) { PastelTheme.MINT }
        
        isDarkTheme = prefs.getBoolean("is_dark_theme", false)
        tempLimitThreshold = prefs.getFloat("temp_threshold", 40f)
    }

    fun toggleDarkMode(dark: Boolean) {
        isDarkTheme = dark
        prefs.edit().putBoolean("is_dark_theme", dark).apply()
    }

    fun changePastelTheme(theme: PastelTheme) {
        currentPastelTheme = theme
        prefs.edit().putString("pastel_theme", theme.name).apply()
    }

    fun updateTempThreshold(threshold: Float) {
        tempLimitThreshold = threshold
        prefs.edit().putFloat("temp_threshold", threshold).apply()
    }

    fun queryCurrentAndCapacity(context: Context) {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return
        
        // current is usually in microamperes, can be positive (charging) or negative (discharging)
        val currentMicro = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        currentNowmA = if (currentMicro != Int.MIN_VALUE) currentMicro / 1000 else 0

        // charge counter is in uAh (microampere-hours)
        val chargeMicro = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        chargeCountermAh = if (chargeMicro != Int.MIN_VALUE) chargeMicro / 1000 else 0
    }

    fun processBatteryUpdate(context: Context, intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (scale > 0) (level * 100 / scale) else level
        batteryLevel = pct

        val tempDeci = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        batteryTemperature = tempDeci / 10f

        batteryVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        
        val statusInt = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val pluggedInt = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        
        isCharging = statusInt == BatteryManager.BATTERY_STATUS_CHARGING || 
                     statusInt == BatteryManager.BATTERY_STATUS_FULL
                     
        batteryStatus = when (statusInt) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }

        powerSource = when (pluggedInt) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Dock"
            else -> "Battery Power"
        }

        val healthInt = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        batteryHealth = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            6 -> "Unspecified Failure"
            else -> "Unknown"
        }

        batteryTechnology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

        // Update instantaneous properties
        queryCurrentAndCapacity(context)

        // Check and trigger notification temperature warnings
        if (batteryTemperature >= tempLimitThreshold) {
            NotificationHelper.sendTemperatureWarning(context, batteryTemperature, tempLimitThreshold)
        }

        // Room database telemetry & updates
        viewModelScope.launch {
            val currentStats = repository.getStatsDirect()
            var nextStats = currentStats

            // 1. Tracker for Charging session counts
            if (lastStatus != -1 && lastStatus != BatteryManager.BATTERY_STATUS_CHARGING && 
                statusInt == BatteryManager.BATTERY_STATUS_CHARGING) {
                nextStats = nextStats.copy(chargingSessionsCount = nextStats.chargingSessionsCount + 1)
            }

            // 2. Tracker for Cumulative Charge Cycles Estimation
            if (isCharging && previousLevel != -1 && pct > previousLevel) {
                val gain = (pct - previousLevel).toFloat()
                var totalPctGained = nextStats.cumulativePercentageGained + gain
                var addedCycles = nextStats.cumulativeChargeCycles
                
                if (totalPctGained >= 100f) {
                    val fullCycles = (totalPctGained / 100).toInt()
                    addedCycles += fullCycles
                    totalPctGained %= 100f
                }
                
                nextStats = nextStats.copy(
                    cumulativePercentageGained = totalPctGained,
                    cumulativeChargeCycles = addedCycles
                )
            }

            if (nextStats != currentStats) {
                repository.updateStats(nextStats)
            }

            // 3. Telemetry Log throttling logic (only write on Level or Status updates, or every 5 mins)
            val now = System.currentTimeMillis()
            val timeElapsed = now - lastLoggedTime
            val isStatusChanged = lastLoggedStatus != batteryStatus
            val isLevelChanged = lastLoggedLevel != batteryLevel
            
            if (lastLoggedTime == 0L || isLevelChanged || isStatusChanged || timeElapsed >= 300000L) {
                val newLog = BatteryLog(
                    level = batteryLevel,
                    temperature = batteryTemperature,
                    voltage = batteryVoltage,
                    isCharging = isCharging,
                    status = batteryStatus
                )
                repository.insertLog(newLog)
                
                lastLoggedLevel = batteryLevel
                lastLoggedStatus = batteryStatus
                lastLoggedTime = now
            }

            lastStatus = statusInt
            previousLevel = pct
        }
    }

    fun exportPdfReport(context: Context, onResult: (File?) -> Unit) {
        viewModelScope.launch {
            val currentLogs = logsHistory.value
            val currentStats = statsState.value
            
            val deviceInfo = mapOf(
                "brand" to android.os.Build.BRAND.uppercase(),
                "model" to android.os.Build.MODEL,
                "api_level" to android.os.Build.VERSION.SDK_INT.toString(),
                "board" to android.os.Build.BOARD,
                "manufacturer" to android.os.Build.MANUFACTURER,
                "ram_tot" to getRamInfo(context),
                "tech" to batteryTechnology,
                "health" to batteryHealth,
                "voltage" to batteryVoltage.toString(),
                "temp" to String.format("%.1f", batteryTemperature)
            )

            val file = PdfReporter.generateBatteryReport(context, deviceInfo, currentStats, currentLogs)
            onResult(file)
        }
    }

    private fun getRamInfo(context: Context): String {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            val gb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
            String.format("%.1f GB", gb)
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun clearLogHistory() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }
}
