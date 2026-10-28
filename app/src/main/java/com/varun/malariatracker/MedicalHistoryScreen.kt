package com.varun.malariatracker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier

/**
 * Section II – past history of malaria. Every question after Q1 is about the past
 * malaria episode, so it is shown (and saved) only when Q1 is "Yes".
 */
@Composable
fun MedicalHistoryScreen(patientId: String, patientName: String, onNavigateNext: (String) -> Unit) {
    var page by rememberSaveable { mutableStateOf(0) }

    var diagnosedMalaria by rememberSaveable { mutableStateOf("") }
    var malariaTimes by rememberSaveable { mutableStateOf("") }
    var lastDetection by rememberSaveable { mutableStateOf("") }
    val placeOptions = listOf("UHC/PHC", "Private clinic", "Govt hospital", "Pharmacy")
    var treatmentPlaces by rememberSaveable { mutableStateOf(setOf<String>()) }
    var otherTreatmentPlace by rememberSaveable { mutableStateOf("") }
    var diagnosisTest by rememberSaveable { mutableStateOf("") }
    var sameDayReport by rememberSaveable { mutableStateOf("") }
    val methodOptions = listOf("RDT (rapid test)", "Microscopy", "Not sure")
    var reportMethods by rememberSaveable { mutableStateOf(setOf<String>()) }
    var medicationPeriod by rememberSaveable { mutableStateOf("") }
    var medicineCourseCompleted by rememberSaveable { mutableStateOf("") }

    val diagnosed = diagnosedMalaria == "Yes"

    FormPager(
        pageCount = if (diagnosed) 2 else 1,
        page = page,
        onPageChange = { page = it },
        finishLabel = "Continue to Travel History",
        validatePage = { p -> if (p == 0 && diagnosedMalaria.isBlank()) "Please answer question 1" else null },
        onFinish = {
            // Answers to hidden questions are saved blank, never stale.
            fun ifDiagnosed(v: String) = if (diagnosed) v else ""
            val tested = diagnosed && diagnosisTest == "Yes"
            val sameDay = tested && sameDayReport == "Yes"

            val places = withOther(joinInOrder(placeOptions, treatmentPlaces), otherTreatmentPlace)
                .let { ifDiagnosed(it) }
            val methods = if (sameDay) joinInOrder(methodOptions, reportMethods) else ""

            val rowData = listOf(
                patientId, diagnosedMalaria,
                ifDiagnosed(malariaTimes), ifDiagnosed(lastDetection), places,
                ifDiagnosed(diagnosisTest), if (tested) sameDayReport else "", methods,
                ifDiagnosed(medicationPeriod), ifDiagnosed(medicineCourseCompleted)
            ).joinToString(",") { CsvHelper.escapeCsv(it) }

            onNavigateNext(rowData)
        },
        header = {
            Text(text = "Medical History", style = MaterialTheme.typography.headlineMedium)
            Text(text = "Patient: $patientName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(text = "ID: $patientId", color = MaterialTheme.colorScheme.secondary)
        }
    ) { p ->
        when (p) {
            0 -> {
                RadioGroup(
                    title = "1. Diagnosed with malaria?",
                    options = listOf("Yes", "No", "Don't know"),
                    selected = diagnosedMalaria,
                    onSelect = { diagnosedMalaria = it }
                )
                if (diagnosed) {
                    OutlinedTextField(value = malariaTimes, onValueChange = { malariaTimes = it }, label = { Text("2. Number of times in last 2 years") }, modifier = Modifier.fillMaxWidth())
                    RadioGroup(
                        title = "3. Date of last malaria detection",
                        options = listOf("<1 year", "1 to 2 year", "Don't know"),
                        selected = lastDetection,
                        onSelect = { lastDetection = it }
                    )
                    CheckboxGroup(
                        title = "4. Place where treatment received",
                        options = placeOptions,
                        selected = treatmentPlaces,
                        onToggle = { opt, on -> treatmentPlaces = treatmentPlaces.toggle(opt, on) }
                    )
                    OutlinedTextField(value = otherTreatmentPlace, onValueChange = { otherTreatmentPlace = it }, label = { Text("Other treatment place (optional)") }, modifier = Modifier.fillMaxWidth())
                }
            }
            else -> {
                RadioGroup(
                    title = "5. Diagnosis test performed?",
                    options = listOf("Yes", "No"),
                    selected = diagnosisTest,
                    onSelect = { diagnosisTest = it }
                )
                if (diagnosisTest == "Yes") {
                    RadioGroup(
                        title = "6. Received report same day?",
                        options = listOf("Yes", "No"),
                        selected = sameDayReport,
                        onSelect = { sameDayReport = it }
                    )
                    if (sameDayReport == "Yes") {
                        CheckboxGroup(
                            title = "If yes, method used:",
                            options = methodOptions,
                            selected = reportMethods,
                            onToggle = { opt, on -> reportMethods = reportMethods.toggle(opt, on) }
                        )
                    }
                }
                RadioGroup(
                    title = "7. Medication period",
                    options = listOf("3 Days", "14 Days (P. vivax)", "28 Days follow-up", "Not informed / Don't know"),
                    selected = medicationPeriod,
                    onSelect = { medicationPeriod = it }
                )
                RadioGroup(
                    title = "8. Completed full course of medicines?",
                    options = listOf("Yes", "No", "Not sure"),
                    selected = medicineCourseCompleted,
                    onSelect = { medicineCourseCompleted = it }
                )
            }
        }
    }
}
