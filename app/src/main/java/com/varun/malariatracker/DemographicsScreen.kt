package com.varun.malariatracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun DemographicsScreen(initialId: String, onNavigateNext: (String, String, String) -> Unit) {
    var id by remember { mutableStateOf(initialId) }
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") } // Default selection
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Patient Demographics", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("Unique Patient ID") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())

        // Age is restricted to a numeric keyboard
        OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

        // NEW: Radio Buttons for Gender
        Text(text = "Gender", style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = gender == "Male", onClick = { gender = "Male" })
            Text("Male", modifier = Modifier.padding(end = 12.dp))

            RadioButton(selected = gender == "Female", onClick = { gender = "Female" })
            Text("Female", modifier = Modifier.padding(end = 12.dp))

            RadioButton(selected = gender == "Other", onClick = { gender = "Other" })
            Text("Other")
        }

        OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = language, onValueChange = { language = it }, label = { Text("Preferred Language") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (id.isBlank() || name.isBlank()) {
                    Toast.makeText(context, "Please fill in ID and Name", Toast.LENGTH_SHORT).show()
                } else if (CsvHelper.doesIdExist(id)) {
                    Toast.makeText(context, "Error: This ID already exists!", Toast.LENGTH_SHORT).show()
                } else {
                    // WE NO LONGER SAVE HERE! We just package the data string.
                    val rowData = "$id,$name,$age,$gender,$mobile,$address,$language"
                    onNavigateNext(id, name, rowData)
                }
            }
        ) {
            Text("Continue to Medical History")
        }
    }
}