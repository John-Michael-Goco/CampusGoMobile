package com.campusgomobile.ui.shell

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationState {
    private val _hasNewStoreItems = MutableStateFlow(false)
    val hasNewStoreItems: StateFlow<Boolean> = _hasNewStoreItems.asStateFlow()

    private val _hasNewQuests = MutableStateFlow(false)
    val hasNewQuests: StateFlow<Boolean> = _hasNewQuests.asStateFlow()

    private val _hasQuestUpdates = MutableStateFlow(false)
    val hasQuestUpdates: StateFlow<Boolean> = _hasQuestUpdates.asStateFlow()

    /** Store item IDs that are newly added since last check */
    private val _newStoreItemIds = MutableStateFlow<Set<Int>>(emptySet())
    val newStoreItemIds: StateFlow<Set<Int>> = _newStoreItemIds.asStateFlow()

    /** Discover quest IDs that are newly available since last check */
    private val _newQuestIds = MutableStateFlow<Set<Int>>(emptySet())
    val newQuestIds: StateFlow<Set<Int>> = _newQuestIds.asStateFlow()

    /** Discover quest IDs that changed from upcoming to ongoing */
    private val _justStartedQuestIds = MutableStateFlow<Set<Int>>(emptySet())
    val justStartedQuestIds: StateFlow<Set<Int>> = _justStartedQuestIds.asStateFlow()

    /** Participation IDs whose status changed (awaiting_ranking -> resolved) with label */
    private val _updatedParticipationIds = MutableStateFlow<Map<Int, String>>(emptyMap())
    val updatedParticipationIds: StateFlow<Map<Int, String>> = _updatedParticipationIds.asStateFlow()

    /** Participation IDs whose stage just advanced */
    private val _stageAdvancedParticipationIds = MutableStateFlow<Set<Int>>(emptySet())
    val stageAdvancedParticipationIds: StateFlow<Set<Int>> = _stageAdvancedParticipationIds.asStateFlow()

    fun setNewStoreItems(value: Boolean) {
        _hasNewStoreItems.value = value
    }

    fun addNewStoreItemIds(ids: Set<Int>) {
        _newStoreItemIds.value = _newStoreItemIds.value + ids
        _hasNewStoreItems.value = true
    }

    fun clearNewStoreItemIds() {
        _newStoreItemIds.value = emptySet()
        _hasNewStoreItems.value = false
    }

    fun setNewQuests(value: Boolean) {
        _hasNewQuests.value = value
    }

    fun addNewQuestIds(ids: Set<Int>) {
        _newQuestIds.value = _newQuestIds.value + ids
        _hasNewQuests.value = true
    }

    fun addJustStartedQuestIds(ids: Set<Int>) {
        _justStartedQuestIds.value = _justStartedQuestIds.value + ids
        _hasQuestUpdates.value = true
    }

    fun addUpdatedParticipation(participantId: Int, label: String) {
        _updatedParticipationIds.value = _updatedParticipationIds.value + (participantId to label)
        _hasQuestUpdates.value = true
    }

    fun addStageAdvancedParticipation(participantId: Int) {
        _stageAdvancedParticipationIds.value = _stageAdvancedParticipationIds.value + participantId
        _hasQuestUpdates.value = true
    }

    fun clearQuestUpdates() {
        _newQuestIds.value = emptySet()
        _justStartedQuestIds.value = emptySet()
        _updatedParticipationIds.value = emptyMap()
        _stageAdvancedParticipationIds.value = emptySet()
        _hasNewQuests.value = false
        _hasQuestUpdates.value = false
    }
}
