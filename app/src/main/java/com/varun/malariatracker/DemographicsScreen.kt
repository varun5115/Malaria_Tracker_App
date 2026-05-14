package com.varun.malariatracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemographicsScreen(initialId: String, onNavigateNext: (String, String, String) -> Unit) {
    var id by remember { mutableStateOf(initialId) }
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") } // Default selection
    var mobile by remember { mutableStateOf("") }
    var alternateMobile by remember { mutableStateOf("") }

    var govIdType by remember { mutableStateOf("Aadhaar") } // Default selection
    var govIdNumber by remember { mutableStateOf("") }

    //var language by remember { mutableStateOf("") }
    var presentAddress by remember { mutableStateOf("") }
    var permanentAddress by remember { mutableStateOf("") }

    val selectedLanguages = remember { mutableStateMapOf<String, Boolean>() }
    var otherLanguageText by remember { mutableStateOf("") }
    val languageOptions = listOf("Hindi", "English", "Bengali", "Other")

    val context = LocalContext.current

    var expanded by remember { mutableStateOf(false) }

    val idOptions = listOf(
        "Aadhar",
        "Voter ID",
        "Passport",
        "Driving License"
    )



    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "User Demographics", style = MaterialTheme.typography.headlineMedium)

//        OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("Unique Patient ID") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("1. Full name (as per ID)") }, modifier = Modifier.fillMaxWidth())

        // Age is restricted to a numeric keyboard
        OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("2. Age (in completed years)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

        // NEW: Radio Buttons for Gender
        Text(text = "3. Gender", style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = gender == "Male", onClick = { gender = "Male" })
            Text("Male", modifier = Modifier.padding(end = 12.dp))

            RadioButton(selected = gender == "Female", onClick = { gender = "Female" })
            Text("Female", modifier = Modifier.padding(end = 12.dp))

            RadioButton(selected = gender == "Other", onClick = { gender = "Other" })
            Text("Other")
        }

        OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("4. Mobile Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = alternateMobile, onValueChange = { alternateMobile = it }, label = { Text("5. Alternate contact number (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())

//        Text(text = "6. Government ID type", style = MaterialTheme.typography.bodyLarge)
//        Row(verticalAlignment = Alignment.CenterVertically) {
//            RadioButton(selected = govIdType == "Aadhaar", onClick = { govIdType = "Aadhaar" })
//            Text("Aadhaar", modifier = Modifier.padding(end = 8.dp))
//            RadioButton(selected = govIdType == "Voter ID", onClick = { govIdType = "Voter ID" })
//            Text("Voter ID", modifier = Modifier.padding(end = 8.dp))
//            RadioButton(selected = govIdType == "Other", onClick = { govIdType = "Other" })
//            Text("Other")
//        }
        Text(text = "6. Government ID Details", style = MaterialTheme.typography.bodyLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1.2f)) {
                OutlinedTextField(value = govIdType, onValueChange = {}, readOnly = true, label = { Text("Type") }, trailingIcon = { TrailingIcon(expanded = expanded) })
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    idOptions.forEach { opt ->
                        DropdownMenuItem(text = { Text(opt) }, onClick = { govIdType = opt; expanded = false })
                    }
                }
                IconButton(onClick = { expanded = true }, modifier = Modifier.matchParentSize()) {}
            }
            OutlinedTextField(value = govIdNumber, onValueChange = { govIdNumber = it }, label = { Text("ID Number") }, modifier = Modifier.weight(2f))
        }

        OutlinedTextField(value = presentAddress, onValueChange = { presentAddress = it }, label = { Text("7. Present Address") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = permanentAddress, onValueChange = { permanentAddress = it }, label = { Text("8. Permanent Address (Home Town)") }, modifier = Modifier.fillMaxWidth())

        //OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
        //OutlinedTextField(value = language, onValueChange = { language = it }, label = { Text("9. Preferred Language for the app") }, modifier = Modifier.fillMaxWidth())

        Text(
            text = "9. Preferred Language for the app",
            style = MaterialTheme.typography.bodyLarge
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
        ) {

            languageOptions.forEach { lang ->

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Checkbox(
                        checked = selectedLanguages[lang] == true,
                        onCheckedChange = { isChecked ->
                            selectedLanguages[lang] = isChecked
                        }
                    )

                    Text(text = lang)
                }
            }

            // Show textbox only if "Other" is checked
            if (selectedLanguages["Other"] == true) {

                OutlinedTextField(
                    value = otherLanguageText,
                    onValueChange = {
                        otherLanguageText = it
                    },
                    label = {
                        Text("Please specify other language")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }



        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (id.isBlank() || name.isBlank()) {
                    Toast.makeText(context, "Please fill in ID and Name", Toast.LENGTH_SHORT).show()
                } else if (CsvHelper.doesIdExist(context, id)) {
                    Toast.makeText(context, "Error: This ID already exists!", Toast.LENGTH_SHORT).show()
                } else {
                    // WE NO LONGER SAVE HERE! We just package the data string.
                    //val rowData = "$id,$name,$age,$gender,$mobile,$address,$language"
                    //onNavigateNext(id, name, rowData)
                    val selectedLangs = selectedLanguages
                        .filter { it.value }
                        .keys
                        .joinToString("; ")

                    val finalLanguage =
                        if (selectedLanguages["Other"] == true &&
                            otherLanguageText.isNotBlank()
                        ) {
                            "$selectedLangs ($otherLanguageText)"
                        } else {
                            selectedLangs
                        }

                    val rowData = "${CsvHelper.escapeCsv(id)}," + "${CsvHelper.escapeCsv(name)}," + "${CsvHelper.escapeCsv(age)}," + "${CsvHelper.escapeCsv(gender)}," + "${CsvHelper.escapeCsv(mobile)}," + "${CsvHelper.escapeCsv(alternateMobile)}," + "${CsvHelper.escapeCsv(govIdType)}," + "${CsvHelper.escapeCsv(govIdNumber)}," + "${CsvHelper.escapeCsv(presentAddress)}," + "${CsvHelper.escapeCsv(permanentAddress)}," + "${CsvHelper.escapeCsv(finalLanguage)}"

//                    val rowData = "$id,$name,$age,$gender,$mobile,$alternateMobile,$govIdType,$presentAddress,$permanentAddress,$language"
                    onNavigateNext(id, name, rowData)
                }
            }
        ) {
            Text("Continue to Medical History")
        }
    }
}