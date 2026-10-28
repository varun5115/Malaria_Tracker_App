package com.varun.malariatracker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier

/** Section IV – migration and travel history. Finishing this screen saves the profile. */
@Composable
fun TravelHistoryScreen(patientId: String, patientName: String, onNavigateNext: (String) -> Unit) {
    var stayDuration by rememberSaveable { mutableStateOf("") }
    var travelledOutsideGoa by rememberSaveable { mutableStateOf("") }
    var visitedStates by rememberSaveable { mutableStateOf("") }
    var homeStayDuration by rememberSaveable { mutableStateOf("") }
    var feverBeforeComing by rememberSaveable { mutableStateOf("") }

    FormPager(
        pageCount = 1,
        page = 0,
        onPageChange = {},
        finishLabel = "Save Profile & Continue",
        onFinish = {
            val states = if (travelledOutsideGoa == "Yes") visitedStates else ""
            val rowData = listOf(
                patientId, stayDuration, travelledOutsideGoa, states, homeStayDuration, feverBeforeComing
            ).joinToString(",") { CsvHelper.escapeCsv(it) }
            onNavigateNext(rowData)
        },
        header = {
            Text(text = "Travel History", style = MaterialTheme.typography.headlineMedium)
            Text(text = "Patient: $patientName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(text = "ID: $patientId", color = MaterialTheme.colorScheme.secondary)
        }
    ) {
        RadioGroup(
            title = "1. How long have you been in Goa this time?",
            options = listOf("Within 1 month", "1 - 6 months", "More than 6 months"),
            selected = stayDuration,
            onSelect = { stayDuration = it }
        )
        RadioGroup(
            title = "2. Travelled outside Goa in the last 3 months?",
            options = listOf("Yes", "No"),
            selected = travelledOutsideGoa,
            onSelect = { travelledOutsideGoa = it }
        )
        if (travelledOutsideGoa == "Yes") {
            OutlinedTextField(value = visitedStates, onValueChange = { visitedStates = it }, label = { Text("3. Which state(s) did you visit?") }, modifier = Modifier.fillMaxWidth())
        }
        RadioGroup(
            title = "4. Duration of last stay in home state",
            options = listOf("7 days", "14 days", "More than 14 days"),
            selected = homeStayDuration,
            onSelect = { homeStayDuration = it }
        )
        RadioGroup(
            title = "5. Fever or malaria in last 3 months before coming to Goa?",
            options = listOf("Yes", "No", "Don't know"),
            selected = feverBeforeComing,
            onSelect = { feverBeforeComing = it }
        )
    }
}
