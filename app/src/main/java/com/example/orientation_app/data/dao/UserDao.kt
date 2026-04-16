package com.example.orientation_app.data.dao

import androidx.room.*
import com.example.orientation_app.data.entity.AIInsightHistory
import com.example.orientation_app.data.entity.UserFavourite
import com.example.orientation_app.data.entity.UserGrade
import com.example.orientation_app.data.entity.UserProfile
import com.example.orientation_app.data.relation.FavouriteWithFiliere
import com.example.orientation_app.data.relation.UserProfileWithGrades
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// UserProfile DAO  (singleton pattern — id is always 1)
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profiles WHERE id = 1")
    fun getProfile(): Flow<UserProfile?>

    @Transaction
    @Query("SELECT * FROM user_profiles WHERE id = 1")
    fun getProfileWithGrades(): Flow<UserProfileWithGrades?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfile)

    @Query("UPDATE user_profiles SET calculatedScore = :score WHERE id = 1")
    suspend fun updateScore(score: Double)

    @Query("UPDATE user_profiles SET selectedSectionId = :sectionId WHERE id = 1")
    suspend fun updateSection(sectionId: Int)

    @Query("UPDATE user_profiles SET homeGovernorateId = :governorateId WHERE id = 1")
    suspend fun updateGovernorate(governorateId: Int)

    @Query("UPDATE user_profiles SET interestText = :text WHERE id = 1")
    suspend fun updateInterests(text: String)
}

// ─────────────────────────────────────────────────────────────────────────────
// UserGrade DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface UserGradeDao {

    @Query("SELECT * FROM user_grades WHERE userId = :userId ORDER BY subjectId")
    fun getGradesForUser(userId: Int = 1): Flow<List<UserGrade>>

    @Query("SELECT * FROM user_grades WHERE userId = :userId AND subjectId = :subjectId")
    suspend fun getGrade(userId: Int, subjectId: Int): UserGrade?

    /** Upsert: inserts or replaces thanks to the UNIQUE(userId, subjectId) index. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(grade: UserGrade)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(grades: List<UserGrade>)

    @Query("DELETE FROM user_grades WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Int = 1)
}

// ─────────────────────────────────────────────────────────────────────────────
// UserFavourite DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface UserFavouriteDao {

    @Transaction
    @Query("SELECT * FROM user_favourites WHERE userId = :userId ORDER BY priorityOrder ASC")
    fun getFavouritesWithFiliere(userId: Int = 1): Flow<List<FavouriteWithFiliere>>

    @Query("SELECT * FROM user_favourites WHERE userId = :userId ORDER BY priorityOrder ASC")
    fun getFavourites(userId: Int = 1): Flow<List<UserFavourite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favourite: UserFavourite)

    @Query("DELETE FROM user_favourites WHERE userId = :userId AND filiereId = :filiereId")
    suspend fun remove(userId: Int, filiereId: Int)

    @Query("UPDATE user_favourites SET priorityOrder = :order WHERE id = :id")
    suspend fun updateOrder(id: Int, order: Int)

    @Query("SELECT COUNT(*) FROM user_favourites WHERE userId = :userId")
    suspend fun count(userId: Int = 1): Int

    @Query("SELECT EXISTS(SELECT 1 FROM user_favourites WHERE userId = :userId AND filiereId = :filiereId)")
    suspend fun isFavourite(userId: Int, filiereId: Int): Boolean
}

// ─────────────────────────────────────────────────────────────────────────────
// AIInsightHistory DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface AIInsightHistoryDao {

    @Query("SELECT * FROM ai_insight_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getHistory(userId: Int = 1): Flow<List<AIInsightHistory>>

    @Query("SELECT * FROM ai_insight_history WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(userId: Int = 1, limit: Int = 20): Flow<List<AIInsightHistory>>

    @Insert
    suspend fun insert(insight: AIInsightHistory)

    @Query("DELETE FROM ai_insight_history WHERE userId = :userId AND timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(userId: Int, beforeTimestamp: Long)

    @Query("DELETE FROM ai_insight_history WHERE userId = :userId")
    suspend fun clearAll(userId: Int = 1)
}
