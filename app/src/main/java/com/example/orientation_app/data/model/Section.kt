package com.example.orientation_app.data.model

/**
 * Represents a baccalaureate section that the student can select.
 */
data class SectionItem(
    val id: String,
    val nameRes: String,
    val iconType: SectionIconType
)

/** Icon identifiers mapped to Material icons in the UI layer. */
enum class SectionIconType {
    MATH,
    SCIENCE,
    TECH,
    INFORMATICS,
    LITERATURE,
    ECONOMY,
    SPORTS
}
