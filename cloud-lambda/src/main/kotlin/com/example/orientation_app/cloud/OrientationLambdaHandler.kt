package com.example.orientation_app.cloud

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import org.json.JSONArray
import org.json.JSONObject
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.ReturnValue
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.services.ssm.model.GetParameterRequest
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Locale

/**
 * AWS Lambda Handler for UniCompass Cloud Layer.
 *
 * Designed for Java 21 + AWS Lambda SnapStart on Graviton2 (arm64).
 *
 * Responsibilities:
 * 1. Protect the Gemini API Key via AWS SSM Parameter Store (KMS SecureString).
 * 2. Serve cached AI recommendations from DynamoDB (`unicompass-prompt-cache`) in ~35ms.
 * 3. Track national student orientation demand in DynamoDB (`unicompass-program-telemetry`).
 * 4. Expose catalog metadata from S3 (`unicompass-catalog-*`) for delta synchronization.
 */
class OrientationLambdaHandler : RequestHandler<Map<String, Any>, Map<String, Any>> {

    override fun handleRequest(event: Map<String, Any>, context: Context?): Map<String, Any> {
        val rawPath = (event["rawPath"] as? String) ?: "/"
        val requestContext = event["requestContext"] as? Map<*, *>
        val httpBlock = requestContext?.get("http") as? Map<*, *>
        val method = ((httpBlock?.get("method") as? String) ?: "GET").uppercase(Locale.ROOT)

        return try {
            when {
                method == "GET" && rawPath.endsWith("/health") -> handleHealth()
                method == "GET" && rawPath.endsWith("/catalog") -> handleCatalog()
                method == "POST" && rawPath.endsWith("/recommendations") -> {
                    val bodyStr = extractBody(event)
                    handleRecommendations(bodyStr)
                }
                else -> jsonResponse(
                    404,
                    JSONObject()
                        .put("error", "Route not found: $method $rawPath")
                        .toString()
                )
            }
        } catch (e: Exception) {
            context?.logger?.log("ERROR in OrientationLambdaHandler: ${e.message}\n${e.stackTraceToString()}")
            jsonResponse(
                500,
                JSONObject()
                    .put("error", e.message ?: "Internal server error")
                    .toString()
            )
        }
    }

    private fun handleHealth(): Map<String, Any> {
        val payload = JSONObject()
            .put("status", "UP")
            .put("service", "unicompass-kotlin-lambda")
            .put("region", awsRegion)
            .put("cacheTable", promptCacheTable)
            .put("telemetryTable", telemetryTable)
            .put("catalogBucket", catalogBucket)
            .put("timestamp", Instant.now().toString())
        return jsonResponse(200, payload.toString())
    }

    private fun handleCatalog(): Map<String, Any> {
        val head = s3Client.headObject(
            HeadObjectRequest.builder()
                .bucket(catalogBucket)
                .key(catalogObjectKey)
                .build()
        )

        val scanResult = dynamoClient.scan(
            ScanRequest.builder()
                .tableName(telemetryTable)
                .limit(50)
                .build()
        )

        val telemetryArray = JSONArray()
        scanResult.items().forEach { item ->
            val code = item["program_code"]?.s() ?: return@forEach
            val count = item["selection_count"]?.n()?.toLongOrNull() ?: 0L
            val nameFr = item["program_name_fr"]?.s() ?: ""
            telemetryArray.put(
                JSONObject()
                    .put("code", code)
                    .put("demandCount", count)
                    .put("nameFr", nameFr)
            )
        }

        val response = JSONObject()
            .put("bucket", catalogBucket)
            .put("objectKey", catalogObjectKey)
            .put("etag", head.eTag()?.trim('"') ?: "")
            .put("contentLengthBytes", head.contentLength() ?: 0L)
            .put("lastModified", head.lastModified()?.toString() ?: "")
            .put("liveTelemetry", telemetryArray)

        return jsonResponse(200, response.toString())
    }

