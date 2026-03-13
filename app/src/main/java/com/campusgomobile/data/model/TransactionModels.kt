package com.campusgomobile.data.model

import com.google.gson.annotations.SerializedName

data class TransactionsResponse(
    val transactions: List<PointTransaction>,
    @SerializedName("points_balance") val pointsBalance: Int,
    val pagination: TransactionsPagination
)

data class PointTransaction(
    val id: Int,
    val amount: Int,
    @SerializedName("transaction_type") val transactionType: String,
    @SerializedName("type_label") val typeLabel: String? = null,
    @SerializedName("reference_id") val referenceId: String? = null,
    @SerializedName("created_at") val createdAt: String
)

data class TransactionsPagination(
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("per_page") val perPage: Int,
    val total: Int,
    @SerializedName("last_page") val lastPage: Int
)
