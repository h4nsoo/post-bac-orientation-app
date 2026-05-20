package com.example.orientation_app.domain.scoring

import com.example.orientation_app.domain.model.SubjectKey

/**
 * Pure-Kotlin, framework-free strategy for computing a student's orientation
 * scores from their subject grades.
 *
 * [averageFromSubjects] returns the weighted *moyenneـ générale* (0–20 scale).
 * [specialtyComponent] returns the raw specialty weighted sum (added on top of
 * moyenne × 4 to produce the *note de farz / FG score*).
 *
 * All implementations are **stateless objects** — they hold no mutable state and
 * depend only on the grades map passed at call time.
 */
interface ScoreFormula {
    fun averageFromSubjects(grades: Map<String, String>): Double
    fun specialtyComponent(grades: Map<String, String>): Double
}

// ── Shared helpers ────────────────────────────────────────────────────────────

/**
 * Safely reads a grade, coerces it to [0, 20].
 * Returns 0.0 if the key is absent or the value is not a valid number.
 */
internal fun g(grades: Map<String, String>, key: String): Double =
    grades[key]?.toDoubleOrNull()?.coerceIn(0.0, 20.0) ?: 0.0

/**
 * Returns the sport grade when the student is NOT exempt (key [SubjectKey.SPORT]
 * is present in the map), or 10.0 as the neutral value when the student is exempt.
 * The denominator stays the same either way — 10.0 is the "neutral grade" per the
 * Tunisian Ministry convention.
 */
internal fun sportOrNeutral(grades: Map<String, String>): Double =
    if (SubjectKey.SPORT in grades) g(grades, SubjectKey.SPORT) else 10.0

/**
 * Optional-subject bonus: max(grade − 10, 0).
 * Only the grades above 10 contribute; grades below do not penalise the student.
 * The denominator is NOT increased, so the bonus inflates the average slightly.
 */
internal fun optionBonus(grades: Map<String, String>): Double {
    val key = grades.keys.firstOrNull { it.startsWith(SubjectKey.OPTIONAL_PREFIX) }
        ?: return 0.0
    return (g(grades, key) - 10.0).coerceAtLeast(0.0)
}

// ── Section formula implementations ──────────────────────────────────────────

/**
 * رياضيات  (Mathematics)
 *
 * Denominator = 4+4+1+1+1+1+1+1 core + 1 sport = 15
 * Specialty   = 2×math + 1.5×physics + 0.5×science + 1×english + 1×french
 */
object MathFormula : ScoreFormula {

    override fun averageFromSubjects(grades: Map<String, String>): Double {
        val sum = 4 * g(grades, SubjectKey.MATH) +
            4 * g(grades, SubjectKey.PHYSICS) +
            1 * g(grades, SubjectKey.SCIENCE) +
            1 * g(grades, SubjectKey.FRENCH) +
            1 * g(grades, SubjectKey.PHILOSOPHY) +
            1 * g(grades, SubjectKey.ARABIC) +
            1 * g(grades, SubjectKey.ENGLISH) +
            1 * g(grades, SubjectKey.INFORMATICS) +
            sportOrNeutral(grades) +
            optionBonus(grades)
        return sum / 15.0
    }

    override fun specialtyComponent(grades: Map<String, String>): Double =
        2.0 * g(grades, SubjectKey.MATH) +
            1.5 * g(grades, SubjectKey.PHYSICS) +
            0.5 * g(grades, SubjectKey.SCIENCE) +
            1.0 * g(grades, SubjectKey.ENGLISH) +
            1.0 * g(grades, SubjectKey.FRENCH)
}

/**
 * علوم تجريبية  (Experimental Sciences)
 *
 * Denominator = 3+4+4+1+1+1+1+1 core + 1 sport = 17
 * Specialty   = 1×math + 1.5×physics + 1.5×science + 1×english + 1×french
 */
object ScienceFormula : ScoreFormula {