    private fun handleRecommendations(bodyStr: String): Map<String, Any> {
        if (bodyStr.isBlank()) {
            return jsonResponse(400, JSONObject().put("error", "Request body is empty").toString())
        }

        val reqJson = JSONObject(bodyStr)
        val sectionName = reqJson.optString("sectionName", "")
        val fgScore = reqJson.optDouble("fgScore", 0.0)
        val interestText = reqJson.optString("interestText", "")
        val programsArray = reqJson.optJSONArray("programs") ?: JSONArray()

        if (programsArray.length() == 0) {
            return jsonResponse(400, JSONObject().put("error", "programs list must not be empty").toString())
        }

        val candidateIds = mutableListOf<Int>()
        val codeById = mutableMapOf<Int, String>()
        val nameFrById = mutableMapOf<Int, String>()

        for (i in 0 until programsArray.length()) {
            val p = programsArray.getJSONObject(i)
            val id = p.getInt("id")
            candidateIds.add(id)
            codeById[id] = p.optString("code", id.toString())
            nameFrById[id] = p.optString("nameFr", "")
        }

        val promptHash = computePromptHash(sectionName, fgScore, interestText, candidateIds)
        val nowSeconds = Instant.now().epochSecond

        // 1. Check DynamoDB Semantic/Exact Cache
        val cachedJson = getCachedRecommendations(promptHash, nowSeconds)
        if (cachedJson != null) {
            val recommendationsArray = JSONArray(cachedJson)
            enrichAndRecordTelemetry(recommendationsArray, codeById, nameFrById, nowSeconds)

            val responsePayload = JSONObject()
                .put("cacheHit", true)
                .put("promptHash", promptHash)
                .put("recommendations", recommendationsArray)
            return jsonResponse(200, responsePayload.toString())
        }

        // 2. Cache MISS -> Fetch decrypted Gemini key from SSM + Call Gemini API
        val apiKey = getGeminiApiKey()
        val userPrompt = buildUserMessage(sectionName, fgScore, interestText, programsArray)
        val rawModelText = callGeminiRestApi(apiKey, userPrompt)
        val cleanedRecommendations = cleanAndValidateJsonArray(rawModelText, candidateIds.toSet())

        // 3. Save to DynamoDB Cache with 48h TTL
        putCachedRecommendations(
            promptHash = promptHash,
            sectionName = sectionName,
            fgScore = fgScore,
            interestText = interestText,
            recommendationsJson = cleanedRecommendations.toString(),
            nowSeconds = nowSeconds
        )

        // 4. Increment live orientation telemetry counters per recommended program
        enrichAndRecordTelemetry(cleanedRecommendations, codeById, nameFrById, nowSeconds)

        val responsePayload = JSONObject()
            .put("cacheHit", false)
            .put("promptHash", promptHash)
            .put("recommendations", cleanedRecommendations)

        return jsonResponse(200, responsePayload.toString())
    }

    private fun getCachedRecommendations(promptHash: String, nowSeconds: Long): String? {
        val resp = dynamoClient.getItem(
            GetItemRequest.builder()
                .tableName(promptCacheTable)
                .key(mapOf("prompt_hash" to AttributeValue.fromS(promptHash)))
                .build()
        )
        if (!resp.hasItem() || resp.item().isEmpty()) return null
        val item = resp.item()
        val ttl = item["ttl"]?.n()?.toLongOrNull() ?: 0L
        if (ttl <= nowSeconds) return null
        return item["response_json"]?.s()
    }

