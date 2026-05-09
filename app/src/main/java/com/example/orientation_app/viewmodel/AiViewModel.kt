package com.example.orientation_app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orientation_app.data.db.UniCompassDatabase
import com.example.orientation_app.data.model.AiRecommendation
import com.example.orientation_app.data.repository.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AiUiState {
    object Idle : AiUiState()
    object Loading : AiUiState()
    data class Success(val recommendations: List<AiRecommendation>) : AiUiState()
    data class Error(val message: String) : AiUiState()
}

class AiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AiRepository by lazy {
        val db = UniCompassDatabase.getInstance(application)
        AiRepository(
            filiereDao          = db.filiereDao(),
            aiInsightHistoryDao = db.aiInsightHistoryDao(),
            userProfileDao      = db.userProfileDao()
        )
    }

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    fun getRecommendation(
        sectionId: String,
        sectionName: String,
        fgScore: Double,
        interestText: String
    ) {
        if (_uiState.value is AiUiState.Loading || _uiState.value is AiUiState.Success) return

        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            try {
                val recommendations = repository.getRecommendation(sectionId, sectionName, fgScore, interestText)
                _uiState.value = AiUiState.Success(recommendations)
            } catch (e: Exception) {
                _uiState.value = AiUiState.Error(
                    e.message ?: "حدث خطأ غير متوقع. تحقق من اتصالك بالإنترنت وحاول مجدداً."
                )
            }
        }
    }

    fun retry(sectionId: String, sectionName: String, fgScore: Double, interestText: String) {
        _uiState.value = AiUiState.Idle
        getRecommendation(sectionId, sectionName, fgScore, interestText)
    }
}
