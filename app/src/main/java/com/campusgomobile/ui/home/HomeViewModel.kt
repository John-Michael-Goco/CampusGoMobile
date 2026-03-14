package com.campusgomobile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusgomobile.data.auth.AuthResult
import com.campusgomobile.data.auth.AuthRepository
import com.campusgomobile.data.inventory.InventoryRepository
import com.campusgomobile.data.inventory.InventoryResult
import com.campusgomobile.data.model.Achievement
import com.campusgomobile.data.model.InventoryEntry
import com.campusgomobile.data.model.Participation
import com.campusgomobile.data.quests.QuestsRepository
import com.campusgomobile.data.quests.QuestsResult
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val activeQuests: List<Participation> = emptyList(),
    val inventoryItems: List<InventoryEntry> = emptyList(),
    val earnedAchievements: List<Achievement> = emptyList()
)

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val questsRepository: QuestsRepository,
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val questsDeferred = async { questsRepository.getParticipating() }
            val inventoryDeferred = async { inventoryRepository.getInventory() }
            val achievementsDeferred = async { authRepository.getAchievements() }

            val questsResult = questsDeferred.await()
            val inventoryResult = inventoryDeferred.await()
            val achievementsResult = achievementsDeferred.await()

            val quests = when (questsResult) {
                is QuestsResult.Success ->
                    questsResult.data.participations.filter { it.status == "active" }.take(3)
                else -> emptyList()
            }

            val inventory = when (inventoryResult) {
                is InventoryResult.Success -> inventoryResult.data.inventory.take(3)
                else -> emptyList()
            }

            val achievements = when (achievementsResult) {
                is AuthResult.Success ->
                    achievementsResult.data.achievements
                        .filter { it.earned }
                        .sortedByDescending { it.earnedAt }
                        .take(3)
                else -> emptyList()
            }

            _uiState.value = HomeUiState(
                isLoading = false,
                activeQuests = quests,
                inventoryItems = inventory,
                earnedAchievements = achievements
            )
        }
    }
}
