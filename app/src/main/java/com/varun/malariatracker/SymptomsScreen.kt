package com.varun.malariatracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.UUID // This lets us generate random unique IDs!

@Composable
fun SymptomsScreen(patientId: String, patientName: String, onFinish: () -> Unit) {
    var date by remember { mutableStateOf("") }
    var symptom by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Symptoms Entry", style = MaterialTheme.typography.headlineMedium)

        Text(text = "Patient: $patientName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = "ID: $patientId", color = MaterialTheme.colorScheme.secondary)

        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Date of Symptom Onset (e.g., 2026-04-18)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = symptom,
            onValueChange = { symptom = it },
            label = { Text("Describe Symptoms (e.g., Fever, Chills)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (symptom.isBlank()) {
                    Toast.makeText(context, "Please enter at least one symptom", Toast.LENGTH_SHORT).show()
                } else {
                    // Generate a quick random ID for this specific symptom entry
                    val entryId = UUID.randomUUID().toString().take(8)

                    val rowData = "$entryId,$patientId,$date,$symptom"
                    val header = "EntryID,PatientID,Date,Symptom"

                    CsvHelper.saveToCsv("Symptoms", header, rowData)
                    Toast.makeText(context, "Patient Profile Complete!", Toast.LENGTH_LONG).show()

                    // This triggers the app to go back to the start!
                    onFinish()
                }
            }
        ) {
            Text("Save & Finish Profile")
        }
    }
}