package com.varun.malariatracker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment

@Composable
fun TravelHistoryScreen(patientId: String, patientName: String, onNavigateNext: (String) -> Unit) {
//    var state by remember { mutableStateOf("") }
//    var travelled by remember { mutableStateOf("") }

    var stayDuration by remember { mutableStateOf("") }
    var travelledOutsideGoa by remember { mutableStateOf("") }
    var visitedStates by remember { mutableStateOf("") }
    var homeStayDuration by remember { mutableStateOf("") }
    var feverBeforeComing by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Travel History", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Patient: $patientName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = "ID: $patientId", color = MaterialTheme.colorScheme.secondary)

//        OutlinedTextField(
//            value = state,
//            onValueChange = { state = it },
//            label = { Text("State or Region visited recently") },
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        OutlinedTextField(
//            value = travelled,
//            onValueChange = { travelled = it },
//            label = { Text("Dates travelled (e.g., Oct 1 - Oct 10)") },
//            modifier = Modifier.fillMaxWidth()
//        )

        Text(
            text = "1. How long have you been in Goa this time?",
            style = MaterialTheme.typography.bodyLarge
        )

        Column {

            listOf(
                "Within 1 month",
                "1 - 6 months",
                "More than 6 months"
            ).forEach { option ->

                Row(verticalAlignment = Alignment.CenterVertically) {

                    RadioButton(
                        selected = stayDuration == option,
                        onClick = {
                            stayDuration = option
                        }
                    )

                    Text(option)
                }
            }
        }

        Text(
            text = "2. Travelled outside Goa in the last 3 months?",
            style = MaterialTheme.typography.bodyLarge
        )

        Row(verticalAlignment = Alignment.CenterVertically) {

            RadioButton(
                selected = travelledOutsideGoa == "Yes",
                onClick = {
                    travelledOutsideGoa = "Yes"
                }
            )
            Text("Yes")

            Spacer(modifier = Modifier.width(12.dp))

            RadioButton(
                selected = travelledOutsideGoa == "No",
                onClick = {
                    travelledOutsideGoa = "No"
                }
            )
            Text("No")
        }

        OutlinedTextField(
            value = visitedStates,
            onValueChange = {
                visitedStates = it
            },
            label = {
                Text("3. Which state(s) did you visit?")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "4. Duration of last stay in home state",
            style = MaterialTheme.typography.bodyLarge
        )

        Column {

            listOf(
                "7 days",
                "14 days",
                "More than 14 days"
            ).forEach { option ->

                Row(verticalAlignment = Alignment.CenterVertically) {

                    RadioButton(
                        selected = homeStayDuration == option,
                        onClick = {
                            homeStayDuration = option
                        }
                    )

                    Text(option)
                }
            }
        }

        Text(
            text = "5. Fever or malaria in last 3 months before coming to Goa?",
            style = MaterialTheme.typography.bodyLarge
        )

        Column {

            listOf(
                "Yes",
                "No",
                "Don't know"
            ).forEach { option ->

                Row(verticalAlignment = Alignment.CenterVertically) {

                    RadioButton(
                        selected = feverBeforeComing == option,
                        onClick = {
                            feverBeforeComing = option
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
                // Package data, do not save yet!
               // val rowData = "$patientId,$state,$travelled"
                val rowData =
                    "$patientId," + "$stayDuration," + "$travelledOutsideGoa," + "$visitedStates," + "$homeStayDuration," + "$feverBeforeComing"
                onNavigateNext(rowData)
            }
        ) {
            Text("Save Profile & Continue to Symptoms")
        }
    }
}