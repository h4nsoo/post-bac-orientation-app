package com.example.orientation_app.viewmodel

import androidx.lifecycle.ViewModel
import com.example.orientation_app.data.model.SectionIconType
import com.example.orientation_app.data.model.SectionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ── UI State ────────────────────────────────────────────────────────────────────

data class WelcomeUiState(
    val sections: List<SectionItem> = emptyList(),
    val selectedSectionIds: Set<String> = emptySet(),
    val optionalSubjects: List<String> = emptyList(),
    val selectedOptionalSubject: String? = null,
    val isDropdownExpanded: Boolean = false,
    val isSportExempt: Boolean = false
) {
    /** True only when ALL required fields are filled. */
    val canProceed: Boolean
        get() = selectedSectionIds.isNotEmpty() && selectedOptionalSubject != null
}

// ── ViewModel ───────────────────────────────────────────────────────────────────

class WelcomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        WelcomeUiState(
            sections = defaultSections(),
            optionalSubjects = defaultOptionalSubjects()
        )
    )
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    /** Toggle a section's selection on / off (single selection only). */
    fun toggleSection(sectionId: String) {
        _uiState.update { state ->
            val updated = if (sectionId in state.selectedSectionIds) {
                emptySet()
            } else {
                setOf(sectionId)
            }
            state.copy(selectedSectionIds = updated)
        }
    }

    fun selectOptionalSubject(subject: String?) {
        _uiState.update { it.copy(selectedOptionalSubject = subject, isDropdownExpanded = false) }
    }

    fun toggleDropdown() {
        _uiState.update { it.copy(isDropdownExpanded = !it.isDropdownExpanded) }
    }

    fun toggleSportExempt() {
        _uiState.update { it.copy(isSportExempt = !it.isSportExempt) }
    }

    // ── Seed data ───────────────────────────────────────────────────────────

    private fun defaultSections() = listOf(
        SectionItem("math", "رياضيات", SectionIconType.MATH),
        SectionItem("science", "علوم تجريبية", SectionIconType.SCIENCE),
        SectionItem("tech", "علوم تقنية", SectionIconType.TECH),
        SectionItem("info", "علوم اعلامية", SectionIconType.INFORMATICS),
        SectionItem("literature", "آداب", SectionIconType.LITERATURE),
        SectionItem("economy", "اقتصاد و تصرف", SectionIconType.ECONOMY),
        SectionItem("sports", "رياضة", SectionIconType.SPORTS)
    )

    private fun defaultOptionalSubjects() = listOf(
        "الإيطالية",
        "الإسبانية",
        "الألمانية",
        "الرسم",
        "الموسيقى",
        "التركية",
        "الروسية",
        "العلوم",
        "الرياضيات"
    )
}
