package com.example.orientation_app.data.db

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room TypeConverters for types that cannot be stored natively in SQLite.
 *
 * Currently handles [Date] ↔ [Long] (epoch millis).
 * Add additional converters here as the schema grows.
 */
class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}
