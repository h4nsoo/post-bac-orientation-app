package com.example.orientation_app.domain.scoring

import javax.inject.Inject

/**
 * Pure-Kotlin domain service that wraps the [ScoreFormula] strategy and exposes
 * a single call-site for computing both the weighted average and the FG (فرز) score.
 *
 * FG formula (Tunisian Ministry spec):
 *   FG = (moyenneـ × 4) + specialtyComponent
 *
 * This class is injectable and has no Android dependencies — it can be unit-tested
 * on the JVM without any Android framework.
 */
class SectionScoreCalculator @Inject constructor() {

    data class Result(val moyenne: Double, val fgScore: Double)

    /**
     * @param sectionId  The section identifier string (e.g. "math", "science").
     * @param grades     Map of [SubjectKey] → raw string grade (as typed by the user).
     *                   Values that cannot be parsed as Double are treated as 0.
     */
    fun calculate(sectionId: String, grades: Map<String, String>): Result {
        val formula = scoreFormulaFor(sectionId)
        val moyenne = formula.averageFromSubjects(grades)
        val fg = (moyenne * 4.0) + formula.specialtyComponent(grades)
        return Result(moyenne = moyenne, fgScore = fg)
    }
}
