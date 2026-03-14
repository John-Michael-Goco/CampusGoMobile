package com.campusgomobile.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusgomobile.data.model.JoinResponse
import com.campusgomobile.data.model.QrResolveResponse
import com.campusgomobile.data.quests.QuestsRepository
import com.campusgomobile.data.quests.QuestsResult
import com.campusgomobile.scanner.parseQuestQrPayload
import com.campusgomobile.util.QuestTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScannerUiState(
    val scanning: Boolean = true,
    val resolving: Boolean = false,
    val resolveResult: QrResolveResponse? = null,
    val resolveError: String? = null,
    val joining: Boolean = false,
    val joinResult: JoinResponse? = null,
    val joinError: String? = null
)

class ScannerViewModel(
    private val questsRepository: QuestsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    /** Called when a QR is scanned. Payload can be "questId:stageId" or full URL. Resolves via API for join/play and AR anchor. */
    fun onQrScanned(rawValue: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(scanning = false, resolving = true, resolveError = null)
            val result = when (val payload = parseQuestQrPayload(rawValue)) {
                null -> questsRepository.resolveQr(rawValue)
                else -> questsRepository.resolveByIds(payload.questId, payload.stageId)
            }
            when (result) {
                is QuestsResult.Success -> _uiState.value = _uiState.value.copy(
                    resolving = false,
                    resolveResult = result.data,
                    resolveError = null
                )
                is QuestsResult.Error -> _uiState.value = _uiState.value.copy(
                    resolving = false,
                    resolveError = result.message,
                    scanning = true
                )
                is QuestsResult.NetworkError -> _uiState.value = _uiState.value.copy(
                    resolving = false,
                    resolveError = "Network error",
                    scanning = true
                )
            }
        }
    }

    fun joinQuest(questId: Int, stageId: Int) {
        val resolve = _uiState.value.resolveResult
        if (resolve != null && resolve.stageNumber == 1) {
            val upcoming = QuestTimeUtils.isStageStartInFuture(resolve.stageStart) ||
                QuestTimeUtils.reasonSuggestsUpcomingOrNotAvailable(resolve.reason)
            if (upcoming) {
                _uiState.value = _uiState.value.copy(joinError = "Quest has not started yet.")
                return
            }
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(joining = true, joinError = null)
            when (val result = questsRepository.joinQuest(questId, stageId)) {
                is QuestsResult.Success -> _uiState.value = _uiState.value.copy(
                    joining = false,
                    joinResult = result.data,
                    joinError = null
                )
                is QuestsResult.Error -> _uiState.value = _uiState.value.copy(
                    joining = false,
                    joinError = result.message
                )
                is QuestsResult.NetworkError -> _uiState.value = _uiState.value.copy(
                    joining = false,
                    joinError = "Network error"
                )
            }
        }
    }

    fun resetScanner() {
        _uiState.value = ScannerUiState()
    }
}
