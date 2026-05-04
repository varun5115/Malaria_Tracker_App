package com.varun.malariatracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    onNavigateNewUser: (String) -> Unit,
    onNavigateReturningUser: (String) -> Unit
) {
    var userId by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Malaria Tracker Login", style = MaterialTheme.typography.headlineLarge)
        Text(text = "Please enter patient ID to begin.", color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = userId,
            onValueChange = { userId = it },
            label = { Text("Unique Patient ID / Phone Number") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (userId.isBlank()) {
                    Toast.makeText(context, "Please enter an ID", Toast.LENGTH_SHORT).show()
                } else {
                    // Check our CSV file to see if they exist!
                    val isRegistered = CsvHelper.doesIdExist(userId)

                    if (isRegistered) {
                        Toast.makeText(context, "Patient Found! Skipping to Symptoms.", Toast.LENGTH_LONG).show()
                        onNavigateReturningUser(userId)
                    } else {
                        Toast.makeText(context, "New Patient. Please register.", Toast.LENGTH_SHORT).show()
                        onNavigateNewUser(userId)
                    }
                }
            }
        ) {
            Text("Login / Start")
        }
    }
}