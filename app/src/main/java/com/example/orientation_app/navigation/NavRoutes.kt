package com.example.orientation_app.navigation

import java.util.Locale

/**
 * Single source of truth for all navigation route strings in UniCompass.
 *
 * Route constants and pattern strings are plain Kotlin — no Android API needed —
 * so they can be referenced from JVM unit tests as well as production NavHost
 * code. Functions that require [android.net.Uri.encode] live in MainActivity
 * alongside the NavController where Android APIs are available.
 */
object NavRoutes {

    // ── Destination constants ─────────────────────────────────────────────────

    const val WELCOME      = "welcome"
    const val CONFIRMATION = "confirmation"
    const val NEXT_PAGE    = "next_page"

    // ── Route patterns (with argument placeholders) ──────────────────────────

    /** Route pattern for the ScoreEntry destination registered in the NavHost. */
    const val SCORE_ENTRY_PATTERN =
        "score_entry/{sectionId}?optionalSubject={optionalSubject}&sportExempt={sportExempt}"

    /** Route pattern for the Results destination registered in the NavHost. */
    const val RESULTS_PATTERN = "results/{sectionId}/{fgScore}/{interestText}"

    // ── Score formatting ──────────────────────────────────────────────────────

    /**
     * Formats [fgScore] to exactly 4 decimal places using US locale (period as
     * decimal separator). The result is URI-encoded by the caller before it is
     * embedded in [RESULTS_PATTERN].
     *
     * Using a fixed locale here is critical: on a device with a French locale the
     * default formatter would produce a comma, which breaks [toDoubleOrNull] on
     * the receiving end.
     */
    fun formatScore(fgScore: Double): String =
        String.format(Locale.US, "%.4f", fgScore)
}
