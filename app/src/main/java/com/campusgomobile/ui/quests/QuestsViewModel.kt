package com.campusgomobile.ui.quests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusgomobile.data.model.DiscoverQuestDetailData
import com.campusgomobile.data.model.MyQuestDetailData
import com.campusgomobile.data.model.Participation
import com.campusgomobile.data.model.Quest
import com.campusgomobile.data.quests.QuestsResult
import com.campusgomobile.data.quests.QuestsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class QuestsSegment {
    MyQuests,
    Discover
}

data class QuestsUiState(
    val segment: QuestsSegment = QuestsSegment.Discover,
    val participations: List<Participation> = emptyList(),
    val quests: List<Quest> = emptyList(),
    val selectedQuestTypeFilter: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val historyList: List<Participation> = emptyList(),
    val historyLoading: Boolean = false,
    val historyError: String? = null,
    val myQuestDetail: MyQuestDetailData? = null,
    val myQuestDetailLoading: Boolean = false,
    val myQuestDetailError: String? = null,
    val questHistoryDetail: MyQuestDetailData? = null,
    val questHistoryDetailLoading: Boolean = false,
    val questHistoryDetailError: String? = null,
    val discoverQuestDetail: DiscoverQuestDetailData? = null,
    val discoverQuestDetailLoading: Boolean = false,
    val discoverQuestDetailError: String? = null,
    val quitLoading: Boolean = false,
    val quitError: String? = null,
    val quitSuccess: Boolean = false
)

