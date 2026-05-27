package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.BatteryLog
import com.example.data.BatteryStats
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReporter {

    fun generateBatteryReport(
        context: Context,
        deviceInfo: Map<String, String>,
        stats: BatteryStats,
        logs: List<BatteryLog>
    ): File? {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        // A4 page specifications: width 595, height 842 (72 pixels per inch)
        val pageWidth = 595
        val pageHeight = 842

        // PAGE 1: Header + Analytics + System Info + Log Table Start
        var pageNum = 1
        var myPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var page = pdfDocument.startPage(myPageInfo)
        var canvas = page.canvas

        // Header Rect (Sage/Mint Pastel Theme - Primary 0xFF6B8E80)
        val headerPaint = Paint().apply {
            color = Color.parseColor("#6B8E80")
            style = Paint.Style.FILL
        }
        canvas.drawRect(25f, 25f, pageWidth - 25f, 100f, headerPaint)

        // Header Title
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("ChargeWise Battery Analytics Report", 45f, 65f, titlePaint)

        val dateParser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = "Generated on: ${dateParser.format(Date())}"
        val subTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            isAntiAlias = true
        }
        canvas.drawText(dateStr, 45f, 85f, subTitlePaint)

        // Section: Device Info
        var currentY = 130f
        val headingPaint = Paint().apply {
            color = Color.parseColor("#2E5043") // Deep Sage Tone
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }

        canvas.drawText("📱 System & Device Profile", 35f, currentY, headingPaint)
        currentY += 20f

        // Draw Device Info box
        val infoBgPaint = Paint().apply {
            color = Color.parseColor("#F4F6F5")
            style = Paint.Style.FILL
        }
        canvas.drawRect(35f, currentY, pageWidth - 35f, currentY + 110f, infoBgPaint)

        var labelX = 50f
        var valX = 130f
        var label2X = 300f
        var val2X = 390f
        
        val labelPaint = Paint().apply {
            color = Color.parseColor("#5F7065")
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = Color.parseColor("#212523")
            textSize = 10f
            isAntiAlias = true
        }

        // Row 1
        canvas.drawText("Brand:", labelX, currentY + 20f, labelPaint)
        canvas.drawText(deviceInfo["brand"] ?: "Unknown", valX, currentY + 20f, valuePaint)
        canvas.drawText("Model:", label2X, currentY + 20f, labelPaint)
        canvas.drawText(deviceInfo["model"] ?: "Unknown", val2X, currentY + 20f, valuePaint)

        // Row 2
        canvas.drawText("Android OS:", labelX, currentY + 40f, labelPaint)
        canvas.drawText("API ${deviceInfo["api_level"] ?: "Unknown"}", valX, currentY + 40f, valuePaint)
        canvas.drawText("Total RAM:", label2X, currentY + 40f, labelPaint)
        canvas.drawText(deviceInfo["ram_tot"] ?: "Unknown", val2X, currentY + 40f, valuePaint)

        // Row 3
        canvas.drawText("Technology:", labelX, currentY + 60f, labelPaint)
        canvas.drawText(deviceInfo["tech"] ?: "Li-ion", valX, currentY + 60f, valuePaint)
        canvas.drawText("Health:", label2X, currentY + 60f, labelPaint)
        canvas.drawText(deviceInfo["health"] ?: "Good", val2X, currentY + 60f, valuePaint)

        // Row 4
        canvas.drawText("Voltage:", labelX, currentY + 80f, labelPaint)
        canvas.drawText("${deviceInfo["voltage"] ?: "0"} mV", valX, currentY + 80f, valuePaint)
        canvas.drawText("Temperature:", label2X, currentY + 80f, labelPaint)
        canvas.drawText("${deviceInfo["temp"] ?: "0"} °C", val2X, currentY + 80f, valuePaint)

        currentY += 135f

        // Section: Longevity Tracker
        canvas.drawText("🔋 Long-Term Battery Longevity", 35f, currentY, headingPaint)
        currentY += 20f

        // Stats Box
        canvas.drawRect(35f, currentY, pageWidth - 35f, currentY + 65f, infoBgPaint)

        canvas.drawText("Charging Cycles Saved:", 50f, currentY + 25f, labelPaint)
        canvas.drawText("${stats.cumulativeChargeCycles} complete cycles", 200f, currentY + 25f, valuePaint)

        canvas.drawText("Charger Insertions Checked:", 50f, currentY + 45f, labelPaint)
        canvas.drawText("${stats.chargingSessionsCount} charging sessions", 200f, currentY + 45f, valuePaint)

        currentY += 90f

        // Section: Historical Logs Table
        canvas.drawText("📋 Battery Monitoring Logs", 35f, currentY, headingPaint)
        currentY += 20f

        // Draw Table Headings
        val tableHeaderPaint = Paint().apply {
            color = Color.parseColor("#E0E6E3")
            style = Paint.Style.FILL
        }
        val tableBorderPaint = Paint().apply {
            color = Color.parseColor("#D0D8D4")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        canvas.drawRect(35f, currentY, pageWidth - 35f, currentY + 25f, tableHeaderPaint)

        val colY = currentY + 16f
        val tableHeaders = arrayOf("Date & Time", "Level", "Temp (°C)", "Voltage", "Status")
        val colXs = floatArrayOf(45f, 185f, 255f, 345f, 445f)

        for (i in tableHeaders.indices) {
            canvas.drawText(tableHeaders[i], colXs[i], colY, labelPaint)
        }
        currentY += 25f

        // Limit the rendering to fit the PDF size neatly
        // We can draw up to 16 rows on page 1, and make additional page if needed
        val maxLogCount = logs.size
        val itemsPerPage = 18

        var printedCount = 0
        val sdf = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())

        val altRowPaint = Paint().apply {
            color = Color.parseColor("#FBFDFC")
            style = Paint.Style.FILL
        }

        // Row Printer
        fun printRow(log: BatteryLog, yPos: Float) {
            if (printedCount % 2 == 1) {
                canvas.drawRect(35f, yPos - 14f, pageWidth - 35f, yPos + 6f, altRowPaint)
            }
            canvas.drawLine(35f, yPos + 6f, pageWidth - 35f, yPos + 6f, tableBorderPaint)

            canvas.drawText(sdf.format(Date(log.timestamp)), colXs[0], yPos, valuePaint)
            canvas.drawText("${log.level}%", colXs[1], yPos, valuePaint)
            canvas.drawText("${String.format("%.1f", log.temperature)} °C", colXs[2], yPos, valuePaint)
            canvas.drawText("${log.voltage} mV", colXs[3], yPos, valuePaint)
            canvas.drawText(log.status, colXs[4], yPos, valuePaint)
        }

        // Print first page rows
        val firstPageItemLimit = minOf(logs.size, 16)
        for (i in 0 until firstPageItemLimit) {
            val log = logs[i]
            val rowY = currentY + 16f
            printRow(log, rowY)
            currentY += 22f
            printedCount++
        }

        pdfDocument.finishPage(page)

        // PAGE 2: More logs if possible
        if (logs.size > firstPageItemLimit) {
            pageNum++
            myPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            page = pdfDocument.startPage(myPageInfo)
            canvas = page.canvas

            // Page 2 header
            val headerPaintMini = Paint().apply {
                color = Color.parseColor("#6B8E80")
                style = Paint.Style.FILL
            }
            canvas.drawRect(25f, 25f, pageWidth - 25f, 65f, headerPaintMini)
            canvas.drawText("ChargeWise Monitor History Table", 45f, 48f, titlePaint.apply { textSize = 14f })

            currentY = 90f
            // Mini table header
            canvas.drawRect(35f, currentY, pageWidth - 35f, currentY + 25f, tableHeaderPaint)
            canvas.drawText("Page 2 of 2 (Extended History Logs)", 35f, 82f, labelPaint)

            for (i in tableHeaders.indices) {
                canvas.drawText(tableHeaders[i], colXs[i], currentY + 16f, labelPaint)
            }
            currentY += 25f

            val secondPageCount = minOf(logs.size - firstPageItemLimit, itemsPerPage)
            for (i in 0 until secondPageCount) {
                val log = logs[firstPageItemLimit + i]
                val rowY = currentY + 16f
                printRow(log, rowY)
                currentY += 22f
                printedCount++
            }
            pdfDocument.finishPage(page)
        }

        // Save PDF File to cache dir
        val fileName = "BatteryHealth_Report_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)

        return try {
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }
}
