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

/** Pathology (Pathologist) questionnaire — saves one row to Pathology.csv. */
@Composable
fun PathologyScreen(initialName: String = "", initialHealthCentre: String = "", onFinish: () -> Unit) {
    var formKey by rememberSaveable { mutableStateOf(0) }
    key(formKey) {
        val context = LocalContext.current

        // General & administrative
        var pathologistName by rememberSaveable { mutableStateOf(initialName) }
        var healthCentre by rememberSaveable { mutableStateOf(initialHealthCentre) }
        var address by rememberSaveable { mutableStateOf("") }
        var patientName by rememberSaveable { mutableStateOf("") }
        var patientId by rememberSaveable { mutableStateOf("") }

        // Laboratory investigation
        var diagnosisTests by rememberSaveable { mutableStateOf(setOf<String>()) }
        var rdtResult by rememberSaveable { mutableStateOf("") }
        var rdtSpecies by rememberSaveable { mutableStateOf("") }
        var rdtOtherSpecies by rememberSaveable { mutableStateOf("") }
        var microscopyResult by rememberSaveable { mutableStateOf("") }
        var microscopySpecies by rememberSaveable { mutableStateOf("") }
        var microscopyOtherSpecies by rememberSaveable { mutableStateOf("") }
        var smearType by rememberSaveable { mutableStateOf("") }
        // Scheduled follow-up slides (green additions, Sept 2026)
        var slidesReceived by rememberSaveable { mutableStateOf("") }
        var anySlidePositive by rememberSaveable { mutableStateOf("") }
        var positiveSlideDay by rememberSaveable { mutableStateOf("") }
        var nonReceiptReasons by rememberSaveable { mutableStateOf(setOf<String>()) }
        var otherReason by rememberSaveable { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Pathology Questionnaire", style = MaterialTheme.typography.headlineMedium)

            Text(text = "General & Administrative Information", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = pathologistName, onValueChange = { pathologistName = it }, label = { Text("Name of the Pathologist") }, modifier = Modifier.fillMaxWidth())
            LabeledDropdown(label = "Health centre name", options = HEALTH_CENTRES, selected = healthCentre, onSelect = { healthCentre = it })
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = patientName, onValueChange = { patientName = it }, label = { Text("Patient Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = patientId, onValueChange = { patientId = it }, label = { Text("Patient ID") }, modifier = Modifier.fillMaxWidth())

            HorizontalDivider()
            Text(text = "Laboratory Investigation", style = MaterialTheme.typography.titleMedium)

            CheckboxGroup(
                title = "1. Malaria Diagnosis test done?",
                options = listOf("RDT", "SMP", "Other"),
                selected = diagnosisTests,
                onToggle = { opt, on -> diagnosisTests = diagnosisTests.toggle(opt, on) }
            )

            RadioGroup(
                title = "2. RDT Result",
                options = listOf("Negative", "Positive"),
                selected = rdtResult,
                onSelect = { rdtResult = it }
            )
            if (rdtResult == "Positive") {
                RadioGroup(title = "RDT species", options = PARASITE_SPECIES, selected = rdtSpecies, onSelect = { rdtSpecies = it })
                if (rdtSpecies == "Other") {
                    OutlinedTextField(value = rdtOtherSpecies, onValueChange = { rdtOtherSpecies = it }, label = { Text("Specify") }, modifier = Modifier.fillMaxWidth())
                }
            }

            RadioGroup(
                title = "3. Microscopy Result",
                options = listOf("Negative", "Positive"),
                selected = microscopyResult,
                onSelect = { microscopyResult = it }
            )
            if (microscopyResult == "Positive") {
                RadioGroup(title = "Microscopy species", options = listOf("P. falciparum", "P. vivax", "Mixed infection", "Other"), selected = microscopySpecies, onSelect = { microscopySpecies = it })
                if (microscopySpecies == "Other") {
                    OutlinedTextField(value = microscopyOtherSpecies, onValueChange = { microscopyOtherSpecies = it }, label = { Text("Specify") }, modifier = Modifier.fillMaxWidth())
                }
            }

            RadioGroup(
                title = "4. Type of Smear read",
                options = listOf("Thick", "Thin", "Both"),
                selected = smearType,
                onSelect = { smearType = it }
            )

            HorizontalDivider()
            Text(text = "Scheduled Follow-up Slides", style = MaterialTheme.typography.titleMedium)

            RadioGroup(
                title = "Scheduled follow-up slides received?",
                options = listOf("Yes", "No"),
                selected = slidesReceived,
                onSelect = { slidesReceived = it }
            )
            if (slidesReceived == "Yes") {
                RadioGroup(
                    title = "Did any follow-up day slide come positive for malaria?",
                    options = listOf("Yes", "No"),
                    selected = anySlidePositive,
                    onSelect = { anySlidePositive = it }
                )
                if (anySlidePositive == "Yes") {
                    OutlinedTextField(value = positiveSlideDay, onValueChange = { positiveSlideDay = it }, label = { Text("Which follow-up day (e.g. Day 7)") }, modifier = Modifier.fillMaxWidth())
                }
            } else if (slidesReceived == "No") {
                CheckboxGroup(
                    title = "Reason for non-receipt of slide",
                    options = listOf("Patient not available / missed visit", "Patient left Goa / migrated", "Refused", "Slide broken / spoiled", "Not collected by worker"),
                    selected = nonReceiptReasons,
                    onToggle = { opt, on -> nonReceiptReasons = nonReceiptReasons.toggle(opt, on) }
                )
                OutlinedTextField(value = otherReason, onValueChange = { otherReason = it }, label = { Text("Other reason (specify)") }, modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (patientName.isBlank() || patientId.isBlank()) {
                        Toast.makeText(context, "Please fill in Patient Name and Patient ID", Toast.LENGTH_SHORT).show()
                    } else {
                        val rdtSpeciesValue = if (rdtResult == "Positive") withOther(rdtSpecies, rdtOtherSpecies) else ""
                        val microscopySpeciesValue = if (microscopyResult == "Positive") withOther(microscopySpecies, microscopyOtherSpecies) else ""

                        val reasonValue = withOther(nonReceiptReasons.joined(), otherReason)
                        val header = "Pathologist,HealthCentre,Address,PatientName,PatientID,DiagnosisTest,RDTResult,RDTSpecies,MicroscopyResult,MicroscopySpecies,SmearType,SlidesReceived,AnySlidePositive,PositiveSlideDay,SlideNonReceiptReason"
                        val rowData = listOf(
                            pathologistName, healthCentre, address, patientName, patientId,
                            diagnosisTests.joined(), rdtResult, rdtSpeciesValue,
                            microscopyResult, microscopySpeciesValue, smearType,
                            slidesReceived, anySlidePositive, positiveSlideDay, reasonValue
                        ).joinToString(",") { CsvHelper.escapeCsv(it) }

                        CsvHelper.saveToCsv(context, "Pathology", header, rowData)
                        Toast.makeText(context, "Pathology record saved!", Toast.LENGTH_SHORT).show()
                        onFinish()
                    }
                }
            ) {
                Text("Save Record")
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { formKey++ }
            ) {
                Text("Clear form")
            }
        }
    }
}
