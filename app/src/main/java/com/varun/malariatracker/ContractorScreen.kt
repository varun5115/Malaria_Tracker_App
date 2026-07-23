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

/** Contractor questionnaire — saves one row to Contractor.csv. */
@Composable
fun ContractorScreen(initialName: String = "", onFinish: () -> Unit) {
    var formKey by rememberSaveable { mutableStateOf(0) }
    key(formKey) {
        val context = LocalContext.current

        // General & administrative
        var contractorName by rememberSaveable { mutableStateOf(initialName) }
        var constructionSite by rememberSaveable { mutableStateOf("") }
        var district by rememberSaveable { mutableStateOf("") }
        var area by rememberSaveable { mutableStateOf("") }
        var patientName by rememberSaveable { mutableStateOf("") }
        var patientId by rememberSaveable { mutableStateOf("") }

        // Status
        var diagnosisStatus by rememberSaveable { mutableStateOf("") }
        var otherDiagnosis by rememberSaveable { mutableStateOf("") }
        var treatmentStatus by rememberSaveable { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Contractor Questionnaire", style = MaterialTheme.typography.headlineMedium)

            Text(text = "General & Administrative Information", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = contractorName, onValueChange = { contractorName = it }, label = { Text("Name of the contractor") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = constructionSite, onValueChange = { constructionSite = it }, label = { Text("Name of the construction site") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("District") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Area") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = patientName, onValueChange = { patientName = it }, label = { Text("Patient Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = patientId, onValueChange = { patientId = it }, label = { Text("Patient ID") }, modifier = Modifier.fillMaxWidth())

            HorizontalDivider()

            RadioGroup(
                title = "1. Malaria diagnosis status",
                options = listOf("Pv", "Pf", "Pv/Pf", "Other"),
                selected = diagnosisStatus,
                onSelect = { diagnosisStatus = it }
            )
            if (diagnosisStatus == "Other") {
                OutlinedTextField(value = otherDiagnosis, onValueChange = { otherDiagnosis = it }, label = { Text("Specify") }, modifier = Modifier.fillMaxWidth())
            }

            RadioGroup(
                title = "2. Treatment status",
                options = listOf("Treated", "Untreated"),
                selected = treatmentStatus,
                onSelect = { treatmentStatus = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (patientName.isBlank() || patientId.isBlank()) {
                        Toast.makeText(context, "Please fill in Patient Name and Patient ID", Toast.LENGTH_SHORT).show()
                    } else {
                        val diagnosisValue = if (diagnosisStatus == "Other") withOther(diagnosisStatus, otherDiagnosis) else diagnosisStatus

                        val header = "ContractorName,ConstructionSite,District,Area,PatientName,PatientID,MalariaDiagnosisStatus,TreatmentStatus"
                        val rowData = listOf(
                            contractorName, constructionSite, district, area, patientName, patientId,
                            diagnosisValue, treatmentStatus
                        ).joinToString(",") { CsvHelper.escapeCsv(it) }

                        CsvHelper.saveToCsv(context, "Contractor", header, rowData)
                        Toast.makeText(context, "Contractor record saved!", Toast.LENGTH_SHORT).show()
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
