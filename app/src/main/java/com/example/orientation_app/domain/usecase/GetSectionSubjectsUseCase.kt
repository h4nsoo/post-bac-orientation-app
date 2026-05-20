package com.example.orientation_app.domain.usecase

import com.example.orientation_app.domain.model.SubjectKey
import javax.inject.Inject

/**
 * Returns the ordered list of subject keys for a given BAC section, optional
 * subject choice, and sport-exemption flag.
 *
 * This is the **single canonical source of truth** for which subjects belong to
 * each section. Both [ScoreViewModel][com.example.orientation_app.viewmodel.ScoreViewModel]
 * and [SectionScoreCalculator][com.example.orientation_app.domain.scoring.SectionScoreCalculator]
 * read from the same [SubjectKey] constants, so key mismatches are a compile
 * error rather than a silent 0.0.
 *
 * NOTE: The *Sports* BAC section is not yet implemented — its Ministry formula
 * coefficients have not been confirmed. Passing sectionId = "sports" returns the
 * same base subjects as Math/Science as a structural placeholder; the navigation
 * guard in [WelcomeScreen] prevents students from reaching this code path.
 */
class GetSectionSubjectsUseCase @Inject constructor() {

    operator fun invoke(
        sectionId: String,
        optionalSubject: String,
        isSportExempt: Boolean
    ): List<String> {
        val base: List<String> = when (sectionId) {
            MATH, SCIENCE -> listOf(
                SubjectKey.MATH, SubjectKey.PHYSICS, SubjectKey.SCIENCE,
                SubjectKey.INFORMATICS, SubjectKey.ARABIC,
                SubjectKey.FRENCH, SubjectKey.ENGLISH, SubjectKey.PHILOSOPHY
            )
            INFO -> listOf(
                SubjectKey.ALGORITHMS, SubjectKey.STI,
                SubjectKey.MATH, SubjectKey.PHYSICS,
                SubjectKey.ARABIC, SubjectKey.FRENCH,
                SubjectKey.ENGLISH, SubjectKey.PHILOSOPHY
            )
            TECH -> listOf(
                SubjectKey.ELECTRICITY, SubjectKey.MECHANICS,
                SubjectKey.MATH, SubjectKey.PHYSICS, SubjectKey.INFORMATICS,
                SubjectKey.ARABIC, SubjectKey.FRENCH,
                SubjectKey.ENGLISH, SubjectKey.PHILOSOPHY
            )
            ECONOMY -> listOf(
                SubjectKey.ECONOMICS, SubjectKey.MANAGEMENT,
                SubjectKey.MATH, SubjectKey.INFORMATICS, SubjectKey.ARABIC,
                SubjectKey.HISTORY_GEO, SubjectKey.FRENCH,
                SubjectKey.ENGLISH, SubjectKey.PHILOSOPHY
            )
            LITERATURE -> listOf(
                SubjectKey.ISLAMIC_EDUCATION, SubjectKey.HISTORY_GEO,
                SubjectKey.INFORMATICS, SubjectKey.ARABIC,
                SubjectKey.FRENCH, SubjectKey.ENGLISH, SubjectKey.PHILOSOPHY
            )
            // Sports BAC: placeholder — spec pending Ministry confirmation.
            else -> listOf(
                SubjectKey.MATH, SubjectKey.PHYSICS, SubjectKey.SCIENCE,
                SubjectKey.INFORMATICS, SubjectKey.ARABIC,
                SubjectKey.FRENCH, SubjectKey.ENGLISH, SubjectKey.PHILOSOPHY
            )
        }

        return buildList {
            addAll(base)
            if (!isSportExempt) add(SubjectKey.SPORT)
            if (optionalSubject.isNotBlank()) {
                add("${SubjectKey.OPTIONAL_PREFIX}$optionalSubject")
            }
        }
    }

    // ── Section ID constants (keep in sync with WelcomeViewModel seed data) ──

    companion object {
        const val MATH      = "math"
        const val SCIENCE   = "science"
        const val INFO      = "info"
        const val TECH      = "tech"
        const val ECONOMY   = "economy"
        const val LITERATURE = "literature"
        const val SPORTS    = "sports"
    }
}
