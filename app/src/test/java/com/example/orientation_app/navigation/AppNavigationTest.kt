package com.example.orientation_app.navigation

import com.example.orientation_app.domain.usecase.GetSectionSubjectsUseCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Navigation tests — route contract verification.
 *
 * These tests are pure JVM: no Android context, no emulator, no Compose test
 * rule. They verify the navigation layer's contracts so that a broken route
 * constant or an off-by-one in the score formatter surfaces as a clear test
 * failure rather than a runtime crash on screen transition.
 *
 * Coverage:
 *  - [NavRoutes] constants match the strings registered in the NavHost.
 *  - Route patterns contain the expected argument placeholders.
 *  - [NavRoutes.formatScore] preserves precision and uses US locale (period,
 *    not comma — critical for [toDoubleOrNull] on the receiving screen).
 *  - Section display names are consistent: [GetSectionSubjectsUseCase.displayName]
 *    covers every known BAC section and returns the section ID itself for
 *    unknown inputs (fallback contract).
 *  - All top-level route constants are mutually distinct (no accidental alias).
 */
class AppNavigationTest {

    // ── Route constants ───────────────────────────────────────────────────────

    @Test fun `WELCOME route constant is correct`() {
        assertThat(NavRoutes.WELCOME).isEqualTo("welcome")
    }

    @Test fun `CONFIRMATION route constant is correct`() {
        assertThat(NavRoutes.CONFIRMATION).isEqualTo("confirmation")
    }

    @Test fun `NEXT_PAGE route constant is correct`() {
        assertThat(NavRoutes.NEXT_PAGE).isEqualTo("next_page")
    }

    @Test fun `all simple route constants are mutually distinct`() {
        val routes = listOf(NavRoutes.WELCOME, NavRoutes.CONFIRMATION, NavRoutes.NEXT_PAGE)
        assertThat(routes.toSet()).hasSize(routes.size)
    }

    // ── Route patterns — ScoreEntry ───────────────────────────────────────────

    @Test fun `SCORE_ENTRY_PATTERN starts with score_entry slash`() {
        assertThat(NavRoutes.SCORE_ENTRY_PATTERN).startsWith("score_entry/")
    }

    @Test fun `SCORE_ENTRY_PATTERN contains sectionId placeholder`() {
        assertThat(NavRoutes.SCORE_ENTRY_PATTERN).contains("{sectionId}")
    }

    @Test fun `SCORE_ENTRY_PATTERN contains optionalSubject query parameter`() {
        assertThat(NavRoutes.SCORE_ENTRY_PATTERN).contains("optionalSubject={optionalSubject}")
    }

    @Test fun `SCORE_ENTRY_PATTERN contains sportExempt query parameter`() {
        assertThat(NavRoutes.SCORE_ENTRY_PATTERN).contains("sportExempt={sportExempt}")
    }

    // ── Route patterns — Results ──────────────────────────────────────────────

    @Test fun `RESULTS_PATTERN starts with results slash`() {
        assertThat(NavRoutes.RESULTS_PATTERN).startsWith("results/")
    }

    @Test fun `RESULTS_PATTERN contains sectionId placeholder`() {
        assertThat(NavRoutes.RESULTS_PATTERN).contains("{sectionId}")
    }

    @Test fun `RESULTS_PATTERN contains fgScore placeholder`() {
        assertThat(NavRoutes.RESULTS_PATTERN).contains("{fgScore}")
    }

    @Test fun `RESULTS_PATTERN contains interestText placeholder`() {
        assertThat(NavRoutes.RESULTS_PATTERN).contains("{interestText}")
    }

    @Test fun `SCORE_ENTRY and RESULTS patterns do not collide`() {
        // Each pattern begins with its own unique prefix — they can never match each other.
        assertThat(NavRoutes.SCORE_ENTRY_PATTERN.startsWith("results")).isFalse()
        assertThat(NavRoutes.RESULTS_PATTERN.startsWith("score_entry")).isFalse()
    }

    // ── Score formatting ──────────────────────────────────────────────────────

    @Test fun `formatScore produces exactly 4 decimal places`() {
        assertThat(NavRoutes.formatScore(185.75)).isEqualTo("185.7500")
    }

    @Test fun `formatScore uses period as decimal separator - not comma`() {
        // French locale would produce "12,5000" which breaks toDoubleOrNull.
        assertThat(NavRoutes.formatScore(12.5)).contains(".")
        assertThat(NavRoutes.formatScore(12.5)).doesNotContain(",")
    }

    @Test fun `formatScore preserves integer values correctly`() {
        assertThat(NavRoutes.formatScore(200.0)).isEqualTo("200.0000")
    }

    @Test fun `formatScore round-trips through parseDouble within tolerance`() {
        val original = 173.4567
        val parsed   = NavRoutes.formatScore(original).toDoubleOrNull()
        assertThat(parsed).isNotNull()
        assertThat(parsed!!).isWithin(1e-4).of(original)
    }

    // ── Section display names ─────────────────────────────────────────────────

    @Test fun `sectionDisplayName math returns correct Arabic label`() {
        assertThat(GetSectionSubjectsUseCase.displayName("math")).isEqualTo("رياضيات")
    }

    @Test fun `sectionDisplayName science returns correct Arabic label`() {
        assertThat(GetSectionSubjectsUseCase.displayName("science")).isEqualTo("علوم تجريبية")
    }

    @Test fun `sectionDisplayName tech returns correct Arabic label`() {
        assertThat(GetSectionSubjectsUseCase.displayName("tech")).isEqualTo("علوم تقنية")
    }

    @Test fun `sectionDisplayName info returns correct Arabic label`() {
        assertThat(GetSectionSubjectsUseCase.displayName("info")).isEqualTo("علوم إعلامية")
    }

    @Test fun `sectionDisplayName literature returns correct Arabic label`() {
        assertThat(GetSectionSubjectsUseCase.displayName("literature")).isEqualTo("آداب")
    }

    @Test fun `sectionDisplayName economy returns correct Arabic label`() {
        assertThat(GetSectionSubjectsUseCase.displayName("economy")).isEqualTo("اقتصاد و تصرف")
    }

    @Test fun `sectionDisplayName sports returns correct Arabic label`() {
        assertThat(GetSectionSubjectsUseCase.displayName("sports")).isEqualTo("رياضة")
    }

    @Test fun `sectionDisplayName returns non-empty label for every known section`() {
        val knownIds = listOf("math", "science", "tech", "info", "literature", "economy", "sports")
        knownIds.forEach { id ->
            val name = GetSectionSubjectsUseCase.displayName(id)
            assertThat(name).isNotEmpty()
            // A proper display name is never the same as the raw id.
            assertThat(name).isNotEqualTo(id)
        }
    }

    @Test fun `sectionDisplayName falls back to raw id for unknown section`() {
        assertThat(GetSectionSubjectsUseCase.displayName("unknown_xyz")).isEqualTo("unknown_xyz")
    }
}
