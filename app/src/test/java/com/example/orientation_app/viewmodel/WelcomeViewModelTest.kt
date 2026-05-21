package com.example.orientation_app.viewmodel

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [WelcomeViewModel].
 *
 * No Android framework or coroutine test infrastructure needed — the ViewModel
 * uses only synchronous [kotlinx.coroutines.flow.StateFlow.update] calls.
 */
class WelcomeViewModelTest {

    private lateinit var viewModel: WelcomeViewModel

    @Before
    fun setUp() {
        viewModel = WelcomeViewModel()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test fun `initial sections list is non-empty`() {
        assertThat(viewModel.uiState.value.sections).isNotEmpty()
    }

    @Test fun `initial sections list contains all 7 BAC sections`() {
        val ids = viewModel.uiState.value.sections.map { it.id }
        assertThat(ids).containsAtLeast(
            "math", "science", "tech", "info", "literature", "economy", "sports"
        )
    }

    @Test fun `initial optional subjects list is non-empty`() {
        assertThat(viewModel.uiState.value.optionalSubjects).isNotEmpty()
    }

    @Test fun `initial canProceed is false`() {
        assertThat(viewModel.uiState.value.canProceed).isFalse()
    }

    @Test fun `initial selectedSectionId is null`() {
        assertThat(viewModel.uiState.value.selectedSectionId).isNull()
    }

    @Test fun `initial selectedOptionalSubject is empty string`() {
        assertThat(viewModel.uiState.value.selectedOptionalSubject).isEmpty()
    }

    @Test fun `initial isSportExempt is false`() {
        assertThat(viewModel.uiState.value.isSportExempt).isFalse()
    }

    @Test fun `initial isDropdownExpanded is false`() {
        assertThat(viewModel.uiState.value.isDropdownExpanded).isFalse()
    }

    // ── toggleSection ─────────────────────────────────────────────────────────

    @Test fun `toggleSection selects a section`() {
        viewModel.toggleSection("math")
        assertThat(viewModel.uiState.value.selectedSectionIds).containsExactly("math")
    }

    @Test fun `toggleSection makes canProceed true`() {
        viewModel.toggleSection("science")
        assertThat(viewModel.uiState.value.canProceed).isTrue()
    }

    @Test fun `toggleSection twice deselects the section`() {
        viewModel.toggleSection("math")
        viewModel.toggleSection("math")
        assertThat(viewModel.uiState.value.selectedSectionIds).isEmpty()
    }

    @Test fun `toggleSection twice resets canProceed to false`() {
        viewModel.toggleSection("math")
        viewModel.toggleSection("math")
        assertThat(viewModel.uiState.value.canProceed).isFalse()
    }

    @Test fun `toggleSection replaces previous selection - only one section at a time`() {
        viewModel.toggleSection("math")
        viewModel.toggleSection("science")
        assertThat(viewModel.uiState.value.selectedSectionIds).containsExactly("science")
        assertThat(viewModel.uiState.value.selectedSectionId).isEqualTo("science")
    }

    @Test fun `selectedSectionId returns the selected section id`() {
        viewModel.toggleSection("tech")
        assertThat(viewModel.uiState.value.selectedSectionId).isEqualTo("tech")
    }

    // ── canProceed — optional subject no longer required ──────────────────────

    @Test fun `canProceed is true with only a section selected and no optional subject`() {
        viewModel.toggleSection("economy")
        // selectedOptionalSubject is empty — that is now explicitly allowed.
        assertThat(viewModel.uiState.value.selectedOptionalSubject).isEmpty()
        assertThat(viewModel.uiState.value.canProceed).isTrue()
    }

    @Test fun `canProceed is true with section and optional subject both set`() {
        viewModel.toggleSection("math")
        viewModel.selectOptionalSubject("الإيطالية")
        assertThat(viewModel.uiState.value.canProceed).isTrue()
    }

    // ── selectOptionalSubject ─────────────────────────────────────────────────

    @Test fun `selectOptionalSubject updates the selection`() {
        viewModel.selectOptionalSubject("الإسبانية")
        assertThat(viewModel.uiState.value.selectedOptionalSubject).isEqualTo("الإسبانية")
    }

    @Test fun `selectOptionalSubject with empty string clears the selection`() {
        viewModel.selectOptionalSubject("الإيطالية")
        viewModel.selectOptionalSubject("")
        assertThat(viewModel.uiState.value.selectedOptionalSubject).isEmpty()
    }

    @Test fun `selectOptionalSubject closes the dropdown`() {
        viewModel.toggleDropdown()
        assertThat(viewModel.uiState.value.isDropdownExpanded).isTrue()

        viewModel.selectOptionalSubject("الألمانية")
        assertThat(viewModel.uiState.value.isDropdownExpanded).isFalse()
    }

    // ── toggleDropdown ────────────────────────────────────────────────────────

    @Test fun `toggleDropdown opens the dropdown`() {
        viewModel.toggleDropdown()
        assertThat(viewModel.uiState.value.isDropdownExpanded).isTrue()
    }

    @Test fun `toggleDropdown closes an already-open dropdown`() {
        viewModel.toggleDropdown()
        viewModel.toggleDropdown()
        assertThat(viewModel.uiState.value.isDropdownExpanded).isFalse()
    }

    // ── toggleSportExempt ─────────────────────────────────────────────────────

    @Test fun `toggleSportExempt sets isSportExempt to true`() {
        viewModel.toggleSportExempt()
        assertThat(viewModel.uiState.value.isSportExempt).isTrue()
    }

    @Test fun `toggleSportExempt is a proper toggle`() {
        viewModel.toggleSportExempt()
        viewModel.toggleSportExempt()
        assertThat(viewModel.uiState.value.isSportExempt).isFalse()
    }
}
