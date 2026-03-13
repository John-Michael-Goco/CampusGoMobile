package com.campusgomobile.data.store

import com.campusgomobile.data.auth.TokenStorage
import com.campusgomobile.data.model.RedeemRequest
import com.campusgomobile.data.model.StoreResponse
import com.campusgomobile.data.network.NetworkModule
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

sealed class StoreResult<out T> {
    data class Success<T>(val data: T) : StoreResult<T>()
    data class Error(val message: String) : StoreResult<Nothing>()
    data class NetworkError(val cause: Throwable) : StoreResult<Nothing>()
}

class StoreRepository(private val tokenStorage: TokenStorage) {

    private suspend fun storeApi() = with(NetworkModule) {
        val token = tokenStorage.token.first() ?: throw IOException("Not logged in")
        createStoreApi(createAuthenticatedClient(token))
    }

    suspend fun getStore(): StoreResult<StoreResponse> {
        return try {
            val api = storeApi()
            val response = api.getStore()
            if (response.isSuccessful) {
                response.body()?.let { StoreResult.Success(it) }
                    ?: StoreResult.Error("Empty response")
            } else {
                val msg = response.errorBody()?.string()?.let { parseMessage(it) }
                    ?: "Failed to load store (${response.code()})"
                StoreResult.Error(msg)
            }
        } catch (e: HttpException) {
            StoreResult.Error(e.message() ?: "Request failed")
        } catch (e: IOException) {
            StoreResult.NetworkError(e)
        }
    }

    suspend fun redeem(storeItemId: Int, quantity: Int = 1): StoreResult<com.campusgomobile.data.model.RedeemResponse> {
        return try {
            val api = storeApi()
            val response = api.redeem(RedeemRequest(storeItemId = storeItemId, quantity = quantity))
            if (response.isSuccessful) {
                response.body()?.let { StoreResult.Success(it) }
                    ?: StoreResult.Error("Empty response")
            } else {
                val msg = response.errorBody()?.string()?.let { parseMessage(it) }
                    ?: "Redeem failed (${response.code()})"
                StoreResult.Error(msg)
            }
        } catch (e: HttpException) {
            StoreResult.Error(e.message() ?: "Request failed")
        } catch (e: IOException) {
            StoreResult.NetworkError(e)
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
