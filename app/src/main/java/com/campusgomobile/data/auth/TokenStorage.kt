package com.campusgomobile.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

class TokenStorage(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USER_EMAIL = stringPreferencesKey("user_email")
    }

    val token: Flow<String?> = context.authDataStore.data.map { prefs ->
        prefs[Keys.TOKEN]
    }

    val userEmail: Flow<String?> = context.authDataStore.data.map { prefs ->
        prefs[Keys.USER_EMAIL]
    }

    suspend fun saveToken(token: String, email: String?) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            email?.let { prefs[Keys.USER_EMAIL] = it }
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
    }
}
