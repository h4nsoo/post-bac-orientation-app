package com.example.orientation_app.domain.usecase

import com.example.orientation_app.domain.model.Recommendation
import com.example.orientation_app.domain.model.ScoredProgram
import com.example.orientation_app.domain.repository.FiliereRepository
import com.example.orientation_app.domain.repository.IAiService
import com.example.orientation_app.domain.repository.UserRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [GetAiRecommendationsUseCase].
 *
 * All collaborators are mocked with MockK so these tests run on the JVM
 * without any Android or Room dependency.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetAiRecommendationsUseCaseTest {

    private val filiereRepository = mockk<FiliereRepository>()
    private val aiService         = mockk<IAiService>()
    private val userRepository    = mockk<UserRepository>(relaxed = true)

    private lateinit var useCase: GetAiRecommendationsUseCase

    private val sampleProgram = ScoredProgram(
        id = 1, code = "TEST-01", nameAr = "برنامج", nameFr = "Programme",
        universityAr = "جامعة", universityFr = "Université",
        lastYearScore = 150.0, capacity = 100, sectionEligibility = 1
    )
    private val sampleRecs = listOf(Recommendation(sampleProgram, "تناسب"))

    @Before
    fun setUp() {
        useCase = GetAiRecommendationsUseCase(filiereRepository, aiService, userRepository)
    }

    // ── Happy path ─────────────────────────────────────────────────────────────

    @Test fun `returns AI recommendations on happy path`() = runTest {
        coEvery { filiereRepository.getEligibleOnce(1, 180.0) } returns listOf(sampleProgram)
        coEvery { aiService.getRecommendations(any(), any(), any(), any()) } returns sampleRecs

        val result = useCase("math", "رياضيات", 180.0, "أحب الرياضيات")

        assertThat(result).isEqualTo(sampleRecs)
    }

    @Test fun `persists interest text before querying programs`() = runTest {
        coEvery { filiereRepository.getEligibleOnce(any(), any()) } returns listOf(sampleProgram)
        coEvery { aiService.getRecommendations(any(), any(), any(), any()) } returns sampleRecs

        useCase("math", "رياضيات", 180.0, "اهتمامات الطالب")

        coVerify { userRepository.updateInterests("اهتمامات الطالب") }
    }

    // ── Section-bit mapping ───────────────────────────────────────────────────

    @Test fun `math section uses bit 1`() = runTest {
        coEvery { filiereRepository.getEligibleOnce(1, any()) } returns listOf(sampleProgram)
        coEvery { aiService.getRecommendations(any(), any(), any(), any()) } returns sampleRecs

        useCase("math", "رياضيات", 160.0, "")

        coVerify { filiereRepository.getEligibleOnce(1, 160.0) }
    }

    @Test fun `science section uses bit 2`() = runTest {
        coEvery { filiereRepository.getEligibleOnce(2, any()) } returns listOf(sampleProgram)
        coEvery { aiService.getRecommendations(any(), any(), any(), any()) } returns sampleRecs

        useCase("science", "علوم تجريبية", 160.0, "")

        coVerify { filiereRepository.getEligibleOnce(2, 160.0) }
    }

    @Test fun `tech section uses bit 4`() = runTest {
        coEvery { filiereRepository.getEligibleOnce(4, any()) } returns listOf(sampleProgram)
        coEvery { aiService.getRecommendations(any(), any(), any(), any()) } returns sampleRecs

        useCase("tech", "علوم تقنية", 160.0, "")

        coVerify { filiereRepository.getEligibleOnce(4, 160.0) }
    }

    @Test fun `info section uses bit 8`() = runTest {
        coEvery { filiereRepository.getEligibleOnce(8, any()) } returns listOf(sampleProgram)
        coEvery { aiService.getRecommendations(any(), any(), any(), any()) } returns sampleRecs

        useCase("info", "علوم إعلامية", 160.0, "")

        coVerify { filiereRepository.getEligibleOnce(8, 160.0) }
    }

    @Test fun `literature section uses bit 16`() = runTest {
        coEvery { filiereRepository.getEligibleOnce(16, any()) } returns listOf(sampleProgram)
        coEvery { aiService.getRecommendations(any(), any(), any(), any()) } returns sampleRecs

        useCase("literature", "آداب", 160.0, "")

        coVerify { filiereRepository.getEligibleOnce(16, 160.0) }
    }

    @Test fun `economy section uses bit 32`() = runTest {
        coEvery { filiereRepository.getEligibleOnce(32, any()) } returns listOf(sampleProgram)
        coEvery { aiService.getRecommendations(any(), any(), any(), any()) } returns sampleRecs

        useCase("economy", "اقتصاد و تصرف", 160.0, "")

        coVerify { filiereRepository.getEligibleOnce(32, 160.0) }
    }

    @Test fun `unknown section maps to bit 0`() = runTest {
        coEvery { filiereRepository.getEligibleOnce(0, any()) } returns listOf(sampleProgram)
        coEvery { aiService.getRecommendations(any(), any(), any(), any()) } returns sampleRecs

        useCase("unknown", "?", 160.0, "")

        coVerify { filiereRepository.getEligibleOnce(0, 160.0) }
    }

    // ── Error paths ───────────────────────────────────────────────────────────

    @Test fun `throws when no eligible programs found`() = runTest {
        coEvery { filiereRepository.getEligibleOnce(any(), any()) } returns emptyList()

        val error = runCatching {
            useCase("math", "رياضيات", 999.0, "")
        }.exceptionOrNull()

        assertThat(error).isNotNull()
        assertThat(error).isInstanceOf(Exception::class.java)
        assertThat(error!!.message).isNotNull()
    }

    @Test fun `propagates AI service exception`() = runTest {
        coEvery { filiereRepository.getEligibleOnce(any(), any()) } returns listOf(sampleProgram)
        coEvery { aiService.getRecommendations(any(), any(), any(), any()) } throws
                RuntimeException("Gemini unavailable")

        val error = runCatching {
            useCase("math", "رياضيات", 150.0, "")
        }.exceptionOrNull()

        assertThat(error).isNotNull()
        assertThat(error!!.message).isEqualTo("Gemini unavailable")
    }

    @Test fun `interest persistence failure does not abort the flow`() = runTest {
        // userRepository is relaxed — all calls succeed silently by default.
        // Override to throw; the use-case must swallow it and continue.
        coEvery { userRepository.updateInterests(any()) } throws RuntimeException("DB error")
        coEvery { filiereRepository.getEligibleOnce(any(), any()) } returns listOf(sampleProgram)
        coEvery { aiService.getRecommendations(any(), any(), any(), any()) } returns sampleRecs

        // Must not throw despite persistence failure.
        val result = useCase("math", "رياضيات", 150.0, "interests")

        assertThat(result).isEqualTo(sampleRecs)
    }
}
