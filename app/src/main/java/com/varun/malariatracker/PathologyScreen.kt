package com.varun.malariatracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Pathology (Pathologist) questionnaire — saves one row to Pathology.csv. */
@Composable
fun PathologyScreen(onFinish: () -> Unit) {
    var formKey by rememberSaveable { mutableStateOf(0) }
    key(formKey) {
        val context = LocalContext.current

        // General & administrative
        var pathologistName by rememberSaveable { mutableStateOf("") }
        var healthCentre by rememberSaveable { mutableStateOf("") }
        var address by rememberSaveable { mutableStateOf("") }
        var patientName by rememberSaveable { mutableStateOf("") }
        var mobile by rememberSaveable { mutableStateOf("") }

        // Laboratory investigation
        var diagnosisTests by rememberSaveable { mutableStateOf(setOf<String>()) }
        var rdtResult by rememberSaveable { mutableStateOf("") }
        var rdtSpecies by rememberSaveable { mutableStateOf("") }
        var rdtOtherSpecies by rememberSaveable { mutableStateOf("") }
        var microscopyResult by rememberSaveable { mutableStateOf("") }
        var microscopySpecies by rememberSaveable { mutableStateOf("") }
        var microscopyOtherSpecies by rememberSaveable { mutableStateOf("") }
        var smearType by rememberSaveable { mutableStateOf("") }

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
            OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())

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

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (patientName.isBlank() || mobile.isBlank()) {
                        Toast.makeText(context, "Please fill in Patient Name and Mobile", Toast.LENGTH_SHORT).show()
                    } else {
                        val rdtSpeciesValue = if (rdtResult == "Positive") withOther(rdtSpecies, rdtOtherSpecies) else ""
                        val microscopySpeciesValue = if (microscopyResult == "Positive") withOther(microscopySpecies, microscopyOtherSpecies) else ""

                        val header = "Pathologist,HealthCentre,Address,PatientName,Mobile,DiagnosisTest,RDTResult,RDTSpecies,MicroscopyResult,MicroscopySpecies,SmearType"
                        val rowData = listOf(
                            pathologistName, healthCentre, address, patientName, mobile,
                            diagnosisTests.joined(), rdtResult, rdtSpeciesValue,
                            microscopyResult, microscopySpeciesValue, smearType
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
