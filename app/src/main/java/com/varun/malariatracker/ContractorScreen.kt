package com.varun.malariatracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/** Contractor questionnaire — saves one row to Contractor.csv. */
@Composable
fun ContractorScreen(initialName: String = "", onFinish: () -> Unit) {
    var formKey by rememberSaveable { mutableStateOf(0) }
    key(formKey) {
        val context = LocalContext.current
        var page by rememberSaveable { mutableStateOf(0) }

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

        // Section I – Travel details of worker
        var travellerName by rememberSaveable { mutableStateOf("") }
        var travellerContact by rememberSaveable { mutableStateOf("") }
        var dateVisitOutside by rememberSaveable { mutableStateOf("") }
        var dateReturnInside by rememberSaveable { mutableStateOf("") }

        FormPager(
            pageCount = 2,
            page = page,
            onPageChange = { page = it },
            finishLabel = "Save Record",
            validatePage = { p ->
                if (p == 0 && (patientName.isBlank() || patientId.isBlank())) "Please fill in Patient Name and Patient ID" else null
            },
            onFinish = {
                val diagnosisValue = if (diagnosisStatus == "Other") withOther(diagnosisStatus, otherDiagnosis) else diagnosisStatus

                val header = "ContractorName,ConstructionSite,District,Area,PatientName,PatientID,MalariaDiagnosisStatus,TreatmentStatus,TravellerName,TravellerContact,DateVisitOutsideGoa,DateReturnInsideGoa"
                val rowData = listOf(
                    contractorName, constructionSite, district, area, patientName, patientId,
                    diagnosisValue, treatmentStatus,
                    travellerName, travellerContact, dateVisitOutside, dateReturnInside
                ).joinToString(",") { CsvHelper.escapeCsv(it) }

                CsvHelper.saveToCsv(context, "Contractor", header, rowData)
                Toast.makeText(context, "Contractor record saved!", Toast.LENGTH_SHORT).show()
                onFinish()
            },
            header = { Text(text = "Contractor Questionnaire", style = MaterialTheme.typography.headlineMedium) },
            footer = {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { formKey++ }) { Text("Clear form") }
            }
        ) { p ->
            when (p) {
                0 -> {
                    Text(text = "General & Administrative Information", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = contractorName, onValueChange = { contractorName = it }, label = { Text("Name of the contractor") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = constructionSite, onValueChange = { constructionSite = it }, label = { Text("Name of the construction site") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("District") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Area") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = patientName, onValueChange = { patientName = it }, label = { Text("Patient Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = patientId, onValueChange = { patientId = it }, label = { Text("Patient ID") }, modifier = Modifier.fillMaxWidth())
                }
                else -> {
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

                    Text(text = "Section I – Travel details of worker (in and outside Goa)", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = travellerName, onValueChange = { travellerName = it }, label = { Text("Name of the traveller planning to visit outside Goa") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = travellerContact, onValueChange = { travellerContact = it }, label = { Text("Traveller Mobile / Name / ID") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dateVisitOutside, onValueChange = { dateVisitOutside = it }, label = { Text("Date of visit outside Goa — DD/MM/YYYY") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dateReturnInside, onValueChange = { dateReturnInside = it }, label = { Text("Date of return inside Goa — DD/MM/YYYY") }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
