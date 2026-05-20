package com.example.orientation_app.domain.usecase

import com.example.orientation_app.domain.model.Recommendation
import com.example.orientation_app.domain.repository.FiliereRepository
import com.example.orientation_app.domain.repository.IAiService
import com.example.orientation_app.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Orchestrates the AI recommendation flow:
 * 1. Persist the user's interest text.
 * 2. Query the DB for eligible programmes (section bit-mask + score filter).
 * 3. Ask the AI service to pick and explain the best matches.
 *
 * Throws an [Exception] with a user-readable Arabic message when no programmes
 * are found or the AI call fails.
 */
class GetAiRecommendationsUseCase @Inject constructor(
    private val filiereRepository: FiliereRepository,
    private val aiService: IAiService,
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(
        sectionId: String,
        sectionName: String,
        fgScore: Double,
        interestText: String
    ): List<Recommendation> {

        // Persist interest text for history (silent fail if DB is not yet ready).
        runCatching { userRepository.updateInterests(interestText) }

        val sectionBit = sectionIdToBit(sectionId)
        val programs = filiereRepository.getEligibleOnce(sectionBit, fgScore)

        if (programs.isEmpty()) {
            throw Exception(
                "لا توجد برامج متاحة لملفك الدراسي. نقطتك قد تكون خارج نطاق البرامج المتوفرة."
            )
        }

        return aiService.getRecommendations(sectionName, fgScore, interestText, programs)
    }

    private fun sectionIdToBit(sectionId: String): Int = when (sectionId) {
        "math"       -> 1
        "science"    -> 2
        "tech"       -> 4
        "info"       -> 8
        "literature" -> 16
        "economy"    -> 32
        "sports"     -> 64
        else         -> 0
    }
}
