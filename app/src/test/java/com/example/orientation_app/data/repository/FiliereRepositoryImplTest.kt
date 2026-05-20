package com.example.orientation_app.data.repository

import com.example.orientation_app.data.dao.FiliereDao
import com.example.orientation_app.data.entity.FiliereMaster
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [FiliereRepositoryImpl].
 *
 * The Room [FiliereDao] is mocked with MockK so no Android context or in-memory
 * database setup is required. These tests verify:
 *  - Correct DAO delegation (sectionBit and userScore forwarded unchanged).
 *  - Entity-to-domain mapping via [toDomain] (all fields preserved).
 *  - Empty-list and limit-boundary edge cases.
 *
 * For full SQL integration tests (bitwise eligibility, ordering, score filter)
 * see the instrumented test suite.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FiliereRepositoryImplTest {

    // UnconfinedTestDispatcher runs withContext() blocks immediately on the calling
    // thread, avoiding the deadlock that StandardTestDispatcher causes when used as
    // an IO dispatcher inside runTest.
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dao            = mockk<FiliereDao>()
    private lateinit var repository: FiliereRepositoryImpl

    @Before
    fun setUp() {
        repository = FiliereRepositoryImpl(dao, testDispatcher)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun fakeEntity(
        id: Int,
        score: Double,
        sectionBit: Int,
        capacity: Int = 50
    ) = FiliereMaster(
        id                 = id,
        code               = "CODE-$id",
        nameAr             = "برنامج $id",
        nameFr             = "Programme $id",
        universityAr       = "جامعة $id",
        universityFr       = "Université $id",
        lastYearScore      = score,
        capacity           = capacity,
        governorateId      = null,
        sectionEligibility = sectionBit
    )

    // ── getEligibleOnce — DAO delegation ──────────────────────────────────────

    @Test fun `getEligibleOnce - delegates sectionBit and userScore to dao`() = runTest {
        coEvery { dao.filterEligibleOnce(1, 160.0) } returns emptyList()

        repository.getEligibleOnce(1, 160.0)

        coVerify(exactly = 1) { dao.filterEligibleOnce(1, 160.0) }
    }

    @Test fun `getEligibleOnce - returns empty list when dao returns empty`() = runTest {
        coEvery { dao.filterEligibleOnce(any(), any()) } returns emptyList()

        val result = repository.getEligibleOnce(1, 999.0)

        assertThat(result).isEmpty()
    }

    // ── getEligibleOnce — entity → domain mapping ─────────────────────────────

    @Test fun `getEligibleOnce - maps entity list to domain model list`() = runTest {
        val entities = listOf(fakeEntity(1, 150.0, 1), fakeEntity(2, 160.0, 3))
        coEvery { dao.filterEligibleOnce(1, 155.0) } returns entities

        val result = repository.getEligibleOnce(1, 155.0)

        assertThat(result).hasSize(2)
        assertThat(result.map { it.id }).containsExactly(1, 2).inOrder()
    }

    @Test fun `getEligibleOnce - preserves all entity fields in domain model`() = runTest {
        val entity = fakeEntity(id = 42, score = 175.5, sectionBit = 3, capacity = 120)
        coEvery { dao.filterEligibleOnce(any(), any()) } returns listOf(entity)

        val program = repository.getEligibleOnce(3, 170.0).single()

        assertThat(program.id).isEqualTo(42)
        assertThat(program.code).isEqualTo("CODE-42")
        assertThat(program.nameAr).isEqualTo("برنامج 42")
        assertThat(program.nameFr).isEqualTo("Programme 42")
        assertThat(program.universityAr).isEqualTo("جامعة 42")
        assertThat(program.universityFr).isEqualTo("Université 42")
        assertThat(program.lastYearScore).isEqualTo(175.5)
        assertThat(program.capacity).isEqualTo(120)
        assertThat(program.sectionEligibility).isEqualTo(3)
    }

    // ── getEligibleOnce — section bit semantics (contract, not SQL) ───────────

    @Test fun `getEligibleOnce - passes math bit 1 correctly`() = runTest {
        coEvery { dao.filterEligibleOnce(1, any()) } returns emptyList()

        repository.getEligibleOnce(sectionBit = 1, userScore = 140.0)

        coVerify { dao.filterEligibleOnce(1, 140.0) }
    }

    @Test fun `getEligibleOnce - passes science bit 2 correctly`() = runTest {
        coEvery { dao.filterEligibleOnce(2, any()) } returns emptyList()

        repository.getEligibleOnce(sectionBit = 2, userScore = 155.0)

        coVerify { dao.filterEligibleOnce(2, 155.0) }
    }

    @Test fun `getEligibleOnce - passes compound bit (math + science = 3) correctly`() = runTest {
        coEvery { dao.filterEligibleOnce(3, any()) } returns emptyList()

        repository.getEligibleOnce(sectionBit = 3, userScore = 150.0)

        coVerify { dao.filterEligibleOnce(3, 150.0) }
    }
}
