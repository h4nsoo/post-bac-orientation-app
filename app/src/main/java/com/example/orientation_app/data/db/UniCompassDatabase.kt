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

/**
 * Central Room database for Unicompass.
 *
 * Version history:
 *   1 → initial schema (pre-release; destructive migration is intentional for v1
 *       since all data is re-seeded from [DatabasePrepopulator] on every fresh
 *       install).  A proper [androidx.room.migration.Migration] must be added
 *       before the first Play Store release.
 *
 * The manual INSTANCE singleton has been removed; Hilt manages the single
 * instance via [com.example.orientation_app.data.di.DatabaseModule].
 */
@Database(
    entities = [
        BacSection::class,
        BacSubject::class,
        SectionSubjectMap::class,
        FiliereMaster::class,
        Governorate::class,
        Career::class,
        FiliereCareerMap::class,
        UserProfile::class,
        UserGrade::class,
        UserFavourite::class,
        AIInsightHistory::class,
        AppMetadata::class,
        OrientationEvent::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class UniCompassDatabase : RoomDatabase() {

    abstract fun bacSectionDao(): BacSectionDao
    abstract fun bacSubjectDao(): BacSubjectDao
    abstract fun sectionSubjectMapDao(): SectionSubjectMapDao
    abstract fun filiereDao(): FiliereDao
    abstract fun governorateDao(): GovernorateDao
    abstract fun careerDao(): CareerDao
    abstract fun filiereCareerMapDao(): FiliereCareerMapDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun userGradeDao(): UserGradeDao
    abstract fun userFavouriteDao(): UserFavouriteDao
    abstract fun aiInsightHistoryDao(): AIInsightHistoryDao
    abstract fun appMetadataDao(): AppMetadataDao
    abstract fun orientationEventDao(): OrientationEventDao

    companion object {

        /**
         * Creates a Room-backed instance of the database.
         * Called once by [com.example.orientation_app.data.di.DatabaseModule]; the
         * result is kept as a Hilt singleton.
         *
         * Pre-release policy: [fallbackToDestructiveMigration] is intentional while
         * the schema is at version 1 and all data is re-seeded from assets.
         * TODO: Replace with explicit Migration objects before first Play Store release.
         */
        /**
         * Build the database and wire the one-time prepopulation callback.
         *
         * Uses a [lateinit] local so the callback can reference the built instance
         * without a recursive [create] call.  Room only fires [RoomDatabase.Callback.onCreate]
         * on the first *access* (not on [build]), so `db` is fully assigned by then.
         */
        fun create(context: Context): UniCompassDatabase {
            lateinit var db: UniCompassDatabase
            db = Room.databaseBuilder(
                context.applicationContext,
                UniCompassDatabase::class.java,
                "unicompass_db"
            )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(sq: SupportSQLiteDatabase) {
                        super.onCreate(sq)
                        CoroutineScope(Dispatchers.IO).launch {
                            DatabasePrepopulator.prepopulate(context.applicationContext, db)
                        }
                    }
                })
                .build()
            return db
        }
    }
}
