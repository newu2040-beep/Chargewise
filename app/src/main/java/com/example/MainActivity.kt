package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.DashboardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    
    private val viewModel: BatteryViewModel by viewModels()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.processBatteryUpdate(context, intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Instantly register with null receiver to pull the sticky battery state
        // This ensures the dashboard instantly displays real percentages on first launch instead of 0% or empty charts
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = registerReceiver(null, filter)
        if (stickyIntent != null) {
            viewModel.processBatteryUpdate(this, stickyIntent)
        }

        setContent {
            MyApplicationTheme(
                darkTheme = viewModel.isDarkTheme,
                appTheme = viewModel.currentPastelTheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
