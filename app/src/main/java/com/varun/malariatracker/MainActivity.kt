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
    DEMOGRAPHICS,
    MEDICAL_HISTORY,
    TRAVEL_HISTORY,
    SYMPTOMS
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

                    // NEW: Memory banks to hold the data until it's safe to save!
                    var pendingDemographics by remember { mutableStateOf("") }
                    var pendingMedical by remember { mutableStateOf("") }

                    when (currentScreen) {
                        AppScreen.LOGIN -> {
                            LoginScreen(
                                onNavigateNewUser = { id ->
                                    currentPatientId = id
                                    currentScreen = AppScreen.DEMOGRAPHICS
                                },
                                onNavigateReturningUser = { id ->
                                    currentPatientId = id
                                    currentPatientName = "Returning Patient"
                                    currentScreen = AppScreen.SYMPTOMS
                                }
                            )
                        }
                        AppScreen.DEMOGRAPHICS -> {
                            DemographicsScreen(
                                initialId = currentPatientId,
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
                                    // THIS IS THE ROLLBACK SAFETY NET!
                                    // We only write to the files if the user makes it this far.
                                    CsvHelper.saveToCsv(context,"Demographics", "ID,Name,Age,Gender,Mobile,AltMobile,GovIDType,GovIDNumber,PresentAddr,PermAddr,Language", pendingDemographics)
                                    CsvHelper.saveToCsv(context, "MedicalHistory", "ID,DiagnosedMalaria,TimesIn2Years,LastDetection,TreatmentPlaces,DiagnosisTestPerformed,SameDayReport,ReportMethods,MedicationPeriod,MedicineCourseCompleted", pendingMedical)
                                    CsvHelper.saveToCsv(context,"TravelHistory", "ID,StayDuration,TravelledOutsideGoa,VisitedStates,HomeStayDuration,FeverBeforeComing", travelCsvData)

                                    Toast.makeText(context, "Initial Profile Saved!", Toast.LENGTH_SHORT).show()

                                    currentScreen = AppScreen.SYMPTOMS
                                }
                            )
                        }
                        AppScreen.SYMPTOMS -> {
                            SymptomsScreen(
                                patientId = currentPatientId,
                                patientName = currentPatientName,
                                onFinish = {
                                    currentPatientId = ""
                                    currentPatientName = ""
                                    pendingDemographics = ""
                                    pendingMedical = ""
                                    currentScreen = AppScreen.LOGIN
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}