    private fun putCachedRecommendations(
        promptHash: String,
        sectionName: String,
        fgScore: Double,
        interestText: String,
        recommendationsJson: String,
        nowSeconds: Long
    ) {
        val ttlSeconds = nowSeconds + CACHE_TTL_SECONDS
        val item = mapOf(
            "prompt_hash" to AttributeValue.fromS(promptHash),
            "section_name" to AttributeValue.fromS(sectionName),
            "fg_score" to AttributeValue.fromN(String.format(Locale.US, "%.2f", fgScore)),
            "interest_text" to AttributeValue.fromS(interestText.take(500)),
            "response_json" to AttributeValue.fromS(recommendationsJson),
            "created_at" to AttributeValue.fromN(nowSeconds.toString()),
            "ttl" to AttributeValue.fromN(ttlSeconds.toString())
        )
        dynamoClient.putItem(
            PutItemRequest.builder()
                .tableName(promptCacheTable)
                .item(item)
                .build()
        )
    }

    private fun enrichAndRecordTelemetry(
        recommendations: JSONArray,
        codeById: Map<Int, String>,
        nameFrById: Map<Int, String>,
        nowSeconds: Long
    ) {
        for (i in 0 until recommendations.length()) {
            val recObj = recommendations.getJSONObject(i)
            val id = recObj.optInt("id", -1)
            val code = codeById[id] ?: continue
            val nameFr = nameFrById[id] ?: ""

            runCatching {
                val updateResp = dynamoClient.updateItem(
                    UpdateItemRequest.builder()
                        .tableName(telemetryTable)
                        .key(mapOf("program_code" to AttributeValue.fromS(code)))
                        .updateExpression("ADD selection_count :inc SET last_selected_at = :ts, program_name_fr = :nameFr")
                        .expressionAttributeValues(
                            mapOf(
                                ":inc" to AttributeValue.fromN("1"),
                                ":ts" to AttributeValue.fromN(nowSeconds.toString()),
                                ":nameFr" to AttributeValue.fromS(nameFr)
                            )
                        )
                        .returnValues(ReturnValue.UPDATED_NEW)
                        .build()
                )
                val newCount = updateResp.attributes()["selection_count"]?.n()?.toLongOrNull() ?: 1L
                recObj.put("demandCount", newCount)
            }
        }
    }

