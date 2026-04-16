package com.example.orientation_app.data.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.orientation_app.data.entity.*

// ─────────────────────────────────────────────────────────────────────────────
// Section ↔ Subjects  (via junction with coefficient)
// ─────────────────────────────────────────────────────────────────────────────

/** Flat row returned by a JOIN query — includes the junction's coefficient. */
data class SubjectWithCoefficient(
    @Embedded val subject: BacSubject,
    val coefficient: Double
)

/** Full section with its related subjects (no coefficient — use DAO join for that). */
data class SectionWithSubjects(
    @Embedded val section: BacSection,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SectionSubjectMap::class,
            parentColumn = "sectionId",
            entityColumn = "subjectId"
        )
    )
    val subjects: List<BacSubject>
)

// ─────────────────────────────────────────────────────────────────────────────
// Filiere ↔ Careers
// ─────────────────────────────────────────────────────────────────────────────

data class FiliereWithCareers(
    @Embedded val filiere: FiliereMaster,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = FiliereCareerMap::class,
            parentColumn = "filiereId",
            entityColumn = "careerId"
        )
    )
    val careers: List<Career>
)

// ─────────────────────────────────────────────────────────────────────────────
// Filiere + Governorate (for display)
// ─────────────────────────────────────────────────────────────────────────────

data class FiliereWithGovernorate(
    @Embedded val filiere: FiliereMaster,
    @Relation(
        parentColumn = "governorateId",
        entityColumn = "id"
    )
    val governorate: Governorate?
)

// ─────────────────────────────────────────────────────────────────────────────
// User Profile + Grades
// ─────────────────────────────────────────────────────────────────────────────

data class UserProfileWithGrades(
    @Embedded val profile: UserProfile,
    @Relation(
        parentColumn = "id",
        entityColumn = "userId"
    )
    val grades: List<UserGrade>
)

// ─────────────────────────────────────────────────────────────────────────────
// User Profile + Favourites  (ordered wish-list with filiere details)
// ─────────────────────────────────────────────────────────────────────────────

data class FavouriteWithFiliere(
    @Embedded val favourite: UserFavourite,
    @Relation(
        parentColumn = "filiereId",
        entityColumn = "id"
    )
    val filiere: FiliereMaster
)
