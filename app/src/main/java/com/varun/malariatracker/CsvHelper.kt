package com.varun.malariatracker // Ensure this matches your package name

import android.os.Environment
import java.io.File
import java.io.FileWriter
import android.content.Context

object CsvHelper {

    // Headers for the worker proforma files. The first column is always the Patient ID.
    const val DEMOGRAPHICS_HEADER = "ID,NimrID,ConstructionSite,PhcUhcName,PhcUhcAddress,GeoLocation,Name,DOB,Age,Gender,Mobile,AltMobile,GovIDType,GovIDNumber,PresentAddr,PermAddr,Language"
    const val MEDICAL_HEADER = "ID,DiagnosedMalaria,TimesIn2Years,LastDetection,TreatmentPlaces,DiagnosisTestPerformed,SameDayReport,ReportMethods,MedicationPeriod,MedicineCourseCompleted,MobileBrand,Qualification,Repellent,MosquitoNet"
    const val TRAVEL_HEADER = "ID,StayDuration,TravelledOutsideGoa,VisitedStates,HomeStayDuration,FeverBeforeComing"
    const val TRAVEL_PLAN_HEADER = "ID,PlanningToVisit,DepartureDate,ReturnDate,PlacesVisited,FeverOrMalaria,TreatmentGiven,FullCourseCompleted,PlaceOfTreatment"
    const val SYMPTOMS_HEADER = "EntryID,Timestamp,PatientID,FeverToday,Temperature,FeverDuration,AssociatedSymptoms,IllnessDate,BreathingDifficulty,AbleForTesting"

    // All CSVs live in the app's external Downloads dir.
    private fun dir(context: Context): File? =
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

    // Helper function to write a row to a specific CSV file in the Downloads folder
    fun saveToCsv(context: Context, fileName: String, header: String, rowData: String) {
        try {
            val downloadsDir = dir(context)
            val file = File(downloadsDir, "$fileName.csv")

            // Check if we need to write the header row
            val isNewFile = !file.exists()

            val writer = FileWriter(file, true) // true = append to the end of the file
            if (isNewFile) {
                writer.append(header).append("\n")
            }
            writer.append(rowData).append("\n")
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun escapeCsv(value: String): String {
        return "\"" +
                value.replace("\"", "\"\"") +
                "\""
    }

    // Un-quotes a CSV field written by escapeCsv (strips wrapping quotes, un-doubles inner quotes).
    // Leaves plain unquoted fields untouched.
    private fun unescapeCsv(field: String): String {
        val raw = field.trim()
        return if (raw.length >= 2 && raw.startsWith("\"") && raw.endsWith("\""))
            raw.substring(1, raw.length - 1).replace("\"\"", "\"")
        else raw
    }

    // Returns the first CSV column of a line, tolerant of quoting.
    private fun firstField(line: String): String = unescapeCsv(line.substringBefore(","))

    // Function to check if a Unique ID already exists in Demographics.csv.
    // NOTE: previously compared the raw (quoted) first column against the plain id,
    // so it never matched — that broke returning-user detection. Now quote-tolerant,
    // and skips the header row.
    fun doesIdExist(context: Context, id: String): Boolean {
        try {
            val file = File(dir(context), "Demographics.csv")
            if (!file.exists()) return false
            return file.readLines().drop(1).any { firstField(it) == id }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * Replaces the row whose first column equals [key], or appends it if there is none.
     * Used for records that are written once and later completed or updated.
     */
    fun upsertRow(context: Context, fileName: String, header: String, key: String, rowData: String) {
        try {
            val file = File(dir(context), "$fileName.csv")
            val rows = if (file.exists()) file.readLines().drop(1).filter { it.isNotBlank() }.toMutableList() else mutableListOf()
            val i = rows.indexOfFirst { firstField(it) == key }
            if (i >= 0) rows[i] = rowData else rows += rowData
            file.writeText((listOf(header) + rows).joinToString("\n", postfix = "\n"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ---------------------------------------------------------------------------
    // Login index files. Key columns come FIRST and are written unescaped so a
    // simple split(",") lookup is safe even if a later Name/HealthCentre field
    // contains a comma.
    //
    //   Workers.csv        -> Phone,PatientID,Status,Name
    //   Professionals.csv  -> Phone,Role,Name,HealthCentre
    // ---------------------------------------------------------------------------

    /** Registered = first six questions saved; Complete = whole proforma saved. */
    const val STATUS_REGISTERED = "REGISTERED"
    const val STATUS_COMPLETE = "COMPLETE"

    data class WorkerRecord(val patientId: String, val status: String, val name: String)

    /** Sequential patient id: P0001, P0002, ... (padding is a minimum width, not a cap). */
    fun nextPatientId(context: Context): String {
        val f = File(dir(context), "Workers.csv")
        val count = if (f.exists()) f.readLines().drop(1).count { it.isNotBlank() } else 0
        return "P" + (count + 1).toString().padStart(4, '0')
    }

    /** The worker registered with this phone, or null if not registered. */
    fun findWorkerByPhone(context: Context, phone: String): WorkerRecord? {
        val f = File(dir(context), "Workers.csv")
        if (!f.exists()) return null
        return f.readLines().drop(1)
            .map { it.split(",", limit = 4) }
            .firstOrNull { it.getOrNull(0) == phone }
            ?.let { WorkerRecord(it.getOrNull(1) ?: "", it.getOrNull(2) ?: "", unescapeCsv(it.getOrNull(3) ?: "")) }
    }

    fun upsertWorker(context: Context, phone: String, patientId: String, status: String, name: String) {
        upsertRow(context, "Workers", "Phone,PatientID,Status,Name", phone, "$phone,$patientId,$status,${escapeCsv(name)}")
    }

    /** All professional roles registered against this phone (empty if none). */
    fun findProfessionalRoles(context: Context, phone: String): List<String> {
        val f = File(dir(context), "Professionals.csv")
        if (!f.exists()) return emptyList()
        return f.readLines().drop(1)
            .map { it.split(",") }
            .filter { it.getOrNull(0) == phone }
            .mapNotNull { it.getOrNull(1) }
            .distinct()
    }

    /** (name, healthCentre) for a given phone+role, or null. */
    fun professionalInfo(context: Context, phone: String, role: String): Pair<String, String>? {
        val f = File(dir(context), "Professionals.csv")
        if (!f.exists()) return null
        return f.readLines().drop(1)
            .map { it.split(",") }
            .firstOrNull { it.getOrNull(0) == phone && it.getOrNull(1) == role }
            ?.let { unescapeCsv(it.getOrNull(2) ?: "") to unescapeCsv(it.getOrNull(3) ?: "") }
    }

    fun isProfessionalRegistered(context: Context, phone: String, role: String): Boolean =
        findProfessionalRoles(context, phone).contains(role)

    fun registerProfessional(context: Context, phone: String, role: String, name: String, healthCentre: String) {
        saveToCsv(
            context, "Professionals", "Phone,Role,Name,HealthCentre",
            "$phone,$role,${escapeCsv(name)},${escapeCsv(healthCentre)}"
        )
    }
}
