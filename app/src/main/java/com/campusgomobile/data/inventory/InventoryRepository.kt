package com.campusgomobile.data.inventory

import com.campusgomobile.data.auth.TokenStorage
import com.campusgomobile.data.model.InventoryHistoryResponse
import com.campusgomobile.data.model.InventoryResponse
import com.campusgomobile.data.model.UseItemResponse
import com.campusgomobile.data.network.NetworkModule
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

sealed class InventoryResult<out T> {
    data class Success<T>(val data: T) : InventoryResult<T>()
    data class Error(val message: String) : InventoryResult<Nothing>()
    data class NetworkError(val cause: Throwable) : InventoryResult<Nothing>()
}

class InventoryRepository(private val tokenStorage: TokenStorage) {

    private suspend fun inventoryApi() = with(NetworkModule) {
        val token = tokenStorage.token.first() ?: throw IOException("Not logged in")
        createInventoryApi(createAuthenticatedClient(token))
    }

    suspend fun getInventory(): InventoryResult<InventoryResponse> {
        return try {
            val api = inventoryApi()
            val response = api.getInventory()
            if (response.isSuccessful) {
                response.body()?.let { InventoryResult.Success(it) }
                    ?: InventoryResult.Error("Empty response")
            } else {
                val msg = response.errorBody()?.string()?.let { parseMessage(it) }
                    ?: "Failed to load inventory (${response.code()})"
                InventoryResult.Error(msg)
            }
        } catch (e: HttpException) {
            InventoryResult.Error(e.message() ?: "Request failed")
        } catch (e: IOException) {
            InventoryResult.NetworkError(e)
        }
    }

    suspend fun getHistory(page: Int = 1, perPage: Int = 20): InventoryResult<InventoryHistoryResponse> {
        return try {
            val api = inventoryApi()
            val response = api.getHistory(page, perPage)
            if (response.isSuccessful) {
                response.body()?.let { InventoryResult.Success(it) }
                    ?: InventoryResult.Error("Empty response")
            } else {
                val msg = response.errorBody()?.string()?.let { parseMessage(it) }
                    ?: "Failed to load history (${response.code()})"
                InventoryResult.Error(msg)
            }
        } catch (e: HttpException) {
            InventoryResult.Error(e.message() ?: "Request failed")
        } catch (e: IOException) {
            InventoryResult.NetworkError(e)
        }
    }

    suspend fun useItem(inventoryId: Int): InventoryResult<UseItemResponse> {
        return try {
            val api = inventoryApi()
            val response = api.useItem(inventoryId)
            if (response.isSuccessful) {
                response.body()?.let { InventoryResult.Success(it) }
                    ?: InventoryResult.Error("Empty response")
            } else {
                val msg = response.errorBody()?.string()?.let { parseMessage(it) }
                    ?: "Failed to use item (${response.code()})"
                InventoryResult.Error(msg)
            }
        } catch (e: HttpException) {
            InventoryResult.Error(e.message() ?: "Request failed")
        } catch (e: IOException) {
            InventoryResult.NetworkError(e)
        }
    }

    private fun parseMessage(body: String): String? {
        return try {
            val json = org.json.JSONObject(body)
            json.optString("message").takeIf { it.isNotBlank() }
                ?: json.optJSONObject("errors")?.let { err ->
                    err.keys().asSequence().flatMap { key ->
                        (err.get(key) as? org.json.JSONArray)?.let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        } ?: emptyList()
                    }.joinToString(" ")
                }?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
