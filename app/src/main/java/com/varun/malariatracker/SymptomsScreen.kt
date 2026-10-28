package com.varun.malariatracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/** Section III – present clinical details. Saves one row to Symptoms.csv per visit. */
@Composable
fun SymptomsScreen(patientId: String, patientName: String, onFinish: () -> Unit) {
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf(0) }

    var feverToday by rememberSaveable { mutableStateOf("") }
    var temperature by rememberSaveable { mutableStateOf("") }
    var feverDuration by rememberSaveable { mutableStateOf("") }
    val symptomOptions = listOf("Chills/shivering", "Headache", "Sweating", "Body ache", "Nausea/vomiting", "Weakness/fatigue", "No other symptoms")
    var associatedSymptoms by rememberSaveable { mutableStateOf(setOf<String>()) }
    var illnessDate by rememberSaveable { mutableStateOf("") }
    var breathingDifficulty by rememberSaveable { mutableStateOf("") }
    var ableForTesting by rememberSaveable { mutableStateOf("") }

    FormPager(
        pageCount = 2,
        page = page,
        onPageChange = { page = it },
        finishLabel = "Save & Finish",
        validatePage = { p -> if (p == 0 && associatedSymptoms.isEmpty()) "Please select at least one symptom" else null },
        onFinish = {
            val entryId = "${patientId}_${System.currentTimeMillis()}"
            val timestamp = System.currentTimeMillis()
            val temp = if (feverToday == "Yes") temperature else ""

            val header = "EntryID,Timestamp,PatientID,FeverToday,Temperature,FeverDuration,AssociatedSymptoms,IllnessDate,BreathingDifficulty,AbleForTesting"
            val rowData = listOf(
                entryId, timestamp.toString(), patientId, feverToday, temp, feverDuration,
                joinInOrder(symptomOptions, associatedSymptoms), illnessDate, breathingDifficulty, ableForTesting
            ).joinToString(",") { CsvHelper.escapeCsv(it) }

            CsvHelper.saveToCsv(context, "Symptoms", header, rowData)
            Toast.makeText(context, "Patient Profile Complete!", Toast.LENGTH_LONG).show()
            onFinish()
        },
        header = {
            Text(text = "Symptoms Entry", style = MaterialTheme.typography.headlineMedium)
            Text(text = "Patient: $patientName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(text = "ID: $patientId", color = MaterialTheme.colorScheme.secondary)
        }
    ) { p ->
        when (p) {
            0 -> {
                RadioGroup(
                    title = "1. Do you have fever today?",
                    options = listOf("Yes", "No", "Not sure"),
                    selected = feverToday,
                    onSelect = { feverToday = it }
                )
                if (feverToday == "Yes") {
                    OutlinedTextField(value = temperature, onValueChange = { temperature = it }, label = { Text("2. Measured temperature (if known)") }, modifier = Modifier.fillMaxWidth())
                }
                RadioGroup(
                    title = "3. Duration of fever",
                    options = listOf("Less than 24 hours", "1–3 days", "More than 3 days", "Not required"),
                    selected = feverDuration,
                    onSelect = { feverDuration = it }
                )
                CheckboxGroup(
                    title = "4. Associated Symptoms",
                    options = symptomOptions,
                    selected = associatedSymptoms,
                    onToggle = { opt, on -> associatedSymptoms = associatedSymptoms.toggle(opt, on) }
                )
            }
            else -> {
                OutlinedTextField(value = illnessDate, onValueChange = { illnessDate = it }, label = { Text("5. Time of onset of current illness (date)") }, modifier = Modifier.fillMaxWidth())
                RadioGroup(
                    title = "6. Any breathing difficulty, chest pain or confusion?",
                    options = listOf("Yes", "No"),
                    selected = breathingDifficulty,
                    onSelect = { breathingDifficulty = it }
                )
                RadioGroup(
                    title = "7. Able to go for testing today if advised?",
                    options = listOf("Yes", "No", "Not required"),
                    selected = ableForTesting,
                    onSelect = { ableForTesting = it }
                )
            }
        }
    }
}
