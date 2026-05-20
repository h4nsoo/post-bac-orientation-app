package com.example.orientation_app.viewmodel

import androidx.lifecycle.ViewModel
import com.example.orientation_app.domain.scoring.SectionScoreCalculator
import com.example.orientation_app.domain.usecase.GetSectionSubjectsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// ── UI state ─────────────────────────────────────────────────────────────────

/**
 * Represents the grade-entry screen state.
 *
 * [subjectGrades] keys are always [SubjectKey][com.example.orientation_app.domain.model.SubjectKey]
 * constants — never raw literals — so a formula lookup can never silently miss.
 */
data class ScoreUiState(
    val sectionId: String = "",
    /** Ordered map: SubjectKey → user-typed grade string (may be empty or partial). */
    val subjectGrades: Map<String, String> = emptyMap(),
    val fgScore: Double = 0.0,
    val computedAverage: Double = 0.0
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ScoreViewModel @Inject constructor(
    private val getSectionSubjects: GetSectionSubjectsUseCase,
    private val calculator: SectionScoreCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScoreUiState())
    val uiState: StateFlow<ScoreUiState> = _uiState.asStateFlow()

    /**
     * Called when the user arrives at the score-entry screen with a new
     * [sectionId], [optionalSubject], or [isSportExempt] value.
     *
     * Rebuilds the subjects map using [GetSectionSubjectsUseCase] — the single
     * canonical source of truth — and preserves any grades the user has already
     * typed for subjects that appear in the new section too.
     *
     * Cross-section grade leakage is prevented: grades for subjects NOT in the
     * new section are discarded. Within a section, changing optional subject or
     * sport-exempt clears only the relevant entry.
     */
    fun configureSubjects(
        sectionId: String,
        optionalSubject: String,
        isSportExempt: Boolean
    ) {
        _uiState.update { state ->
            val subjects = getSectionSubjects(sectionId, optionalSubject, isSportExempt)
            val remapped = LinkedHashMap<String, String>(subjects.size)
            subjects.forEach { key ->
                // Preserve an existing grade only if the key matches exactly.
                remapped[key] = if (state.sectionId == sectionId) {
                    state.subjectGrades[key].orEmpty()
                } else {
                    ""  // Fresh section — start with empty grades to prevent cross-section carryover.
                }
            }
            recalculate(state.copy(sectionId = sectionId, subjectGrades = remapped))
        }
    }

    /** Updates a single subject grade and immediately recalculates the score. */
    fun updateSubjectGrade(subject: String, value: String) {
        if (!isValidGradeInput(value)) return
        _uiState.update { state ->
            val updated = state.subjectGrades.toMutableMap().apply { this[subject] = value }
            recalculate(state.copy(subjectGrades = updated))
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun recalculate(state: ScoreUiState): ScoreUiState {
        val result = calculator.calculate(state.sectionId, state.subjectGrades)
        return state.copy(computedAverage = result.moyenne, fgScore = result.fgScore)
    }

    /**
     * Accepts the grade string while the user is still typing.
     * Rules:
     *  - Empty string → allowed (cleared field)
     *  - A lone "." → allowed (user starting to type a decimal)
     *  - At most one decimal point
     *  - Maximum 5 characters (e.g. "19.50")
     *  - Parsed value must be in [0, 20]
     */
    internal fun isValidGradeInput(value: String): Boolean {
        if (value.isEmpty()) return true
        if (value.length > 5) return false
        if (!value.all { it.isDigit() || it == '.' }) return false
        if (value.count { it == '.' } > 1) return false
        if (value == ".") return true
        val parsed = value.toDoubleOrNull() ?: return false
        return parsed in 0.0..20.0
    }
}
