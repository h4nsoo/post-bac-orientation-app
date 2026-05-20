package com.example.orientation_app.domain.model

/**
 * Single source of truth for every Arabic subject key used in grade maps and
 * score formulas. All strings carry the "ال" definite-article prefix exactly as
 * used by the Tunisian Ministry of Education grading sheets, with the exception
 * of [HISTORY_GEO] and [ISLAMIC_EDUCATION] which do not take the article in the
 * official nomenclature.
 *
 * Adding a new subject here is the ONLY place you need to change to introduce a
 * new key — the use-case and the formula both read from this object.
 */
object SubjectKey {

    // ── Shared core subjects ─────────────────────────────────────────────────
    const val ARABIC     = "العربية"
    const val FRENCH     = "الفرنسية"
    const val ENGLISH    = "الإنجليزية"
    const val PHILOSOPHY = "الفلسفة"
    const val INFORMATICS = "الإعلامية"

    /** Added to the grades map when the student is NOT sport-exempt.
     *  The formula uses [sportOrNeutral] to substitute 10.0 when absent. */
    const val SPORT = "الرياضة"

    // ── Math & Science sections ──────────────────────────────────────────────
    const val MATH    = "الرياضيات"
    const val PHYSICS = "الفيزياء"
    const val SCIENCE = "العلوم"

    // ── Informatics section ──────────────────────────────────────────────────
    const val ALGORITHMS = "الخوارزميات"
    const val STI        = "STI"

    // ── Technique section ────────────────────────────────────────────────────
    const val ELECTRICITY = "الكهرباء"
    const val MECHANICS   = "الميكانيك"

    // ── Economy section ──────────────────────────────────────────────────────
    const val ECONOMICS   = "الاقتصاد"
    const val MANAGEMENT  = "التصرف"
    const val HISTORY_GEO = "تاريخ و جغرافيا"   // no "ال" — correct per official name

    // ── Letters section ──────────────────────────────────────────────────────
    const val ISLAMIC_EDUCATION = "التربية الإسلامية (PISL)"

    // ── Optional subject ─────────────────────────────────────────────────────
    /** Keys for optional subjects are composed as "$OPTIONAL_PREFIX$subjectName". */
    const val OPTIONAL_PREFIX = "اختياري: "
}
