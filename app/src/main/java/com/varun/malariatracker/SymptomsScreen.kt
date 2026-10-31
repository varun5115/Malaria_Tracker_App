package com.varun.malariatracker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType

/**
 * Section III – present clinical details. Builds one Symptoms row and hands it to
 * [onFinish]; MainActivity saves it (completing the proforma, or as a new visit).
 */
@Composable
fun SymptomsScreen(draftKey: String, patientId: String, patientName: String, onFinish: (rowData: String) -> Unit) {
    var page by rememberDraftPage(draftKey, "sym.page")

    var feverToday by rememberDraft(draftKey, "sym.fever")
    var temperature by rememberDraft(draftKey, "sym.temperature")
    var feverDuration by rememberDraft(draftKey, "sym.duration")
    val symptomOptions = listOf("Chills/shivering", "Headache", "Sweating", "Body ache", "Nausea/vomiting", "Weakness/fatigue", "No other symptoms")
    var associatedSymptoms by rememberDraftSet(draftKey, "sym.symptoms")
    var illnessDate by rememberDraft(draftKey, "sym.onset")
    var breathingDifficulty by rememberDraft(draftKey, "sym.breathing")
    var ableForTesting by rememberDraft(draftKey, "sym.testing")

    FormPager(
        pageCount = 2,
        page = page,
        onPageChange = { page = it },
        finishLabel = "Save & Finish",
        validatePage = { p -> if (p == 0 && associatedSymptoms.isEmpty()) "Please select at least one symptom" else null },
        onFinish = {
            val timestamp = System.currentTimeMillis()
            val temp = if (feverToday == "Yes") temperature else ""
            val rowData = listOf(
                "${patientId}_$timestamp", timestamp.toString(), patientId, feverToday, temp, feverDuration,
                joinInOrder(symptomOptions, associatedSymptoms), illnessDate, breathingDifficulty, ableForTesting
            ).joinToString(",") { CsvHelper.escapeCsv(it) }
            onFinish(rowData)
        },
        header = {
            Text(text = "Symptoms Entry", style = MaterialTheme.typography.headlineMedium)
            Text(text = "Patient: $patientName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(text = "ID: $patientId", color = MaterialTheme.colorScheme.secondary)
        }
    ) { p ->
        when (p) {
            0 -> {
                RadioGroup(
                    title = "1. Do you have fever today?",
                    options = listOf("Yes", "No", "Not sure"),
                    selected = feverToday,
                    onSelect = { feverToday = it }
                )
                if (feverToday == "Yes") {
                    FormField(value = temperature, onValueChange = { temperature = it }, label = "2. If yes, measured temperature (if known)", keyboardType = KeyboardType.Decimal)
                }
                RadioGroup(
                    title = "3. Duration of fever",
                    options = listOf("24 hours", "1–3 days", "More than 3 days", "Not required"),
                    selected = feverDuration,
                    onSelect = { feverDuration = it }
                )
                CheckboxGroup(
                    title = "4. Associated Symptoms",
                    options = symptomOptions,
                    selected = associatedSymptoms,
                    onToggle = { opt, on -> associatedSymptoms = associatedSymptoms.toggle(opt, on) }
                )
            }
            else -> {
                FormField(value = illnessDate, onValueChange = { illnessDate = it }, label = "5. Time of onset of current illness (date)")
                RadioGroup(
                    title = "6. Any breathing difficulty, chest pain or confusion?",
                    options = listOf("Yes", "No"),
                    selected = breathingDifficulty,
                    onSelect = { breathingDifficulty = it }
                )
                RadioGroup(
                    title = "7. Are you able to go for testing to the health centre today if advised?",
                    options = listOf("Yes", "No", "Not required"),
                    selected = ableForTesting,
                    onSelect = { ableForTesting = it }
                )
            }
        }
    }
}
