package com.example.orientation_app.data.dao

import androidx.room.*
import com.example.orientation_app.data.entity.Career
import com.example.orientation_app.data.entity.FiliereMaster
import com.example.orientation_app.data.entity.FiliereCareerMap
import com.example.orientation_app.data.relation.FiliereWithCareers
import com.example.orientation_app.data.relation.FiliereWithGovernorate
import kotlinx.coroutines.flow.Flow

/**
 * The most important DAO — handles filiere filtering with bitwise eligibility
 * checks and score-margin comparison.
 */
@Dao
interface FiliereDao {

    // ── Basic CRUD ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM filiere_master ORDER BY nameAr")
    fun getAll(): Flow<List<FiliereMaster>>

    @Query("SELECT * FROM filiere_master WHERE id = :id")
    suspend fun getById(id: Int): FiliereMaster?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(filieres: List<FiliereMaster>)

    @Update
    suspend fun update(filiere: FiliereMaster)

    @Delete
    suspend fun delete(filiere: FiliereMaster)

    // ── Core filtering query  ───────────────────────────────────────────────
    //
    //  Bitwise AND checks whether the filiere accepts the student's section.
    //  The +5 safety margin lets students see filieres slightly above their
    //  calculated score — useful for borderline candidates.
    //

    /**
     * Returns filieres that:
     * 1. Accept the student's bac section  (bitwise: `sectionEligibility & sectionBit ≠ 0`)
     * 2. Had a cut-off score the student can realistically reach (`lastYearScore ≤ userScore + 5`)
     *
     * Results are ordered by proximity to the student's score (closest first).
     */
    @Query("""
        SELECT * FROM filiere_master
        WHERE (sectionEligibility & :sectionBit) != 0
          AND lastYearScore <= (:userScore + 5.0)
        ORDER BY ABS(lastYearScore - :userScore) ASC
    """)
    fun filterEligible(
        sectionBit: Int,
        userScore: Double
    ): Flow<List<FiliereMaster>>

    /**
     * Overload with governorate filter for proximity bonus results.
     */
    @Query("""
        SELECT * FROM filiere_master
        WHERE (sectionEligibility & :sectionBit) != 0
          AND lastYearScore <= (:userScore + 5.0)
          AND governorateId = :governorateId
        ORDER BY lastYearScore DESC
    """)
    fun filterEligibleInGovernorate(
        sectionBit: Int,
        userScore: Double,
        governorateId: Int
    ): Flow<List<FiliereMaster>>

    /**
     * Full-text-style search within eligible filieres.
     */
    @Query("""
        SELECT * FROM filiere_master
        WHERE (sectionEligibility & :sectionBit) != 0
          AND (nameAr LIKE '%' || :query || '%'
               OR nameFr LIKE '%' || :query || '%'
               OR universityAr LIKE '%' || :query || '%')
        ORDER BY lastYearScore DESC
    """)
    fun searchEligible(
        sectionBit: Int,
        query: String
    ): Flow<List<FiliereMaster>>

    // ── Relation queries ────────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM filiere_master WHERE id = :id")
    suspend fun getFiliereWithCareers(id: Int): FiliereWithCareers?

    @Transaction
    @Query("""
        SELECT * FROM filiere_master
        WHERE (sectionEligibility & :sectionBit) != 0
          AND lastYearScore <= (:userScore + 5.0)
        ORDER BY ABS(lastYearScore - :userScore) ASC
    """)
    fun filterEligibleWithGovernorate(
        sectionBit: Int,
        userScore: Double
    ): Flow<List<FiliereWithGovernorate>>

    // ── Statistics ───────────────────────────────────────────────────────────

    @Query("""
        SELECT COUNT(*) FROM filiere_master
        WHERE (sectionEligibility & :sectionBit) != 0
          AND lastYearScore <= (:userScore + 5.0)
    """)
    suspend fun countEligible(sectionBit: Int, userScore: Double): Int
}

// ─────────────────────────────────────────────────────────────────────────────
// Career DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface CareerDao {

    @Query("SELECT * FROM careers ORDER BY category, nameAr")
    fun getAll(): Flow<List<Career>>

    @Query("SELECT * FROM careers WHERE category = :category")
    fun getByCategory(category: String): Flow<List<Career>>

    @Query("""
        SELECT c.* FROM careers c
        INNER JOIN filiere_career_map m ON c.id = m.careerId
        WHERE m.filiereId = :filiereId
    """)
    fun getCareersForFiliere(filiereId: Int): Flow<List<Career>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(careers: List<Career>)
}

// ─────────────────────────────────────────────────────────────────────────────
// FiliereCareerMap DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface FiliereCareerMapDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mappings: List<FiliereCareerMap>)

    @Query("DELETE FROM filiere_career_map WHERE filiereId = :filiereId")
    suspend fun deleteForFiliere(filiereId: Int)
}

// ─────────────────────────────────────────────────────────────────────────────
// Governorate DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface GovernorateDao {

    @Query("SELECT * FROM governorates ORDER BY nameAr")
    fun getAll(): Flow<List<com.example.orientation_app.data.entity.Governorate>>

    @Query("SELECT * FROM governorates WHERE id = :id")
    suspend fun getById(id: Int): com.example.orientation_app.data.entity.Governorate?

    @Query("SELECT * FROM governorates WHERE regionCode = :regionCode")
    fun getByRegion(regionCode: String): Flow<List<com.example.orientation_app.data.entity.Governorate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(governorates: List<com.example.orientation_app.data.entity.Governorate>)
}
