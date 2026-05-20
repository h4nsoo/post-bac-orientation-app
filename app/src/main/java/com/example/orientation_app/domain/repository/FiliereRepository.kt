package com.example.orientation_app.domain.repository

import com.example.orientation_app.domain.model.ScoredProgram
import kotlinx.coroutines.flow.Flow

/**
 * Contract for fetching and filtering university programmes.
 * Implementations live in the data layer and hide Room entirely.
 */
interface FiliereRepository {

    /**
     * One-shot query: returns programmes eligible for [sectionBit] whose last-year
     * orientation score is at most [userScore] + 5 (safety margin), ordered by
     * proximity to [userScore], limited to 30 results for the AI context window.
     */
    suspend fun getEligibleOnce(sectionBit: Int, userScore: Double): List<ScoredProgram>

    /**
     * Reactive stream: same filter as [getEligibleOnce] but updates on DB changes.
     */
    fun getEligible(sectionBit: Int, userScore: Double): Flow<List<ScoredProgram>>

    /** Total count of programmes passing the eligibility filter. */
    suspend fun countEligible(sectionBit: Int, userScore: Double): Int
}
