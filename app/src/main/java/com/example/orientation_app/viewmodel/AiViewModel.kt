package com.example.orientation_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.orientation_app.domain.model.Recommendation
import com.example.orientation_app.domain.usecase.GetAiRecommendationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI state ─────────────────────────────────────────────────────────────────

sealed class AiUiState {
    object Idle    : AiUiState()
    object Loading : AiUiState()
    data class Success(val recommendations: List<Recommendation>) : AiUiState()
    data class Error(val message: String) : AiUiState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AiViewModel @Inject constructor(
    private val getRecommendations: GetAiRecommendationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    /**
     * Captures the inputs that produced the last [AiUiState.Success] result.
     * A new call with the same inputs is a no-op; a call with different inputs
     * (e.g. the user changed their interest text) triggers a fresh fetch.
     */
    private data class RequestParams(
        val sectionId    : String,
        val fgScore      : Double,
        val interestText : String
    )
    private var lastSuccessParams: RequestParams? = null

    /**
     * Fetches AI recommendations.
     *
     * Skips the call if:
     * - A request is already in-flight ([AiUiState.Loading]).
     * - The last successful result used the exact same [sectionId], [fgScore],
     *   and [interestText] — returning to the screen without changing inputs
     *   should not burn an extra API call.
     *
     * A changed [interestText] (user went back and rewrote their interests)
     * always triggers a new fetch, even if a previous [AiUiState.Success] is
     * already in state.
     */
    fun getRecommendation(
        sectionId    : String,
        sectionName  : String,
        fgScore      : Double,
        interestText : String
    ) {
        val params  = RequestParams(sectionId, fgScore, interestText)
        val current = _uiState.value
        if (current is AiUiState.Loading) return
        if (current is AiUiState.Success && lastSuccessParams == params) return

        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            val result = runCatching {
                getRecommendations(sectionId, sectionName, fgScore, interestText)
            }
            if (result.isSuccess) {
                lastSuccessParams = params
                _uiState.value = AiUiState.Success(result.getOrThrow())
            } else {
                _uiState.value = AiUiState.Error(
                    result.exceptionOrNull()?.message
                        ?: "حدث خطأ غير متوقع. تحقق من اتصالك بالإنترنت وحاول مجدداً."
                )
            }
        }
    }

    /** Resets to [AiUiState.Idle] then re-fetches, ignoring the param cache. */
    fun retry(sectionId: String, sectionName: String, fgScore: Double, interestText: String) {
        lastSuccessParams = null
        _uiState.value = AiUiState.Idle
        getRecommendation(sectionId, sectionName, fgScore, interestText)
    }
}
