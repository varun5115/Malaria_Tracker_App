package com.varun.malariatracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Section V – Travel in and outside Goa (planned/future travel).
 * A worker-flow screen; saves one row to TravelPlan.csv keyed by the patient's ID.
 */
@Composable
fun TravelPlanScreen(patientId: String, patientName: String, onNavigateNext: () -> Unit) {
    val context = LocalContext.current

    var planningToVisit by rememberSaveable { mutableStateOf("") }
    var departureDate by rememberSaveable { mutableStateOf("") }
    var returnDate by rememberSaveable { mutableStateOf("") }
    var placesVisited by rememberSaveable { mutableStateOf("") }
    var feverOrMalaria by rememberSaveable { mutableStateOf("") }
    var treatmentGiven by rememberSaveable { mutableStateOf("") }
    var fullCourseCompleted by rememberSaveable { mutableStateOf("") }
    var placeOfTreatment by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Section V – Travel in and outside Goa", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Patient: $patientName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = "ID: $patientId", color = MaterialTheme.colorScheme.secondary)

        RadioGroup(
            title = "Are you planning to visit outside Goa?",
            options = listOf("Yes", "No"),
            selected = planningToVisit,
            onSelect = { planningToVisit = it }
        )

        if (planningToVisit == "Yes") {
            OutlinedTextField(value = departureDate, onValueChange = { departureDate = it }, label = { Text("Date of departure (Out of Goa) — DD/MM/YYYY") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = returnDate, onValueChange = { returnDate = it }, label = { Text("Date of return (Into Goa) — DD/MM/YYYY") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = placesVisited, onValueChange = { placesVisited = it }, label = { Text("Place(s) / State(s) visited") }, modifier = Modifier.fillMaxWidth())

            RadioGroup(
                title = "During this travel, did you have fever or were you diagnosed with malaria?",
                options = listOf("Yes", "No"),
                selected = feverOrMalaria,
                onSelect = { feverOrMalaria = it }
            )

            if (feverOrMalaria == "Yes") {
                RadioGroup(
                    title = "Was treatment given?",
                    options = listOf("Yes", "No"),
                    selected = treatmentGiven,
                    onSelect = { treatmentGiven = it }
                )
                if (treatmentGiven == "Yes") {
                    RadioGroup(
                        title = "Was the full course of medicines completed?",
                        options = listOf("Yes", "No"),
                        selected = fullCourseCompleted,
                        onSelect = { fullCourseCompleted = it }
                    )
                }
                OutlinedTextField(value = placeOfTreatment, onValueChange = { placeOfTreatment = it }, label = { Text("Place of treatment (if any)") }, modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val header = "ID,PlanningToVisit,DepartureDate,ReturnDate,PlacesVisited,FeverOrMalaria,TreatmentGiven,FullCourseCompleted,PlaceOfTreatment"
                val rowData = listOf(
                    patientId, planningToVisit, departureDate, returnDate, placesVisited,
                    feverOrMalaria, treatmentGiven, fullCourseCompleted, placeOfTreatment
                ).joinToString(",") { CsvHelper.escapeCsv(it) }

                CsvHelper.saveToCsv(context, "TravelPlan", header, rowData)
                Toast.makeText(context, "Travel details saved!", Toast.LENGTH_SHORT).show()
                onNavigateNext()
            }
        ) {
            Text("Continue")
        }
    }
}
