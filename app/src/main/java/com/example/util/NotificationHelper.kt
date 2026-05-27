package com.example.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "battery_temp_alerts"
    private const val CHANNEL_NAME = "Battery Temp Warnings"
    private const val NOTIFICATION_ID = 4125

    // Cooldown of 60 seconds (60,000 milliseconds)
    private var lastAlertTime = 0L
    private const val ALERT_COOLDOWN_MS = 60000L

    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Alerts you when the battery exceeds safe temperature limits."
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun sendTemperatureWarning(context: Context, currentTemp: Float, threshold: Float) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAlertTime < ALERT_COOLDOWN_MS) {
            return // Cooldown active, don't spam
        }

        // Initialize channel just in case
        initChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning) // system fallback, highly reliable
            .setContentTitle("⚠️ Battery Temperature Warning!")
            .setContentText("The battery is running hot at ${String.format("%.1f", currentTemp)}°C (Limit: ${String.format("%.1f", threshold)}°C). Please close heavy apps!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, notification)
            }
            lastAlertTime = currentTime
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
