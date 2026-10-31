package com.varun.malariatracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType

/** Contractor questionnaire — saves one row to Contractor.csv. */
@Composable
fun ContractorScreen(draftKey: String, initialName: String = "", onFinish: () -> Unit) {
    var formKey by rememberSaveable { mutableStateOf(0) }
    key(formKey) {
        val context = LocalContext.current
        var page by rememberDraftPage(draftKey)

        // General & administrative
        var contractorName by rememberDraft(draftKey, "contractorName", initialName)
        var constructionSite by rememberDraft(draftKey, "constructionSite")
        var district by rememberDraft(draftKey, "district")
        var area by rememberDraft(draftKey, "area")
        var patientName by rememberDraft(draftKey, "patientName")
        var patientMobile by rememberDraft(draftKey, "patientMobile")

        // Status
        var diagnosisStatus by rememberDraft(draftKey, "diagnosisStatus")
        var otherDiagnosis by rememberDraft(draftKey, "otherDiagnosis")
        var treatmentStatus by rememberDraft(draftKey, "treatmentStatus")

        // Section I – Travel details of worker
        var travellerName by rememberDraft(draftKey, "travellerName")
        var travellerContact by rememberDraft(draftKey, "travellerContact")
        var dateVisitOutside by rememberDraft(draftKey, "dateVisitOutside")
        var dateReturnInside by rememberDraft(draftKey, "dateReturnInside")

        FormPager(
            pageCount = 2,
            page = page,
            onPageChange = { page = it },
            finishLabel = "Save Record",
            validatePage = { p ->
                if (p == 0) patientError(context, patientName, patientMobile) else null
            },
            onFinish = {
                val linkedPatientId = CsvHelper.findWorkerByPhone(context, cleanPhone(patientMobile))?.patientId ?: ""
                val diagnosisValue = if (diagnosisStatus == "Other") withOther(diagnosisStatus, otherDiagnosis) else diagnosisStatus

                val header = "ContractorName,ConstructionSite,District,Area,PatientName,PatientMobile,PatientID,MalariaDiagnosisStatus,TreatmentStatus,TravellerName,TravellerContact,DateVisitOutsideGoa,DateReturnInsideGoa"
                val rowData = listOf(
                    contractorName, constructionSite, district, area, patientName, cleanPhone(patientMobile), linkedPatientId,
                    diagnosisValue, treatmentStatus,
                    travellerName, travellerContact, dateVisitOutside, dateReturnInside
                ).joinToString(",") { CsvHelper.escapeCsv(it) }

                CsvHelper.saveToCsv(context, "Contractor", header, rowData)
                Toast.makeText(context, "Contractor record saved!", Toast.LENGTH_SHORT).show()
                Drafts.clear(context, draftKey)
                onFinish()
            },
            header = { Text(text = "Contractor Questionnaire", style = MaterialTheme.typography.headlineMedium) },
            footer = {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { Drafts.clear(context, draftKey); formKey++ }) { Text("Clear form") }
            }
        ) { p ->
            when (p) {
                0 -> {
                    Text(text = "General & Administrative Information", style = MaterialTheme.typography.titleMedium)
                    FormField(value = contractorName, onValueChange = { contractorName = it }, label = "Name of the contractor")
                    FormField(value = constructionSite, onValueChange = { constructionSite = it }, label = "Name of the construction site")
                    FormField(value = district, onValueChange = { district = it }, label = "District")
                    FormField(value = area, onValueChange = { area = it }, label = "Area")
                    FormField(value = patientName, onValueChange = { patientName = it }, label = "Patient Name")
                    FormField(value = patientMobile, onValueChange = { patientMobile = it }, label = "Patient Mobile Number", keyboardType = KeyboardType.Phone)
                }
                else -> {
                    RadioGroup(
                        title = "1. Malaria diagnosis status",
                        options = listOf("Pv", "Pf", "Pv/Pf", "Other"),
                        selected = diagnosisStatus,
                        onSelect = { diagnosisStatus = it }
                    )
                    if (diagnosisStatus == "Other") {
                        FormField(value = otherDiagnosis, onValueChange = { otherDiagnosis = it }, label = "Specify")
                    }
                    RadioGroup(
                        title = "2. Treatment status",
                        options = listOf("Treated", "Untreated"),
                        selected = treatmentStatus,
                        onSelect = { treatmentStatus = it }
                    )

                    Text(text = "Section I – Travel details of worker (in and outside Goa)", style = MaterialTheme.typography.titleMedium)
                    FormField(value = travellerName, onValueChange = { travellerName = it }, label = "Name of the traveller planning to visit outside Goa")
                    FormField(value = travellerContact, onValueChange = { travellerContact = it }, label = "Traveller Mobile / Name / ID")
                    FormField(value = dateVisitOutside, onValueChange = { dateVisitOutside = it }, label = "Date of visit outside Goa — DD/MM/YYYY")
                    FormField(value = dateReturnInside, onValueChange = { dateReturnInside = it }, label = "Date of return inside Goa — DD/MM/YYYY")
                }
            }
        }
    }
}
