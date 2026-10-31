package com.varun.malariatracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType

/**
 * Section I – personal details, over three pages:
 *   0. proforma header fields
 *   1. Q1–Q6, ending in "Save & Register" (the worker is REGISTERED from here on)
 *   2. Q7–Q9
 * Once registered, pages 0–1 are read-only. Answers live in the worker's draft.
 */
@Composable
fun DemographicsScreen(
    draftKey: String,
    phone: String,
    patientId: String, // blank until registered
    onRegistered: (patientId: String, name: String) -> Unit,
    onNavigateNext: (name: String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val registered = patientId.isNotBlank()
    var page by rememberDraftPage(draftKey, "demo.page")
    // A returning registered worker resumes after the frozen questions.
    LaunchedEffect(Unit) { if (registered && page < 2) page = 2 }

    // Proforma header
    var nimrId by rememberDraft(draftKey, "demo.nimrId")
    var constructionSite by rememberDraft(draftKey, "demo.constructionSite")
    var phcUhcName by rememberDraft(draftKey, "demo.phcUhcName")
    var phcUhcAddress by rememberDraft(draftKey, "demo.phcUhcAddress")
    var geoLocation by rememberDraft(draftKey, "demo.geoLocation")

    // Q1–Q6
    var name by rememberDraft(draftKey, "demo.name")
    var dob by rememberDraft(draftKey, "demo.dob")
    var age by rememberDraft(draftKey, "demo.age")
    var gender by rememberDraft(draftKey, "demo.gender", "Male")
    var mobile by rememberDraft(draftKey, "demo.mobile", phone)
    var alternateMobile by rememberDraft(draftKey, "demo.altMobile")
    var govIdType by rememberDraft(draftKey, "demo.govIdType", "Aadhaar")
    var otherIdType by rememberDraft(draftKey, "demo.otherIdType")
    var govIdNumber by rememberDraft(draftKey, "demo.govIdNumber")

    // Q7–Q9
    var presentAddress by rememberDraft(draftKey, "demo.presentAddress")
    var permanentAddress by rememberDraft(draftKey, "demo.permanentAddress")
    val languageOptions = listOf("Hindi", "English", "Bengali", "Other")
    var selectedLanguages by rememberDraftSet(draftKey, "demo.languages")
    var otherLanguageText by rememberDraft(draftKey, "demo.otherLanguage")

    var confirmCancel by remember { mutableStateOf(false) }

    fun buildRow(id: String): String {
        val selectedLangs = joinInOrder(languageOptions, selectedLanguages)
        val finalLanguage =
            if ("Other" in selectedLanguages && otherLanguageText.isNotBlank()) "$selectedLangs ($otherLanguageText)"
            else selectedLangs
        val idType = if (govIdType == "Other") withOther(govIdType, otherIdType) else govIdType
        return listOf(
            id, nimrId, constructionSite, phcUhcName, phcUhcAddress, geoLocation,
            name, dob, age, gender, cleanPhone(mobile), cleanPhone(alternateMobile),
            idType, govIdNumber, presentAddress, permanentAddress, finalLanguage
        ).joinToString(",") { CsvHelper.escapeCsv(it) }
    }

    FormPager(
        pageCount = 3,
        page = page,
        onPageChange = { page = it },
        finishLabel = "Continue to Medical History",
        nextLabel = { p -> if (p == 1 && !registered) "Save & Register" else "Next" },
        validatePage = { p ->
            if (p != 1 || registered) null
            else when {
                name.isBlank() -> "Please enter Name"
                !isValidPhone(mobile) -> "Enter a valid 10-digit Mobile Number"
                alternateMobile.isNotBlank() && !isValidPhone(alternateMobile) -> "Alternate number must be 10 digits"
                govIdType == "Aadhaar" && govIdNumber.isNotBlank() && !isValidAadhaar(govIdNumber) -> "Aadhaar number must be 12 digits"
                else -> null
            }
        },
        onPageComplete = { p ->
            if (p == 1 && !registered) {
                // REGISTERED: write the worker and the first part of their proforma now.
                val id = CsvHelper.nextPatientId(context)
                CsvHelper.upsertWorker(context, phone, id, CsvHelper.STATUS_REGISTERED, name)
                CsvHelper.upsertRow(context, "Demographics", CsvHelper.DEMOGRAPHICS_HEADER, id, buildRow(id))
                Toast.makeText(context, "Registered! Patient ID: $id", Toast.LENGTH_LONG).show()
                onRegistered(id, name)
            }
        },
        onFinish = {
            Drafts.put(context, draftKey, "row:Demographics", buildRow(patientId))
            onNavigateNext(name)
        },
        header = {
            Text(text = "User Demographics", style = MaterialTheme.typography.headlineMedium)
            if (registered) Text(text = "Patient ID: $patientId", color = MaterialTheme.colorScheme.secondary)
        },
        footer = {
            if (!registered) {
                TextButton(modifier = Modifier.fillMaxWidth(), onClick = { confirmCancel = true }) {
                    Text("Cancel registration")
                }
            }
        }
    ) { p ->
        val editable = !registered
        if (registered && p < 2) {
            Text(text = "These details are saved and can no longer be changed.", color = MaterialTheme.colorScheme.secondary)
        }
        when (p) {
            0 -> {
                FormField(value = nimrId, onValueChange = { nimrId = it }, label = "NIMR ID", enabled = editable)
                FormField(value = constructionSite, onValueChange = { constructionSite = it }, label = "Construction site name", enabled = editable)
                FormField(value = phcUhcName, onValueChange = { phcUhcName = it }, label = "PHC/UHC name", enabled = editable)
                FormField(value = phcUhcAddress, onValueChange = { phcUhcAddress = it }, label = "PHC/UHC address", enabled = editable)
                FormField(value = geoLocation, onValueChange = { geoLocation = it }, label = "Geographical location", enabled = editable)
            }
            1 -> {
                FormField(value = name, onValueChange = { name = it }, label = "1. Full name (as per ID)", enabled = editable)
                FormField(value = dob, onValueChange = { dob = it }, label = "2. Date of Birth — DD/MM/YYYY", enabled = editable)
                FormField(value = age, onValueChange = { age = it }, label = "Age (in completed years)", keyboardType = KeyboardType.Number, enabled = editable)
                RadioGroup(title = "3. Gender", options = listOf("Male", "Female", "Other"), selected = gender, onSelect = { gender = it }, enabled = editable)
                FormField(value = mobile, onValueChange = { mobile = it }, label = "4. Mobile number", keyboardType = KeyboardType.Phone, enabled = editable)
                FormField(value = alternateMobile, onValueChange = { alternateMobile = it }, label = "5. Alternate contact number (optional)", keyboardType = KeyboardType.Phone, enabled = editable)
                LabeledDropdown(label = "6. Government ID type", options = listOf("Aadhaar", "Voter ID", "Other"), selected = govIdType, onSelect = { govIdType = it }, enabled = editable)
                if (govIdType == "Other") {
                    FormField(value = otherIdType, onValueChange = { otherIdType = it }, label = "Specify ID type", enabled = editable)
                }
                FormField(value = govIdNumber, onValueChange = { govIdNumber = it }, label = "ID Number", enabled = editable)
            }
            else -> {
                FormField(value = presentAddress, onValueChange = { presentAddress = it }, label = "7. Present Address")
                FormField(value = permanentAddress, onValueChange = { permanentAddress = it }, label = "8. Permanent Address (Home Town)")
                CheckboxGroup(
                    title = "9. Preferred language for the app",
                    options = languageOptions,
                    selected = selectedLanguages,
                    onToggle = { opt, on -> selectedLanguages = selectedLanguages.toggle(opt, on) }
                )
                if ("Other" in selectedLanguages) {
                    FormField(value = otherLanguageText, onValueChange = { otherLanguageText = it }, label = "Please specify other language")
                }
            }
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Cancel registration?") },
            text = { Text("Everything entered so far will be deleted.") },
            confirmButton = { TextButton(onClick = { confirmCancel = false; onCancel() }) { Text("Cancel registration") } },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep going") } }
        )
    }
}
