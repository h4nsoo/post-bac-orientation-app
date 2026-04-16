package com.example.orientation_app.data.dao

import androidx.room.*
import com.example.orientation_app.data.entity.AppMetadata
import com.example.orientation_app.data.entity.OrientationEvent
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// AppMetadata DAO  (generic key-value store)
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface AppMetadataDao {

    @Query("SELECT configValue FROM app_metadata WHERE configKey = :key")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setValue(entry: AppMetadata)

    /** Convenience wrapper: set a key-value pair. */
    @Transaction
    suspend fun put(key: String, value: String) {
        setValue(AppMetadata(configKey = key, configValue = value))
    }

    @Query("DELETE FROM app_metadata WHERE configKey = :key")
    suspend fun remove(key: String)

    @Query("SELECT * FROM app_metadata")
    suspend fun getAll(): List<AppMetadata>
}

// ─────────────────────────────────────────────────────────────────────────────
// OrientationEvent DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface OrientationEventDao {

    @Query("SELECT * FROM orientation_events WHERE eventDate >= :nowMillis ORDER BY eventDate ASC")
    fun getUpcoming(nowMillis: Long = System.currentTimeMillis()): Flow<List<OrientationEvent>>

    @Query("SELECT * FROM orientation_events ORDER BY eventDate ASC")
    fun getAll(): Flow<List<OrientationEvent>>

    @Query("SELECT * FROM orientation_events WHERE id = :id")
    suspend fun getById(id: Int): OrientationEvent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: OrientationEvent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<OrientationEvent>)

    @Query("UPDATE orientation_events SET isReminderEnabled = :enabled WHERE id = :id")
    suspend fun toggleReminder(id: Int, enabled: Boolean)

    @Delete
    suspend fun delete(event: OrientationEvent)
}
