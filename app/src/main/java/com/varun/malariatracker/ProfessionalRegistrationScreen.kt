package com.varun.malariatracker

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * One-time registration for healthcare professionals. Step 1: pick a role. Step 2: a
 * short form (phone + name + optional health centre) saved to Professionals.csv. After
 * registering, the professional logs in from the normal login screen with their phone.
 */
@Composable
fun ProfessionalRegistrationScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var role by rememberSaveable { mutableStateOf("") } // "" while still choosing a role
    // Back from the registration form returns to the role list first.
    BackHandler(enabled = role.isNotBlank()) { role = "" }

    if (role.isBlank()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Healthcare Professional", style = MaterialTheme.typography.headlineMedium)
            Text(text = "Select your role to register", color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(16.dp))
            PROFESSIONAL_ROLES.forEach { r ->
                Button(onClick = { role = r }, modifier = Modifier.fillMaxWidth()) { Text(r) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Back to Login") }
        }
    } else {
        var phone by rememberSaveable { mutableStateOf("") }
        var name by rememberSaveable { mutableStateOf("") }
        var healthCentre by rememberSaveable { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Register — $role", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
            LabeledDropdown(label = "Health centre (if applicable)", options = HEALTH_CENTRES, selected = healthCentre, onSelect = { healthCentre = it })

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val p = cleanPhone(phone)
                    if (!isValidPhone(phone)) {
                        Toast.makeText(context, "Enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show()
                    } else if (name.isBlank()) {
                        Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
                    } else if (CsvHelper.isProfessionalRegistered(context, p, role)) {
                        Toast.makeText(context, "Already registered as $role. Just log in with your phone.", Toast.LENGTH_LONG).show()
                        onDone()
                    } else {
                        CsvHelper.registerProfessional(context, p, role, name, healthCentre)
                        Toast.makeText(context, "Registered! Log in with your phone number.", Toast.LENGTH_LONG).show()
                        onDone()
                    }
                }
            ) {
                Text("Register")
            }

            OutlinedButton(onClick = { role = "" }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        }
    }
}
