package com.example.orientation_app.data.remote

import com.example.orientation_app.BuildConfig
import com.example.orientation_app.domain.model.Recommendation
import com.example.orientation_app.domain.model.ScoredProgram
import com.example.orientation_app.domain.repository.IAiService
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production [IAiService] backed by Gemini 2.5 Flash.
 *
 * Parsing strategy: Gemini is prompted to return a bare JSON array. The response
 * is cleaned of any accidental markdown fences, then parsed with [JSONArray].
 * Each element must contain "id" (Int) and "reason" (String).
 *
 * Temperature is set to 1.0 so the model does not always converge to the same
 * highest-score programmes — diversity of output is desirable here.
 */
@Singleton
class GeminiServiceImpl @Inject constructor() : IAiService {

    private val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName         = "gemini-2.5-flash",
            apiKey            = BuildConfig.GEMINI_API_KEY,
            systemInstruction = content { text(SYSTEM_PROMPT) },
            generationConfig  = generationConfig { temperature = 1.0f }
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
        // Include both Arabic and French names so the model can recognise
        // domain keywords in either language (e.g. "Médecine", "Informatique").
        val programsJson = programs.joinToString(",\n", "[\n", "\n]") { p ->
            val nameAr = p.nameAr.substringBefore("(").trim()
            val nameFr = p.nameFr.substringBefore("(").trim()
            """  {"id":${p.id},"nameAr":"$nameAr","nameFr":"$nameFr","minScore":${p.lastYearScore}}"""
        }
        return buildString {
            appendLine("Student profile:")
            appendLine("  BAC section : $sectionName")
            appendLine("  FG score    : ${String.format("%.2f", fgScore)}")
            appendLine("  Interests (student's own words) :")
            appendLine("    \"$interestText\"")
            appendLine()
            appendLine("Eligible programmes (already filtered for section + score) :")
            append(programsJson)
        }
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

    Your task is to read a student's BAC profile and their personal interests,
    then pick university programmes that genuinely match what the student cares about.

    Important context:
    - Every programme in the list is already eligible for the student (section and
      minimum score are pre-filtered). Do NOT re-rank by score — that work is done.
    - Your only job is to match programmes to the student's stated interests and
      aspirations as closely as possible.

    Selection rules:
    1. Read the "Interests" field carefully. Identify the academic domains the student
       mentions (medicine, technology, economics, arts, languages, etc.).
    2. Choose 3 to 5 programmes whose field best aligns with those interests.
       Prefer variety over clustering in a single domain when interests span multiple areas.
    3. If interests are vague, prefer programmes whose names overlap with any keyword
       the student used.
    4. Do NOT default to the highest minScore programmes — interest fit comes first.

    For each chosen programme write a personalised Arabic reason (1–2 sentences) that:
    - References something specific the student wrote about themselves.
    - Explains concretely why this programme fits that interest.
    - Sounds warm and encouraging, not generic.

    Output format — respond with ONLY a valid JSON array, no markdown, no code fence,
    no explanation, no extra text before or after:
    [{"id": <integer>, "reason": "<personalised Arabic reason>"}]
    Use only ids that appear in the provided programme list.
""".trimIndent()
