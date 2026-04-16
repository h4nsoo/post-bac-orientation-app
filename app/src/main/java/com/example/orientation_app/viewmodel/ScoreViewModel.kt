package com.example.orientation_app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ── UI State ────────────────────────────────────────────────────────────────────

/**
 * Holds every grade field on the score-entry screen.
 * Subject keys follow the design: رياضيات, فيزياء, علوم, تقنية, فرنسية, إنجليزية.
 */
data class ScoreUiState(
    val sectionId: String = "",
    val subjectGrades: Map<String, String> = defaultSubjectGrades(),
    val fgScore: Double = 0.0,
    val computedAverage: Double = 0.0,
    val scoreWithRegionalBonus: Double = 0.0
)

private fun defaultSubjectGrades(): Map<String, String> = linkedMapOf(
    "رياضيات" to "",
    "فيزياء" to "",
    "علوم" to "",
    "تقنية" to "",
    "فرنسية" to "",
    "إنجليزية" to ""
)

// ── ViewModel ───────────────────────────────────────────────────────────────────

class ScoreViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScoreUiState())
    val uiState: StateFlow<ScoreUiState> = _uiState.asStateFlow()

    /** Update a single subject grade. */
    fun updateSubjectGrade(subject: String, value: String) {
        if (isValidGradeInput(value)) {
            _uiState.update { state ->
                val updatedGrades = state.subjectGrades.toMutableMap().apply {
                    this[subject] = value
                }
                recalculate(state.copy(subjectGrades = updatedGrades))
            }
        }
    }

    /** Configure subject fields dynamically based on selected section rules. */
    fun configureSubjects(sectionId: String, subjects: List<String>) {
        _uiState.update { state ->
            val uniqueSubjects = subjects.distinct().filter { it.isNotBlank() }
            val remapped = linkedMapOf<String, String>()
            uniqueSubjects.forEach { subject ->
                remapped[subject] = state.subjectGrades[subject].orEmpty()
            }
            recalculate(
                state.copy(
                    sectionId = sectionId,
                    subjectGrades = remapped
                )
            )
        }
    }

    private fun recalculate(state: ScoreUiState): ScoreUiState {
        val formula = scoreFormulaForSection(state.sectionId)

        val computedMoyenne = formula.averageFromSubjects(state.subjectGrades)
        val fg = (computedMoyenne * 4.0) + formula.specialtyComponent(state.subjectGrades)
        val score7 = fg * 1.07

        return state.copy(
            computedAverage = computedMoyenne,
            fgScore = fg,
            scoreWithRegionalBonus = score7
        )
    }

    private fun isValidGradeInput(value: String): Boolean {
        if (value.isEmpty()) return true
        if (value.length > 5) return false
        if (!value.all { it.isDigit() || it == '.' }) return false
        if (value.count { it == '.' } > 1) return false
        if (value == ".") return true

        val parsed = value.toDoubleOrNull() ?: return false
        return parsed in 0.0..20.0
    }
}

private interface ScoreFormula {
    fun averageFromSubjects(grades: Map<String, String>): Double
    fun specialtyComponent(grades: Map<String, String>): Double
}

private fun scoreFormulaForSection(sectionId: String): ScoreFormula = when (sectionId) {
    "math" -> MathFormula
    "science" -> ScienceFormula
    "info" -> InformaticsFormula
    "tech" -> TechniqueFormula
    "economy" -> EconomyFormula
    "literature" -> LettersFormula
    else -> MathFormula
}

private object MathFormula : ScoreFormula {
    override fun averageFromSubjects(grades: Map<String, String>): Double {
        val sum = 4 * g(grades, "الرياضيات") +
            4 * g(grades, "الفيزياء") +
            g(grades, "العلوم") +
            g(grades, "الفرنسية") +
            g(grades, "الفلسفة") +
            g(grades, "العربية") +
            g(grades, "الإنجليزية") +
            g(grades, "الإعلامية") +
            sportOrNeutral(grades) +
            optionBonus(grades)
        return sum / 15.0
    }

    override fun specialtyComponent(grades: Map<String, String>): Double =
        (2 * g(grades, "الرياضيات")) +
            (1.5 * g(grades, "الفيزياء")) +
            (0.5 * g(grades, "العلوم")) +
            g(grades, "الإنجليزية") +
            g(grades, "الفرنسية")
}

private object ScienceFormula : ScoreFormula {
    override fun averageFromSubjects(grades: Map<String, String>): Double {
        val sum = 3 * g(grades, "الرياضيات") +
            4 * g(grades, "الفيزياء") +
            4 * g(grades, "العلوم") +
            g(grades, "الفرنسية") +
            g(grades, "الفلسفة") +
            g(grades, "العربية") +
            g(grades, "الإنجليزية") +
            g(grades, "الإعلامية") +
            sportOrNeutral(grades) +
            optionBonus(grades)
        return sum / 17.0
    }

