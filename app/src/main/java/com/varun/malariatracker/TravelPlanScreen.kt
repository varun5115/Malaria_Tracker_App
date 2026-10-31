package com.varun.malariatracker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * Section V – Travel in and outside Goa (planned/future travel). The row is kept in the
 * draft and written to TravelPlan.csv when the proforma is completed.
 */
@Composable
fun TravelPlanScreen(draftKey: String, patientId: String, patientName: String, onNavigateNext: () -> Unit) {
    val context = LocalContext.current

    var planningToVisit by rememberDraft(draftKey, "plan.planning")
    var departureDate by rememberDraft(draftKey, "plan.departure")
    var returnDate by rememberDraft(draftKey, "plan.return")
    var placesVisited by rememberDraft(draftKey, "plan.places")
    var feverOrMalaria by rememberDraft(draftKey, "plan.fever")
    var treatmentGiven by rememberDraft(draftKey, "plan.treatment")
    var fullCourseCompleted by rememberDraft(draftKey, "plan.fullCourse")
    var placeOfTreatment by rememberDraft(draftKey, "plan.placeOfTreatment")

    FormPager(
        pageCount = 1,
        page = 0,
        onPageChange = {},
        finishLabel = "Continue",
        onFinish = {
            // Answers to hidden questions are saved blank.
            val planning = planningToVisit == "Yes"
            val fever = planning && feverOrMalaria == "Yes"
            val treated = fever && treatmentGiven == "Yes"
            val rowData = listOf(
                patientId, planningToVisit,
                if (planning) departureDate else "", if (planning) returnDate else "", if (planning) placesVisited else "",
                if (planning) feverOrMalaria else "", if (fever) treatmentGiven else "",
                if (treated) fullCourseCompleted else "", if (fever) placeOfTreatment else ""
            ).joinToString(",") { CsvHelper.escapeCsv(it) }
            Drafts.put(context, draftKey, "row:TravelPlan", rowData)
            onNavigateNext()
        },
        header = {
            Text(text = "Section V – Travel in and outside Goa", style = MaterialTheme.typography.headlineMedium)
            Text(text = "Patient: $patientName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(text = "ID: $patientId", color = MaterialTheme.colorScheme.secondary)
        }
    ) {
        RadioGroup(
            title = "Are you planning to visit outside Goa?",
            options = listOf("Yes", "No"),
            selected = planningToVisit,
            onSelect = { planningToVisit = it }
        )
        if (planningToVisit == "Yes") {
            FormField(value = departureDate, onValueChange = { departureDate = it }, label = "If yes, Date of departure (Out of Goa) — DD/MM/YYYY")
            FormField(value = returnDate, onValueChange = { returnDate = it }, label = "Date of return back (Into Goa) — DD/MM/YYYY")
            FormField(value = placesVisited, onValueChange = { placesVisited = it }, label = "Place(s) / State(s) visited")
            RadioGroup(
                title = "During this travel outside Goa, did you have fever or were you diagnosed with malaria?",
                options = listOf("Yes", "No"),
                selected = feverOrMalaria,
                onSelect = { feverOrMalaria = it }
            )
            if (feverOrMalaria == "Yes") {
                RadioGroup(
                    title = "If yes (fever/malaria), was treatment given?",
                    options = listOf("Yes", "No"),
                    selected = treatmentGiven,
                    onSelect = { treatmentGiven = it }
                )
                if (treatmentGiven == "Yes") {
                    RadioGroup(
                        title = "If treatment given, was the full course of medicines completed?",
                        options = listOf("Yes", "No"),
                        selected = fullCourseCompleted,
                        onSelect = { fullCourseCompleted = it }
                    )
                }
                FormField(value = placeOfTreatment, onValueChange = { placeOfTreatment = it }, label = "Place of treatment (if any)")
            }
        }
    }
}
