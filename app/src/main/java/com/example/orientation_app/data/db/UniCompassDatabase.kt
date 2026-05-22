package com.example.orientation_app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.orientation_app.data.dao.*
import com.example.orientation_app.data.entity.*
import androidx.sqlite.db.SimpleSQLiteQuery
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
    version = 2,
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
         * Builds the database and wires the one-time prepopulation callback.
         *
         * Pre-release policy: destructive migration is intentional while the schema
         * is at version 1 and all data is re-seeded from assets on every fresh install.
         * TODO: Replace with explicit [androidx.room.migration.Migration] objects
         *       before the first Play Store release.
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
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(object : RoomDatabase.Callback() {
                    // Called on fresh install — seed from assets.
                    override fun onCreate(sq: SupportSQLiteDatabase) {
                        super.onCreate(sq)
                        seed()
                    }
                    // onDestructiveMigration is NOT guaranteed to fire on all AGP /
                    // Room alpha combinations, so we also guard here: if the
                    // filiere_master table is empty (schema was just wiped by a
                    // destructive migration) we re-seed immediately.
                    override fun onOpen(sq: SupportSQLiteDatabase) {
                        super.onOpen(sq)
                        CoroutineScope(Dispatchers.IO).launch {
                            val cursor = sq.query(
                                SimpleSQLiteQuery("SELECT COUNT(*) FROM filiere_master")
                            )
                            var isEmpty = true
                            try {
                                if (cursor.moveToFirst()) isEmpty = cursor.getInt(0) == 0
                            } finally {
                                cursor.close()
                            }
                            if (isEmpty) seed()
                        }
                    }
                    private fun seed() {
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
