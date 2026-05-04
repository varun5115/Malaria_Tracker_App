package com.varun.malariatracker // Make sure this matches your actual package name at the top!

data class PatientDemographics(
    val id: String,
    val name: String,
    val age: String,
    val gender: String,
    val mobile: String,
    val address: String,
    val language: String
)

data class MedicalHistory(
    val id: String,
    val episode: String,
    val lastDiagnosed: String
)

data class TravelHistory(
    val id: String,
    val state: String,
    val travelled: String
)

data class SymptomEntry(
    val entryId: String,
    val id: String,
    val date: String,
    val symptom: String
)