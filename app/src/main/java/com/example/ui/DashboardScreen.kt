package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BatteryViewModel
import com.example.data.BatteryLog
import com.example.ui.components.BatteryCircle
import com.example.ui.theme.PastelTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: BatteryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    
    val logs by viewModel.logsHistory.collectAsStateWithLifecycle()
    val stats by viewModel.statsState.collectAsStateWithLifecycle()

    var hasNotificationPermission by remember { mutableStateOf(true) }

    fun checkPermission(ctx: Context) {
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "🔔 Overheat tracking alerts activated!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Post-notifications must be enabled to receive warning alerts!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        checkPermission(context)
        if (!hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val onRequestPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Toast.makeText(context, "System notifications are active and ready!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🔋 ChargeWise Monitor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Tips") },
                    label = { Text("Longevity", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_tips")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.List, contentDescription = "History") },
                    label = { Text("Log Reports", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_logs")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Setup", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> TelemetryTab(
                    viewModel = viewModel,
                    stats = stats,
                    hasNotificationPermission = hasNotificationPermission,
                    onRequestPermission = onRequestPermission
                )
                1 -> EducationTab()
                2 -> LogsTab(viewModel = viewModel, logs = logs)
                3 -> SettingsTab(
                    viewModel = viewModel,
                    hasNotificationPermission = hasNotificationPermission,
                    onRequestPermission = onRequestPermission
                )
            }
        }
    }
}

@Composable
fun TelemetryTab(
    viewModel: BatteryViewModel,
    stats: com.example.data.BatteryStats,
    hasNotificationPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val isDark = viewModel.isDarkTheme

    // Bento Color Schemes derived from user preferences / standard bento theme
    val heroBg = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.25f) else Color(0xFFD3E3FD)
    val heroText = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E3A8A)
    val heroValueText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1B365D)

    val healthBg = if (isDark) Color(0xFF14532D).copy(alpha = 0.25f) else Color(0xFFE7F3E8)
    val healthText = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
    val healthValueText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF14532D)

    val tempBg = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.25f) else Color(0xFFFDE7E7)
    val tempText = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
    val tempValueText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF7F1D1D)

    val cyclesBg = if (isDark) Color(0xFF78350F).copy(alpha = 0.25f) else Color(0xFFFFF4D9)
    val cyclesText = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
    val cyclesValueText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF78350F)

    val voltageBg = if (isDark) Color(0xFF3B0764).copy(alpha = 0.25f) else Color(0xFFF3E7FD)
    val voltageText = if (isDark) Color(0xFFD8B4FE) else Color(0xFF7E22CE)
    val voltageValueText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF3B0764)

    val currentBg = if (isDark) Color(0xFF0C4A6E).copy(alpha = 0.25f) else Color(0xFFE0F2FE)
    val currentText = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1)
    val currentValueText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0C4A6E)

    val sourceBg = if (isDark) Color(0xFF115E59).copy(alpha = 0.25f) else Color(0xFFCCFBF1)
    val sourceText = if (isDark) Color(0xFF5EEAD4) else Color(0xFF0D9488)
    val sourceValueText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF115E59)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // 1. Bento Hero Charging Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(heroBg)
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "CURRENT CHARGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = heroText.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = "${viewModel.batteryLevel}",
                            fontSize = 58.sp,
                            fontWeight = FontWeight.Black,
                            color = heroValueText,
                            lineHeight = 58.sp
                        )
                        Text(
                            text = "%",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = heroText,
                            modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val colorDot = if (viewModel.isCharging) Color(0xFF22C55E) else Color(0xFFF97316)
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(colorDot, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${viewModel.batteryStatus} • ${viewModel.powerSource}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = heroText.copy(alpha = 0.9f)
                        )
                    }
                }
                
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BatteryCircle(
                        percentage = viewModel.batteryLevel,
                        isCharging = viewModel.isCharging,
                        status = "",
                        modifier = Modifier.fillMaxSize(),
                        percentageFontSize = 18.sp,
                        boltFontSize = 14.sp,
                        strokeWidthDp = 8.dp,
                        paddingDp = 4.dp
                    )
                }
            }
        }

        // Notification permission request banner (if not granted)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRequestPermission() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Enable Notifications",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable System Notifications",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Keep track of heat limits, charging anomalies, and critical battery thresholds with live warning logs.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                lineHeight = 15.sp
                            )
                        }
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Animated Thermal warning alert (Bento style Red card)
        item {
            AnimatedVisibility(
                visible = viewModel.batteryTemperature >= viewModel.tempLimitThreshold,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE),
                        contentColor = Color(0xFFC62828)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Thermal warning",
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Thermal Limit Exceeded!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF7F1D1D)
                            )
                            Text(
                                "Battery is hot at ${String.format("%.1f", viewModel.batteryTemperature)}°C. Unplug the charger or shut down high-resource apps immediately to secure longevity.",
                                fontSize = 11.sp,
                                color = Color(0xFF991B1B),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. Bento Grid Column Pairs
        // Row 1: Health & Temperature
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoCard(
                    category = "HEALTH",
                    title = viewModel.batteryHealth,
                    subtitle = "Capacity check",
                    icon = Icons.Default.CheckCircle,
                    iconColor = healthText,
                    containerColor = healthBg,
                    contentColor = healthValueText,
                    modifier = Modifier.weight(1f)
                )

                val tempF = String.format("%.1f", viewModel.batteryTemperature * 1.8f + 32)
                BentoCard(
                    category = "TEMP",
                    title = "${String.format("%.1f", viewModel.batteryTemperature)}°C",
                    subtitle = "$tempF°F • Range Check",
                    icon = Icons.Default.Warning,
                    iconColor = tempText,
                    containerColor = tempBg,
                    contentColor = tempValueText,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 2: Cycles & Voltage
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoCard(
                    category = "CYCLES",
                    title = "${stats.cumulativeChargeCycles}",
                    subtitle = "${stats.chargingSessionsCount} Connections",
                    icon = Icons.Default.Refresh,
                    iconColor = cyclesText,
                    containerColor = cyclesBg,
                    contentColor = cyclesValueText,
                    modifier = Modifier.weight(1f)
                )

                BentoCard(
                    category = "VOLTAGE",
                    title = "${viewModel.batteryVoltage} mV",
                    subtitle = "Stable Input",
                    icon = Icons.Default.Info,
                    iconColor = voltageText,
                    containerColor = voltageBg,
                    contentColor = voltageValueText,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 3: Current & Power Source
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val flowText = if (viewModel.currentNowmA != 0) "${viewModel.currentNowmA} mA" else "N/A"
                val flowStatus = if (viewModel.currentNowmA > 0) "Inflow speed" else "Drainage speed"
                BentoCard(
                    category = "CURRENT",
                    title = flowText,
                    subtitle = flowStatus,
                    icon = Icons.Default.Info,
                    iconColor = currentText,
                    containerColor = currentBg,
                    contentColor = currentValueText,
                    modifier = Modifier.weight(1f)
                )

                val shortSource = when (viewModel.powerSource) {
                    "AC Wall Charger" -> "AC Wall"
                    "USB Port" -> "USB Port"
                    "Wireless Dock" -> "Wireless"
                    else -> "Battery"
                }

                BentoCard(
                    category = "SOURCE",
                    title = shortSource,
                    subtitle = viewModel.batteryTechnology,
                    icon = Icons.Default.Home,
                    iconColor = sourceText,
                    containerColor = sourceBg,
                    contentColor = sourceValueText,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Actionable Insights Bento Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Alert logo",
                            tint = Color(0xFF93C5FD),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Charging Habit Advisory",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Frequent 0% to 100% deep cycles decrease longevity. Aim for 20-80% boundaries to maximize lifespan.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // 5. Device Identity Profile Header
        item {
            Text(
                text = "📱 Device Identity Profile",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        // 6. Device Identity Profile Bento Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SpecsRow(label = "Brand / Vendor", value = Build.BRAND.uppercase())
                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    SpecsRow(label = "Model Name", value = Build.MODEL)
                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    SpecsRow(label = "Android OS", value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    SpecsRow(label = "Hardware Board", value = Build.BOARD)
                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    SpecsRow(label = "Processor Arch", value = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown")
                }
            }
        }
    }
}

@Composable
fun BentoCard(
    category: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    badgeBgColor: Color = Color.White.copy(alpha = 0.5f)
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(badgeBgColor, shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = category,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = iconColor
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = contentColor,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun TelemetryRow(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SpecsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun EducationTab() {
    var subTabState by remember { mutableStateOf(0) } // 0 = Habits, 1 = Tips

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Section selection button toggles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (subTabState == 0) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                    .clickable { subTabState = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "⚠️ Wrong Habits",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (subTabState == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (subTabState == 1) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                    .clickable { subTabState = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🌱 Actionable Tips",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (subTabState == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (subTabState == 0) {
                val badHabits = listOf(
                    Triple(
                        "Overnight Overcharging",
                        "Leaving the phone connected all night holds it constantly at 100% capacity. This prompts elevated localized heat and 'mini-cycles' of tension.",
                        "Stress index: HIGH"
                    ),
                    Triple(
                        "Heavy load gaming while charging",
                        "Playing intensive games or editing videos while plugged in generates immense composite heat from the system chip and raw charging flow.",
                        "Stress index: EXTREMELY HIGH"
                    ),
                    Triple(
                        "Letting Battery Hit 0% Completely",
                        "Deep discharge represents chemical starvation. Leaving the cell dead damages lithium components and speeds up permanent health drop.",
                        "Stress index: HIGH"
                    ),
                    Triple(
                        "Poor Quality Uncertified Chargers",
                        "Substandard cords can suffer from electrical ripples and lack reliable thermal cut-offs, damaging internal micro-coatings over time.",
                        "Stress index: SEVERE"
                    ),
                    Triple(
                        "Leaving Device in Heat Zones",
                        "Placing the phone on a car dashboard or under direct noon sunlight triggers rapid active chemical degradation, permanently cutting capacity.",
                        "Stress index: EXTREMELY HIGH"
                    )
                )

                items(badHabits) { habit ->
                    EducationCard(
                        title = habit.first,
                        description = habit.second,
                        tag = habit.third,
                        themeColor = Color(0xFFC62828).copy(alpha = 0.08f),
                        borderColor = Color(0xFFE57373),
                        icon = Icons.Default.Warning
                    )
                }
            } else {
                val goodTips = listOf(
                    Triple(
                        "The 80-20 Zone",
                        "Unplug once your battery is at 80% and plug it back when you hover down to 20%. This holds lithium inside the most stress-free state.",
                        "Longevity Gained: +2X Capacity life"
                    ),
                    Triple(
                        "Strip Off Bulk Cases on Chargers",
                        "Tight, insulated bumper cases prevent battery heat dissipation. Uncasing your device during charges allows cooling air circulation.",
                        "Longevity Gained: High"
                    ),
                    Triple(
                        "Charge in Cool Spots",
                        "Protect the device from ambient heat. Place it on metal or clean surfaces rather than pillows or couches during power feeds.",
                        "Longevity Gained: Critical"
                    ),
                    Triple(
                        "Prefer Slow/Standard Charges",
                        "Super-fast charging feeds extreme current that raises temperature instantly. Use standard wall plugs when you have overnight downtime.",
                        "Longevity Gained: Moderate"
                    ),
                    Triple(
                        "Turn on Adaptive Charging",
                        "Enable 'Adaptive' schemes in system options. It smartly halts charging at 80%, finishing up right before you start your day.",
                        "Longevity Gained: High"
                    )
                )

                items(goodTips) { tip ->
                    EducationCard(
                        title = tip.first,
                        description = tip.second,
                        tag = tip.third,
                        themeColor = Color(0xFF2E7D32).copy(alpha = 0.08f),
                        borderColor = Color(0xFF81C784),
                        icon = Icons.Default.CheckCircle
                    )
                }
            }
        }
    }
}

@Composable
fun EducationCard(
    title: String,
    description: String,
    tag: String,
    themeColor: Color,
    borderColor: Color,
    icon: ImageVector
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = themeColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .background(borderColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = borderColor
                    )
                }
            }
        }
    }
}

@Composable
fun LogsTab(
    viewModel: BatteryViewModel,
    logs: List<BatteryLog>
) {
    val context = LocalContext.current
    var isGeneratingPdf by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Historical Logs",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "${logs.size} recorded log actions",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Clear button with simpler icon fallback
                if (logs.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            viewModel.clearLogHistory()
                            Toast.makeText(context, "Log history cleared", Toast.LENGTH_SHORT).show()
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear logs")
                    }
                }

                // PDF Compile action button using standard Share icon symbol
                Button(
                    onClick = {
                        if (logs.isEmpty()) {
                            Toast.makeText(context, "Please collect history logs before exporting!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isGeneratingPdf = true
                        viewModel.exportPdfReport(context) { file ->
                            isGeneratingPdf = false
                            if (file != null) {
                                sharePdfFile(context, file)
                            } else {
                                Toast.makeText(context, "Error compiling report!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("export_pdf_button")
                ) {
                    if (isGeneratingPdf) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "PDF compiling share action",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (logs.isEmpty()) {
            EmptyLogsView()
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (log.isCharging) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (log.isCharging) MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${log.level}%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = log.status,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sdf.format(Date(log.timestamp)),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${String.format("%.1f", log.temperature)} °C",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (log.temperature >= 40f) Color(0xFFC62828) else MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "${log.voltage} mV",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyLogsView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = "Empty list view placeholder",
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No Logs Recorded Yet",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Keep ChargeWise running in background. It logs battery level and temperature shifts automatically so you can export a long-term PDF trend report.",
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun SettingsTab(
    viewModel: BatteryViewModel,
    hasNotificationPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // notification permissions banner
        item {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🔔 System Notifications Alert",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "ChargeWise needs permission to immediately notify you if battery temperatures exceed safe thresholds.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRequestPermission,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("grant_notification_button")
                        ) {
                            Text("Enable Warnings Alerts", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Palette layouts Customizations
        item {
            Text(
                "🎨 Color Scheming",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Dark Mode Toggle Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Dark Mode Canvas",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Switch between light and dark modes",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = viewModel.isDarkTheme,
                            onCheckedChange = { viewModel.toggleDarkMode(it) },
                            modifier = Modifier.testTag("dark_mode_toggle")
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    // Pastel Theme selection dots
                    Text(
                        "Pastel Theme Choices",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        "Select a visual palette inspired by modern pastel hues",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PastelThemeButton(
                            color = Color(0xFF759281), // Mint Sage Green core tone
                            title = "Mint S.",
                            selected = viewModel.currentPastelTheme == PastelTheme.MINT,
                            onClick = { viewModel.changePastelTheme(PastelTheme.MINT) }
                        )

                        PastelThemeButton(
                            color = Color(0xFFB77284), // Rose Blush Pink tone
                            title = "Rose B.",
                            selected = viewModel.currentPastelTheme == PastelTheme.ROSE,
                            onClick = { viewModel.changePastelTheme(PastelTheme.ROSE) }
                        )

                        PastelThemeButton(
                            color = Color(0xFF7970A2), // Lavender Mist Purple core tone
                            title = "Lavender",
                            selected = viewModel.currentPastelTheme == PastelTheme.LAVENDER,
                            onClick = { viewModel.changePastelTheme(PastelTheme.LAVENDER) }
                        )

                        PastelThemeButton(
                            color = Color(0xFFB8714C), // Peach Sorbet Orange/Salmon tone
                            title = "Peach",
                            selected = viewModel.currentPastelTheme == PastelTheme.PEACH,
                            onClick = { viewModel.changePastelTheme(PastelTheme.PEACH) }
                        )
                    }
                }
            }
        }

        // Configuration warning slider
        item {
            Text(
                "🚨 Alarm Configuration",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Thermal Warning Trigger",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Raises alert notification if battery temperature is greater than this limit",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                        Text(
                            "${viewModel.tempLimitThreshold.toInt()} °C",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }

                    Slider(
                        value = viewModel.tempLimitThreshold,
                        onValueChange = { viewModel.updateTempThreshold(it) },
                        valueRange = 35f..45f,
                        steps = 9,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .testTag("temp_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("35 °C", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("40 °C (Recommended)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("45 °C", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun PastelThemeButton(
    color: Color,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color, shape = CircleShape)
                .border(
                    width = if (selected) 4.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected Theme Checkmark",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getTemperatureColor(temp: Float, threshold: Float): Color {
    return when {
        temp >= threshold -> Color(0xFFD32F2F) // Overheating warn red
        temp >= threshold - 3f -> Color(0xFFF57C00) // Warming orange
        else -> Color(0xFF2E7D32) // Healthy Green
    }
}

private fun sharePdfFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Battery Health Analytics - ChargeWise")
            putExtra(Intent.EXTRA_TEXT, "Here is my long-term battery monitoring log report generated with ChargeWise.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Battery Health Report"))
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failure sharing PDF report!", Toast.LENGTH_SHORT).show()
    }
}