class QuestsViewModel(private val questsRepository: QuestsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestsUiState())
    val uiState: StateFlow<QuestsUiState> = _uiState.asStateFlow()

    init {
        loadAll(silent = false)
    }

    fun setSegment(segment: QuestsSegment) {
        _uiState.value = _uiState.value.copy(segment = segment)
    }

    /** Filter by quest type: null = All, "daily" | "event" | "custom". Only these three are shown in the filter; enrollment-only list shows no filter. */
    fun setQuestTypeFilter(type: String?) {
        _uiState.value = _uiState.value.copy(selectedQuestTypeFilter = type)
    }

    fun loadAll(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            var error: String? = null
            when (val result = questsRepository.getParticipating()) {
                is QuestsResult.Success -> {
                    _uiState.value = _uiState.value.copy(participations = result.data.participations)
                }
                is QuestsResult.Error -> {
                    _uiState.value = _uiState.value.copy(participations = emptyList())
                    error = result.message
                }
                is QuestsResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(participations = emptyList())
                    error = result.cause.message ?: "Network error"
                }
            }
            when (val result = questsRepository.getQuests()) {
                is QuestsResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        quests = result.data.quests,
                        errorMessage = error,
                        isLoading = false
                    )
                }
                is QuestsResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        quests = emptyList(),
                        errorMessage = error ?: result.message,
                        isLoading = false
                    )
                }
                is QuestsResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        quests = emptyList(),
                        errorMessage = error ?: (result.cause.message ?: "Network error"),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun refresh() {
        loadAll(silent = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun loadHistory(search: String?, questType: String?, dateFrom: String? = null, dateTo: String? = null, page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                historyLoading = true,
                historyError = null
            )
            when (val result = questsRepository.getQuestHistory(
                search = search,
                questType = questType,
                dateFrom = dateFrom,
                dateTo = dateTo,
                page = page,
                perPage = 20
            )) {
                is QuestsResult.Success -> {
                    val list = result.data.history.map { it.toParticipation() }
                    _uiState.value = _uiState.value.copy(
                        historyList = list,
                        historyLoading = false,
                        historyError = null
                    )
                }
                is QuestsResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        historyList = emptyList(),
                        historyLoading = false,
                        historyError = result.message
                    )
                }
                is QuestsResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        historyList = emptyList(),
                        historyLoading = false,
                        historyError = result.cause.message ?: "Network error"
                    )
                }
            }
        }
    }

    fun clearHistoryError() {
        _uiState.value = _uiState.value.copy(historyError = null)
    }

    fun loadMyQuestDetail(participantId: Int, questId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                myQuestDetail = null,
                myQuestDetailLoading = true,
                myQuestDetailError = null
            )
            when (val result = questsRepository.loadMyQuestDetail(participantId, questId)) {
                is QuestsResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        myQuestDetail = result.data,
                        myQuestDetailLoading = false,
                        myQuestDetailError = null
                    )
                }
                is QuestsResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        myQuestDetail = null,
                        myQuestDetailLoading = false,
                        myQuestDetailError = result.message
                    )
                }
                is QuestsResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        myQuestDetail = null,
                        myQuestDetailLoading = false,
                        myQuestDetailError = result.cause.message ?: "Network error"
                    )
                }
            }
        }
    }

    fun clearMyQuestDetail() {
        _uiState.value = _uiState.value.copy(
            myQuestDetail = null,
            myQuestDetailLoading = false,
            myQuestDetailError = null
        )
    }

    fun loadQuestHistoryDetail(
        participantId: Int,
        questId: Int,
        status: String,
        currentStage: Int,
        totalStages: Int
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                questHistoryDetail = null,
                questHistoryDetailLoading = true,
                questHistoryDetailError = null
            )
            when (val result = questsRepository.loadQuestHistoryDetail(
                participantId = participantId,
                questId = questId,
                status = status,
                currentStage = currentStage,
                totalStages = totalStages
            )) {
                is QuestsResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        questHistoryDetail = result.data,
                        questHistoryDetailLoading = false,
                        questHistoryDetailError = null
                    )
                }
                is QuestsResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        questHistoryDetail = null,
                        questHistoryDetailLoading = false,
                        questHistoryDetailError = result.message
                    )
                }
                is QuestsResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        questHistoryDetail = null,
                        questHistoryDetailLoading = false,
                        questHistoryDetailError = result.cause.message ?: "Network error"
                    )
                }
            }
        }
    }

    fun clearQuestHistoryDetail() {
        _uiState.value = _uiState.value.copy(
            questHistoryDetail = null,
            questHistoryDetailLoading = false,
            questHistoryDetailError = null
        )
    }

    fun loadDiscoverQuestDetail(questId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                discoverQuestDetail = null,
                discoverQuestDetailLoading = true,
                discoverQuestDetailError = null
            )
            when (val result = questsRepository.loadDiscoverQuestDetail(questId)) {
                is QuestsResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        discoverQuestDetail = result.data,
                        discoverQuestDetailLoading = false,
                        discoverQuestDetailError = null
                    )
                }
                is QuestsResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        discoverQuestDetail = null,
                        discoverQuestDetailLoading = false,
                        discoverQuestDetailError = result.message
                    )
                }
                is QuestsResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        discoverQuestDetail = null,
                        discoverQuestDetailLoading = false,
                        discoverQuestDetailError = result.cause.message ?: "Network error"
                    )
                }
            }
        }
    }

    fun clearDiscoverQuestDetail() {
        _uiState.value = _uiState.value.copy(
            discoverQuestDetail = null,
            discoverQuestDetailLoading = false,
            discoverQuestDetailError = null
        )
    }

    fun quitQuest(participantId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(quitLoading = true, quitError = null)
            when (val result = questsRepository.quitQuest(participantId)) {
                is QuestsResult.Success -> {
                    clearMyQuestDetail()
                    loadAll(silent = true)
                    _uiState.value = _uiState.value.copy(
                        quitLoading = false,
                        quitError = null,
                        quitSuccess = true
                    )
                }
                is QuestsResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        quitLoading = false,
                        quitError = result.message,
                        quitSuccess = false
                    )
                }
                is QuestsResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        quitLoading = false,
                        quitError = result.cause.message ?: "Network error",
                        quitSuccess = false
                    )
                }
            }
        }
    }

    fun clearQuitSuccess() {
        _uiState.value = _uiState.value.copy(quitSuccess = false)
    }

    fun clearQuitError() {
        _uiState.value = _uiState.value.copy(quitError = null)
    }
}
