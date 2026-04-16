package com.example.orientation_app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ─────────────────────────────────────────────────────────────────────────────
// User Profile  (singleton — PK always = 1)
// ─────────────────────────────────────────────────────────────────────────────

@Entity(
    tableName = "user_profiles",
    foreignKeys = [
        ForeignKey(
            entity = BacSection::class,
            parentColumns = ["id"],
            childColumns = ["selectedSectionId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Governorate::class,
            parentColumns = ["id"],
            childColumns = ["homeGovernorateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("selectedSectionId"), Index("homeGovernorateId")]
)
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val selectedSectionId: Int? = null,
    val homeGovernorateId: Int? = null,
    val calculatedScore: Double = 0.0,
    /** Free-text field the user fills in to describe their interests (fed to AI). */
    val interestText: String = ""
)

// ─────────────────────────────────────────────────────────────────────────────
// User Grade  (one row per subject)
// ─────────────────────────────────────────────────────────────────────────────

@Entity(
    tableName = "user_grades",
    foreignKeys = [
        ForeignKey(
            entity = UserProfile::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
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
        Index("userId"),
        Index("subjectId"),
        Index(value = ["userId", "subjectId"], unique = true)
    ]
)
data class UserGrade(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val subjectId: Int,
    val gradeValue: Double
)

// ─────────────────────────────────────────────────────────────────────────────
// User Favourite  (wish-list filieres with ordering)
// ─────────────────────────────────────────────────────────────────────────────

@Entity(
    tableName = "user_favourites",
    foreignKeys = [
        ForeignKey(
            entity = UserProfile::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FiliereMaster::class,
            parentColumns = ["id"],
            childColumns = ["filiereId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),
        Index("filiereId"),
        Index(value = ["userId", "filiereId"], unique = true)
    ]
)
data class UserFavourite(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val filiereId: Int,
    /** User-defined priority for the orientation wish-list. */
    val priorityOrder: Int
)

// ─────────────────────────────────────────────────────────────────────────────
// AI Insight History  (logged prompts + responses)
// ─────────────────────────────────────────────────────────────────────────────

@Entity(
    tableName = "ai_insight_history",
    foreignKeys = [
        ForeignKey(
            entity = UserProfile::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class AIInsightHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val queryText: String,
    val aiResponse: String,
    /** System.currentTimeMillis() at creation. */
    val timestamp: Long
)
