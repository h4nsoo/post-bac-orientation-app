package com.example.orientation_app.viewmodel

import com.example.orientation_app.domain.model.SubjectKey
import com.example.orientation_app.domain.scoring.SectionScoreCalculator
import com.example.orientation_app.domain.usecase.GetSectionSubjectsUseCase
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ScoreViewModel].
 *
 * No Android framework or coroutine test setup is required because
 * [ScoreViewModel] uses only synchronous [kotlinx.coroutines.flow.StateFlow.update]
 * calls — [viewModelScope] is never touched.
 */
class ScoreViewModelTest {

    private lateinit var viewModel: ScoreViewModel

    @Before
    fun setUp() {
        viewModel = ScoreViewModel(
            getSectionSubjects = GetSectionSubjectsUseCase(),
            calculator         = SectionScoreCalculator()
        )
    }

    // ── configureSubjects — subject key composition ───────────────────────────

    @Test fun `configureSubjects - math section contains expected SubjectKeys`() {
        viewModel.configureSubjects("math", "", isSportExempt = false)
        val keys = viewModel.uiState.value.subjectGrades.keys

        assertThat(keys).containsAtLeast(
            SubjectKey.MATH, SubjectKey.PHYSICS, SubjectKey.SCIENCE,
            SubjectKey.ARABIC, SubjectKey.FRENCH, SubjectKey.ENGLISH,
            SubjectKey.PHILOSOPHY, SubjectKey.SPORT
        )
        // Tech-only subjects must NOT bleed into math
        assertThat(keys).containsNoneOf(SubjectKey.ELECTRICITY, SubjectKey.MECHANICS)
    }

    @Test fun `configureSubjects - tech section contains electricity and mechanics`() {
        viewModel.configureSubjects("tech", "", isSportExempt = false)
        val keys = viewModel.uiState.value.subjectGrades.keys
        assertThat(keys).containsAtLeast(SubjectKey.ELECTRICITY, SubjectKey.MECHANICS)
        // Science is NOT a tech subject
        assertThat(keys).doesNotContain(SubjectKey.SCIENCE)
    }

    @Test fun `configureSubjects - economy section contains economics and management`() {
        viewModel.configureSubjects("economy", "", isSportExempt = false)
        val keys = viewModel.uiState.value.subjectGrades.keys
        assertThat(keys).containsAtLeast(SubjectKey.ECONOMICS, SubjectKey.MANAGEMENT)
    }

    @Test fun `configureSubjects - info section contains algorithms and STI`() {
        viewModel.configureSubjects("info", "", isSportExempt = false)
        val keys = viewModel.uiState.value.subjectGrades.keys
        assertThat(keys).containsAtLeast(SubjectKey.ALGORITHMS, SubjectKey.STI)
    }

    @Test fun `configureSubjects - sport exempt omits sport key`() {
        viewModel.configureSubjects("math", "", isSportExempt = true)
        assertThat(viewModel.uiState.value.subjectGrades.keys).doesNotContain(SubjectKey.SPORT)
    }

    @Test fun `configureSubjects - not sport exempt includes sport key`() {
        viewModel.configureSubjects("math", "", isSportExempt = false)
        assertThat(viewModel.uiState.value.subjectGrades.keys).contains(SubjectKey.SPORT)
    }

    @Test fun `configureSubjects - optional subject adds prefixed key`() {
        viewModel.configureSubjects("math", "Italian", isSportExempt = false)
        assertThat(viewModel.uiState.value.subjectGrades.keys)
            .contains("${SubjectKey.OPTIONAL_PREFIX}Italian")
    }

    @Test fun `configureSubjects - blank optional subject does not add extra key`() {
        viewModel.configureSubjects("math", "", isSportExempt = false)
        val keys = viewModel.uiState.value.subjectGrades.keys
        assertThat(keys.none { it.startsWith(SubjectKey.OPTIONAL_PREFIX) }).isTrue()
    }

    // ── configureSubjects — grade preservation and clearing ──────────────────

    @Test fun `configureSubjects - switching section clears all grades`() {
        viewModel.configureSubjects("math", "", isSportExempt = false)
        viewModel.updateSubjectGrade(SubjectKey.MATH, "18")
        assertThat(viewModel.uiState.value.subjectGrades[SubjectKey.MATH]).isEqualTo("18")

        viewModel.configureSubjects("science", "", isSportExempt = false)

        // All grades must be blank — cross-section carryover is forbidden.
        viewModel.uiState.value.subjectGrades.values.forEach { grade ->
            assertThat(grade).isEmpty()
        }
    }

    @Test fun `configureSubjects - same section preserves previously typed grades`() {
        viewModel.configureSubjects("math", "", isSportExempt = false)
        viewModel.updateSubjectGrade(SubjectKey.MATH, "18")
        viewModel.updateSubjectGrade(SubjectKey.ARABIC, "15")

        // Re-configure same section with an optional subject added
        viewModel.configureSubjects("math", "Italian", isSportExempt = false)

        assertThat(viewModel.uiState.value.subjectGrades[SubjectKey.MATH]).isEqualTo("18")
        assertThat(viewModel.uiState.value.subjectGrades[SubjectKey.ARABIC]).isEqualTo("15")
    }

