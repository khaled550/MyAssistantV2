package com.example.myassistantv2

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ExcelExporter(private val context: Context) {

    suspend fun exportTripsToExcel(tripsFlow: Flow<List<Trip>>): String? {
        val trips = tripsFlow.first() // Collect data from Flow
        if (trips.isEmpty()) return null  // No data to export

        val fileName = "TripsData.xlsx"
        val filePath = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Trips")
            // Create Header Row
            val headers = arrayOf("Day",
                "Vehicle type",
                "PL",
                "Vender",
                "From",
                "To",
                "If Call-out",
                "Requester",
                "Remarks")
            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, title -> headerRow.createCell(index).setCellValue(title) }

            // Fill Data Rows
            trips.forEachIndexed { index, trip ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(trip.date)
                row.createCell(1).setCellValue(trip.vehicleType)
                row.createCell(2).setCellValue(trip.productLine)
                row.createCell(3).setCellValue(trip.driver)
                row.createCell(4).setCellValue(trip.startPoint)
                row.createCell(5).setCellValue(trip.endPoint)
                row.createCell(6).setCellValue(trip.cost)
                row.createCell(7).setCellValue(trip.requester)
                row.createCell(7).setCellValue(trip.notes)
            }

            // Write to File
            FileOutputStream(filePath).use { workbook.write(it) }
            workbook.close()

            filePath.absolutePath  // Return the saved file path

        } catch (e: IOException) {
            Log.e("ExcelExport", "Error writing Excel file", e)
            null
        }
    }
}
