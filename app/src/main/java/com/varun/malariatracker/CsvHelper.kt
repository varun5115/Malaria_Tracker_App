package com.varun.malariatracker // Ensure this matches your package name

import android.os.Environment
import java.io.File
import java.io.FileWriter

object CsvHelper {

    // Helper function to write a row to a specific CSV file in the Downloads folder
    fun saveToCsv(fileName: String, header: String, rowData: String) {
        try {
            // Target the public Downloads directory so you can extract it easily
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "$fileName.csv")

            // Check if we need to write the header row
            val isNewFile = !file.exists()

            val writer = FileWriter(file, true) // true = append to the end of the file
            if (isNewFile) {
                writer.append(header).append("\n")
            }
            writer.append(rowData).append("\n")
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Function to check if a Unique ID already exists in Demographics.csv
    // Function to check if a Unique ID already exists in Demographics.csv
    fun doesIdExist(id: String): Boolean {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "Demographics.csv")

            if (!file.exists()) return false

            // Read lines into a list and use a standard 'for' loop to avoid lambda scope issues
            val lines = file.readLines()
            for (line in lines) {
                val columns = line.split(",")
                if (columns.isNotEmpty() && columns[0] == id) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}