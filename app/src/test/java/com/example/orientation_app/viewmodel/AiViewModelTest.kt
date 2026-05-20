package com.example.orientation_app.viewmodel

import app.cash.turbine.test
import com.example.orientation_app.domain.model.Recommendation
import com.example.orientation_app.domain.model.ScoredProgram
import com.example.orientation_app.domain.usecase.GetAiRecommendationsUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AiViewModel].
 *
 * [StandardTestDispatcher] is installed on [Dispatchers.Main] so that
 * [viewModelScope] coroutines only advance when the test explicitly calls
 * [kotlinx.coroutines.test.TestCoroutineScheduler.advanceUntilIdle] (or when
 * [runTest] does so at the end of the block).
 *
 * Turbine is used to assert on the [uiState] Flow without race conditions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val useCase        = mockk<GetAiRecommendationsUseCase>()

    private lateinit var viewModel: AiViewModel

    // ── Sample data ───────────────────────────────────────────────────────────

    private val sampleProgram = ScoredProgram(
        id                 = 1,
        code               = "TEST-01",
        nameAr             = "برنامج تجريبي",
        nameFr             = "Programme Test",
        universityAr       = "جامعة التجارب",
        universityFr       = "Université Test",
        lastYearScore      = 150.0,
        capacity           = 100,
        sectionEligibility = 1
    )
    private val sampleRecs = listOf(Recommendation(sampleProgram, "تناسب ممتاز"))

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AiViewModel(useCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test fun `initial state is Idle`() {
        assertThat(viewModel.uiState.value).isInstanceOf(AiUiState.Idle::class.java)
    }

    // ── Happy path: Idle → Loading → Success ──────────────────────────────────

    @Test fun `getRecommendation - happy path emits Loading then Success`() = runTest {
        coEvery {
            useCase("math", "رياضيات", 180.0, "أحب الرياضيات")
        } returns sampleRecs

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(AiUiState.Idle::class.java)

            viewModel.getRecommendation("math", "رياضيات", 180.0, "أحب الرياضيات")
            assertThat(awaitItem()).isInstanceOf(AiUiState.Loading::class.java)

            val success = awaitItem() as AiUiState.Success
            assertThat(success.recommendations).isEqualTo(sampleRecs)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Error path: Idle → Loading → Error ───────────────────────────────────

    @Test fun `getRecommendation - exception emits Loading then Error`() = runTest {
        coEvery { useCase(any(), any(), any(), any()) } throws RuntimeException("network error")

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(AiUiState.Idle::class.java)

            viewModel.getRecommendation("math", "رياضيات", 0.0, "")
            assertThat(awaitItem()).isInstanceOf(AiUiState.Loading::class.java)

            val error = awaitItem() as AiUiState.Error
            assertThat(error.message).isEqualTo("network error")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `getRecommendation - null exception message uses fallback Arabic string`() = runTest {
        coEvery { useCase(any(), any(), any(), any()) } throws RuntimeException(null as String?)

        viewModel.getRecommendation("math", "رياضيات", 0.0, "")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as AiUiState.Error
        assertThat(state.message).isNotEmpty()
    }

    // ── Idempotency ───────────────────────────────────────────────────────────

    @Test fun `getRecommendation - is a no-op when already Loading`() = runTest {
        // Park the use-case so the coroutine stays suspended in Loading.
        // StandardTestDispatcher is lazy — runCurrent() advances the launch body
        // to its first suspension point (the useCase call) before the second
        // getRecommendation invocation sees Loading and short-circuits.
        val gate = CompletableDeferred<List<Recommendation>>()
        coEvery { useCase(any(), any(), any(), any()) } coAnswers { gate.await() }

        viewModel.getRecommendation("math", "رياضيات", 180.0, "")
        testDispatcher.scheduler.runCurrent()  // run up to useCase() suspension
        assertThat(viewModel.uiState.value).isInstanceOf(AiUiState.Loading::class.java)

        // Second call must be a no-op since the ViewModel is already Loading.
        viewModel.getRecommendation("math", "رياضيات", 180.0, "")

        gate.complete(sampleRecs)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { useCase(any(), any(), any(), any()) }
        assertThat(viewModel.uiState.value).isInstanceOf(AiUiState.Success::class.java)
    }

    @Test fun `getRecommendation - is a no-op when already in Success`() = runTest {
        coEvery { useCase(any(), any(), any(), any()) } returns sampleRecs

        viewModel.getRecommendation("math", "رياضيات", 180.0, "")
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(AiUiState.Success::class.java)

        viewModel.getRecommendation("math", "رياضيات", 180.0, "")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { useCase(any(), any(), any(), any()) }
    }

    // ── retry ─────────────────────────────────────────────────────────────────

    @Test fun `retry - after Success resets and fetches again`() = runTest {
        coEvery { useCase(any(), any(), any(), any()) } returns sampleRecs

        viewModel.getRecommendation("math", "رياضيات", 180.0, "")
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(AiUiState.Success::class.java)

        viewModel.retry("math", "رياضيات", 180.0, "")
        testDispatcher.scheduler.advanceUntilIdle()

        // Two total calls — one initial, one from retry.
        coVerify(exactly = 2) { useCase(any(), any(), any(), any()) }
        assertThat(viewModel.uiState.value).isInstanceOf(AiUiState.Success::class.java)
    }

    @Test fun `retry - after Error resets and fetches again`() = runTest {
        coEvery { useCase(any(), any(), any(), any()) }
            .throws(RuntimeException("fail"))
            .andThen(sampleRecs)

        viewModel.getRecommendation("math", "رياضيات", 180.0, "")
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(AiUiState.Error::class.java)

        viewModel.retry("math", "رياضيات", 180.0, "")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(AiUiState.Success::class.java)
    }
}
