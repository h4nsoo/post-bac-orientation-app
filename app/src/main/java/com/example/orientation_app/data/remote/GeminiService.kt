package com.example.orientation_app.data.remote

import com.example.orientation_app.BuildConfig
import com.example.orientation_app.domain.model.Recommendation
import com.example.orientation_app.domain.model.ScoredProgram
import com.example.orientation_app.domain.repository.IAiService
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production [IAiService] backed by Gemini 2.5 Flash.
 *
 * Parsing strategy: Gemini is prompted to return a bare JSON array. The response
 * is cleaned of any accidental markdown fences, then parsed with [JSONArray].
 * Each element must contain "id" (Int) and "reason" (String).
 */
@Singleton
class GeminiServiceImpl @Inject constructor() : IAiService {

    private val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            systemInstruction = content { text(SYSTEM_PROMPT) }
        )
    }

    override suspend fun getRecommendations(
        sectionName: String,
        fgScore: Double,
        interestText: String,
        programs: List<ScoredProgram>
    ): List<Recommendation> {
        val rawJson = model
            .generateContent(buildUserMessage(sectionName, fgScore, interestText, programs))
            .text ?: throw Exception("الذكاء الاصطناعي لم يُرجع أي نتيجة. حاول مجدداً.")

        return parseResponse(rawJson, programs)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildUserMessage(
        sectionName: String,
        fgScore: Double,
        interestText: String,
        programs: List<ScoredProgram>
    ): String {
        val programsJson = programs.joinToString(",", "[", "]") { p ->
            val name = p.nameAr.substringBefore("(").trim()
            """{"id":${p.id},"nameAr":"$name","score":${p.lastYearScore}}"""
        }
        return """
            Student:
            - Section: $sectionName
            - FG Score: ${String.format("%.2f", fgScore)}
            - Interests: $interestText

            Available programs:
            $programsJson
        """.trimIndent()
    }

    private fun parseResponse(
        raw: String,
        programs: List<ScoredProgram>
    ): List<Recommendation> {
        val index = programs.associateBy { it.id }
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        val array = JSONArray(cleaned)
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.getJSONObject(i)
            val program = index[obj.getInt("id")] ?: return@mapNotNull null
            Recommendation(program = program, reason = obj.getString("reason"))
        }
    }
}

// ── System prompt ─────────────────────────────────────────────────────────────

private val SYSTEM_PROMPT = """
    You are a Tunisian university orientation assistant.
    You will receive a student profile and a list of available university programs.
    You MUST respond with ONLY a valid JSON array — no markdown, no code block, no explanation, no extra text.
    Each element must have exactly two fields:
      "id"     : integer — the program id from the provided list
      "reason" : string  — one short Arabic sentence explaining why this program fits the student
    Select 3 to 5 programs. Only use ids that exist in the provided list.
""".trimIndent()
