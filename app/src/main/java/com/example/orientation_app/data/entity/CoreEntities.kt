package com.example.orientation_app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ─────────────────────────────────────────────────────────────────────────────
// Baccalaureate Section  (رياضيات, علوم تجريبية, …)
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "bac_sections")
data class BacSection(
    @PrimaryKey val id: Int,
    /** Arabic display name, e.g. "رياضيات" */
    val nameAr: String,
    /**
     * Strategy-pattern key used to pick the correct score formula at runtime.
     * Examples: "FORMULA_MATH", "FORMULA_SCIENCE", "FORMULA_TECH", …
     */
    val formulaType: String
)

// ─────────────────────────────────────────────────────────────────────────────
// Subject  (مادة — رياضيات, فيزياء, عربية, …)
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "bac_subjects")
data class BacSubject(
    @PrimaryKey val id: Int,
    val nameAr: String,
    /** True for core subjects that appear across all sections. */
    val isBaseSubject: Boolean
)

// ─────────────────────────────────────────────────────────────────────────────
// Junction: Section ↔ Subject  (Many-to-Many with coefficient)
// ─────────────────────────────────────────────────────────────────────────────

@Entity(
    tableName = "section_subject_map",
    foreignKeys = [
        ForeignKey(
            entity = BacSection::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BacSubject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sectionId"),
        Index("subjectId"),
        Index(value = ["sectionId", "subjectId"], unique = true)
    ]
)
data class SectionSubjectMap(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sectionId: Int,
    val subjectId: Int,
    /** Weight of this subject within the section's formula. */
    val coefficient: Double
)
