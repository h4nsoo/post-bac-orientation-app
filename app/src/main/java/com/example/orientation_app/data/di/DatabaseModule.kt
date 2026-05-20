package com.example.orientation_app.data.di

import android.content.Context
import androidx.room.Room
import com.example.orientation_app.data.dao.*
import com.example.orientation_app.data.db.UniCompassDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the Room database and every DAO as singletons.
 * The manual [UniCompassDatabase.getInstance] singleton is removed; Hilt
 * manages the single instance here instead.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): UniCompassDatabase =
        UniCompassDatabase.create(context)

    @Provides fun provideFiliereDao(db: UniCompassDatabase): FiliereDao = db.filiereDao()
    @Provides fun provideBacSectionDao(db: UniCompassDatabase): BacSectionDao = db.bacSectionDao()
    @Provides fun provideBacSubjectDao(db: UniCompassDatabase): BacSubjectDao = db.bacSubjectDao()
    @Provides fun provideGovernorateDao(db: UniCompassDatabase): GovernorateDao = db.governorateDao()
    @Provides fun provideUserProfileDao(db: UniCompassDatabase): UserProfileDao = db.userProfileDao()
    @Provides fun provideUserGradeDao(db: UniCompassDatabase): UserGradeDao = db.userGradeDao()
    @Provides fun provideAiInsightHistoryDao(db: UniCompassDatabase): AIInsightHistoryDao = db.aiInsightHistoryDao()
    @Provides fun provideAppMetadataDao(db: UniCompassDatabase): AppMetadataDao = db.appMetadataDao()
}