    @Test fun `configureSubjects - sectionId is recorded in state`() {
        viewModel.configureSubjects("economy", "", isSportExempt = false)
        assertThat(viewModel.uiState.value.sectionId).isEqualTo("economy")
    }

    @Test fun `configureSubjects - sectionDisplayName is populated for known sections`() {
        viewModel.configureSubjects("math",       "", isSportExempt = false)
        assertThat(viewModel.uiState.value.sectionDisplayName).isEqualTo("رياضيات")

        viewModel.configureSubjects("science",    "", isSportExempt = false)
        assertThat(viewModel.uiState.value.sectionDisplayName).isEqualTo("علوم تجريبية")

        viewModel.configureSubjects("info",       "", isSportExempt = false)
        assertThat(viewModel.uiState.value.sectionDisplayName).isEqualTo("علوم إعلامية")

        viewModel.configureSubjects("tech",       "", isSportExempt = false)
        assertThat(viewModel.uiState.value.sectionDisplayName).isEqualTo("علوم تقنية")

        viewModel.configureSubjects("economy",    "", isSportExempt = false)
        assertThat(viewModel.uiState.value.sectionDisplayName).isEqualTo("اقتصاد و تصرف")

        viewModel.configureSubjects("literature", "", isSportExempt = false)
        assertThat(viewModel.uiState.value.sectionDisplayName).isEqualTo("آداب")
    }

    @Test fun `configureSubjects - unknown sectionId falls back to raw id`() {
        viewModel.configureSubjects("unknown_section", "", isSportExempt = false)
        assertThat(viewModel.uiState.value.sectionDisplayName).isEqualTo("unknown_section")
    }

    @Test fun `configureSubjects - initial grades are all empty strings`() {
        viewModel.configureSubjects("science", "", isSportExempt = false)
        viewModel.uiState.value.subjectGrades.values.forEach { grade ->
            assertThat(grade).isEmpty()
        }
    }

    // ── updateSubjectGrade ────────────────────────────────────────────────────

    @Test fun `updateSubjectGrade - valid grade updates map entry`() {
        viewModel.configureSubjects("math", "", isSportExempt = false)
        viewModel.updateSubjectGrade(SubjectKey.MATH, "17.5")
        assertThat(viewModel.uiState.value.subjectGrades[SubjectKey.MATH]).isEqualTo("17.5")
    }

    @Test fun `updateSubjectGrade - triggers score recalculation`() {
        viewModel.configureSubjects("math", "", isSportExempt = true)
        // Set only math to 20; everything else blank (treated as 0 by formula).
        viewModel.updateSubjectGrade(SubjectKey.MATH, "20")
        assertThat(viewModel.uiState.value.fgScore).isGreaterThan(0.0)
    }

    @Test fun `updateSubjectGrade - invalid grade leaves map unchanged`() {
        viewModel.configureSubjects("math", "", isSportExempt = false)
        viewModel.updateSubjectGrade(SubjectKey.MATH, "25") // > 20 → rejected
        assertThat(viewModel.uiState.value.subjectGrades[SubjectKey.MATH]).isEqualTo("")
    }

    @Test fun `updateSubjectGrade - clearing a grade is allowed`() {
        viewModel.configureSubjects("math", "", isSportExempt = false)
        viewModel.updateSubjectGrade(SubjectKey.MATH, "10")
        viewModel.updateSubjectGrade(SubjectKey.MATH, "")
        assertThat(viewModel.uiState.value.subjectGrades[SubjectKey.MATH]).isEqualTo("")
    }

    // ── isValidGradeInput ─────────────────────────────────────────────────────

    @Test fun `isValidGradeInput - empty string is allowed`() {
        assertThat(viewModel.isValidGradeInput("")).isTrue()
    }

    @Test fun `isValidGradeInput - lone decimal point is allowed`() {
        assertThat(viewModel.isValidGradeInput(".")).isTrue()
    }

    @Test fun `isValidGradeInput - zero is allowed`() {
        assertThat(viewModel.isValidGradeInput("0")).isTrue()
    }

    @Test fun `isValidGradeInput - twenty is allowed`() {
        assertThat(viewModel.isValidGradeInput("20")).isTrue()
    }

    @Test fun `isValidGradeInput - valid decimal is allowed`() {
        assertThat(viewModel.isValidGradeInput("19.50")).isTrue()
    }

    @Test fun `isValidGradeInput - above 20 is rejected`() {
        assertThat(viewModel.isValidGradeInput("20.1")).isFalse()
        assertThat(viewModel.isValidGradeInput("25")).isFalse()
    }

    @Test fun `isValidGradeInput - negative values are rejected`() {
        assertThat(viewModel.isValidGradeInput("-1")).isFalse()
    }

    @Test fun `isValidGradeInput - string longer than 5 chars is rejected`() {
        assertThat(viewModel.isValidGradeInput("19.501")).isFalse()
    }

    @Test fun `isValidGradeInput - letters are rejected`() {
        assertThat(viewModel.isValidGradeInput("abc")).isFalse()
    }

    @Test fun `isValidGradeInput - multiple decimal points are rejected`() {
        assertThat(viewModel.isValidGradeInput("1.2.3")).isFalse()
    }
}
