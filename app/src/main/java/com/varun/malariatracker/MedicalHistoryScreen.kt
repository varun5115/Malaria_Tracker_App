package com.varun.malariatracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun MedicalHistoryScreen(patientId: String, patientName: String, onNavigateNext: (String) -> Unit) {
//    var episode by remember { mutableStateOf("") }
//    var lastDiagnosed by remember { mutableStateOf("") }

    var diagnosedMalaria by remember { mutableStateOf("") }
    var malariaTimes by remember { mutableStateOf("") }
    var lastDetection by remember { mutableStateOf("") }
    val treatmentPlaces = remember { mutableStateMapOf("UHC/PHC" to false, "Private clinic" to false, "Govt hospital" to false, "Pharmacy" to false) }
    var otherTreatmentPlace by remember { mutableStateOf("") }
    var diagnosisTest by remember { mutableStateOf("") }
    var sameDayReport by remember { mutableStateOf("") }
    val reportMethods = remember { mutableStateMapOf("RDT (rapid test)" to false, "Microscopy" to false, "Not sure" to false) }
    var medicationPeriod by remember { mutableStateOf("") }
    var medicineCourseCompleted by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Medical History", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Patient: $patientName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = "ID: $patientId", color = MaterialTheme.colorScheme.secondary)

//        OutlinedTextField(
//            value = episode,
//            onValueChange = { episode = it },
//            label = { Text("Is this a new or recurring episode?") },
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        OutlinedTextField(
//            value = lastDiagnosed,
//            onValueChange = { lastDiagnosed = it },
//            label = { Text("Date of last diagnosis (if applicable)") },
//            modifier = Modifier.fillMaxWidth()
//        )


        Text(
            text = "1. Diagnosed with malaria?",
            style = MaterialTheme.typography.bodyLarge
        )

        Row(verticalAlignment = Alignment.CenterVertically) {

            RadioButton(
                selected = diagnosedMalaria == "Yes",
                onClick = { diagnosedMalaria = "Yes" }
            )
            Text("Yes")

            Spacer(modifier = Modifier.width(8.dp))

            RadioButton(
                selected = diagnosedMalaria == "No",
                onClick = { diagnosedMalaria = "No" }
            )
            Text("No")

            Spacer(modifier = Modifier.width(8.dp))

            RadioButton(
                selected = diagnosedMalaria == "Don't know",
                onClick = { diagnosedMalaria = "Don't know" }
            )
            Text("Don't know")
        }

        OutlinedTextField(
            value = malariaTimes,
            onValueChange = { malariaTimes = it },
            label = {
                Text("2. Number of times in last 2 years")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "3. Date of last malaria detection",
            style = MaterialTheme.typography.bodyLarge
        )

        Row(verticalAlignment = Alignment.CenterVertically) {

            RadioButton(
                selected = lastDetection == "<1 year",
                onClick = { lastDetection = "<1 year" }
            )
            Text("<1 year")

            Spacer(modifier = Modifier.width(8.dp))

            RadioButton(
                selected = lastDetection == "1 to 2 year",
                onClick = { lastDetection = "1 to 2 year" }
            )
            Text("1 to 2 year")

            Spacer(modifier = Modifier.width(8.dp))

            RadioButton(
                selected = lastDetection == "Don't know",
                onClick = { lastDetection = "Don't know" }
            )
            Text("Don't know")
        }

        Text(
            text = "4. Place where treatment received",
            style = MaterialTheme.typography.bodyLarge
        )

        treatmentPlaces.keys.forEach { place ->

            Row(verticalAlignment = Alignment.CenterVertically) {

                Checkbox(
                    checked = treatmentPlaces[place] == true,
                    onCheckedChange = {
                        treatmentPlaces[place] = it
                    }
                )

                Text(place)
            }
        }

        OutlinedTextField(value = otherTreatmentPlace, onValueChange = { otherTreatmentPlace = it }, label = { Text("Other treatment place (optional)") }, modifier = Modifier.fillMaxWidth())

        Text(
            text = "5. Diagnosis test performed?",
            style = MaterialTheme.typography.bodyLarge
        )

        Row(verticalAlignment = Alignment.CenterVertically) {

            RadioButton(
                selected = diagnosisTest == "Yes",
                onClick = { diagnosisTest = "Yes" }
            )
            Text("Yes")

            Spacer(modifier = Modifier.width(8.dp))

            RadioButton(
                selected = diagnosisTest == "No",
                onClick = { diagnosisTest = "No" }
            )
            Text("No")
        }

        Text(
            text = "6. Received report same day?",
            style = MaterialTheme.typography.bodyLarge
        )

        Row(verticalAlignment = Alignment.CenterVertically) {

            RadioButton(
                selected = sameDayReport == "Yes",
                onClick = { sameDayReport = "Yes" }
            )
            Text("Yes")

            Spacer(modifier = Modifier.width(8.dp))

            RadioButton(
                selected = sameDayReport == "No",
                onClick = { sameDayReport = "No" }
            )
            Text("No")
        }

        if (sameDayReport == "Yes") {

            Text(
                text = "If yes, method used:",
                style = MaterialTheme.typography.bodyMedium
            )

            reportMethods.keys.forEach { method ->

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Checkbox(
                        checked = reportMethods[method] == true,
                        onCheckedChange = {
                            reportMethods[method] = it
                        }
                    )

                    Text(method)
                }
            }
        }

        Text(
            text = "7. Medication period",
            style = MaterialTheme.typography.bodyLarge
        )

        Column {

            listOf(
                "3 Days",
                "14 Days (P. vivax)",
                "28 Days follow-up",
                "Not informed / Don't know"
            ).forEach { option ->

                Row(verticalAlignment = Alignment.CenterVertically) {

                    RadioButton(
                        selected = medicationPeriod == option,
                        onClick = {
                            medicationPeriod = option
                        }
                    )

                    Text(option)
                }
            }
        }

        Text(
            text = "8. Completed full course of medicines?",
            style = MaterialTheme.typography.bodyLarge
        )

        Row(verticalAlignment = Alignment.CenterVertically) {

            RadioButton(
                selected = medicineCourseCompleted == "Yes",
                onClick = { medicineCourseCompleted = "Yes" }
            )
            Text("Yes")

            Spacer(modifier = Modifier.width(8.dp))

            RadioButton(
                selected = medicineCourseCompleted == "No",
                onClick = { medicineCourseCompleted = "No" }
            )
            Text("No")

            Spacer(modifier = Modifier.width(8.dp))

            RadioButton(
                selected = medicineCourseCompleted == "Not sure",
                onClick = { medicineCourseCompleted = "Not sure" }
            )
            Text("Not sure")
        }


        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (diagnosedMalaria.isBlank()) {
                    Toast.makeText(context, "Please enter the episode type", Toast.LENGTH_SHORT).show()
                } else {
                    // Package data, do not save yet!
                    //val rowData = "$patientId,$episode,$lastDiagnosed"

                    //val selectedPlaces = treatmentPlaces.filter { it.value }.keys.joinToString("; ")

                    val selectedPlaces = buildString { append(treatmentPlaces.filter { it.value }.keys.joinToString("; "))
                        if (otherTreatmentPlace.isNotBlank()) {
                            if (isNotBlank()) { append("; ") }
                            append(otherTreatmentPlace) } }

                    val selectedMethods = reportMethods.filter { it.value }.keys.joinToString("; ")

                    val rowData =
                        "${CsvHelper.escapeCsv(patientId)}," +
                                "${CsvHelper.escapeCsv(diagnosedMalaria)}," +
                                "${CsvHelper.escapeCsv(malariaTimes)}," +
                                "${CsvHelper.escapeCsv(lastDetection)}," +
                                "${CsvHelper.escapeCsv(selectedPlaces)}," +
                                "${CsvHelper.escapeCsv(diagnosisTest)}," +
                                "${CsvHelper.escapeCsv(sameDayReport)}," +
                                "${CsvHelper.escapeCsv(selectedMethods)}," +
                                "${CsvHelper.escapeCsv(medicationPeriod)}," +
                                "${CsvHelper.escapeCsv(medicineCourseCompleted)}"

                    onNavigateNext(rowData)
                }
            }
        ) {
            Text("Continue to Travel History")
        }
    }
}