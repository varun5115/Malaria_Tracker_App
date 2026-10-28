package com.varun.malariatracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType

/** Research Lab questionnaire (PCR & drug resistance) — saves one row to ResearchLab.csv. */
@Composable
fun ResearchLabScreen(initialName: String = "", onFinish: () -> Unit) {
    var formKey by rememberSaveable { mutableStateOf(0) }
    key(formKey) {
        val context = LocalContext.current
        var page by rememberSaveable { mutableStateOf(0) }

        // General & administrative
        var labName by rememberSaveable { mutableStateOf(initialName) }
        var address by rememberSaveable { mutableStateOf("") }
        var patientName by rememberSaveable { mutableStateOf("") }
        var patientId by rememberSaveable { mutableStateOf("") }

        // Sample collection for PCR and drug resistance
        var rdtResult by rememberSaveable { mutableStateOf("") }
        var rdtSpecies by rememberSaveable { mutableStateOf("") }
        var rdtOtherSpecies by rememberSaveable { mutableStateOf("") }
        var microscopyResult by rememberSaveable { mutableStateOf("") }
        var microscopySpecies by rememberSaveable { mutableStateOf("") }
        var microscopyOtherSpecies by rememberSaveable { mutableStateOf("") }
        var smearType by rememberSaveable { mutableStateOf("") }
        var parasiteDensity by rememberSaveable { mutableStateOf("") }
        var gametocyte by rememberSaveable { mutableStateOf("") }
        var pvResistance by rememberSaveable { mutableStateOf(setOf<String>()) }
        var pfResistance by rememberSaveable { mutableStateOf(setOf<String>()) }
        var hrpDeletion by rememberSaveable { mutableStateOf("") }

        FormPager(
            pageCount = 3,
            page = page,
            onPageChange = { page = it },
            finishLabel = "Save Record",
            validatePage = { p ->
                if (p == 0 && (patientName.isBlank() || patientId.isBlank())) "Please fill in Patient Name and Patient ID" else null
            },
            onFinish = {
                val rdtSpeciesValue = if (rdtResult == "Positive") withOther(rdtSpecies, rdtOtherSpecies) else ""
                val microscopySpeciesValue = if (microscopyResult == "Positive") withOther(microscopySpecies, microscopyOtherSpecies) else ""

                val header = "ResearchLab,Address,PatientName,PatientID,RDTResult,RDTSpecies,MicroscopyResult,MicroscopySpecies,SmearType,ParasiteDensity,GametocytePresence,PvDrugResistance,PfDrugResistance,HrpDeletion"
                val rowData = listOf(
                    labName, address, patientName, patientId,
                    rdtResult, rdtSpeciesValue, microscopyResult, microscopySpeciesValue, smearType,
                    parasiteDensity, gametocyte, pvResistance.joined(), pfResistance.joined(), hrpDeletion
                ).joinToString(",") { CsvHelper.escapeCsv(it) }

                CsvHelper.saveToCsv(context, "ResearchLab", header, rowData)
                Toast.makeText(context, "Research Lab record saved!", Toast.LENGTH_SHORT).show()
                onFinish()
            },
            header = { Text(text = "Research Lab Questionnaire", style = MaterialTheme.typography.headlineMedium) },
            footer = {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { formKey++ }) { Text("Clear form") }
            }
        ) { p ->
            when (p) {
                0 -> {
                    Text(text = "General & Administrative Information", style = MaterialTheme.typography.titleMedium)
                    LabeledDropdown(label = "Name of the Research Laboratory", options = RESEARCH_LABS, selected = labName, onSelect = { labName = it })
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = patientName, onValueChange = { patientName = it }, label = { Text("Patient Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = patientId, onValueChange = { patientId = it }, label = { Text("Patient ID") }, modifier = Modifier.fillMaxWidth())
                }
                1 -> {
                    Text(text = "Sample Collection for PCR and Drug Resistance", style = MaterialTheme.typography.titleMedium)
                    RadioGroup(title = "1. RDT Result", options = listOf("Negative", "Positive"), selected = rdtResult, onSelect = { rdtResult = it })
                    if (rdtResult == "Positive") {
                        RadioGroup(title = "RDT species", options = PARASITE_SPECIES, selected = rdtSpecies, onSelect = { rdtSpecies = it })
                        if (rdtSpecies == "Other") {
                            OutlinedTextField(value = rdtOtherSpecies, onValueChange = { rdtOtherSpecies = it }, label = { Text("Specify") }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    RadioGroup(title = "2. Microscopy Result", options = listOf("Negative", "Positive"), selected = microscopyResult, onSelect = { microscopyResult = it })
                    if (microscopyResult == "Positive") {
                        RadioGroup(title = "Microscopy species", options = listOf("P. falciparum", "P. vivax", "Mixed infection", "Other"), selected = microscopySpecies, onSelect = { microscopySpecies = it })
                        if (microscopySpecies == "Other") {
                            OutlinedTextField(value = microscopyOtherSpecies, onValueChange = { microscopyOtherSpecies = it }, label = { Text("Specify") }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    RadioGroup(title = "3. Type of Smear read", options = listOf("Thick", "Thin", "Both"), selected = smearType, onSelect = { smearType = it })
                    OutlinedTextField(value = parasiteDensity, onValueChange = { parasiteDensity = it }, label = { Text("4. Parasite density (parasites/µL)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
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
