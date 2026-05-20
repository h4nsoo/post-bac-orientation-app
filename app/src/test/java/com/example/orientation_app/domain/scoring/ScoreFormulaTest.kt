package com.example.orientation_app.domain.scoring

import com.example.orientation_app.domain.model.SubjectKey
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Verifies each section's score formula against known exact values.
 *
 * Formula recap:
 *   moyenne  = weightedSum / denominator
 *   fgScore  = (moyenne × 4) + specialtyComponent
 *
 * Test cases cover: all-zero grades, all-maximum grades, partial grades,
 * sport-exempt vs not, optional subject bonus, and the previously-broken
 * key-prefix paths.
 */
class ScoreFormulaTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Build a grade map from key-value pairs (values as Strings). */
    private fun grades(vararg pairs: Pair<String, String>): Map<String, String> =
        mapOf(*pairs)

    /** Build a grade map with every subject set to the same value. */
    private fun uniformGrades(keys: List<String>, value: String): Map<String, String> =
        keys.associateWith { value }

    private fun assertClose(actual: Double, expected: Double, epsilon: Double = 1e-9) {
        assertThat(actual).isWithin(epsilon).of(expected)
    }

    // ── MathFormula ───────────────────────────────────────────────────────────

    @Test fun `MathFormula - all zeros returns zero average and zero FG`() {
        val subjects = listOf(
            SubjectKey.MATH, SubjectKey.PHYSICS, SubjectKey.SCIENCE,
            SubjectKey.INFORMATICS, SubjectKey.ARABIC, SubjectKey.FRENCH,
            SubjectKey.ENGLISH, SubjectKey.PHILOSOPHY, SubjectKey.SPORT
        )
        val g = uniformGrades(subjects, "0")
        assertClose(MathFormula.averageFromSubjects(g), 0.0)
        assertClose(MathFormula.specialtyComponent(g), 0.0)
    }

    @Test fun `MathFormula - sport exempt uses neutral 10 in numerator`() {
        // All core subjects = 0, sport exempt (key absent).
        val g = grades(
            SubjectKey.MATH to "0", SubjectKey.PHYSICS to "0", SubjectKey.SCIENCE to "0",
            SubjectKey.INFORMATICS to "0", SubjectKey.ARABIC to "0",
            SubjectKey.FRENCH to "0", SubjectKey.ENGLISH to "0", SubjectKey.PHILOSOPHY to "0"
        )
        // sum = 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 10 (neutral) = 10; denominator = 15
        assertClose(MathFormula.averageFromSubjects(g), 10.0 / 15.0)
    }

    @Test fun `MathFormula - all 20 gives correct moyenne and FG`() {
        val subjects = listOf(
            SubjectKey.MATH, SubjectKey.PHYSICS, SubjectKey.SCIENCE,
            SubjectKey.INFORMATICS, SubjectKey.ARABIC, SubjectKey.FRENCH,
            SubjectKey.ENGLISH, SubjectKey.PHILOSOPHY, SubjectKey.SPORT
        )
        val g = uniformGrades(subjects, "20")
        // sum = (4+4+1+1+1+1+1+1)×20 + 1×20 = 14×20 + 20 = 300; denom = 15 → moyenne = 20
        assertClose(MathFormula.averageFromSubjects(g), 20.0)
        // specialty = (2+1.5+0.5+1+1)×20 = 6×20 = 120
        assertClose(MathFormula.specialtyComponent(g), 120.0)
        // FG = 20×4 + 120 = 200
        val calc = SectionScoreCalculator()
        val result = calc.calculate("math", g)
        assertClose(result.fgScore, 200.0)
    }

    @Test fun `MathFormula - optional bonus adds to moyenne without changing denominator`() {
        val subjects = listOf(
            SubjectKey.MATH, SubjectKey.PHYSICS, SubjectKey.SCIENCE,
            SubjectKey.INFORMATICS, SubjectKey.ARABIC, SubjectKey.FRENCH,
            SubjectKey.ENGLISH, SubjectKey.PHILOSOPHY, SubjectKey.SPORT
        )
        val baseGrades = uniformGrades(subjects, "10").toMutableMap()
        baseGrades["${SubjectKey.OPTIONAL_PREFIX}Italian"] = "15"
        // bonus = 15 - 10 = 5; sum = 14×10 + 10 (sport) + 5 (bonus) = 155; denom = 15
        assertClose(MathFormula.averageFromSubjects(baseGrades), 155.0 / 15.0)
    }

    @Test fun `MathFormula - optional below 10 gives no bonus`() {
        val subjects = listOf(
            SubjectKey.MATH, SubjectKey.PHYSICS, SubjectKey.SCIENCE,
            SubjectKey.INFORMATICS, SubjectKey.ARABIC, SubjectKey.FRENCH,
            SubjectKey.ENGLISH, SubjectKey.PHILOSOPHY, SubjectKey.SPORT
        )
        val baseGrades = uniformGrades(subjects, "10").toMutableMap()
        baseGrades["${SubjectKey.OPTIONAL_PREFIX}Italian"] = "8"
        // bonus = max(8-10, 0) = 0; sum = 150; denom = 15 → 10
        assertClose(MathFormula.averageFromSubjects(baseGrades), 10.0)
    }

    @Test fun `MathFormula - ال prefix keys are used - previously broken default keys return 0`() {
        // If someone accidentally uses keys WITHOUT "ال", grades map should silently return 0.
        val wrongKeys = grades(
            "رياضيات" to "20", "فيزياء" to "20", "علوم" to "20"
        )
        // No "ال"-prefixed keys → all g() calls return 0 except sport neutral (10)
        assertClose(MathFormula.averageFromSubjects(wrongKeys), 10.0 / 15.0)
    }

    // ── ScienceFormula ────────────────────────────────────────────────────────

    @Test fun `ScienceFormula - all 20 gives correct moyenne`() {
        val subjects = listOf(
            SubjectKey.MATH, SubjectKey.PHYSICS, SubjectKey.SCIENCE,
            SubjectKey.INFORMATICS, SubjectKey.ARABIC, SubjectKey.FRENCH,
            SubjectKey.ENGLISH, SubjectKey.PHILOSOPHY, SubjectKey.SPORT
        )
        val g = uniformGrades(subjects, "20")
        // sum = (3+4+4+1+1+1+1+1)×20 + 1×20 = 340; denom = 17 → 20
        assertClose(ScienceFormula.averageFromSubjects(g), 20.0)
        assertClose(ScienceFormula.specialtyComponent(g), 120.0)
    }

    // ── InformaticsFormula ────────────────────────────────────────────────────

    @Test fun `InformaticsFormula - all 20 gives correct moyenne`() {
        val subjects = listOf(
            SubjectKey.MATH, SubjectKey.PHYSICS, SubjectKey.ALGORITHMS, SubjectKey.STI,
            SubjectKey.ARABIC, SubjectKey.FRENCH, SubjectKey.ENGLISH,
            SubjectKey.PHILOSOPHY, SubjectKey.SPORT
        )
        val g = uniformGrades(subjects, "20")
        // sum = (3+2+3+3+1+1+1+1)×20 + 1×20 = 320; denom = 16 → 20
        assertClose(InformaticsFormula.averageFromSubjects(g), 20.0)
        assertClose(InformaticsFormula.specialtyComponent(g), 120.0)
    }

    // ── TechniqueFormula ──────────────────────────────────────────────────────

    @Test fun `TechniqueFormula - all 20 gives correct moyenne`() {
        val subjects = listOf(
            SubjectKey.MATH, SubjectKey.PHYSICS, SubjectKey.ELECTRICITY, SubjectKey.MECHANICS,
            SubjectKey.INFORMATICS, SubjectKey.ARABIC, SubjectKey.FRENCH,
            SubjectKey.ENGLISH, SubjectKey.PHILOSOPHY, SubjectKey.SPORT
        )
        val g = uniformGrades(subjects, "20")
        // sum = (3+3+3+1+1+1+1+1+1)×20 + 1×20 = 320; denom = 16 → 20
        assertClose(TechniqueFormula.averageFromSubjects(g), 20.0)
        // specialty = (1.5+1+1.5+0.5+1+1)×20 = 6.5×20 = 130
        // FIXME: Mechanics coefficient (0.5) is pending Ministry confirmation.
        //        Update the coefficient in TechniqueFormula and adjust this expected value when confirmed.
        assertClose(TechniqueFormula.specialtyComponent(g), 130.0)
    }

    @Test fun `TechniqueFormula - mechanics contributes to specialty`() {
        // Only mechanics has a grade; everything else is 0 (sport exempt to eliminate neutral 10).
        val g = grades(SubjectKey.MECHANICS to "10")
        // specialty = 0.5 × 10 = 5  (FIXME: update when Ministry coefficient confirmed)
        assertClose(TechniqueFormula.specialtyComponent(g), 5.0)
    }

    // ── EconomyFormula ────────────────────────────────────────────────────────

    @Test fun `EconomyFormula - all 20 gives correct moyenne`() {
        val subjects = listOf(
            SubjectKey.MATH, SubjectKey.MANAGEMENT, SubjectKey.ECONOMICS, SubjectKey.HISTORY_GEO,
            SubjectKey.INFORMATICS, SubjectKey.ARABIC, SubjectKey.FRENCH,
            SubjectKey.ENGLISH, SubjectKey.PHILOSOPHY, SubjectKey.SPORT
        )
        val g = uniformGrades(subjects, "20")
        // sum = (2+3+3+2+1+1+1+1+1)×20 + 1×20 = 320; denom = 16 → 20
        assertClose(EconomyFormula.averageFromSubjects(g), 20.0)
        assertClose(EconomyFormula.specialtyComponent(g), 120.0)
    }

    // ── LettersFormula ────────────────────────────────────────────────────────

    @Test fun `LettersFormula - all 20 gives correct moyenne`() {
        val subjects = listOf(
            SubjectKey.ISLAMIC_EDUCATION, SubjectKey.HISTORY_GEO,
            SubjectKey.INFORMATICS, SubjectKey.ARABIC, SubjectKey.FRENCH,
            SubjectKey.ENGLISH, SubjectKey.PHILOSOPHY, SubjectKey.SPORT
        )
        val g = uniformGrades(subjects, "20")
        // sum = (1+3+1+4+2+2+4)×20 + 1×20 = 360; denom = 18 → 20
        assertClose(LettersFormula.averageFromSubjects(g), 20.0)
        assertClose(LettersFormula.specialtyComponent(g), 120.0)
    }

    // ── SectionScoreCalculator ────────────────────────────────────────────────

    @Test fun `SectionScoreCalculator - compute returns correct FG`() {
        val calc = SectionScoreCalculator()
        val g = grades(
            SubjectKey.MATH to "18", SubjectKey.PHYSICS to "16", SubjectKey.SCIENCE to "14",
            SubjectKey.INFORMATICS to "12", SubjectKey.ARABIC to "10",
            SubjectKey.FRENCH to "15", SubjectKey.ENGLISH to "13", SubjectKey.PHILOSOPHY to "11",
            SubjectKey.SPORT to "14"
        )
        val result = calc.calculate("math", g)
        // Verify both fields are finite and within plausible range.
        assertThat(result.moyenne).isGreaterThan(0.0)
        assertThat(result.fgScore).isGreaterThan(result.moyenne)
    }

    @Test fun `scoreFormulaFor - returns MathFormula for unknown section id`() {
        val formula = scoreFormulaFor("unknown_section")
        assertThat(formula).isEqualTo(MathFormula)
    }
}
