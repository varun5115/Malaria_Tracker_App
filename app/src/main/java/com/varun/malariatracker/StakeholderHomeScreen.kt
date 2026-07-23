package com.varun.malariatracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * New launch screen. Replaces the standalone login as the entry point: it shows one
 * button per stakeholder. "Worker" leads into the existing intake/login flow; the
 * other buttons open that stakeholder's questionnaire form. Login (worker vs clinician
 * vs lab) is intentionally skipped for now, per sir's brief.
 */
@Composable
fun StakeholderHomeScreen(
    onWorker: () -> Unit,
    onClinician: () -> Unit,
    onPathology: () -> Unit,
    onHealthWorker: () -> Unit,
    onResearchLab: () -> Unit,
    onContractor: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Malaria Tracker",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Select your role to begin",
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onWorker, modifier = Modifier.fillMaxWidth()) { Text("Worker") }
        Button(onClick = onClinician, modifier = Modifier.fillMaxWidth()) { Text("Clinician") }
        Button(onClick = onPathology, modifier = Modifier.fillMaxWidth()) { Text("Path Lab") }
        Button(onClick = onHealthWorker, modifier = Modifier.fillMaxWidth()) { Text("Health Worker") }
        Button(onClick = onResearchLab, modifier = Modifier.fillMaxWidth()) { Text("Research Lab") }
        Button(onClick = onContractor, modifier = Modifier.fillMaxWidth()) { Text("Contractor") }
    }
}
