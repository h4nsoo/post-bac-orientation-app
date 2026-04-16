package com.example.orientation_app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ─────────────────────────────────────────────────────────────────────────────
// Governorate  (ولاية — required before FiliereMaster due to FK)
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "governorates")
data class Governorate(
    @PrimaryKey val id: Int,
    val nameAr: String,
    /** Regional code used to calculate the 7 % proximity bonus. */
    val regionCode: String
)

// ─────────────────────────────────────────────────────────────────────────────
// Filiere Master  (اختصاص جامعي)
// ─────────────────────────────────────────────────────────────────────────────

@Entity(
    tableName = "filiere_master",
    foreignKeys = [
        ForeignKey(
            entity = Governorate::class,
            parentColumns = ["id"],
            childColumns = ["governorateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("governorateId"),
        Index("sectionEligibility"),
        Index("lastYearScore")
    ]
)
data class FiliereMaster(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** Short code, e.g. "LF-INFO-01" */
    val code: String,
    val nameAr: String,
    val nameFr: String,
    val universityAr: String,
    val universityFr: String,
    /** Minimum orientation score from the previous year. */
    val lastYearScore: Double,
    /** Total available seats. */
    val capacity: Int,
    val governorateId: Int?,
    /**
     * Bitwise eligibility mask — each bit position maps to a [BacSection.id]:
     *
     * | Bit | Section         | Value |
     * |-----|-----------------|-------|
     * |  0  | رياضيات          |   1   |
     * |  1  | علوم تجريبية     |   2   |
     * |  2  | علوم تقنية       |   4   |
     * |  3  | علوم إعلامية     |   8   |
     * |  4  | آداب             |  16   |
     * |  5  | اقتصاد و تصرّف   |  32   |
     * |  6  | رياضة            |  64   |
     *
     * Example: `sectionEligibility = 15` → eligible for bits 0-3
     * (Math + Science + Tech + Informatics).
     */
    val sectionEligibility: Int
)

// ─────────────────────────────────────────────────────────────────────────────
// Career  (مهنة)
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "careers")
data class Career(
    @PrimaryKey val id: Int,
    val nameAr: String,
    /** Broad grouping, e.g. "هندسة", "طب", "تكنولوجيا", "تعليم". */
    val category: String
)

// ─────────────────────────────────────────────────────────────────────────────
// Junction: Filiere ↔ Career  (composite PK, no surrogate)
// ─────────────────────────────────────────────────────────────────────────────

@Entity(
    tableName = "filiere_career_map",
    primaryKeys = ["filiereId", "careerId"],
    foreignKeys = [
        ForeignKey(
            entity = FiliereMaster::class,
            parentColumns = ["id"],
            childColumns = ["filiereId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Career::class,
            parentColumns = ["id"],
            childColumns = ["careerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("filiereId"), Index("careerId")]
)
data class FiliereCareerMap(
    val filiereId: Int,
    val careerId: Int
)
