package com.varun.malariatracker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

                    var currentScreen by remember { mutableStateOf(AppScreen.LOGIN) }
                    var currentPatientId by remember { mutableStateOf("") }
                    var currentPatientName by remember { mutableStateOf("") }
                    var currentPhone by remember { mutableStateOf("") }

                    // Pre-fill for professional forms (from Professionals.csv on login).
                    var proName by remember { mutableStateOf("") }
                    var proHealthCentre by remember { mutableStateOf("") }

                    // Memory banks holding worker data until it's safe to save (rollback net).
                    var pendingDemographics by remember { mutableStateOf("") }
                    var pendingMedical by remember { mutableStateOf("") }

                    when (currentScreen) {
                        AppScreen.LOGIN -> {
                            LoginScreen(
                                onNewWorker = { phone ->
                                    // Brand-new worker: generate their Patient ID now and carry it through signup.
                                    currentPhone = phone
                                    currentPatientId = CsvHelper.nextPatientId(context)
                                    currentPatientName = ""
                                    currentScreen = AppScreen.DEMOGRAPHICS
                                },
                                onReturningWorker = { patientId ->
                                    currentPatientId = patientId
                                    currentPatientName = "Returning Patient"
                                    currentScreen = AppScreen.SYMPTOMS
                                },
                                onProfessionalLogin = { role, phone ->
                                    val info = CsvHelper.professionalInfo(context, phone, role)
                                    proName = info?.first ?: ""
                                    proHealthCentre = info?.second ?: ""
                                    currentScreen = roleToScreen(role)
                                },
                                onRegisterProfessional = { currentScreen = AppScreen.PROFESSIONAL_REGISTRATION }
                            )
                        }
                        AppScreen.PROFESSIONAL_REGISTRATION -> {
                            ProfessionalRegistrationScreen(onDone = { currentScreen = AppScreen.LOGIN })
                        }
                        AppScreen.DEMOGRAPHICS -> {
                            DemographicsScreen(
                                initialId = currentPatientId,
                                initialMobile = currentPhone,
                                onNavigateNext = { id, name, csvData ->
                                    currentPatientId = id
                                    currentPatientName = name
                                    pendingDemographics = csvData // Hold in memory
                                    currentScreen = AppScreen.MEDICAL_HISTORY
                                }
                            )
                        }
                        AppScreen.MEDICAL_HISTORY -> {
                            MedicalHistoryScreen(
                                patientId = currentPatientId,
                                patientName = currentPatientName,
                                onNavigateNext = { csvData ->
                                    pendingMedical = csvData // Hold in memory
                                    currentScreen = AppScreen.TRAVEL_HISTORY
                                }
                            )
                        }
                        AppScreen.TRAVEL_HISTORY -> {
                            TravelHistoryScreen(
                                patientId = currentPatientId,
                                patientName = currentPatientName,
                                onNavigateNext = { travelCsvData ->
                                    // ROLLBACK SAFETY NET: only write once the user reaches the end.
                                    CsvHelper.saveToCsv(context,"Demographics", "ID,Name,Age,Gender,Mobile,AltMobile,GovIDType,GovIDNumber,PresentAddr,PermAddr,Language", pendingDemographics)
                                    CsvHelper.saveToCsv(context, "MedicalHistory", "ID,DiagnosedMalaria,TimesIn2Years,LastDetection,TreatmentPlaces,DiagnosisTestPerformed,SameDayReport,ReportMethods,MedicationPeriod,MedicineCourseCompleted", pendingMedical)
                                    CsvHelper.saveToCsv(context,"TravelHistory", "ID,StayDuration,TravelledOutsideGoa,VisitedStates,HomeStayDuration,FeverBeforeComing", travelCsvData)

                                    // Register the worker in the phone->PatientID login index.
                                    CsvHelper.registerWorker(context, currentPhone, currentPatientId, currentPatientName)

                                    Toast.makeText(context, "Profile saved! Patient ID: $currentPatientId", Toast.LENGTH_LONG).show()

                                    currentScreen = AppScreen.TRAVEL_PLAN
                                }
                            )
                        }
                        AppScreen.TRAVEL_PLAN -> {
                            TravelPlanScreen(
                                patientId = currentPatientId,
                                patientName = currentPatientName,
                                onNavigateNext = { currentScreen = AppScreen.SYMPTOMS }
                            )
                        }
                        AppScreen.SYMPTOMS -> {
                            SymptomsScreen(
                                patientId = currentPatientId,
                                patientName = currentPatientName,
                                onFinish = {
                                    currentPatientId = ""
                                    currentPatientName = ""
                                    currentPhone = ""
                                    pendingDemographics = ""
                                    pendingMedical = ""
                                    currentScreen = AppScreen.LOGIN
                                }
                            )
                        }
                        AppScreen.CLINICIAN -> {
                            ClinicianScreen(initialName = proName, initialHealthCentre = proHealthCentre, onFinish = { currentScreen = AppScreen.LOGIN })
                        }
                        AppScreen.PATHOLOGY -> {
                            PathologyScreen(initialName = proName, initialHealthCentre = proHealthCentre, onFinish = { currentScreen = AppScreen.LOGIN })
                        }
                        AppScreen.HEALTH_WORKER -> {
                            HealthWorkerScreen(initialName = proName, initialHealthCentre = proHealthCentre, onFinish = { currentScreen = AppScreen.LOGIN })
                        }
                        AppScreen.RESEARCH_LAB -> {
                            ResearchLabScreen(initialName = proName, onFinish = { currentScreen = AppScreen.LOGIN })
                        }
                        AppScreen.CONTRACTOR -> {
                            ContractorScreen(initialName = proName, onFinish = { currentScreen = AppScreen.LOGIN })
                        }
                    }
                }
            }
        }
    }
}