    override fun averageFromSubjects(grades: Map<String, String>): Double {
        val sum = 3 * g(grades, SubjectKey.MATH) +
            4 * g(grades, SubjectKey.PHYSICS) +
            4 * g(grades, SubjectKey.SCIENCE) +
            1 * g(grades, SubjectKey.FRENCH) +
            1 * g(grades, SubjectKey.PHILOSOPHY) +
            1 * g(grades, SubjectKey.ARABIC) +
            1 * g(grades, SubjectKey.ENGLISH) +
            1 * g(grades, SubjectKey.INFORMATICS) +
            sportOrNeutral(grades) +
            optionBonus(grades)
        return sum / 17.0
    }

    override fun specialtyComponent(grades: Map<String, String>): Double =
        1.0 * g(grades, SubjectKey.MATH) +
            1.5 * g(grades, SubjectKey.PHYSICS) +
            1.5 * g(grades, SubjectKey.SCIENCE) +
            1.0 * g(grades, SubjectKey.ENGLISH) +
            1.0 * g(grades, SubjectKey.FRENCH)
}

/**
 * علوم إعلامية  (Computer Science / Informatics)
 *
 * Denominator = 3+2+3+3+1+1+1+1 core + 1 sport = 16
 * Specialty   = 1.5×math + 0.5×physics + 1.5×algorithms + 0.5×STI + 1×english + 1×french
 */
object InformaticsFormula : ScoreFormula {

    override fun averageFromSubjects(grades: Map<String, String>): Double {
        val sum = 3 * g(grades, SubjectKey.MATH) +
            2 * g(grades, SubjectKey.PHYSICS) +
            3 * g(grades, SubjectKey.ALGORITHMS) +
            3 * g(grades, SubjectKey.STI) +
            1 * g(grades, SubjectKey.FRENCH) +
            1 * g(grades, SubjectKey.PHILOSOPHY) +
            1 * g(grades, SubjectKey.ARABIC) +
            1 * g(grades, SubjectKey.ENGLISH) +
            sportOrNeutral(grades) +
            optionBonus(grades)
        return sum / 16.0
    }

    override fun specialtyComponent(grades: Map<String, String>): Double =
        1.5 * g(grades, SubjectKey.MATH) +
            0.5 * g(grades, SubjectKey.PHYSICS) +
            1.5 * g(grades, SubjectKey.ALGORITHMS) +
            0.5 * g(grades, SubjectKey.STI) +
            1.0 * g(grades, SubjectKey.ENGLISH) +
            1.0 * g(grades, SubjectKey.FRENCH)
}

/**
 * علوم تقنية  (Technical Sciences)
 *
 * Denominator = 3+3+3+1+1+1+1+1+1 core + 1 sport = 16
 * Specialty   = 1.5×math + 1×physics + 1.5×electricity + 0.5×mechanics + 1×english + 1×french
 *
 * NOTE: الميكانيك coefficient in specialty = 0.5.
 * FIXME: Confirm exact Mechanics specialty coefficient with the Tunisian Ministry spec.
 *        Current value 0.5 keeps the specialty total = 6, matching all other sections.
 */
object TechniqueFormula : ScoreFormula {

    override fun averageFromSubjects(grades: Map<String, String>): Double {
        val sum = 3 * g(grades, SubjectKey.MATH) +
            3 * g(grades, SubjectKey.PHYSICS) +
            3 * g(grades, SubjectKey.ELECTRICITY) +
            1 * g(grades, SubjectKey.MECHANICS) +
            1 * g(grades, SubjectKey.INFORMATICS) +
            1 * g(grades, SubjectKey.FRENCH) +
            1 * g(grades, SubjectKey.PHILOSOPHY) +
            1 * g(grades, SubjectKey.ARABIC) +
            1 * g(grades, SubjectKey.ENGLISH) +
            sportOrNeutral(grades) +
            optionBonus(grades)
        return sum / 16.0
    }

