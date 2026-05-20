package com.example.orientation_app.data.repository

import com.example.orientation_app.data.dao.UserProfileDao
import com.example.orientation_app.data.di.IoDispatcher
import com.example.orientation_app.data.entity.UserProfile
import com.example.orientation_app.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val dao: UserProfileDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UserRepository {

    override suspend fun initializeProfile() = withContext(ioDispatcher) {
        // REPLACE semantics: inserts id=1 if absent, leaves it unchanged otherwise.
        dao.insertOrUpdate(UserProfile(id = 1))
    }

    override suspend fun updateInterests(text: String) = withContext(ioDispatcher) {
        dao.updateInterests(text)
    }

    override suspend fun updateScore(score: Double) = withContext(ioDispatcher) {
        dao.updateScore(score)
    }

    override suspend fun updateSection(sectionId: Int) = withContext(ioDispatcher) {
        dao.updateSection(sectionId)
    }
}
