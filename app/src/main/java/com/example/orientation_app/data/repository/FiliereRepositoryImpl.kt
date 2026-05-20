package com.example.orientation_app.data.repository

import com.example.orientation_app.data.dao.FiliereDao
import com.example.orientation_app.data.di.IoDispatcher
import com.example.orientation_app.domain.model.ScoredProgram
import com.example.orientation_app.domain.repository.FiliereRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Room-backed implementation of [FiliereRepository].
 * Translates between the [FiliereMaster][com.example.orientation_app.data.entity.FiliereMaster]
 * Room entity and the [ScoredProgram] domain model so that the domain layer stays
 * free of persistence dependencies.
 */
class FiliereRepositoryImpl @Inject constructor(
    private val dao: FiliereDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FiliereRepository {

    override suspend fun getEligibleOnce(sectionBit: Int, userScore: Double): List<ScoredProgram> =
        withContext(ioDispatcher) {
            dao.filterEligibleOnce(sectionBit, userScore).map { it.toDomain() }
        }

    override fun getEligible(sectionBit: Int, userScore: Double): Flow<List<ScoredProgram>> =
        dao.filterEligible(sectionBit, userScore)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun countEligible(sectionBit: Int, userScore: Double): Int =
        withContext(ioDispatcher) { dao.countEligible(sectionBit, userScore) }
}
