package com.example.orientation_app.domain.repository

/**
 * Contract for persisting user-session data across launches.
 */
interface UserRepository {

    /** Ensures the singleton UserProfile row (id = 1) exists in the DB. */
    suspend fun initializeProfile()

    /** Persists the free-text interest description entered by the user. */
    suspend fun updateInterests(text: String)

    /** Persists the student's computed orientation score. */
    suspend fun updateScore(score: Double)

    /** Persists the selected BAC section ID (integer FK). */
    suspend fun updateSection(sectionId: Int)
}
