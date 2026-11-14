package com.example.orientation_app.data.remote

import com.example.orientation_app.BuildConfig
import com.example.orientation_app.domain.model.Recommendation
import com.example.orientation_app.domain.model.ScoredProgram
import com.example.orientation_app.domain.repository.IAiService
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production [IAiService] backed by the AWS Serverless Cloud Layer
 * (API Gateway HTTP v2 -> Kotlin AWS Lambda -> DynamoDB Prompt Cache + SSM + Gemini 2.5 Flash),
 * with automatic fallback to on-device Gemini SDK if the cloud stack is offline/destroyed.
 */
@Singleton
class GeminiServiceImpl @Inject constructor() : IAiService {

    private val fallbackModel: GenerativeModel by lazy {
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
        val cloudUrl = BuildConfig.CLOUD_API_URL.trim().trimEnd('/')
        if (cloudUrl.isNotEmpty()) {
            runCatching {
                return fetchFromAwsCloud(cloudUrl, sectionName, fgScore, interestText, programs)
            }.onFailure { error ->
                println("GeminiService ▶ AWS Cloud BFF unreachable (${error.message}), falling back to direct Gemini SDK.")
            }
        }

        val userMsg = buildUserMessage(sectionName, fgScore, interestText, programs)
        println("GeminiService ▶ Sending to Gemini (Direct Fallback):\n$userMsg")

        val rawJson = fallbackModel
            .generateContent(userMsg)
            .text ?: throw Exception("الذكاء الاصطناعي لم يُرجع أي نتيجة. حاول مجدداً.")

        println("GeminiService ▶ Raw Gemini response:\n$rawJson")
        return parseResponse(rawJson, programs)
    }

    // ── AWS Cloud BFF Call ────────────────────────────────────────────────────

    private suspend fun fetchFromAwsCloud(
        baseUrl: String,
        sectionName: String,
        fgScore: Double,
        interestText: String,
        programs: List<ScoredProgram>
    ): List<Recommendation> = withContext(Dispatchers.IO) {
        val endpoint = "$baseUrl/v1/recommendations"
        val programsArray = JSONArray().apply {
            programs.forEach { p ->
                put(
                    JSONObject()
                        .put("id", p.id)
                        .put("code", p.code)
                        .put("nameAr", p.nameAr.substringBefore("(").trim())
                        .put("nameFr", p.nameFr.substringBefore("(").trim())
                        .put("minScore", p.lastYearScore)
                )
            }
        }

        val requestPayload = JSONObject()
            .put("sectionName", sectionName)
            .put("fgScore", fgScore)
            .put("interestText", interestText)
            .put("programs", programsArray)
            .toString()

        println("GeminiService ▶ Calling AWS Cloud BFF: POST $endpoint")

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

        try {
            connection.outputStream.use { os ->
                os.write(requestPayload.toByteArray(StandardCharsets.UTF_8))
            }

            val status = connection.responseCode
            val responseBody = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(StandardCharsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            if (status !in 200..299) {
                throw IllegalStateException("AWS Cloud API returned HTTP $status: $responseBody")
            }

            val responseJson = JSONObject(responseBody)
            val cacheHit = responseJson.optBoolean("cacheHit", false)
            val recommendationsArray = responseJson.getJSONArray("recommendations")
            println("GeminiService ▶ AWS Cloud BFF responded (cacheHit=$cacheHit, count=${recommendationsArray.length()})")

            val index = programs.associateBy { it.id }
            (0 until recommendationsArray.length()).mapNotNull { i ->
                val obj = recommendationsArray.getJSONObject(i)
                val program = index[obj.getInt("id")] ?: return@mapNotNull null
                Recommendation(
                    program = program,
                    reason = obj.getString("reason")
                )
            }.ifEmpty {
                throw IllegalStateException("AWS Cloud API returned 0 matching recommendations")
            }
        } finally {
            connection.disconnect()
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildUserMessage(
        sectionName: String,
        fgScore: Double,
        interestText: String,
        programs: List<ScoredProgram>
    ): String {
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
            appendLine("Important: the programme list below is already deduplicated and ranked by local relevance.")
            appendLine("Choose only from this shortlist. Do not invent or switch to unrelated programmes.")
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
