package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BatteryCircle(
    percentage: Int,
    isCharging: Boolean,
    status: String,
    modifier: Modifier = Modifier,
    percentageFontSize: androidx.compose.ui.unit.TextUnit = 42.sp,
    boltFontSize: androidx.compose.ui.unit.TextUnit = 32.sp,
    strokeWidthDp: androidx.compose.ui.unit.Dp = 14.dp,
    paddingDp: androidx.compose.ui.unit.Dp = 16.dp,
    statusFontSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage / 100f,
        animationSpec = tween(durationMillis = 1000),
        label = "BatteryArc"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

    // Dynamic color coding based on percentage levels
    val fillProgressColor = when {
        isCharging -> primaryColor
        percentage <= 20 -> Color(0xFFD32F2F) // Pastel red
        percentage <= 50 -> Color(0xFFF57C00) // Pastel orange
        else -> primaryColor // Pastel theme primary accent
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Draw real-time percentage arc ring
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingDp)
        ) {
            val strokeWidthPx = strokeWidthDp.toPx()
            
            // Draw track
            drawCircle(
                color = trackColor,
                style = Stroke(width = strokeWidthPx)
            )

            // Draw progress
            drawArc(
                color = fillProgressColor,
                startAngle = -90f,
                sweepAngle = animatedPercentage * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        // Center status readings
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isCharging && boltFontSize.value > 0f) {
                    Text(
                        text = "⚡",
                        fontSize = boltFontSize,
                        color = fillProgressColor,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                }
                Text(
                    text = "$percentage%",
                    fontSize = percentageFontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (status.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = status,
                    fontSize = statusFontSize,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
