package com.varun.malariatracker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TravelHistoryScreen(patientId: String, patientName: String, onNavigateNext: (String) -> Unit) {
    var state by remember { mutableStateOf("") }
    var travelled by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Travel History", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Patient: $patientName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = "ID: $patientId", color = MaterialTheme.colorScheme.secondary)

        OutlinedTextField(
            value = state,
            onValueChange = { state = it },
            label = { Text("State or Region visited recently") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = travelled,
            onValueChange = { travelled = it },
            label = { Text("Dates travelled (e.g., Oct 1 - Oct 10)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                // Package data, do not save yet!
                val rowData = "$patientId,$state,$travelled"
                onNavigateNext(rowData)
            }
        ) {
            Text("Save Profile & Continue to Symptoms")
        }
    }
}