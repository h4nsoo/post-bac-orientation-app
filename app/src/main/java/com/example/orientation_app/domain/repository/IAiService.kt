package com.example.orientation_app.domain.repository

import com.example.orientation_app.domain.model.Recommendation
import com.example.orientation_app.domain.model.ScoredProgram

/**
 * Contract for the external AI recommendation service.
 * The production implementation calls Gemini; the test implementation returns
 * deterministic fakes, making ViewModel tests fully synchronous.
 */
interface IAiService {

    /**
     * Given a student profile and a pre-filtered list of eligible programmes,
     * asks the AI to pick 3–5 best matches and return them with Arabic reasons.
     *
     * @throws Exception when the network call fails or the response is malformed.
     */
    suspend fun getRecommendations(
        sectionName: String,
        fgScore: Double,
        interestText: String,
        programs: List<ScoredProgram>
    ): List<Recommendation>
}
