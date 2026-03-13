package com.campusgomobile.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusgomobile.data.model.InventoryEntry
import com.campusgomobile.data.model.UsedItemHistoryEntry
import com.campusgomobile.data.inventory.InventoryResult
import com.campusgomobile.data.inventory.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InventoryUiState(
    val items: List<InventoryEntry> = emptyList(),
    val isLoading: Boolean = false,
    val useLoadingId: Int? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val historyEntries: List<UsedItemHistoryEntry> = emptyList(),
    val historyLoading: Boolean = false,
    val historyError: String? = null
)

class InventoryViewModel(private val inventoryRepository: InventoryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        loadInventory()
    }

    fun loadInventory(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = inventoryRepository.getInventory()) {
                is InventoryResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        items = result.data.inventory,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                is InventoryResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                is InventoryResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.cause.message ?: "Network error"
                    )
                }
            }
        }
    }

    fun useItem(entry: InventoryEntry) {
        if (entry.quantity < 1) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                useLoadingId = entry.id,
                errorMessage = null,
                successMessage = null
            )
            when (val result = inventoryRepository.useItem(entry.id)) {
                is InventoryResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        useLoadingId = null,
                        successMessage = result.data.itemName ?: "Item"
                    )
                    loadInventory(silent = true)
                }
                is InventoryResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        useLoadingId = null,
                        errorMessage = result.message
                    )
                }
                is InventoryResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        useLoadingId = null,
                        errorMessage = result.cause.message ?: "Network error"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun loadHistory(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _uiState.value = _uiState.value.copy(historyLoading = true, historyError = null)
            when (val result = inventoryRepository.getHistory(page = 1, perPage = 50)) {
                is InventoryResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        historyEntries = result.data.history.orEmpty(),
                        historyLoading = false,
                        historyError = null
                    )
                }
                is InventoryResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        historyLoading = false,
                        historyError = result.message
                    )
                }
                is InventoryResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        historyLoading = false,
                        historyError = result.cause.message ?: "Network error"
                    )
                }
            }
        }
    }
}
