package com.example.orientation_app.domain.model

/**
 * Pure domain model for a university programme.
 * Mirrors [com.example.orientation_app.data.entity.FiliereMaster] but belongs to
 * the domain layer and carries no Room annotations, so ViewModels and use-cases
 * never depend on the persistence layer.
 */
data class ScoredProgram(
    val id: Int,
    val code: String,
    val nameAr: String,
    val nameFr: String,
    val universityAr: String,
    val universityFr: String,
    val lastYearScore: Double,
    val capacity: Int,
    val sectionEligibility: Int
)

/**
 * A programme recommended by the AI, paired with a one-sentence Arabic reason.
 */
data class Recommendation(
    val program: ScoredProgram,
    val reason: String
)
