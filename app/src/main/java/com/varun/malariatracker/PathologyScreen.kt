package com.varun.malariatracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType

/** Pathology (Pathologist) questionnaire — saves one row to Pathology.csv. */
@Composable
fun PathologyScreen(draftKey: String, initialName: String = "", initialHealthCentre: String = "", onFinish: () -> Unit) {
    var formKey by rememberSaveable { mutableStateOf(0) }
    key(formKey) {
        val context = LocalContext.current
        var page by rememberDraftPage(draftKey)

        // General & administrative
        var pathologistName by rememberDraft(draftKey, "pathologistName", initialName)
        var healthCentre by rememberDraft(draftKey, "healthCentre", initialHealthCentre)
        var address by rememberDraft(draftKey, "address")
        var patientName by rememberDraft(draftKey, "patientName")
        var patientMobile by rememberDraft(draftKey, "patientMobile")

        // Laboratory investigation
        var diagnosisTests by rememberDraftSet(draftKey, "diagnosisTests")
        var rdtResult by rememberDraft(draftKey, "rdtResult")
        var rdtSpecies by rememberDraft(draftKey, "rdtSpecies")
        var rdtOtherSpecies by rememberDraft(draftKey, "rdtOtherSpecies")
        var microscopyResult by rememberDraft(draftKey, "microscopyResult")
        var microscopySpecies by rememberDraft(draftKey, "microscopySpecies")
        var microscopyOtherSpecies by rememberDraft(draftKey, "microscopyOtherSpecies")
        var smearType by rememberDraft(draftKey, "smearType")
        // Scheduled follow-up slides
        var slidesReceived by rememberDraft(draftKey, "slidesReceived")
        var anySlidePositive by rememberDraft(draftKey, "anySlidePositive")
        var positiveSlideDay by rememberDraft(draftKey, "positiveSlideDay")
        var nonReceiptReasons by rememberDraftSet(draftKey, "nonReceiptReasons")
        var otherReason by rememberDraft(draftKey, "otherReason")

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
                val rdtSpeciesValue = if (rdtResult == "Positive") withOther(rdtSpecies, rdtOtherSpecies) else ""
                val microscopySpeciesValue = if (microscopyResult == "Positive") withOther(microscopySpecies, microscopyOtherSpecies) else ""
                val received = slidesReceived == "Yes"
                val positiveValue = if (received) anySlidePositive else ""
                val dayValue = if (received && anySlidePositive == "Yes") positiveSlideDay else ""
                val reasonValue = if (slidesReceived == "No") withOther(nonReceiptReasons.joined(), otherReason) else ""

                val header = "Pathologist,HealthCentre,Address,PatientName,PatientMobile,PatientID,DiagnosisTest,RDTResult,RDTSpecies,MicroscopyResult,MicroscopySpecies,SmearType,SlidesReceived,AnySlidePositive,PositiveSlideDay,SlideNonReceiptReason"
                val rowData = listOf(
                    pathologistName, healthCentre, address, patientName, cleanPhone(patientMobile), linkedPatientId,
                    diagnosisTests.joined(), rdtResult, rdtSpeciesValue,
                    microscopyResult, microscopySpeciesValue, smearType,
                    slidesReceived, positiveValue, dayValue, reasonValue
                ).joinToString(",") { CsvHelper.escapeCsv(it) }

                CsvHelper.saveToCsv(context, "Pathology", header, rowData)
                Toast.makeText(context, "Pathology record saved!", Toast.LENGTH_SHORT).show()
                Drafts.clear(context, draftKey)
                onFinish()
            },
            header = { Text(text = "Pathology Questionnaire", style = MaterialTheme.typography.headlineMedium) },
            footer = {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { Drafts.clear(context, draftKey); formKey++ }) { Text("Clear form") }
            }
        ) { p ->
            when (p) {
                0 -> {
                    Text(text = "General & Administrative Information", style = MaterialTheme.typography.titleMedium)
                    FormField(value = pathologistName, onValueChange = { pathologistName = it }, label = "Name of the Pathologist")
                    LabeledDropdown(label = "Facility Name", options = HEALTH_CENTRES, selected = healthCentre, onSelect = { healthCentre = it })
                    FormField(value = address, onValueChange = { address = it }, label = "Address")
                    FormField(value = patientName, onValueChange = { patientName = it }, label = "Patient Name")
                    FormField(value = patientMobile, onValueChange = { patientMobile = it }, label = "Patient Mobile Number", keyboardType = KeyboardType.Phone)
                }
                1 -> {
                    Text(text = "Laboratory Investigation", style = MaterialTheme.typography.titleMedium)
                    CheckboxGroup(
                        title = "1. Malaria Diagnosis test done?",
                        options = listOf("RDT", "SMP", "Other"),
                        selected = diagnosisTests,
                        onToggle = { opt, on -> diagnosisTests = diagnosisTests.toggle(opt, on) }
                    )
                    RadioGroup(title = "2. RDT Result", options = listOf("Negative", "Positive"), selected = rdtResult, onSelect = { rdtResult = it })
                    if (rdtResult == "Positive") {
                        RadioGroup(title = "RDT species", options = PARASITE_SPECIES, selected = rdtSpecies, onSelect = { rdtSpecies = it })
                        if (rdtSpecies == "Other") {
                            FormField(value = rdtOtherSpecies, onValueChange = { rdtOtherSpecies = it }, label = "Specify")
                        }
                    }
                    RadioGroup(title = "3. Microscopy Result", options = listOf("Negative", "Positive"), selected = microscopyResult, onSelect = { microscopyResult = it })
                    if (microscopyResult == "Positive") {
                        RadioGroup(title = "Microscopy species", options = listOf("P. falciparum", "P. vivax", "Mixed infection", "Other"), selected = microscopySpecies, onSelect = { microscopySpecies = it })
                        if (microscopySpecies == "Other") {
                            FormField(value = microscopyOtherSpecies, onValueChange = { microscopyOtherSpecies = it }, label = "Specify")
                        }
                    }
                    RadioGroup(title = "4. Type of Smear read", options = listOf("Thick", "Thin", "Both"), selected = smearType, onSelect = { smearType = it })
                }
                else -> {
                    Text(text = "Scheduled Follow-up Slides", style = MaterialTheme.typography.titleMedium)
                    RadioGroup(title = "Scheduled follow-up slides received?", options = listOf("Yes", "No"), selected = slidesReceived, onSelect = { slidesReceived = it })
                    if (slidesReceived == "Yes") {
                        RadioGroup(title = "Did any follow-up day slide come positive for malaria?", options = listOf("Yes", "No"), selected = anySlidePositive, onSelect = { anySlidePositive = it })
                        if (anySlidePositive == "Yes") {
                            FormField(value = positiveSlideDay, onValueChange = { positiveSlideDay = it }, label = "Which follow-up day (e.g. Day 7)")
                        }
                    } else if (slidesReceived == "No") {
                        CheckboxGroup(
                            title = "Reason for non-receipt of slide",
                            options = listOf("Patient not available / missed visit", "Patient left Goa / migrated", "Refused", "Slide broken / spoiled", "Not collected by worker"),
                            selected = nonReceiptReasons,
                            onToggle = { opt, on -> nonReceiptReasons = nonReceiptReasons.toggle(opt, on) }
                        )
                        FormField(value = otherReason, onValueChange = { otherReason = it }, label = "Other reason (specify)")
                    }
                }
            }
        }
    }
}
