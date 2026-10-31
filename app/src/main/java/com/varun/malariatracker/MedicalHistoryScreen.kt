package com.varun.malariatracker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType

/**
 * Section II – past history of malaria. Q2–Q8 are about the past episode, so they are
 * shown (and saved) only when Q1 is "Yes". The last page holds the four general
 * questions (mobile brand, qualification, repellent, mosquito net), which always show.
 */
@Composable
fun MedicalHistoryScreen(draftKey: String, patientId: String, patientName: String, onNavigateNext: () -> Unit) {
    val context = LocalContext.current
    var page by rememberDraftPage(draftKey, "med.page")

    var diagnosedMalaria by rememberDraft(draftKey, "med.diagnosed")
    var malariaTimes by rememberDraft(draftKey, "med.times")
    var lastDetection by rememberDraft(draftKey, "med.lastDetection")
    val placeOptions = listOf("UHC / PHC", "Private clinic", "Govt. Hospital", "Pharmacy")
    var treatmentPlaces by rememberDraftSet(draftKey, "med.places")
    var otherTreatmentPlace by rememberDraft(draftKey, "med.otherPlace")
    var diagnosisTest by rememberDraft(draftKey, "med.test")
    var sameDayReport by rememberDraft(draftKey, "med.sameDay")
    val methodOptions = listOf("RDT (rapid test)", "Microscopy", "Not sure")
    var reportMethods by rememberDraftSet(draftKey, "med.methods")
    var medicationPeriod by rememberDraft(draftKey, "med.medication")
    var medicineCourseCompleted by rememberDraft(draftKey, "med.course")
    var mobileBrand by rememberDraft(draftKey, "med.mobileBrand")
    var qualification by rememberDraft(draftKey, "med.qualification")
    var repellent by rememberDraft(draftKey, "med.repellent")
    var mosquitoNet by rememberDraft(draftKey, "med.mosquitoNet")

    val diagnosed = diagnosedMalaria == "Yes"
    val pageCount = if (diagnosed) 3 else 2
    val generalPage = pageCount - 1

    FormPager(
        pageCount = pageCount,
        page = page,
        onPageChange = { page = it },
        finishLabel = "Continue to Travel History",
        validatePage = { p -> if (p == 0 && diagnosedMalaria.isBlank()) "Please answer question 1" else null },
        onFinish = {
            // Answers to hidden questions are saved blank, never stale.
            fun ifDiagnosed(v: String) = if (diagnosed) v else ""
            val tested = diagnosed && diagnosisTest == "Yes"
            val sameDay = tested && sameDayReport == "Yes"
            val places = ifDiagnosed(withOther(joinInOrder(placeOptions, treatmentPlaces), otherTreatmentPlace))
            val methods = if (sameDay) joinInOrder(methodOptions, reportMethods) else ""

            val rowData = listOf(
                patientId, diagnosedMalaria,
                ifDiagnosed(malariaTimes), ifDiagnosed(lastDetection), places,
                ifDiagnosed(diagnosisTest), if (tested) sameDayReport else "", methods,
                ifDiagnosed(medicationPeriod), ifDiagnosed(medicineCourseCompleted),
                mobileBrand, qualification, repellent, mosquitoNet
            ).joinToString(",") { CsvHelper.escapeCsv(it) }

            Drafts.put(context, draftKey, "row:MedicalHistory", rowData)
            onNavigateNext()
        },
        header = {
            Text(text = "Medical History", style = MaterialTheme.typography.headlineMedium)
            Text(text = "Patient: $patientName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(text = "ID: $patientId", color = MaterialTheme.colorScheme.secondary)
        }
    ) { p ->
        when {
            p == generalPage -> {
                FormField(value = mobileBrand, onValueChange = { mobileBrand = it }, label = "Mobile Brand")
                FormField(value = qualification, onValueChange = { qualification = it }, label = "Qualification")
                RadioGroup(title = "Repellent", options = listOf("Yes", "No"), selected = repellent, onSelect = { repellent = it })
                RadioGroup(title = "Mosquito Net", options = listOf("Yes", "No"), selected = mosquitoNet, onSelect = { mosquitoNet = it })
            }
            p == 0 -> {
                RadioGroup(
                    title = "1. Diagnosed with malaria?",
                    options = listOf("Yes", "No", "Don't know"),
                    selected = diagnosedMalaria,
                    onSelect = { diagnosedMalaria = it }
                )
                if (diagnosed) {
                    FormField(value = malariaTimes, onValueChange = { malariaTimes = it }, label = "2. If yes, how many times in the last 2 years?", keyboardType = KeyboardType.Number)
                    RadioGroup(
                        title = "3. Date of last malaria detection (month/year)",
                        options = listOf("≤ 1 Year", "1–2 Years", "Don't know"),
                        selected = lastDetection,
                        onSelect = { lastDetection = it }
                    )
                    CheckboxGroup(
                        title = "4. Place where you received treatment for last malaria",
                        options = placeOptions,
                        selected = treatmentPlaces,
                        onToggle = { opt, on -> treatmentPlaces = treatmentPlaces.toggle(opt, on) }
                    )
                    FormField(value = otherTreatmentPlace, onValueChange = { otherTreatmentPlace = it }, label = "Others")
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
                        title = "6. Did you receive your test report on the same day?",
                        options = listOf("Yes", "No"),
                        selected = sameDayReport,
                        onSelect = { sameDayReport = it }
                    )
                    if (sameDayReport == "Yes") {
                        CheckboxGroup(
                            title = "If yes:",
                            options = methodOptions,
                            selected = reportMethods,
                            onToggle = { opt, on -> reportMethods = reportMethods.toggle(opt, on) }
                        )
                    }
                }
                RadioGroup(
                    title = "7. Medication / Treatment given period",
                    options = listOf("3 Days", "14 Days (P.vivax)", "28 Days follow-up", "Not informed / Don't know"),
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
