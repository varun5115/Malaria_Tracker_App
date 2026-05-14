package com.varun.malariatracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
//import java.util.UUID // This lets us generate random unique IDs!
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment

@Composable
fun SymptomsScreen(patientId: String, patientName: String, onFinish: () -> Unit) {
//    var date by remember { mutableStateOf("") }
//    var symptom by remember { mutableStateOf("") }


    var feverToday by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var feverDuration by remember { mutableStateOf("") }
    val associatedSymptoms = remember { mutableStateMapOf("Chills/shivering" to false, "Headache" to false,         "Sweating" to false,         "Body ache" to false,         "Nausea/vomiting" to false,         "Weakness/fatigue" to false, "No other symptoms" to false     ) }

    var illnessDate by remember { mutableStateOf("") }
    var breathingDifficulty by remember { mutableStateOf("") }
    var ableForTesting by remember { mutableStateOf("") }


    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Symptoms Entry", style = MaterialTheme.typography.headlineMedium)

        Text(text = "Patient: $patientName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = "ID: $patientId", color = MaterialTheme.colorScheme.secondary)

//        OutlinedTextField(
//            value = date,
//            onValueChange = { date = it },
//            label = { Text("Date of Symptom Onset (e.g., 2026-04-18)") },
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        OutlinedTextField(
//            value = symptom,
//            onValueChange = { symptom = it },
//            label = { Text("Describe Symptoms (e.g., Fever, Chills)") },
//            modifier = Modifier.fillMaxWidth()
//        )


        Text(text = "1. Do you have fever today?", style = MaterialTheme.typography.bodyLarge)

        Row(verticalAlignment = Alignment.CenterVertically) {

            listOf("Yes", "No", "Not sure").forEach { option ->
                Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = feverToday == option, onClick = { feverToday = option })
                    Text(option) }

                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        OutlinedTextField(value = temperature, onValueChange = { temperature = it },
            label = { Text("2. Measured temperature (if known)") },
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "3. Duration of fever", style = MaterialTheme.typography.bodyLarge)

        Column {
            listOf("Less than 24 hours", "1–3 days", "More than 3 days", "Not required").forEach { option ->

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = feverDuration == option, onClick = { feverDuration = option }
                    )

                    Text(option)
                }
            }
        }

        Text(
            text = "4. Associated Symptoms",
            style = MaterialTheme.typography.bodyLarge
        )

        associatedSymptoms.keys.forEach { symptom ->

            Row(verticalAlignment = Alignment.CenterVertically) {

                Checkbox(
                    checked = associatedSymptoms[symptom] == true,
                    onCheckedChange = {
                        associatedSymptoms[symptom] = it
                    }
                )

                Text(symptom)
            }
        }

        OutlinedTextField(
            value = illnessDate,
            onValueChange = {
                illnessDate = it
            },
            label = {
                Text("5. Time of onset of current illness (date)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "6. Any breathing difficulty, chest pain or confusion?",
            style = MaterialTheme.typography.bodyLarge
        )

        Row(verticalAlignment = Alignment.CenterVertically) {

            listOf(
                "Yes",
                "No"
            ).forEach { option ->

                Row(verticalAlignment = Alignment.CenterVertically) {

                    RadioButton(
                        selected = breathingDifficulty == option,
                        onClick = {
                            breathingDifficulty = option
                        }
                    )

                    Text(option)
                }

                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        Text(
            text = "7. Able to go for testing today if advised?",
            style = MaterialTheme.typography.bodyLarge
        )

        Column {

            listOf(
                "Yes",
                "No",
                "Not required"
            ).forEach { option ->

                Row(verticalAlignment = Alignment.CenterVertically) {

                    RadioButton(
                        selected = ableForTesting == option,
                        onClick = {
                            ableForTesting = option
                        }
                    )

                    Text(option)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val hasSelectedSymptoms = associatedSymptoms.values.any { it }

                if (!hasSelectedSymptoms) { Toast.makeText(context,"Please select at least one symptom", Toast.LENGTH_SHORT).show()
                } else {
                    // Generate a quick random ID for this specific symptom entry
                    //val entryId = UUID.randomUUID().toString().take(8)

                    val entryId = "${patientId}_${System.currentTimeMillis()}"
                    val timestamp = System.currentTimeMillis()

                    //val rowData = "$entryId,$patientId,$date,$symptom"
                    val selectedSymptoms = associatedSymptoms.filter { it.value }.keys.joinToString("; ")

                    val rowData =
                        "${CsvHelper.escapeCsv(entryId)}," +
                                "${CsvHelper.escapeCsv(timestamp.toString())}," +
                                "${CsvHelper.escapeCsv(patientId)}," +
                                "${CsvHelper.escapeCsv(feverToday)}," +
                                "${CsvHelper.escapeCsv(temperature)}," +
                                "${CsvHelper.escapeCsv(feverDuration)}," +
                                "${CsvHelper.escapeCsv(selectedSymptoms)}," +
                                "${CsvHelper.escapeCsv(illnessDate)}," +
                                "${CsvHelper.escapeCsv(breathingDifficulty)}," +
                                "${CsvHelper.escapeCsv(ableForTesting)}"                   // val header = "EntryID,PatientID,Date,Symptom"
                    val header = "EntryID,Timestamp,PatientID,FeverToday,Temperature,FeverDuration,AssociatedSymptoms,IllnessDate,BreathingDifficulty,AbleForTesting"
                    CsvHelper.saveToCsv(context, "Symptoms", header, rowData)
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