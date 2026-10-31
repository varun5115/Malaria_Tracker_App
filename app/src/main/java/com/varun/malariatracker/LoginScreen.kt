package com.varun.malariatracker

import android.widget.Toast
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
 * Unified login. Everyone enters their phone number. The app looks the phone up in
 * both index files and decides:
 *   - no match          -> brand-new worker, start signup
 *   - exactly one match -> go straight in (workers never see a chooser)
 *   - more than one      -> show a "Sign in as..." chooser (dual-role / multi-role only)
 *
 * The "Healthcare Professional Login / Registration" button is a one-time registration;
 * afterwards professionals log in here with their phone like everyone else.
 */
@Composable
fun LoginScreen(
    onNewWorker: (phone: String) -> Unit,
    onReturningWorker: (phone: String, worker: CsvHelper.WorkerRecord) -> Unit,
    onProfessionalLogin: (role: String, phone: String) -> Unit,
    onRegisterProfessional: () -> Unit
) {
    val context = LocalContext.current
    var phone by rememberSaveable { mutableStateOf("") }
    // Non-empty only when there is more than one identity to choose between.
    var chooser by rememberSaveable { mutableStateOf(listOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "M3 Platform", style = MaterialTheme.typography.headlineLarge)
        Text(text = "Enter phone number to begin.", color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; chooser = emptyList() },
            label = { Text("Phone Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (!isValidPhone(phone)) {
                    Toast.makeText(context, "Enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val p = cleanPhone(phone)
                val worker = CsvHelper.findWorkerByPhone(context, p)
                val roles = CsvHelper.findProfessionalRoles(context, p)
                val matches = buildList {
                    if (worker != null) add("Worker")
                    addAll(roles)
                }
                when {
                    matches.isEmpty() -> onNewWorker(p)
                    matches.size == 1 -> routeTo(matches.first(), worker, p, onReturningWorker, onProfessionalLogin)
                    else -> chooser = matches // ambiguous -> let them pick
                }
            }
        ) {
            Text("Login / Start")
        }

        // Chooser shown only when a phone maps to more than one identity.
        if (chooser.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Sign in as:", style = MaterialTheme.typography.titleMedium)
            chooser.forEach { role ->
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val p = cleanPhone(phone)
                        routeTo(role, CsvHelper.findWorkerByPhone(context, p), p, onReturningWorker, onProfessionalLogin)
                    }
                ) { Text(if (role == "Worker") "Worker (patient)" else role) }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onRegisterProfessional
        ) {
            Text("Healthcare Professional Login / Registration")
        }
    }
}

private fun routeTo(
    role: String,
    worker: CsvHelper.WorkerRecord?,
    phone: String,
    onReturningWorker: (String, CsvHelper.WorkerRecord) -> Unit,
    onProfessionalLogin: (String, String) -> Unit
) {
    if (role == "Worker") {
        if (worker != null) onReturningWorker(phone, worker)
    } else {
        onProfessionalLogin(role, phone)
    }
}
