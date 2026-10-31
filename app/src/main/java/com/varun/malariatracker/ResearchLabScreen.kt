package com.varun.malariatracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType

/** Research Lab questionnaire (PCR & drug resistance) — saves one row to ResearchLab.csv. */
@Composable
fun ResearchLabScreen(draftKey: String, initialName: String = "", onFinish: () -> Unit) {
    var formKey by rememberSaveable { mutableStateOf(0) }
    key(formKey) {
        val context = LocalContext.current
        var page by rememberDraftPage(draftKey)

        // General & administrative
        var labName by rememberDraft(draftKey, "labName", initialName)
        var address by rememberDraft(draftKey, "address")
        var patientName by rememberDraft(draftKey, "patientName")
        var patientMobile by rememberDraft(draftKey, "patientMobile")

        // Sample collection for PCR and drug resistance
        var rdtResult by rememberDraft(draftKey, "rdtResult")
        var rdtSpecies by rememberDraft(draftKey, "rdtSpecies")
        var rdtOtherSpecies by rememberDraft(draftKey, "rdtOtherSpecies")
        var microscopyResult by rememberDraft(draftKey, "microscopyResult")
        var microscopySpecies by rememberDraft(draftKey, "microscopySpecies")
        var microscopyOtherSpecies by rememberDraft(draftKey, "microscopyOtherSpecies")
        var smearType by rememberDraft(draftKey, "smearType")
        var parasiteDensity by rememberDraft(draftKey, "parasiteDensity")
        var gametocyte by rememberDraft(draftKey, "gametocyte")
        var pvResistance by rememberDraftSet(draftKey, "pvResistance")
        var pfResistance by rememberDraftSet(draftKey, "pfResistance")
        var hrpDeletion by rememberDraft(draftKey, "hrpDeletion")

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

                val header = "ResearchLab,Address,PatientName,PatientMobile,PatientID,RDTResult,RDTSpecies,MicroscopyResult,MicroscopySpecies,SmearType,ParasiteDensity,GametocytePresence,PvDrugResistance,PfDrugResistance,HrpDeletion"
                val rowData = listOf(
                    labName, address, patientName, cleanPhone(patientMobile), linkedPatientId,
                    rdtResult, rdtSpeciesValue, microscopyResult, microscopySpeciesValue, smearType,
                    parasiteDensity, gametocyte, pvResistance.joined(), pfResistance.joined(), hrpDeletion
                ).joinToString(",") { CsvHelper.escapeCsv(it) }

                CsvHelper.saveToCsv(context, "ResearchLab", header, rowData)
                Toast.makeText(context, "Research Lab record saved!", Toast.LENGTH_SHORT).show()
                Drafts.clear(context, draftKey)
                onFinish()
            },
            header = { Text(text = "Research Lab Questionnaire", style = MaterialTheme.typography.headlineMedium) },
            footer = {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { Drafts.clear(context, draftKey); formKey++ }) { Text("Clear form") }
            }
        ) { p ->
            when (p) {
                0 -> {
                    Text(text = "General & Administrative Information", style = MaterialTheme.typography.titleMedium)
                    LabeledDropdown(label = "Name of the Research Laboratory", options = RESEARCH_LABS, selected = labName, onSelect = { labName = it })
                    FormField(value = address, onValueChange = { address = it }, label = "Address")
                    FormField(value = patientName, onValueChange = { patientName = it }, label = "Patient Name")
                    FormField(value = patientMobile, onValueChange = { patientMobile = it }, label = "Patient Mobile Number", keyboardType = KeyboardType.Phone)
                }
                1 -> {
                    Text(text = "Sample Collection for PCR and Drug Resistance", style = MaterialTheme.typography.titleMedium)
                    RadioGroup(title = "1. RDT Result", options = listOf("Negative", "Positive"), selected = rdtResult, onSelect = { rdtResult = it })
                    if (rdtResult == "Positive") {
                        RadioGroup(title = "RDT species", options = PARASITE_SPECIES, selected = rdtSpecies, onSelect = { rdtSpecies = it })
                        if (rdtSpecies == "Other") {
                            FormField(value = rdtOtherSpecies, onValueChange = { rdtOtherSpecies = it }, label = "Specify")
                        }
                    }
                    RadioGroup(title = "2. Microscopy Result", options = listOf("Negative", "Positive"), selected = microscopyResult, onSelect = { microscopyResult = it })
                    if (microscopyResult == "Positive") {
                        RadioGroup(title = "Microscopy species", options = listOf("P. falciparum", "P. vivax", "Mixed infection", "Other"), selected = microscopySpecies, onSelect = { microscopySpecies = it })
                        if (microscopySpecies == "Other") {
                            FormField(value = microscopyOtherSpecies, onValueChange = { microscopyOtherSpecies = it }, label = "Specify")
                        }
                    }
                    RadioGroup(title = "3. Type of Smear read", options = listOf("Thick", "Thin", "Both"), selected = smearType, onSelect = { smearType = it })
                    FormField(value = parasiteDensity, onValueChange = { parasiteDensity = it }, label = "4. Parasite density (parasites/µL)", keyboardType = KeyboardType.Number)
                }
                else -> {
                    RadioGroup(title = "5. Gametocyte presence", options = listOf("Present", "Absent", "Not recorded"), selected = gametocyte, onSelect = { gametocyte = it })
                    CheckboxGroup(
                        title = "6. Drug resistance result — P. vivax",
                        options = listOf("PvCRT-o", "Pv MDR1", "Other"),
                        selected = pvResistance,
                        onToggle = { opt, on -> pvResistance = pvResistance.toggle(opt, on) }
                    )
                    CheckboxGroup(
                        title = "7. Drug resistance result — P. falciparum",
                        options = listOf("Pfkech13", "PFDHFR", "Pfdhps", "Other"),
                        selected = pfResistance,
                        onToggle = { opt, on -> pfResistance = pfResistance.toggle(opt, on) }
                    )
                    RadioGroup(title = "8. Pf hrp2 / Pf hrp3 gene deletion", options = listOf("Yes", "No", "Other"), selected = hrpDeletion, onSelect = { hrpDeletion = it })
                }
            }
        }
    }
}
