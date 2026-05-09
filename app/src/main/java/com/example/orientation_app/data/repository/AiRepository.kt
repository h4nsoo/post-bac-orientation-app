package com.example.orientation_app.data.repository

import com.example.orientation_app.data.dao.AIInsightHistoryDao
import com.example.orientation_app.data.dao.FiliereDao
import com.example.orientation_app.data.dao.UserProfileDao
import com.example.orientation_app.data.entity.AIInsightHistory
import com.example.orientation_app.data.model.AiRecommendation
import com.example.orientation_app.data.remote.GeminiService
import org.json.JSONArray

class AiRepository(
    private val filiereDao: FiliereDao,
    private val aiInsightHistoryDao: AIInsightHistoryDao,
    private val userProfileDao: UserProfileDao
) {

    suspend fun getRecommendation(
        sectionId: String,
        sectionName: String,
        fgScore: Double,
        interestText: String
    ): List<AiRecommendation> {
        try { userProfileDao.updateInterests(interestText) } catch (_: Exception) {}

        val sectionBit = sectionIdToBit(sectionId)
        val programs = filiereDao.filterEligibleOnce(sectionBit, fgScore)

        if (programs.isEmpty()) {
            throw Exception("لا توجد برامج متاحة لملفك الدراسي. نقطتك قد تكون خارج نطاق البرامج المتوفرة.")
        }

        val rawJson = GeminiService.getRecommendation(sectionName, fgScore, interestText, programs)

        val recommendations = parseRecommendations(rawJson, programs)

        try {
            aiInsightHistoryDao.insert(
                AIInsightHistory(
                    userId = 1,
                    queryText = "[$sectionName | FG: ${String.format("%.2f", fgScore)}] $interestText",
                    aiResponse = rawJson,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {}

        return recommendations
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

    private fun parseRecommendations(
        json: String,
        programs: List<com.example.orientation_app.data.entity.FiliereMaster>
    ): List<AiRecommendation> {
        val index = programs.associateBy { it.id }
        val cleaned = json.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val array = JSONArray(cleaned)
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.getJSONObject(i)
            val program = index[obj.getInt("id")] ?: return@mapNotNull null
            AiRecommendation(program = program, reason = obj.getString("reason"))
        }
    }
}
