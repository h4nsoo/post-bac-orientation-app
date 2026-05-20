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
     * Fetches AI recommendations. Idempotent — silently skips if a request is
     * already in-flight or a successful result is already cached in [uiState].
     */
    fun getRecommendation(
        sectionId: String,
        sectionName: String,
        fgScore: Double,
        interestText: String
    ) {
        val current = _uiState.value
        if (current is AiUiState.Loading || current is AiUiState.Success) return

        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            _uiState.value = runCatching {
                AiUiState.Success(getRecommendations(sectionId, sectionName, fgScore, interestText))
            }.getOrElse { e ->
                AiUiState.Error(
                    e.message ?: "حدث خطأ غير متوقع. تحقق من اتصالك بالإنترنت وحاول مجدداً."
                )
            }
        }
    }

    /** Resets to [AiUiState.Idle] then re-fetches. */
    fun retry(sectionId: String, sectionName: String, fgScore: Double, interestText: String) {
        _uiState.value = AiUiState.Idle
        getRecommendation(sectionId, sectionName, fgScore, interestText)
    }
}