    override fun specialtyComponent(grades: Map<String, String>): Double =
        g(grades, "الرياضيات") +
            (1.5 * g(grades, "الفيزياء")) +
            (1.5 * g(grades, "العلوم")) +
            g(grades, "الإنجليزية") +
            g(grades, "الفرنسية")
}

private object InformaticsFormula : ScoreFormula {
    override fun averageFromSubjects(grades: Map<String, String>): Double {
        val sum = 3 * g(grades, "الرياضيات") +
            2 * g(grades, "الفيزياء") +
            3 * g(grades, "الخوارزميات") +
            3 * g(grades, "STI") +
            g(grades, "الفرنسية") +
            g(grades, "الفلسفة") +
            g(grades, "العربية") +
            g(grades, "الإنجليزية") +
            sportOrNeutral(grades) +
            optionBonus(grades)
        return sum / 16.0
    }

    override fun specialtyComponent(grades: Map<String, String>): Double =
        (1.5 * g(grades, "الرياضيات")) +
            (0.5 * g(grades, "الفيزياء")) +
            (1.5 * g(grades, "الخوارزميات")) +
            (0.5 * g(grades, "STI")) +
            g(grades, "الإنجليزية") +
            g(grades, "الفرنسية")
}

private object TechniqueFormula : ScoreFormula {
    override fun averageFromSubjects(grades: Map<String, String>): Double {
        val sum = 3 * g(grades, "الرياضيات") +
            3 * g(grades, "الفيزياء") +
            3 * g(grades, "الكهرباء") +
            g(grades, "الميكانيك") +
            g(grades, "الإعلامية") +
            g(grades, "الفرنسية") +
            g(grades, "الفلسفة") +
            g(grades, "العربية") +
            g(grades, "الإنجليزية") +
            sportOrNeutral(grades) +
            optionBonus(grades)
        return sum / 16.0
    }

    override fun specialtyComponent(grades: Map<String, String>): Double =
        (1.5 * g(grades, "الرياضيات")) +
            g(grades, "الفيزياء") +
            (1.5 * g(grades, "الكهرباء")) +
            g(grades, "الإنجليزية") +
            g(grades, "الفرنسية")
}

private object EconomyFormula : ScoreFormula {
    override fun averageFromSubjects(grades: Map<String, String>): Double {
        val sum = 2 * g(grades, "الرياضيات") +
            3 * g(grades, "التصرف") +
            3 * g(grades, "الاقتصاد") +
            2 * g(grades, "تاريخ و جغرافيا") +
            g(grades, "الفرنسية") +
            g(grades, "الفلسفة") +
            g(grades, "العربية") +
            g(grades, "الإنجليزية") +
            g(grades, "الإعلامية") +
            sportOrNeutral(grades) +
            optionBonus(grades)
        return sum / 16.0
    }

    override fun specialtyComponent(grades: Map<String, String>): Double =
        (0.5 * g(grades, "الرياضيات")) +
            (1.5 * g(grades, "الاقتصاد")) +
            (1.5 * g(grades, "التصرف")) +
            (0.5 * g(grades, "تاريخ و جغرافيا")) +
            g(grades, "الإنجليزية") +
            g(grades, "الفرنسية")
}

private object LettersFormula : ScoreFormula {
    override fun averageFromSubjects(grades: Map<String, String>): Double {
        val sum = g(grades, "التربية الإسلامية (PISL)") +
            3 * g(grades, "تاريخ و جغرافيا") +
            2 * g(grades, "الفرنسية") +
            4 * g(grades, "الفلسفة") +
            4 * g(grades, "العربية") +
            2 * g(grades, "الإنجليزية") +
            g(grades, "الإعلامية") +
            sportOrNeutral(grades) +
            optionBonus(grades)
        return sum / 18.0
    }

    override fun specialtyComponent(grades: Map<String, String>): Double =
        (1.5 * g(grades, "العربية")) +
            (1.5 * g(grades, "الفلسفة")) +
            g(grades, "تاريخ و جغرافيا") +
            g(grades, "الإنجليزية") +
            g(grades, "الفرنسية")
}

private fun g(grades: Map<String, String>, key: String): Double =
    grades[key]?.toDoubleOrNull()?.coerceIn(0.0, 20.0) ?: 0.0

private fun optionBonus(grades: Map<String, String>): Double {
    val optionKey = grades.keys.firstOrNull { it.startsWith("اختياري:") } ?: return 0.0
    val option = g(grades, optionKey)
    return (option - 10.0).coerceAtLeast(0.0)
}

private fun sportOrNeutral(grades: Map<String, String>): Double {
    return if ("الرياضة" in grades) g(grades, "الرياضة") else 10.0
}
