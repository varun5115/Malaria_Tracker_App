package com.varun.malariatracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType

/** Clinician (Medical Officer) questionnaire — saves one row to Clinician.csv. */
@Composable
fun ClinicianScreen(draftKey: String, initialName: String = "", initialHealthCentre: String = "", onFinish: () -> Unit) {
    // Bumping formKey re-keys the whole form below, resetting every field — used by "Clear form".
    var formKey by rememberSaveable { mutableStateOf(0) }
    key(formKey) {
        val context = LocalContext.current
        var page by rememberDraftPage(draftKey)

        // General & administrative
        var officerName by rememberDraft(draftKey, "officerName", initialName)
        var healthCentre by rememberDraft(draftKey, "healthCentre", initialHealthCentre)
        var address by rememberDraft(draftKey, "address")
        var patientName by rememberDraft(draftKey, "patientName")
        var patientMobile by rememberDraft(draftKey, "patientMobile")

        // Clinical assessment
        var bodyTemp by rememberDraft(draftKey, "bodyTemp")
        var feverDuration by rememberDraft(draftKey, "feverDuration")
        var symptoms by rememberDraftSet(draftKey, "symptoms")
        var otherSymptom by rememberDraft(draftKey, "otherSymptom")
        var generalCondition by rememberDraft(draftKey, "generalCondition")
        var bloodPressure by rememberDraft(draftKey, "bloodPressure")
        var comorbidities by rememberDraftSet(draftKey, "comorbidities")
        var otherComorbidity by rememberDraft(draftKey, "otherComorbidity")
        var treatmentPrescribed by rememberDraft(draftKey, "treatmentPrescribed")
        var admitted by rememberDraft(draftKey, "admitted")
        // A. If admitted = Yes
        var admissionSpecies by rememberDraft(draftKey, "admissionSpecies")
        var admissionOtherSpecies by rememberDraft(draftKey, "admissionOtherSpecies")
        var admissionSymptoms by rememberDraftSet(draftKey, "admissionSymptoms")
        var admissionOtherSymptom by rememberDraft(draftKey, "admissionOtherSymptom")
        var admissionSeverity by rememberDraft(draftKey, "admissionSeverity")
        var admissionTreatmentCompleted by rememberDraft(draftKey, "admissionTreatmentCompleted")
        var admissionReasonNotCompleted by rememberDraft(draftKey, "admissionReasonNotCompleted")
        var dischargeDate by rememberDraft(draftKey, "dischargeDate")
        var dischargeOutcome by rememberDraft(draftKey, "dischargeOutcome")
        var dischargeOtherOutcome by rememberDraft(draftKey, "dischargeOtherOutcome")
        var remarks by rememberDraft(draftKey, "remarks")
        // B. If admitted = No
        var notAdmittedTreatmentGiven by rememberDraft(draftKey, "notAdmittedTreatmentGiven")
        var healthWorkerAssigned by rememberDraft(draftKey, "healthWorkerAssigned")

        val fullCourse = "Yes – full course completed"

        FormPager(
            pageCount = if (admitted == "Yes") 5 else 4,
            page = page,
            onPageChange = { page = it },
            finishLabel = "Save Record",
            validatePage = { p ->
                if (p == 0) patientError(context, patientName, patientMobile) else null
            },
            onFinish = {
                val linkedPatientId = CsvHelper.findWorkerByPhone(context, cleanPhone(patientMobile))?.patientId ?: ""
                // Only the branch that applies is saved; the other branch is saved blank.
                val isAdmitted = admitted == "Yes"
                val notAdmitted = admitted == "No"
                fun ifAdmitted(v: String) = if (isAdmitted) v else ""
                fun ifNotAdmitted(v: String) = if (notAdmitted) v else ""

                val symptomsValue = withOther(symptoms.joined(), otherSymptom)
                val comorbiditiesValue = withOther(comorbidities.joined(), otherComorbidity)
                val reasonValue = if (isAdmitted && admissionTreatmentCompleted.isNotBlank() && admissionTreatmentCompleted != fullCourse) admissionReasonNotCompleted else ""

                val header = "MedicalOfficer,HealthCentre,Address,PatientName,PatientMobile,PatientID,BodyTempC,FeverDuration,KeySymptoms,GeneralCondition,BloodPressure,Comorbidities,TreatmentPrescribed,Admitted,AdmissionSpecies,AdmissionSymptoms,AdmissionSeverity,AdmissionTreatmentCompleted,AdmissionReasonNotCompleted,DischargeDate,DischargeOutcome,Remarks,NotAdmittedTreatmentGiven,HealthWorkerAssigned"
                val rowData = listOf(
                    officerName, healthCentre, address, patientName, cleanPhone(patientMobile), linkedPatientId,
                    bodyTemp, feverDuration, symptomsValue, generalCondition, bloodPressure,
                    comorbiditiesValue, treatmentPrescribed, admitted,
                    ifAdmitted(withOther(admissionSpecies, admissionOtherSpecies)),
                    ifAdmitted(withOther(admissionSymptoms.joined(), admissionOtherSymptom)),
                    ifAdmitted(admissionSeverity), ifAdmitted(admissionTreatmentCompleted), reasonValue,
                    ifAdmitted(dischargeDate), ifAdmitted(withOther(dischargeOutcome, dischargeOtherOutcome)), ifAdmitted(remarks),
                    ifNotAdmitted(notAdmittedTreatmentGiven), ifNotAdmitted(healthWorkerAssigned)
                ).joinToString(",") { CsvHelper.escapeCsv(it) }

                CsvHelper.saveToCsv(context, "Clinician", header, rowData)
                Toast.makeText(context, "Clinician record saved!", Toast.LENGTH_SHORT).show()
                Drafts.clear(context, draftKey)
                onFinish()
            },
            header = { Text(text = "Clinician Questionnaire", style = MaterialTheme.typography.headlineMedium) },
            footer = {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { Drafts.clear(context, draftKey); formKey++ }) { Text("Clear form") }
            }
        ) { p ->
            when (p) {
                0 -> {
                    Text(text = "General & Administrative Information", style = MaterialTheme.typography.titleMedium)
                    FormField(value = officerName, onValueChange = { officerName = it }, label = "Name of the Medical Officer")
                    LabeledDropdown(label = "Facility Type", options = HEALTH_CENTRES, selected = healthCentre, onSelect = { healthCentre = it })
                    FormField(value = address, onValueChange = { address = it }, label = "Address")
                    FormField(value = patientName, onValueChange = { patientName = it }, label = "Patient Name")
                    FormField(value = patientMobile, onValueChange = { patientMobile = it }, label = "Patient Mobile Number", keyboardType = KeyboardType.Phone)
                }
                1 -> {
                    Text(text = "Clinical Assessment and Treatment", style = MaterialTheme.typography.titleMedium)
                    FormField(value = bodyTemp, onValueChange = { bodyTemp = it }, label = "1. Body temperature (°C)", keyboardType = KeyboardType.Decimal)
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
                    FormField(value = otherSymptom, onValueChange = { otherSymptom = it }, label = "Other symptoms (specify)")
                }
                2 -> {
                    RadioGroup(
                        title = "4. General condition",
                        options = listOf("Stable", "Moderately ill", "Severe"),
                        selected = generalCondition,
                        onSelect = { generalCondition = it }
                    )
                    FormField(value = bloodPressure, onValueChange = { bloodPressure = it }, label = "5. Blood pressure (mmHg)")
                    CheckboxGroup(
                        title = "6. Co-morbidities",
                        options = listOf("Diabetes", "Hypertension", "None"),
                        selected = comorbidities,
                        onToggle = { opt, on -> comorbidities = comorbidities.toggle(opt, on) }
                    )
                    FormField(value = otherComorbidity, onValueChange = { otherComorbidity = it }, label = "Other co-morbidity (specify)")
                    FormField(value = treatmentPrescribed, onValueChange = { treatmentPrescribed = it }, label = "7. Treatment Prescribed")
                }
                3 -> {
                    RadioGroup(
                        title = "8. Patient admitted",
                        options = listOf("Yes", "No"),
                        selected = admitted,
                        onSelect = { admitted = it }
                    )
                    if (admitted == "Yes") {
                        Text(text = "A. If admitted = Yes", style = MaterialTheme.typography.titleMedium)
                        RadioGroup(
                            title = "Species / diagnosis at admission",
                            options = listOf("P. falciparum (Pf)", "P. vivax (Pv)", "Mixed (Pf + Pv)", "Other"),
                            selected = admissionSpecies,
                            onSelect = { admissionSpecies = it }
                        )
                        if (admissionSpecies == "Other") {
                            FormField(value = admissionOtherSpecies, onValueChange = { admissionOtherSpecies = it }, label = "Specify")
                        }
                        CheckboxGroup(
                            title = "Symptoms at the time of admission (tick all that apply)",
                            options = listOf("High fever", "Chills / rigors", "Severe headache", "Vomiting", "Abdominal pain", "Breathlessness / chest pain", "Confusion", "Severe weakness", "Jaundice", "Bleeding"),
                            selected = admissionSymptoms,
                            onToggle = { opt, on -> admissionSymptoms = admissionSymptoms.toggle(opt, on) }
                        )
                        FormField(value = admissionOtherSymptom, onValueChange = { admissionOtherSymptom = it }, label = "Other symptom (specify)")
                    } else if (admitted == "No") {
                        Text(text = "B. If admitted = No", style = MaterialTheme.typography.titleMedium)
                        RadioGroup(
                            title = "Treatment given?",
                            options = listOf("Yes", "No"),
                            selected = notAdmittedTreatmentGiven,
                            onSelect = { notAdmittedTreatmentGiven = it }
                        )
                        RadioGroup(
                            title = "Health Worker assigned for follow-up?",
                            options = listOf("Yes", "No"),
                            selected = healthWorkerAssigned,
                            onSelect = { healthWorkerAssigned = it }
                        )
                    }
                }
                else -> {
                    Text(text = "A. If admitted = Yes (continued)", style = MaterialTheme.typography.titleMedium)
                    RadioGroup(
                        title = "Clinical severity at admission",
                        options = listOf("Mild Malaria", "Severe Malaria"),
                        selected = admissionSeverity,
                        onSelect = { admissionSeverity = it }
                    )
                    RadioGroup(
                        title = "Treatment completed during admission?",
                        options = listOf(fullCourse, "No – still on treatment at discharge / transfer", "No – stopped early", "Referred to District /GMC"),
                        selected = admissionTreatmentCompleted,
                        onSelect = { admissionTreatmentCompleted = it }
                    )
                    if (admissionTreatmentCompleted.isNotBlank() && admissionTreatmentCompleted != fullCourse) {
                        FormField(value = admissionReasonNotCompleted, onValueChange = { admissionReasonNotCompleted = it }, label = "If not completed, reason")
                    }
                    FormField(value = dischargeDate, onValueChange = { dischargeDate = it }, label = "Date of discharge / transfer — DD/MM/YYYY")
                    RadioGroup(
                        title = "Outcome at discharge",
                        options = listOf("Recovered / symptom-free", "Improved, continue OPD medicines", "Referred District Hospital / GMC", "Other"),
                        selected = dischargeOutcome,
                        onSelect = { dischargeOutcome = it }
                    )
                    if (dischargeOutcome == "Other") {
                        FormField(value = dischargeOtherOutcome, onValueChange = { dischargeOtherOutcome = it }, label = "Specify")
                    }
                    FormField(value = remarks, onValueChange = { remarks = it }, label = "Remarks")
                }
            }
        }
    }
}
