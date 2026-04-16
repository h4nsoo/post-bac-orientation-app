package com.example.orientation_app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─────────────────────────────────────────────────────────────────────────────
// App Metadata  (key-value config store)
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "app_metadata")
data class AppMetadata(
    @PrimaryKey val configKey: String,
    val configValue: String
)

// ─────────────────────────────────────────────────────────────────────────────
// Orientation Event  (salons, open-days, deadlines)
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "orientation_events")
data class OrientationEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titleAr: String,
    /** Stored as epoch millis (System.currentTimeMillis). */
    val eventDate: Long,
    val isReminderEnabled: Boolean = false
)
