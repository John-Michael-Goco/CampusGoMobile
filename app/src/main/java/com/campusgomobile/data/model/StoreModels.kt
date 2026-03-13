package com.campusgomobile.data.model

import com.google.gson.annotations.SerializedName

data class StoreResponse(
    @SerializedName("points_balance") val pointsBalance: Int,
    val items: List<StoreItem>
)

data class StoreItem(
    val id: Int,
    val name: String,
    val description: String? = null,
    @SerializedName("cost_points") val costPoints: Int,
    val stock: Int? = null,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("is_available") val isAvailable: Boolean = true,
    @SerializedName("can_afford") val canAfford: Boolean = false,
    @SerializedName("image_url") val imageUrl: String? = null
)

data class RedeemRequest(
    @SerializedName("store_item_id") val storeItemId: Int,
    val quantity: Int = 1
)

data class RedeemResponse(
    val message: String? = null,
    @SerializedName("points_balance") val pointsBalance: Int,
    val redeemed: RedeemedItem? = null
)

data class RedeemedItem(
    @SerializedName("store_item_id") val storeItemId: Int,
    val name: String,
    val quantity: Int,
    @SerializedName("cost_points") val costPoints: Int
)
