package com.varun.malariatracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Section I – personal details, over two pages. Nothing is saved here: the row is
 * handed to MainActivity and written only when the worker finishes the intake.
 */
@Composable
fun DemographicsScreen(initialId: String, initialMobile: String = "", onNavigateNext: (String, String, String) -> Unit) {
    val id = initialId
    var page by rememberSaveable { mutableStateOf(0) }

    var name by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("Male") }
    var mobile by rememberSaveable { mutableStateOf(initialMobile) }
    var alternateMobile by rememberSaveable { mutableStateOf("") }

    var govIdType by rememberSaveable { mutableStateOf("Aadhaar") }
    var govIdNumber by rememberSaveable { mutableStateOf("") }
    var presentAddress by rememberSaveable { mutableStateOf("") }
    var permanentAddress by rememberSaveable { mutableStateOf("") }

    val languageOptions = listOf("Hindi", "English", "Bengali", "Other")
    var selectedLanguages by rememberSaveable { mutableStateOf(setOf<String>()) }
    var otherLanguageText by rememberSaveable { mutableStateOf("") }

    val idOptions = listOf("Aadhaar", "Voter ID", "Passport", "Driving License")

    FormPager(
        pageCount = 2,
        page = page,
        onPageChange = { page = it },
        finishLabel = "Continue to Medical History",
        validatePage = { p ->
            when (p) {
                0 -> when {
                    name.isBlank() -> "Please enter Name"
                    !isValidPhone(mobile) -> "Enter a valid 10-digit Mobile Number"
                    alternateMobile.isNotBlank() && !isValidPhone(alternateMobile) -> "Alternate number must be 10 digits"
                    else -> null
                }
                else -> {
                    val isAadhaar = govIdType.contains("Aadha", ignoreCase = true)
                    if (isAadhaar && govIdNumber.isNotBlank() && !isValidAadhaar(govIdNumber)) "Aadhaar number must be 12 digits" else null
                }
            }
        },
        onFinish = {
            // Package the row; MainActivity saves it at the end of the intake.
            val selectedLangs = joinInOrder(languageOptions, selectedLanguages)
            val finalLanguage =
                if ("Other" in selectedLanguages && otherLanguageText.isNotBlank()) "$selectedLangs ($otherLanguageText)"
                else selectedLangs

            val rowData = listOf(
                id, name, age, gender, cleanPhone(mobile), cleanPhone(alternateMobile),
                govIdType, govIdNumber, presentAddress, permanentAddress, finalLanguage
            ).joinToString(",") { CsvHelper.escapeCsv(it) }

            onNavigateNext(id, name, rowData)
        },
        header = { Text(text = "User Demographics", style = MaterialTheme.typography.headlineMedium) }
    ) { p ->
        when (p) {
            0 -> {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("1. Full name (as per ID)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("2. Age (in completed years)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                Text(text = "3. Gender", style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf("Male", "Female", "Other").forEach { option ->
                        RadioButton(selected = gender == option, onClick = { gender = option })
                        Text(option, modifier = Modifier.padding(end = 12.dp))
                    }
                }

                OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("4. Mobile Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = alternateMobile, onValueChange = { alternateMobile = it }, label = { Text("5. Alternate contact number (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
            }
            else -> {
                Text(text = "6. Government ID Details", style = MaterialTheme.typography.bodyLarge)
                LabeledDropdown(label = "Type", options = idOptions, selected = govIdType, onSelect = { govIdType = it })
                OutlinedTextField(value = govIdNumber, onValueChange = { govIdNumber = it }, label = { Text("ID Number") }, modifier = Modifier.fillMaxWidth())

                OutlinedTextField(value = presentAddress, onValueChange = { presentAddress = it }, label = { Text("7. Present Address") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = permanentAddress, onValueChange = { permanentAddress = it }, label = { Text("8. Permanent Address (Home Town)") }, modifier = Modifier.fillMaxWidth())

                CheckboxGroup(
                    title = "9. Preferred Language for the app",
                    options = languageOptions,
                    selected = selectedLanguages,
                    onToggle = { opt, on -> selectedLanguages = selectedLanguages.toggle(opt, on) }
                )
                if ("Other" in selectedLanguages) {
                    OutlinedTextField(value = otherLanguageText, onValueChange = { otherLanguageText = it }, label = { Text("Please specify other language") }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
