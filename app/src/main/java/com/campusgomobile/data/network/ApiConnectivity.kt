package com.campusgomobile.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object ApiConnectivity {

    /**
     * Checks if the app can reach the API base URL.
     * Runs on IO dispatcher. Returns true if connection succeeds (any response), false on failure.
     */
    suspend fun checkReachable(baseUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(baseUrl)
            (url.openConnection() as? HttpURLConnection)?.apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                connect()
                disconnect()
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
