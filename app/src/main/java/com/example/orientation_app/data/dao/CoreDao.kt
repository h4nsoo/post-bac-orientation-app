package com.example.orientation_app.data.dao

import androidx.room.*
import com.example.orientation_app.data.entity.BacSection
import com.example.orientation_app.data.entity.BacSubject
import com.example.orientation_app.data.entity.SectionSubjectMap
import com.example.orientation_app.data.relation.SectionWithSubjects
import com.example.orientation_app.data.relation.SubjectWithCoefficient
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// BacSection DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface BacSectionDao {

    @Query("SELECT * FROM bac_sections ORDER BY id")
    fun getAll(): Flow<List<BacSection>>

    @Query("SELECT * FROM bac_sections WHERE id = :id")
    suspend fun getById(id: Int): BacSection?

    @Transaction
    @Query("SELECT * FROM bac_sections WHERE id = :id")
    suspend fun getSectionWithSubjects(id: Int): SectionWithSubjects?

    @Transaction
    @Query("SELECT * FROM bac_sections")
    fun getAllWithSubjects(): Flow<List<SectionWithSubjects>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sections: List<BacSection>)

    @Update
    suspend fun update(section: BacSection)
}

// ─────────────────────────────────────────────────────────────────────────────
// BacSubject DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface BacSubjectDao {

    @Query("SELECT * FROM bac_subjects ORDER BY id")
    fun getAll(): Flow<List<BacSubject>>

    @Query("SELECT * FROM bac_subjects WHERE id = :id")
    suspend fun getById(id: Int): BacSubject?

    @Query("SELECT * FROM bac_subjects WHERE isBaseSubject = 1")
    fun getBaseSubjects(): Flow<List<BacSubject>>

    /**
     * Returns subjects for a given section **with their coefficient**.
     * Uses a manual JOIN to include the junction table's coefficient column.
     */
    @Query("""
        SELECT s.*, m.coefficient
        FROM bac_subjects s
        INNER JOIN section_subject_map m ON s.id = m.subjectId
        WHERE m.sectionId = :sectionId
        ORDER BY m.coefficient DESC
    """)
    fun getSubjectsForSection(sectionId: Int): Flow<List<SubjectWithCoefficient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subjects: List<BacSubject>)
}

// ─────────────────────────────────────────────────────────────────────────────
// SectionSubjectMap DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface SectionSubjectMapDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mappings: List<SectionSubjectMap>)

    @Query("DELETE FROM section_subject_map WHERE sectionId = :sectionId")
    suspend fun deleteForSection(sectionId: Int)

    @Query("SELECT * FROM section_subject_map WHERE sectionId = :sectionId")
    suspend fun getMappingsForSection(sectionId: Int): List<SectionSubjectMap>
}
