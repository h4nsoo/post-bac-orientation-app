package com.example.orientation_app.data.remote

import com.example.orientation_app.BuildConfig
import com.example.orientation_app.data.entity.FiliereMaster
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

object GeminiService {

    private val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            systemInstruction = content { text(SYSTEM_PROMPT) }
        )
    }

    suspend fun getRecommendation(
        sectionName: String,
        fgScore: Double,
        interestText: String,
        programs: List<FiliereMaster>
    ): String {
        val response = model.generateContent(buildUserMessage(sectionName, fgScore, interestText, programs))
        return response.text
            ?: throw Exception("الذكاء الاصطناعي لم يُرجع أي نتيجة. حاول مجدداً.")
    }

    private fun buildUserMessage(
        sectionName: String,
        fgScore: Double,
        interestText: String,
        programs: List<FiliereMaster>
    ): String {
        val programsJson = programs.joinToString(",", "[", "]") { p ->
            """{"id":${p.id},"nameAr":"${p.nameAr}","nameFr":"${p.nameFr}","score":${p.lastYearScore}}"""
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
}

private val SYSTEM_PROMPT = """
    You are a Tunisian university orientation assistant.
    You will receive a student profile and a list of available university programs.
    You MUST respond with ONLY a valid JSON array — no markdown, no code block, no explanation, no extra text.
    Each element must have exactly two fields:
      "id"     : integer — the program id from the provided list
      "reason" : string  — one short Arabic sentence explaining why this program fits the student
    Select 3 to 5 programs. Only use ids that exist in the provided list.
""".trimIndent()
