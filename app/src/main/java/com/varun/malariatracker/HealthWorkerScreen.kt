package com.varun.malariatracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType

/** Health Worker questionnaire (follow-up & adherence) — saves one row to HealthWorker.csv. */
@Composable
fun HealthWorkerScreen(draftKey: String, initialName: String = "", initialHealthCentre: String = "", onFinish: () -> Unit) {
    var formKey by rememberSaveable { mutableStateOf(0) }
    key(formKey) {
        val context = LocalContext.current
        var page by rememberDraftPage(draftKey)

        // General & administrative
        var workerName by rememberDraft(draftKey, "workerName", initialName)
        var healthCentre by rememberDraft(draftKey, "healthCentre", initialHealthCentre)
        var address by rememberDraft(draftKey, "address")
        var patientName by rememberDraft(draftKey, "patientName")
        var patientMobile by rememberDraft(draftKey, "patientMobile")

        // Follow-up & adherence monitoring
        var followUpPf by rememberDraftSet(draftKey, "followUpPf")
        var followUpPv by rememberDraftSet(draftKey, "followUpPv")
        var patientStatus by rememberDraft(draftKey, "patientStatus")
        var treatmentDetails by rememberDraft(draftKey, "treatmentDetails")
        var nonAdherenceReason by rememberDraft(draftKey, "nonAdherenceReason")
        var otherReason by rememberDraft(draftKey, "otherReason")
        var finalOutcome by rememberDraft(draftKey, "finalOutcome")

        // Non-adherence reason only applies when the course was not completed.
        val reasonApplies = treatmentDetails == "Partially completed" || treatmentDetails == "Stopped early"

        FormPager(
            pageCount = 3,
            page = page,
            onPageChange = { page = it },
            finishLabel = "Save Record",
            validatePage = { p ->
                if (p == 0) patientError(context, patientName, patientMobile) else null
            },
            onFinish = {
                val linkedPatientId = CsvHelper.findWorkerByPhone(context, cleanPhone(patientMobile))?.patientId ?: ""
                val reasonValue = when {
                    !reasonApplies -> ""
                    nonAdherenceReason == "Other" -> withOther(nonAdherenceReason, otherReason)
                    else -> nonAdherenceReason
                }

                val header = "HealthWorker,HealthCentre,Address,PatientName,PatientMobile,PatientID,FollowUpPf,FollowUpPv,PatientStatus,TreatmentDetails,NonAdherenceReason,FinalOutcome"
                val rowData = listOf(
                    workerName, healthCentre, address, patientName, cleanPhone(patientMobile), linkedPatientId,
                    followUpPf.joined(), followUpPv.joined(), patientStatus,
                    treatmentDetails, reasonValue, finalOutcome
                ).joinToString(",") { CsvHelper.escapeCsv(it) }

                CsvHelper.saveToCsv(context, "HealthWorker", header, rowData)
                Toast.makeText(context, "Health Worker record saved!", Toast.LENGTH_SHORT).show()
                Drafts.clear(context, draftKey)
                onFinish()
            },
            header = { Text(text = "Health Worker Questionnaire", style = MaterialTheme.typography.headlineMedium) },
            footer = {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { Drafts.clear(context, draftKey); formKey++ }) { Text("Clear form") }
            }
        ) { p ->
            when (p) {
                0 -> {
                    Text(text = "General & Administrative Information", style = MaterialTheme.typography.titleMedium)
                    FormField(value = workerName, onValueChange = { workerName = it }, label = "Name of the Health Worker")
                    LabeledDropdown(label = "Name of the health centre", options = HEALTH_CENTRES, selected = healthCentre, onSelect = { healthCentre = it })
                    FormField(value = address, onValueChange = { address = it }, label = "Address")
                    FormField(value = patientName, onValueChange = { patientName = it }, label = "Patient Name")
                    FormField(value = patientMobile, onValueChange = { patientMobile = it }, label = "Patient Mobile Number", keyboardType = KeyboardType.Phone)
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
                            FormField(value = otherReason, onValueChange = { otherReason = it }, label = "Specify reason")
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
