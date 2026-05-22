package com.example.orientation_app.domain.usecase

import com.example.orientation_app.domain.model.Recommendation
import com.example.orientation_app.domain.model.ScoredProgram
import com.example.orientation_app.domain.repository.FiliereRepository
import com.example.orientation_app.domain.repository.IAiService
import com.example.orientation_app.domain.repository.UserRepository
import javax.inject.Inject
import java.text.Normalizer
import java.util.Locale

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
        println("AiUseCase ▶ sectionId=$sectionId  sectionBit=$sectionBit  fgScore=$fgScore")
        println("AiUseCase ▶ interestText=\"$interestText\"")

        val programs = filiereRepository.getEligibleOnce(sectionBit, fgScore)
        println("AiUseCase ▶ DB returned ${programs.size} eligible programs")
        programs.take(5).forEachIndexed { i, p ->
            println("AiUseCase   [$i] id=${p.id} score=${p.lastYearScore} nameFr=${p.nameFr.take(60)}")
        }

        // prioritizePrograms already boosts interest-matched programmes and
        // penalises unrelated ones — no secondary hard family filter needed.
        // We take the top 20 so Gemini has enough variety to pick from.
        val shortlistedPrograms = prioritizePrograms(sectionName, interestText, programs)
            .take(20)
        println("AiUseCase ▶ shortlisted ${shortlistedPrograms.size} programs after relevance ranking")
        shortlistedPrograms.take(5).forEachIndexed { i, p ->
            println("AiUseCase   shortlist[$i] id=${p.id} score=${p.lastYearScore} nameFr=${p.nameFr.take(60)}")
        }

        if (shortlistedPrograms.isEmpty()) {
            throw Exception(
                "لا توجد برامج متاحة لملفك الدراسي. نقطتك قد تكون خارج نطاق البرامج المتوفرة."
            )
        }

        val recommendations = aiService.getRecommendations(
            sectionName,
            fgScore,
            interestText,
            shortlistedPrograms
        )
        println("AiUseCase ▶ LLM returned ${recommendations.size} recommendations:")
        recommendations.forEach { rec ->
            println("AiUseCase   • id=${rec.program.id} nameFr=${rec.program.nameFr.take(60)}")
        }
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

    private fun prioritizePrograms(
        sectionName: String,
        interestText: String,
        programs: List<ScoredProgram>
    ): List<ScoredProgram> {
        val normalizedInterest = normalizeText(interestText)
        val normalizedSection = normalizeText(sectionName)
        val primaryFamily = detectFamily(normalizedInterest) ?: detectFamily(normalizedSection)

        return programs
            .distinctBy { it.code.trim().lowercase(Locale.ROOT) }
            .sortedByDescending { program ->
                programRelevanceScore(program, normalizedInterest, normalizedSection, primaryFamily)
            }
    }

    private fun programRelevanceScore(
        program: ScoredProgram,
        normalizedInterest: String,
        normalizedSection: String,
        primaryFamily: ProgramFamily?
    ): Double {
        val programText = normalizeText(
            listOf(program.nameAr, program.nameFr, program.universityAr, program.universityFr, program.code)
                .joinToString(" ")
        )

        var score = 0.0
        score += overlapScore(normalizedInterest, programText) * 3.0
        score += overlapScore(normalizedSection, programText) * 1.5

        val programFamily = detectFamily(programText)
        if (primaryFamily != null && programFamily == primaryFamily) {
            score += 20.0
        }

        if (primaryFamily == ProgramFamily.TECH && programFamily == ProgramFamily.LITERATURE) {
            score -= 14.0
        }
        if (primaryFamily == ProgramFamily.ECONOMY && programFamily == ProgramFamily.LITERATURE) {
            score -= 8.0
        }

        if (programFamily == ProgramFamily.TECH && containsAny(normalizedInterest, TECH_INTEREST_KEYWORDS)) {
            score += 8.0
        }

        if (programFamily == ProgramFamily.LITERATURE && containsAny(normalizedInterest, LANGUAGE_INTEREST_KEYWORDS)) {
            score += 8.0
        }

        return score
    }

    private fun overlapScore(haystack: String, needle: String): Double {
        if (haystack.isBlank() || needle.isBlank()) return 0.0
        val tokens = haystack.split(Regex("\\s+"))
            .filter { it.length >= 3 }
            .distinct()
        return tokens.count { needle.contains(it) }.toDouble()
    }

    private fun detectFamily(text: String): ProgramFamily? = when {
        containsAny(text, TECH_INTEREST_KEYWORDS) -> ProgramFamily.TECH
        containsAny(text, LANGUAGE_INTEREST_KEYWORDS) -> ProgramFamily.LITERATURE
        containsAny(text, ECONOMY_INTEREST_KEYWORDS) -> ProgramFamily.ECONOMY
        containsAny(text, SCIENCE_INTEREST_KEYWORDS) -> ProgramFamily.SCIENCE
        containsAny(text, ENGINEERING_INTEREST_KEYWORDS) -> ProgramFamily.ENGINEERING
        containsAny(text, MEDICAL_INTEREST_KEYWORDS) -> ProgramFamily.MEDICAL
        containsAny(text, ARTS_INTEREST_KEYWORDS) -> ProgramFamily.ARTS
        containsAny(text, LAW_INTEREST_KEYWORDS) -> ProgramFamily.LAW
        else -> null
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean =
        keywords.any { keyword -> containsKeyword(text, keyword) }

    private fun containsKeyword(text: String, keyword: String): Boolean {
        if (keyword.isBlank() || text.isBlank()) return false
        return if (keyword.contains(' ')) {
            text.contains(keyword)
        } else {
            text.split(Regex("\\s+")).any { it == keyword }
        }
    }

    private fun normalizeText(value: String): String {
        // NFD decomposition splits accented letters into base + combining mark.
        // We strip the combining marks (\p{M}) FIRST so they are simply removed
        // rather than becoming spaces that would split words like "réseau" into
        // "re" and "seau" (breaking every accented French keyword).
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{M}"), "")   // drop combining diacritics without spacing
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{Nd}]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private enum class ProgramFamily {
        TECH,
        LITERATURE,
        ECONOMY,
        SCIENCE,
        ENGINEERING,
        MEDICAL,
        ARTS,
        LAW
    }

    private companion object {
        val TECH_INTEREST_KEYWORDS = listOf(
            // Arabic – interests
            "ذكاء اصطناعي",
            "الذكاء الاصطناعي",
            "تعلم آلي",
            "التعلم الآلي",
            "معلوماتية",
            "اعلامية",
            "الإعلامية",
            "حاسوب",
            "الحاسوب",
            "برمجة",
            "معطيات",
            "رقمي",
            "شبكات",
            "هندسة برمجيات",
            // English – interests
            "ai",
            "artificial intelligence",
            "machine learning",
            "algorithm",
            "informatics",
            "computer",
            "software",
            "programming",
            "coding",
            "data",
            "digital",
            "cyber",
            "network",
            "robot",
            // French – programme name keywords (accent-free; normalizeText strips accents)
            "informatique",
            "algorithme",
            "intelligence artificielle",
            "logiciel",
            "logiciels",
            "reseau",          // réseau → reseau
            "reseaux",         // réseaux → reseaux
            "ingenierie",      // ingénierie → ingenierie
            "electronique",    // électronique → electronique
            "electroniques",
            "multimedia",      // multimédia → multimedia
            "programmation",
            "algorithmique",
            "numerique",       // numérique → numerique
            "telecommunication",
            "telecomunication"
        ).map(::normalizeKeyword)

        val LANGUAGE_INTEREST_KEYWORDS = listOf(
            "language",
            "languages",
            "literature",
            "litterature",
            "civilization",
            "civilisation",
            "translation",
            "linguistics",
            "arabic",
            "english",
            "french",
            "italian",
            "spanish",
            "german"
        ).map(::normalizeKeyword)

        val ECONOMY_INTEREST_KEYWORDS = listOf(
            "economy",
            "economics",
            "management",
            "finance",
            "business",
            "marketing",
            "commerce",
            "accounting",
            "trade"
        ).map(::normalizeKeyword)

        val SCIENCE_INTEREST_KEYWORDS = listOf(
            "science",
            "sciences",
            "biology",
            "chemistry",
            "physics",
            "mathematics",
            "math",
            "research"
        ).map(::normalizeKeyword)

        val ENGINEERING_INTEREST_KEYWORDS = listOf(
            "engineering",
            "engineer",
            "mechanical",
            "mechanics",
            "electricity",
            "electrical",
            "industrial",
            "civil",
            "architecture",
            "mechatronics"
        ).map(::normalizeKeyword)

        val MEDICAL_INTEREST_KEYWORDS = listOf(
            "medicine",
            "medical",
            "health",
            "pharmacy",
            "nursing",
            "dental",
            "doctor"
        ).map(::normalizeKeyword)

        val ARTS_INTEREST_KEYWORDS = listOf(
            "arts",
            "design",
            "music",
            "theatre",
            "theater",
            "cinema",
            "media"
        ).map(::normalizeKeyword)

        val LAW_INTEREST_KEYWORDS = listOf(
            "law",
            "legal",
            "juridical",
            "juridique",
            "rights",
            "justice"
        ).map(::normalizeKeyword)

        fun normalizeKeyword(value: String): String {
            val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
            return decomposed
                .replace(Regex("\\p{M}"), "")
                .lowercase(Locale.ROOT)
                .trim()
        }
    }
}
