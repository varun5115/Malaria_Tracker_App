package com.varun.malariatracker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.varun.malariatracker.ui.theme.MalariaTrackerTheme

enum class AppScreen {
    LOGIN,
    PROFESSIONAL_REGISTRATION,
    DEMOGRAPHICS,
    MEDICAL_HISTORY,
    TRAVEL_HISTORY,
    TRAVEL_PLAN,
    SYMPTOMS,
    CLINICIAN,
    PATHOLOGY,
    HEALTH_WORKER,
    RESEARCH_LAB,
    CONTRACTOR
}

private fun roleToScreen(role: String): AppScreen = when (role) {
    "Clinician" -> AppScreen.CLINICIAN
    "Pathology" -> AppScreen.PATHOLOGY
    "Health Worker" -> AppScreen.HEALTH_WORKER
    "Research Lab" -> AppScreen.RESEARCH_LAB
    "Contractor" -> AppScreen.CONTRACTOR
    else -> AppScreen.LOGIN
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MalariaTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    // Keeps each screen's answers while the user moves back and forth.
                    val screenStates = rememberSaveableStateHolder()

                    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.LOGIN) }
                    var currentPatientId by rememberSaveable { mutableStateOf("") }
                    var currentPatientName by rememberSaveable { mutableStateOf("") }
                    var currentPhone by rememberSaveable { mutableStateOf("") }

                    // Pre-fill for professional forms (from Professionals.csv on login).
                    var proName by rememberSaveable { mutableStateOf("") }
                    var proHealthCentre by rememberSaveable { mutableStateOf("") }

                    // Memory banks holding worker data until it's safe to save (rollback net).
                    var pendingDemographics by rememberSaveable { mutableStateOf("") }
                    var pendingMedical by rememberSaveable { mutableStateOf("") }
                    // Set once the worker's profile is written; Back never crosses that save.
                    var profileSaved by rememberSaveable { mutableStateOf(false) }

                    fun goTo(screen: AppScreen) {
                        if (screen == AppScreen.LOGIN) {
                            // Leaving a flow: drop every screen's draft answers and the session.
                            AppScreen.entries.forEach { screenStates.removeState(it.name) }
                            currentPatientId = ""
                            currentPatientName = ""
                            currentPhone = ""
                            proName = ""
                            proHealthCentre = ""
                            pendingDemographics = ""
                            pendingMedical = ""
                            profileSaved = false
                        }
                        currentScreen = screen
                    }

                    fun blockBack(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

                    // Screen-level Back. Pages inside a form handle Back first (see FormPager).
                    BackHandler(enabled = currentScreen != AppScreen.LOGIN) {
                        when (currentScreen) {
                            AppScreen.MEDICAL_HISTORY -> blockBack("Personal details can't be changed after they are submitted.")
                            AppScreen.TRAVEL_HISTORY -> goTo(AppScreen.MEDICAL_HISTORY)
                            AppScreen.TRAVEL_PLAN -> blockBack("The profile is already saved, so you can't go back.")
                            AppScreen.SYMPTOMS ->
                                if (profileSaved) blockBack("The profile is already saved, so you can't go back.")
                                else goTo(AppScreen.LOGIN)
                            else -> goTo(AppScreen.LOGIN)
                        }
                    }

                    // safeDrawingPadding keeps content clear of the status bar and the keyboard,
                    // so the field being typed into always stays visible.
                    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                        if (currentScreen != AppScreen.LOGIN) {
                            val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                            TextButton(
                                onClick = { dispatcher?.onBackPressed() },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Text("← Back")
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            screenStates.SaveableStateProvider(currentScreen.name) {
                                when (currentScreen) {
                                    AppScreen.LOGIN -> {
                                        LoginScreen(
                                            onNewWorker = { phone ->
                                                // Brand-new worker: generate their Patient ID now and carry it through signup.
                                                currentPhone = phone
                                                currentPatientId = CsvHelper.nextPatientId(context)
                                                currentPatientName = ""
                                                goTo(AppScreen.DEMOGRAPHICS)
                                            },
                                            onReturningWorker = { patientId ->
                                                currentPatientId = patientId
                                                currentPatientName = "Returning Patient"
                                                goTo(AppScreen.SYMPTOMS)
                                            },
                                            onProfessionalLogin = { role, phone ->
                                                val info = CsvHelper.professionalInfo(context, phone, role)
                                                proName = info?.first ?: ""
                                                proHealthCentre = info?.second ?: ""
                                                goTo(roleToScreen(role))
                                            },
                                            onRegisterProfessional = { goTo(AppScreen.PROFESSIONAL_REGISTRATION) }
                                        )
                                    }
                                    AppScreen.PROFESSIONAL_REGISTRATION -> {
                                        ProfessionalRegistrationScreen(onDone = { goTo(AppScreen.LOGIN) })
                                    }
                                    AppScreen.DEMOGRAPHICS -> {
                                        DemographicsScreen(
                                            initialId = currentPatientId,
                                            initialMobile = currentPhone,
                                            onNavigateNext = { id, name, csvData ->
                                                currentPatientId = id
                                                currentPatientName = name
                                                pendingDemographics = csvData // Hold in memory
                                                goTo(AppScreen.MEDICAL_HISTORY)
                                            }
                                        )
                                    }
                                    AppScreen.MEDICAL_HISTORY -> {
                                        MedicalHistoryScreen(
                                            patientId = currentPatientId,
                                            patientName = currentPatientName,
                                            onNavigateNext = { csvData ->
                                                pendingMedical = csvData // Hold in memory
                                                goTo(AppScreen.TRAVEL_HISTORY)
                                            }
                                        )
                                    }
                                    AppScreen.TRAVEL_HISTORY -> {
                                        TravelHistoryScreen(
                                            patientId = currentPatientId,
                                            patientName = currentPatientName,
                                            onNavigateNext = { travelCsvData ->
                                                // ROLLBACK SAFETY NET: only write once the user reaches the end.
                                                CsvHelper.saveToCsv(context, "Demographics", "ID,Name,Age,Gender,Mobile,AltMobile,GovIDType,GovIDNumber,PresentAddr,PermAddr,Language", pendingDemographics)
                                                CsvHelper.saveToCsv(context, "MedicalHistory", "ID,DiagnosedMalaria,TimesIn2Years,LastDetection,TreatmentPlaces,DiagnosisTestPerformed,SameDayReport,ReportMethods,MedicationPeriod,MedicineCourseCompleted", pendingMedical)
                                                CsvHelper.saveToCsv(context, "TravelHistory", "ID,StayDuration,TravelledOutsideGoa,VisitedStates,HomeStayDuration,FeverBeforeComing", travelCsvData)

                                                // Register the worker in the phone->PatientID login index.
                                                CsvHelper.registerWorker(context, currentPhone, currentPatientId, currentPatientName)
                                                profileSaved = true

                                                Toast.makeText(context, "Profile saved! Patient ID: $currentPatientId", Toast.LENGTH_LONG).show()
                                                goTo(AppScreen.TRAVEL_PLAN)
                                            }
                                        )
                                    }
                                    AppScreen.TRAVEL_PLAN -> {
                                        TravelPlanScreen(
                                            patientId = currentPatientId,
                                            patientName = currentPatientName,
                                            onNavigateNext = { goTo(AppScreen.SYMPTOMS) }
                                        )
                                    }
                                    AppScreen.SYMPTOMS -> {
                                        SymptomsScreen(
                                            patientId = currentPatientId,
                                            patientName = currentPatientName,
                                            onFinish = { goTo(AppScreen.LOGIN) }
                                        )
                                    }
                                    AppScreen.CLINICIAN -> {
                                        ClinicianScreen(initialName = proName, initialHealthCentre = proHealthCentre, onFinish = { goTo(AppScreen.LOGIN) })
                                    }
                                    AppScreen.PATHOLOGY -> {
                                        PathologyScreen(initialName = proName, initialHealthCentre = proHealthCentre, onFinish = { goTo(AppScreen.LOGIN) })
                                    }
                                    AppScreen.HEALTH_WORKER -> {
                                        HealthWorkerScreen(initialName = proName, initialHealthCentre = proHealthCentre, onFinish = { goTo(AppScreen.LOGIN) })
                                    }
                                    AppScreen.RESEARCH_LAB -> {
                                        ResearchLabScreen(initialName = proName, onFinish = { goTo(AppScreen.LOGIN) })
                                    }
                                    AppScreen.CONTRACTOR -> {
                                        ContractorScreen(initialName = proName, onFinish = { goTo(AppScreen.LOGIN) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
