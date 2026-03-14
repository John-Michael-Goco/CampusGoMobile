package com.campusgomobile.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusgomobile.data.model.StoreItem
import com.campusgomobile.data.store.StoreResult
import com.campusgomobile.data.store.StoreRepository
import com.campusgomobile.ui.shell.NotificationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StoreUiState(
    val pointsBalance: Int = 0,
    val items: List<StoreItem> = emptyList(),
    val isLoading: Boolean = false,
    val redeemLoadingId: Int? = null,
    val errorMessage: String? = null,
    val redeemSuccessMessage: String? = null
)

class StoreViewModel(private val storeRepository: StoreRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    private var lastKnownStoreItemIds: Set<Int> = emptySet()

    init {
        loadStore()
    }

    fun loadStore(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = storeRepository.getStore()) {
                is StoreResult.Success -> {
                    val currentIds = result.data.items.map { it.id }.toSet()
                    if (lastKnownStoreItemIds.isNotEmpty()) {
                        val newIds = currentIds - lastKnownStoreItemIds
                        if (newIds.isNotEmpty()) {
                            NotificationState.addNewStoreItemIds(newIds)
                        }
                    }
                    lastKnownStoreItemIds = currentIds
                    _uiState.value = _uiState.value.copy(
                        pointsBalance = result.data.pointsBalance,
                        items = result.data.items,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                is StoreResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                is StoreResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.cause.message ?: "Network error"
                    )
                }
            }
        }
    }

    fun redeem(item: StoreItem) {
        if (!item.isAvailable || !item.canAfford) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                redeemLoadingId = item.id,
                errorMessage = null,
                redeemSuccessMessage = null
            )
            when (val result = storeRepository.redeem(item.id, 1)) {
                is StoreResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        pointsBalance = result.data.pointsBalance,
                        redeemLoadingId = null,
                        redeemSuccessMessage = result.data.redeemed?.name ?: "Item"
                    )
                    loadStore(silent = true)
                }
                is StoreResult.Error -> {
                    _uiState.value = _uiState.value.copy(redeemLoadingId = null, errorMessage = result.message)
                }
                is StoreResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        redeemLoadingId = null,
                        errorMessage = result.cause.message ?: "Network error"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearRedeemSuccess() {
        _uiState.value = _uiState.value.copy(redeemSuccessMessage = null)
    }
}