    override fun specialtyComponent(grades: Map<String, String>): Double =
        1.5 * g(grades, SubjectKey.MATH) +
            1.0 * g(grades, SubjectKey.PHYSICS) +
            1.5 * g(grades, SubjectKey.ELECTRICITY) +
            0.5 * g(grades, SubjectKey.MECHANICS) +    // FIXME: confirm coefficient
            1.0 * g(grades, SubjectKey.ENGLISH) +
            1.0 * g(grades, SubjectKey.FRENCH)
}

/**
 * اقتصاد و تصرف  (Economics and Management)
 *
 * Denominator = 2+3+3+2+1+1+1+1+1 core + 1 sport = 16
 * Specialty   = 0.5×math + 1.5×economics + 1.5×management + 0.5×history + 1×english + 1×french
 */
object EconomyFormula : ScoreFormula {

    override fun averageFromSubjects(grades: Map<String, String>): Double {
        val sum = 2 * g(grades, SubjectKey.MATH) +
            3 * g(grades, SubjectKey.MANAGEMENT) +
            3 * g(grades, SubjectKey.ECONOMICS) +
            2 * g(grades, SubjectKey.HISTORY_GEO) +
            1 * g(grades, SubjectKey.FRENCH) +
            1 * g(grades, SubjectKey.PHILOSOPHY) +
            1 * g(grades, SubjectKey.ARABIC) +
            1 * g(grades, SubjectKey.ENGLISH) +
            1 * g(grades, SubjectKey.INFORMATICS) +
            sportOrNeutral(grades) +
            optionBonus(grades)
        return sum / 16.0
    }

    override fun specialtyComponent(grades: Map<String, String>): Double =
        0.5 * g(grades, SubjectKey.MATH) +
            1.5 * g(grades, SubjectKey.ECONOMICS) +
            1.5 * g(grades, SubjectKey.MANAGEMENT) +
            0.5 * g(grades, SubjectKey.HISTORY_GEO) +
            1.0 * g(grades, SubjectKey.ENGLISH) +
            1.0 * g(grades, SubjectKey.FRENCH)
}

/**
 * آداب  (Literatures)
 *
 * Denominator = 1+3+2+4+4+2+1 core + 1 sport = 18
 * Specialty   = 1.5×arabic + 1.5×philosophy + 1×history + 1×english + 1×french
 */
object LettersFormula : ScoreFormula {

    override fun averageFromSubjects(grades: Map<String, String>): Double {
        val sum = 1 * g(grades, SubjectKey.ISLAMIC_EDUCATION) +
            3 * g(grades, SubjectKey.HISTORY_GEO) +
            2 * g(grades, SubjectKey.FRENCH) +
            4 * g(grades, SubjectKey.PHILOSOPHY) +
            4 * g(grades, SubjectKey.ARABIC) +
            2 * g(grades, SubjectKey.ENGLISH) +
            1 * g(grades, SubjectKey.INFORMATICS) +
            sportOrNeutral(grades) +
            optionBonus(grades)
        return sum / 18.0
    }

    override fun specialtyComponent(grades: Map<String, String>): Double =
        1.5 * g(grades, SubjectKey.ARABIC) +
            1.5 * g(grades, SubjectKey.PHILOSOPHY) +
            1.0 * g(grades, SubjectKey.HISTORY_GEO) +
            1.0 * g(grades, SubjectKey.ENGLISH) +
            1.0 * g(grades, SubjectKey.FRENCH)
}

// ── Factory ───────────────────────────────────────────────────────────────────

/**
 * Returns the [ScoreFormula] for the given section ID.
 * Adding a new section: implement [ScoreFormula], register it here.
 * No existing formula class needs to change — Open/Closed Principle.
 *
 * NOTE: "sports" falls back to [MathFormula] as a placeholder.
 * Its correct formula must be confirmed with the Ministry before shipping.
 */
fun scoreFormulaFor(sectionId: String): ScoreFormula = when (sectionId) {
    "math"       -> MathFormula
    "science"    -> ScienceFormula
    "info"       -> InformaticsFormula
    "tech"       -> TechniqueFormula
    "economy"    -> EconomyFormula
    "literature" -> LettersFormula
    else         -> MathFormula   // "sports" placeholder
}
