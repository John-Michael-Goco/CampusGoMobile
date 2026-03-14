package com.campusgomobile.ui.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusgomobile.data.model.PlayStateResponse
import com.campusgomobile.data.quests.QuestsRepository
import com.campusgomobile.data.quests.QuestsResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayUiState(
    val loading: Boolean = true,
    val playState: PlayStateResponse? = null,
    val error: String? = null,
    // question id -> selected choice id
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val submitting: Boolean = false,
    val submitResult: PlayStateResponse? = null,
    val submitError: String? = null
)

class PlayViewModel(
    private val questsRepository: QuestsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayUiState())
    val uiState: StateFlow<PlayUiState> = _uiState.asStateFlow()

    fun loadPlayState(participantId: Int) {
        viewModelScope.launch {
            _uiState.value = PlayUiState(loading = true)
            when (val result = questsRepository.getPlayState(participantId)) {
                is QuestsResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        playState = result.data,
                        error = null
                    )
                }
                is QuestsResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = result.message
                    )
                }
                is QuestsResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = result.cause.message ?: "Network error"
                    )
                }
            }
        }
    }

    fun selectAnswer(questionId: Int, choiceId: Int) {
        val current = _uiState.value.selectedAnswers.toMutableMap()
        current[questionId] = choiceId
        _uiState.value = _uiState.value.copy(selectedAnswers = current)
    }

    fun submit(participantId: Int) {
        val answers = _uiState.value.selectedAnswers
        if (answers.isEmpty()) return
        if (_uiState.value.submitting) return

        _uiState.value = _uiState.value.copy(submitting = true, submitError = null)

        val answersList = answers.map { (qId, cId) ->
            mapOf("question_id" to qId, "choice_id" to cId)
        }

        viewModelScope.launch {
            when (val result = questsRepository.submitAnswers(participantId, answersList)) {
                is QuestsResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        submitting = false,
                        submitResult = result.data,
                        submitError = null
                    )
                }
                is QuestsResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        submitting = false,
                        submitError = result.message
                    )
                }
                is QuestsResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        submitting = false,
                        submitError = result.cause.message ?: "Network error"
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.value = PlayUiState()
    }
}
