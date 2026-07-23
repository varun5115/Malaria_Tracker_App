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

/** Clinician (Medical Officer) questionnaire — saves one row to Clinician.csv. */
@Composable
fun ClinicianScreen(initialName: String = "", initialHealthCentre: String = "", onFinish: () -> Unit) {
    // Bumping formKey re-keys the whole form below, resetting every field — used by "Clear form".
    var formKey by rememberSaveable { mutableStateOf(0) }
    key(formKey) {
        val context = LocalContext.current

        // General & administrative
        var officerName by rememberSaveable { mutableStateOf(initialName) }
        var healthCentre by rememberSaveable { mutableStateOf(initialHealthCentre) }
        var address by rememberSaveable { mutableStateOf("") }
        var patientName by rememberSaveable { mutableStateOf("") }
        var patientId by rememberSaveable { mutableStateOf("") }

        // Clinical assessment
        var bodyTemp by rememberSaveable { mutableStateOf("") }
        var feverDuration by rememberSaveable { mutableStateOf("") }
        var symptoms by rememberSaveable { mutableStateOf(setOf<String>()) }
        var otherSymptom by rememberSaveable { mutableStateOf("") }
        var generalCondition by rememberSaveable { mutableStateOf("") }
        var bloodPressure by rememberSaveable { mutableStateOf("") }
        var comorbidities by rememberSaveable { mutableStateOf(setOf<String>()) }
        var otherComorbidity by rememberSaveable { mutableStateOf("") }
        var treatmentPrescribed by rememberSaveable { mutableStateOf("") }
        var admitted by rememberSaveable { mutableStateOf("") }
        var admittedType by rememberSaveable { mutableStateOf("") }
        var otherAdmittedType by rememberSaveable { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Clinician Questionnaire", style = MaterialTheme.typography.headlineMedium)

            Text(text = "General & Administrative Information", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = officerName, onValueChange = { officerName = it }, label = { Text("Name of the Medical Officer") }, modifier = Modifier.fillMaxWidth())
            LabeledDropdown(label = "Health centre name", options = HEALTH_CENTRES, selected = healthCentre, onSelect = { healthCentre = it })
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = patientName, onValueChange = { patientName = it }, label = { Text("Patient Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = patientId, onValueChange = { patientId = it }, label = { Text("Patient ID") }, modifier = Modifier.fillMaxWidth())

            HorizontalDivider()
            Text(text = "Clinical Assessment and Treatment", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(value = bodyTemp, onValueChange = { bodyTemp = it }, label = { Text("1. Body temperature (°C)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())

            RadioGroup(
                title = "2. Duration of fever (as reported)",
                options = listOf("< 24 hours", "1–3 days", "4–7 days", "> 7 days"),
                selected = feverDuration,
                onSelect = { feverDuration = it }
            )

            CheckboxGroup(
                title = "3. Key symptoms (multiple select)",
                options = listOf("Chills / rigors", "Sweating", "Headache", "Nausea / vomiting", "Abdominal pain", "Joint / body aches", "Weakness / fatigue"),
                selected = symptoms,
                onToggle = { opt, on -> symptoms = symptoms.toggle(opt, on) }
            )
            OutlinedTextField(value = otherSymptom, onValueChange = { otherSymptom = it }, label = { Text("Other symptoms (specify)") }, modifier = Modifier.fillMaxWidth())

            RadioGroup(
                title = "4. General condition",
                options = listOf("Stable", "Moderately ill", "Severe"),
                selected = generalCondition,
                onSelect = { generalCondition = it }
            )

            OutlinedTextField(value = bloodPressure, onValueChange = { bloodPressure = it }, label = { Text("5. Blood pressure (mmHg)") }, modifier = Modifier.fillMaxWidth())

            CheckboxGroup(
                title = "6. Co-morbidities",
                options = listOf("Diabetes", "Hypertension", "None"),
                selected = comorbidities,
                onToggle = { opt, on -> comorbidities = comorbidities.toggle(opt, on) }
            )
            OutlinedTextField(value = otherComorbidity, onValueChange = { otherComorbidity = it }, label = { Text("Other co-morbidity (specify)") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = treatmentPrescribed, onValueChange = { treatmentPrescribed = it }, label = { Text("7. Treatment Prescribed") }, modifier = Modifier.fillMaxWidth())

            RadioGroup(
                title = "8. Patient admitted",
                options = listOf("Yes", "No"),
                selected = admitted,
                onSelect = { admitted = it }
            )
            if (admitted == "Yes") {
                RadioGroup(
                    title = "If Yes",
                    options = listOf("Pf", "Pv", "Other"),
                    selected = admittedType,
                    onSelect = { admittedType = it }
                )
                if (admittedType == "Other") {
                    OutlinedTextField(value = otherAdmittedType, onValueChange = { otherAdmittedType = it }, label = { Text("Specify") }, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (patientName.isBlank() || patientId.isBlank()) {
                        Toast.makeText(context, "Please fill in Patient Name and Patient ID", Toast.LENGTH_SHORT).show()
                    } else {
                        val symptomsValue = withOther(symptoms.joined(), otherSymptom)
                        val comorbiditiesValue = withOther(comorbidities.joined(), otherComorbidity)
                        val admittedTypeValue = if (admitted == "Yes") withOther(admittedType, otherAdmittedType) else ""

                        val header = "MedicalOfficer,HealthCentre,Address,PatientName,PatientID,BodyTempC,FeverDuration,KeySymptoms,GeneralCondition,BloodPressure,Comorbidities,TreatmentPrescribed,Admitted,AdmittedType"
                        val rowData = listOf(
                            officerName, healthCentre, address, patientName, patientId,
                            bodyTemp, feverDuration, symptomsValue, generalCondition, bloodPressure,
                            comorbiditiesValue, treatmentPrescribed, admitted, admittedTypeValue
                        ).joinToString(",") { CsvHelper.escapeCsv(it) }

                        CsvHelper.saveToCsv(context, "Clinician", header, rowData)
                        Toast.makeText(context, "Clinician record saved!", Toast.LENGTH_SHORT).show()
                        onFinish()
                    }
                }
            ) {
                Text("Save Record")
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { formKey++ }
            ) {
                Text("Clear form")
            }
        }
    }
}