    private fun callGeminiRestApi(apiKey: String, userMessage: String): String {
        val requestBody = JSONObject()
            .put(
                "system_instruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT))
                )
            )
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
                )
            )
            .put(
                "generationConfig",
                JSONObject().put("temperature", 1.0)
            )

        val uri = URI.create(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        )

        val request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("Gemini API returned HTTP ${response.statusCode()}: ${response.body()}")
        }

        val responseJson = JSONObject(response.body())
        val candidates = responseJson.optJSONArray("candidates")
            ?: throw IllegalStateException("Gemini returned no candidates")
        if (candidates.length() == 0) {
            throw IllegalStateException("Gemini candidates array is empty")
        }

        val content = candidates.getJSONObject(0).optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""
        if (text.isBlank()) {
            throw IllegalStateException("Gemini returned empty text")
        }
        return text
    }

    private fun extractBody(event: Map<String, Any>): String {
        val rawBody = (event["body"] as? String) ?: return ""
        val isBase64 = (event["isBase64Encoded"] as? Boolean) ?: false
        return if (isBase64) {
            String(Base64.getDecoder().decode(rawBody), StandardCharsets.UTF_8)
        } else {
            rawBody
        }
    }

    private fun jsonResponse(statusCode: Int, body: String): Map<String, Any> = mapOf(
        "statusCode" to statusCode,
        "headers" to mapOf(
            "Content-Type" to "application/json; charset=utf-8",
            "Access-Control-Allow-Origin" to "*"
        ),
        "body" to body
    )

    companion object {
        private const val CACHE_TTL_SECONDS = 48L * 3600L // 48 hours

        private val awsRegion: String = System.getenv("AWS_REGION") ?: "eu-central-1"
        private val promptCacheTable: String = System.getenv("PROMPT_CACHE_TABLE") ?: "unicompass-prompt-cache"
        private val telemetryTable: String = System.getenv("TELEMETRY_TABLE") ?: "unicompass-program-telemetry"
        private val catalogBucket: String = System.getenv("CATALOG_BUCKET") ?: ""
        private val catalogObjectKey: String = System.getenv("CATALOG_OBJECT_KEY") ?: "programs_by_section.json"
        private val ssmGeminiKeyParam: String = System.getenv("SSM_GEMINI_KEY_PARAM") ?: "/unicompass/gemini_api_key"

        private val dynamoClient: DynamoDbClient by lazy {
            DynamoDbClient.builder()
                .region(Region.of(awsRegion))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build()
        }

        private val ssmClient: SsmClient by lazy {
            SsmClient.builder()
                .region(Region.of(awsRegion))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build()
        }

        private val s3Client: S3Client by lazy {
            S3Client.builder()
                .region(Region.of(awsRegion))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build()
        }

        private val httpClient: HttpClient by lazy {
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()
        }

        @Volatile
        private var cachedGeminiKey: String? = null

        private fun getGeminiApiKey(): String {
            cachedGeminiKey?.let { if (it.isNotBlank()) return it }
            synchronized(this) {
                cachedGeminiKey?.let { if (it.isNotBlank()) return it }
                val param = ssmClient.getParameter(
                    GetParameterRequest.builder()
                        .name(ssmGeminiKeyParam)
                        .withDecryption(true)
                        .build()
                )
                val key = param.parameter().value()
                cachedGeminiKey = key
                return key
            }
        }

        internal fun computePromptHash(
            sectionName: String,
            fgScore: Double,
            interestText: String,
            candidateIds: List<Int>
        ): String {
            val normalizedSection = sectionName.trim().lowercase(Locale.ROOT)
            // Bucket score to nearest 0.5 to maximize cache hit ratio for identical student profiles
            val scoreBucket = String.format(Locale.US, "%.1f", Math.round(fgScore * 2.0) / 2.0)
            val normalizedInterest = interestText.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
            val sortedIds = candidateIds.sorted().joinToString(",")
            val rawKey = "$normalizedSection|$scoreBucket|$normalizedInterest|$sortedIds"

            val digest = MessageDigest.getInstance("SHA-256")
                .digest(rawKey.toByteArray(StandardCharsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }

        internal fun buildUserMessage(
            sectionName: String,
            fgScore: Double,
            interestText: String,
            programsArray: JSONArray
        ): String {
            val formattedPrograms = (0 until programsArray.length()).joinToString(",\n", "[\n", "\n]") { i ->
                val p = programsArray.getJSONObject(i)
                val id = p.getInt("id")
                val nameAr = p.optString("nameAr", "").substringBefore("(").trim()
                val nameFr = p.optString("nameFr", "").substringBefore("(").trim()
                val minScore = p.optDouble("minScore", 0.0)
                """  {"id":$id,"nameAr":"$nameAr","nameFr":"$nameFr","minScore":$minScore}"""
            }
            return buildString {
                appendLine("Student profile:")
                appendLine("  BAC section : $sectionName")
                appendLine("  FG score    : ${String.format(Locale.US, "%.2f", fgScore)}")
                appendLine("  Interests (student's own words) :")
                appendLine("    \"$interestText\"")
                appendLine()
                appendLine("Important: the programme list below is already deduplicated and ranked by local relevance.")
                appendLine("Choose only from this shortlist. Do not invent or switch to unrelated programmes.")
                appendLine()
                appendLine("Eligible programmes (already filtered for section + score) :")
                append(formattedPrograms)
            }
        }

        internal fun cleanAndValidateJsonArray(raw: String, validIds: Set<Int>): JSONArray {
            val cleaned = raw.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val parsed = JSONArray(cleaned)
            val validated = JSONArray()
            for (i in 0 until parsed.length()) {
                val obj = parsed.getJSONObject(i)
                val id = obj.optInt("id", -1)
                val reason = obj.optString("reason", "")
                if (id in validIds && reason.isNotBlank()) {
                    validated.put(
                        JSONObject()
                            .put("id", id)
                            .put("reason", reason)
                    )
                }
            }
            return validated
        }

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
    }
}
