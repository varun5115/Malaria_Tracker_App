package com.varun.malariatracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun MedicalHistoryScreen(patientId: String, patientName: String, onNavigateNext: (String) -> Unit) {
    var episode by remember { mutableStateOf("") }
    var lastDiagnosed by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Medical History", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Patient: $patientName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = "ID: $patientId", color = MaterialTheme.colorScheme.secondary)

        OutlinedTextField(
            value = episode,
            onValueChange = { episode = it },
            label = { Text("Is this a new or recurring episode?") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = lastDiagnosed,
            onValueChange = { lastDiagnosed = it },
            label = { Text("Date of last diagnosis (if applicable)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (episode.isBlank()) {
                    Toast.makeText(context, "Please enter the episode type", Toast.LENGTH_SHORT).show()
                } else {
                    // Package data, do not save yet!
                    val rowData = "$patientId,$episode,$lastDiagnosed"
                    onNavigateNext(rowData)
                }
            }
        ) {
            Text("Continue to Travel History")
        }
    }
}