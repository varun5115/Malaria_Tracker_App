package com.varun.malariatracker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.varun.malariatracker.ui.theme.MalariaTrackerTheme

/** Worker proforma screens in page order; used to rebuild the back stack when a worker resumes. */
private val WORKER_ROUTES = listOf("demographics", "medical", "travel", "travel_plan", "symptoms")

private fun roleToRoute(role: String): String = when (role) {
    "Clinician" -> "clinician"
    "Pathology" -> "pathology"
    "Health Worker" -> "health_worker"
    "Research Lab" -> "research_lab"
    "Contractor" -> "contractor"
    else -> "login"
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
                    val focusManager = LocalFocusManager.current
                    val nav = rememberNavController()
                    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route

                    var currentPhone by rememberSaveable { mutableStateOf("") }
                    var currentPatientId by rememberSaveable { mutableStateOf("") } // blank until registered
                    var currentPatientName by rememberSaveable { mutableStateOf("") }
                    // True when a worker with a completed proforma is logging a new visit.
                    var isVisit by rememberSaveable { mutableStateOf(false) }

                    // Professional session (pre-fill + draft key).
                    var proPhone by rememberSaveable { mutableStateOf("") }
                    var proName by rememberSaveable { mutableStateOf("") }
                    var proHealthCentre by rememberSaveable { mutableStateOf("") }

                    val workerDraft = "worker:$currentPhone"

                    fun toLogin() {
                        currentPhone = ""
                        currentPatientId = ""
                        currentPatientName = ""
                        isVisit = false
                        proPhone = ""
                        proName = ""
                        proHealthCentre = ""
                        nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
                    }

                    // Moves forward in the worker proforma and remembers how far the worker has got.
                    fun goWorker(route: String) {
                        Drafts.put(context, workerDraft, "route", route)
                        nav.navigate(route)
                    }

                    // End of the proforma: write every section, mark the worker complete, drop the draft.
                    fun completeProfile(symptomsRow: String) {
                        listOf(
                            "Demographics" to CsvHelper.DEMOGRAPHICS_HEADER,
                            "MedicalHistory" to CsvHelper.MEDICAL_HEADER,
                            "TravelHistory" to CsvHelper.TRAVEL_HEADER,
                            "TravelPlan" to CsvHelper.TRAVEL_PLAN_HEADER
                        ).forEach { (file, header) ->
                            Drafts.get(context, workerDraft, "row:$file")?.let {
                                CsvHelper.upsertRow(context, file, header, currentPatientId, it)
                            }
                        }
                        CsvHelper.saveToCsv(context, "Symptoms", CsvHelper.SYMPTOMS_HEADER, symptomsRow)
                        CsvHelper.upsertWorker(context, currentPhone, currentPatientId, CsvHelper.STATUS_COMPLETE, currentPatientName)
                        Drafts.clear(context, workerDraft)
                        Toast.makeText(context, "Profile complete!", Toast.LENGTH_LONG).show()
                    }

                    // safeDrawingPadding keeps content clear of the status bar and the keyboard.
                    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                        if (currentRoute != null && currentRoute != "login") {
                            val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                            TextButton(
                                onClick = { dispatcher?.onBackPressed() },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Text("← Back")
                            }
                        }

                        NavHost(
                            navController = nav,
                            startDestination = "login",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                // Tapping outside a field closes the keyboard.
                                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                        ) {
                            composable("login") {
                                LoginScreen(
                                    onNewWorker = { phone ->
                                        currentPhone = phone
                                        currentPatientId = ""
                                        currentPatientName = ""
                                        isVisit = false
                                        nav.navigate("demographics")
                                    },
                                    onReturningWorker = { phone, worker ->
                                        currentPhone = phone
                                        currentPatientId = worker.patientId
                                        currentPatientName = worker.name
                                        if (worker.status == CsvHelper.STATUS_COMPLETE) {
                                            isVisit = true
                                            nav.navigate("symptoms")
                                        } else {
                                            // Registered but incomplete: resume where they left off, with
                                            // every earlier page on the back stack.
                                            isVisit = false
                                            val last = Drafts.get(context, "worker:$phone", "route") ?: "demographics"
                                            val upTo = WORKER_ROUTES.indexOf(last).coerceAtLeast(0)
                                            WORKER_ROUTES.take(upTo + 1).forEach { nav.navigate(it) }
                                        }
                                    },
                                    onProfessionalLogin = { role, phone ->
                                        val info = CsvHelper.professionalInfo(context, phone, role)
                                        proPhone = phone
                                        proName = info?.first ?: ""
                                        proHealthCentre = info?.second ?: ""
                                        nav.navigate(roleToRoute(role))
                                    },
                                    onRegisterProfessional = { nav.navigate("pro_registration") }
                                )
                            }
                            composable("pro_registration") {
                                ProfessionalRegistrationScreen(onDone = { toLogin() })
                            }
                            composable("demographics") {
                                DemographicsScreen(
                                    draftKey = workerDraft,
                                    phone = currentPhone,
                                    patientId = currentPatientId,
                                    onRegistered = { id, name ->
                                        currentPatientId = id
                                        currentPatientName = name
                                    },
                                    onNavigateNext = { name ->
                                        currentPatientName = name
                                        goWorker("medical")
                                    },
                                    onCancel = {
                                        Drafts.clear(context, workerDraft)
                                        toLogin()
                                    }
                                )
                            }
                            composable("medical") {
                                MedicalHistoryScreen(workerDraft, currentPatientId, currentPatientName) { goWorker("travel") }
                            }
                            composable("travel") {
                                TravelHistoryScreen(workerDraft, currentPatientId, currentPatientName) { goWorker("travel_plan") }
                            }
                            composable("travel_plan") {
                                TravelPlanScreen(workerDraft, currentPatientId, currentPatientName) { goWorker("symptoms") }
                            }
                            composable("symptoms") {
                                val visitDraft = "visit:$currentPhone"
                                SymptomsScreen(
                                    draftKey = if (isVisit) visitDraft else workerDraft,
                                    patientId = currentPatientId,
                                    patientName = currentPatientName,
                                    onFinish = { row ->
                                        if (isVisit) {
                                            CsvHelper.saveToCsv(context, "Symptoms", CsvHelper.SYMPTOMS_HEADER, row)
                                            Drafts.clear(context, visitDraft)
                                            Toast.makeText(context, "Visit saved!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            completeProfile(row)
                                        }
                                        toLogin()
                                    }
                                )
                            }
                            composable("clinician") {
                                ClinicianScreen(draftKey = "pro:$proPhone:Clinician", initialName = proName, initialHealthCentre = proHealthCentre, onFinish = { toLogin() })
                            }
                            composable("pathology") {
                                PathologyScreen(draftKey = "pro:$proPhone:Pathology", initialName = proName, initialHealthCentre = proHealthCentre, onFinish = { toLogin() })
                            }
                            composable("health_worker") {
                                HealthWorkerScreen(draftKey = "pro:$proPhone:Health Worker", initialName = proName, initialHealthCentre = proHealthCentre, onFinish = { toLogin() })
                            }
                            composable("research_lab") {
                                ResearchLabScreen(draftKey = "pro:$proPhone:Research Lab", initialName = proName, onFinish = { toLogin() })
                            }
                            composable("contractor") {
                                ContractorScreen(draftKey = "pro:$proPhone:Contractor", initialName = proName, onFinish = { toLogin() })
                            }
                        }
                    }
                }
            }
        }
    }
}
