package com.campusgomobile.data.model

import com.google.gson.annotations.SerializedName

data class InventoryResponse(
    val inventory: List<InventoryEntry>
)

data class InventoryEntry(
    val id: Int,
    @SerializedName("item_id") val itemId: Int? = null,
    val quantity: Int,
    @SerializedName("acquired_at") val acquiredAt: String,
    @SerializedName("source_quest_id") val sourceQuestId: Int? = null,
    @SerializedName("store_item") val storeItem: StoreItemRef? = null,
    @SerializedName("custom_prize_description") val customPrizeDescription: String? = null
)

data class StoreItemRef(
    val id: Int,
    val name: String,
    val description: String? = null
)

data class UseItemResponse(
    val message: String? = null,
    @SerializedName("item_name") val itemName: String? = null,
    @SerializedName("remaining_quantity") val remainingQuantity: Int? = null
)

data class InventoryHistoryResponse(
    val history: List<UsedItemHistoryEntry>? = null,
    val pagination: InventoryHistoryPagination? = null
)

data class UsedItemHistoryEntry(
    @SerializedName("item_name") val itemName: String? = null,
    @SerializedName("used_at") val usedAt: String? = null
)

data class InventoryHistoryPagination(
    @SerializedName("current_page") val currentPage: Int = 1,
    @SerializedName("per_page") val perPage: Int = 20,
    val total: Int = 0,
    @SerializedName("last_page") val lastPage: Int = 1
)
