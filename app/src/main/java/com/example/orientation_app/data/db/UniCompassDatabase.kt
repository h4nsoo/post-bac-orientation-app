package com.example.orientation_app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.orientation_app.data.dao.*
import com.example.orientation_app.data.entity.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        // Core configuration
        BacSection::class,
        BacSubject::class,
        SectionSubjectMap::class,
        // Knowledge base
        FiliereMaster::class,
        Governorate::class,
        Career::class,
        FiliereCareerMap::class,
        // User state
        UserProfile::class,
        UserGrade::class,
        UserFavourite::class,
        AIInsightHistory::class,
        // Metadata
        AppMetadata::class,
        OrientationEvent::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class UniCompassDatabase : RoomDatabase() {

    // ── Core configuration ──────────────────────────────────────────────────
    abstract fun bacSectionDao(): BacSectionDao
    abstract fun bacSubjectDao(): BacSubjectDao
    abstract fun sectionSubjectMapDao(): SectionSubjectMapDao

    // ── Knowledge base ──────────────────────────────────────────────────────
    abstract fun filiereDao(): FiliereDao
    abstract fun governorateDao(): GovernorateDao
    abstract fun careerDao(): CareerDao
    abstract fun filiereCareerMapDao(): FiliereCareerMapDao

    // ── User state ──────────────────────────────────────────────────────────
    abstract fun userProfileDao(): UserProfileDao
    abstract fun userGradeDao(): UserGradeDao
    abstract fun userFavouriteDao(): UserFavouriteDao
    abstract fun aiInsightHistoryDao(): AIInsightHistoryDao

    // ── Metadata ────────────────────────────────────────────────────────────
    abstract fun appMetadataDao(): AppMetadataDao
    abstract fun orientationEventDao(): OrientationEventDao

    companion object {
        @Volatile
        private var INSTANCE: UniCompassDatabase? = null

        /**
         * Thread-safe singleton accessor.
         *
         * Uses `.fallbackToDestructiveMigration()` during development.
         * Replace with proper [Migration] objects before shipping to production.
         */
        fun getInstance(context: Context): UniCompassDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    UniCompassDatabase::class.java,
                    "unicompass_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Database is created. Setup the coroutine to prepopulate it on the IO thread
                CoroutineScope(Dispatchers.IO).launch {
                    getInstance(context).let { database ->
                        DatabasePrepopulator.prepopulateFiliereMaster(context, database)
                    }
                }
            }
        }
    }
}
