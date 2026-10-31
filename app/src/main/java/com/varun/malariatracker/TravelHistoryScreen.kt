package com.varun.malariatracker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/** Section IV – migration and travel history. The row is kept in the draft until the proforma is completed. */
@Composable
fun TravelHistoryScreen(draftKey: String, patientId: String, patientName: String, onNavigateNext: () -> Unit) {
    val context = LocalContext.current
    var stayDuration by rememberDraft(draftKey, "travel.stay")
    var travelledOutsideGoa by rememberDraft(draftKey, "travel.travelled")
    var visitedStates by rememberDraft(draftKey, "travel.states")
    var homeStayDuration by rememberDraft(draftKey, "travel.homeStay")
    var feverBeforeComing by rememberDraft(draftKey, "travel.feverBefore")

    FormPager(
        pageCount = 1,
        page = 0,
        onPageChange = {},
        finishLabel = "Continue",
        onFinish = {
            val states = if (travelledOutsideGoa == "Yes") visitedStates else ""
            val rowData = listOf(
                patientId, stayDuration, travelledOutsideGoa, states, homeStayDuration, feverBeforeComing
            ).joinToString(",") { CsvHelper.escapeCsv(it) }
            Drafts.put(context, draftKey, "row:TravelHistory", rowData)
            onNavigateNext()
        },
        header = {
            Text(text = "Travel History", style = MaterialTheme.typography.headlineMedium)
            Text(text = "Patient: $patientName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(text = "ID: $patientId", color = MaterialTheme.colorScheme.secondary)
        }
    ) {
        RadioGroup(
            title = "1. How long have you been in Goa this time?",
            options = listOf("≤ 1 month", "1–6 month", "≥ 6 month"),
            selected = stayDuration,
            onSelect = { stayDuration = it }
        )
        RadioGroup(
            title = "2. Travelled outside Goa in the last 3 months?",
            options = listOf("Yes", "No"),
            selected = travelledOutsideGoa,
            onSelect = { travelledOutsideGoa = it }
        )
        if (travelledOutsideGoa == "Yes") {
            FormField(value = visitedStates, onValueChange = { visitedStates = it }, label = "3. If yes, which state(s) did you visit?")
        }
        RadioGroup(
            title = "4. Duration of last stay in home state",
            options = listOf("7 days", "14 days", "≤ 1 Month"),
            selected = homeStayDuration,
            onSelect = { homeStayDuration = it }
        )
        RadioGroup(
            title = "5. Did you have fever or malaria in the last 3 months before coming to Goa?",
            options = listOf("Yes", "No", "Don't know"),
            selected = feverBeforeComing,
            onSelect = { feverBeforeComing = it }
        )
    }
}
