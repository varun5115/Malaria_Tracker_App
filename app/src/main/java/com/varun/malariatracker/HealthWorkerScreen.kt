package com.varun.malariatracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/** Health Worker questionnaire (follow-up & adherence) — saves one row to HealthWorker.csv. */
@Composable
fun HealthWorkerScreen(initialName: String = "", initialHealthCentre: String = "", onFinish: () -> Unit) {
    var formKey by rememberSaveable { mutableStateOf(0) }
    key(formKey) {
        val context = LocalContext.current
        var page by rememberSaveable { mutableStateOf(0) }

        // General & administrative
        var workerName by rememberSaveable { mutableStateOf(initialName) }
        var healthCentre by rememberSaveable { mutableStateOf(initialHealthCentre) }
        var address by rememberSaveable { mutableStateOf("") }
        var patientName by rememberSaveable { mutableStateOf("") }
        var patientId by rememberSaveable { mutableStateOf("") }

        // Follow-up & adherence monitoring
        var followUpPf by rememberSaveable { mutableStateOf(setOf<String>()) }
        var followUpPv by rememberSaveable { mutableStateOf(setOf<String>()) }
        var patientStatus by rememberSaveable { mutableStateOf("") }
        var treatmentDetails by rememberSaveable { mutableStateOf("") }
        var nonAdherenceReason by rememberSaveable { mutableStateOf("") }
        var otherReason by rememberSaveable { mutableStateOf("") }
        var finalOutcome by rememberSaveable { mutableStateOf("") }

        // Non-adherence reason only applies when the course was not completed.
        val reasonApplies = treatmentDetails == "Partially completed" || treatmentDetails == "Stopped early"

        FormPager(
            pageCount = 3,
            page = page,
            onPageChange = { page = it },
            finishLabel = "Save Record",
            validatePage = { p ->
                if (p == 0 && (patientName.isBlank() || patientId.isBlank())) "Please fill in Patient Name and Patient ID" else null
            },
            onFinish = {
                val reasonValue = when {
                    !reasonApplies -> ""
                    nonAdherenceReason == "Other" -> withOther(nonAdherenceReason, otherReason)
                    else -> nonAdherenceReason
                }

                val header = "HealthWorker,HealthCentre,Address,PatientName,PatientID,FollowUpPf,FollowUpPv,PatientStatus,TreatmentDetails,NonAdherenceReason,FinalOutcome"
                val rowData = listOf(
                    workerName, healthCentre, address, patientName, patientId,
                    followUpPf.joined(), followUpPv.joined(), patientStatus,
                    treatmentDetails, reasonValue, finalOutcome
                ).joinToString(",") { CsvHelper.escapeCsv(it) }

                CsvHelper.saveToCsv(context, "HealthWorker", header, rowData)
                Toast.makeText(context, "Health Worker record saved!", Toast.LENGTH_SHORT).show()
                onFinish()
            },
            header = { Text(text = "Health Worker Questionnaire", style = MaterialTheme.typography.headlineMedium) },
            footer = {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { formKey++ }) { Text("Clear form") }
            }
        ) { p ->
            when (p) {
                0 -> {
                    Text(text = "General & Administrative Information", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = workerName, onValueChange = { workerName = it }, label = { Text("Name of the Health Worker") }, modifier = Modifier.fillMaxWidth())
                    LabeledDropdown(label = "Name of the health centre", options = HEALTH_CENTRES, selected = healthCentre, onSelect = { healthCentre = it })
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = patientName, onValueChange = { patientName = it }, label = { Text("Patient Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = patientId, onValueChange = { patientId = it }, label = { Text("Patient ID") }, modifier = Modifier.fillMaxWidth())
                }
                1 -> {
                    Text(text = "Follow-up & Adherence Monitoring", style = MaterialTheme.typography.titleMedium)
                    CheckboxGroup(
                        title = "1. Follow-up for Pf",
                        options = listOf("Day-1", "Day-2", "Day-3", "Day-7", "Day-14"),
                        selected = followUpPf,
                        onToggle = { opt, on -> followUpPf = followUpPf.toggle(opt, on) }
                    )
                    CheckboxGroup(
                        title = "2. Follow-up for Pv",
                        options = listOf("Day-3", "Day-7", "Day-14", "Day-28"),
                        selected = followUpPv,
                        onToggle = { opt, on -> followUpPv = followUpPv.toggle(opt, on) }
                    )
                    RadioGroup(
                        title = "3. Follow-up patient status",
                        options = listOf("Symptom free", "Fever persists", "New/worsening symptoms", "Did not report (missed)"),
                        selected = patientStatus,
                        onSelect = { patientStatus = it }
                    )
                }
                else -> {
                    RadioGroup(
                        title = "4. Treatment Details",
                        options = listOf("Completed full course", "Partially completed", "Stopped early", "Unknown"),
                        selected = treatmentDetails,
                        onSelect = { treatmentDetails = it }
                    )
                    if (reasonApplies) {
                        RadioGroup(
                            title = "5. Reason for non-adherence",
                            options = listOf("Felt better and stopped", "Side effects", "Drug not available", "Left Goa / migrated", "Other"),
                            selected = nonAdherenceReason,
                            onSelect = { nonAdherenceReason = it }
                        )
                        if (nonAdherenceReason == "Other") {
                            OutlinedTextField(value = otherReason, onValueChange = { otherReason = it }, label = { Text("Specify reason") }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    RadioGroup(
                        title = "6. Final outcome of malaria episode",
                        options = listOf("Cured (no symptoms by day-28)", "Ongoing treatment", "Recurrent malaria (re-infection/relapse)", "Lost to follow-up"),
                        selected = finalOutcome,
                        onSelect = { finalOutcome = it }
                    )
                }
            }
        }
    }
}
