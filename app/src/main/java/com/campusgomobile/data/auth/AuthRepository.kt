package com.campusgomobile.data.auth

import com.campusgomobile.data.api.AuthApi
import com.campusgomobile.data.model.SignInRequest
import com.campusgomobile.data.model.SignUpRequest
import com.campusgomobile.data.model.User
import com.campusgomobile.data.network.NetworkModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val fieldErrors: Map<String, List<String>>? = null) : AuthResult<Nothing>()
    data class NetworkError(val cause: Throwable) : AuthResult<Nothing>()
}

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage,
    private val useDemoAuth: Boolean = false
) {

    val isLoggedIn: Flow<Boolean> = tokenStorage.token.map { it != null && it.isNotBlank() }
    val currentUserEmail: Flow<String?> = tokenStorage.userEmail

    suspend fun signIn(email: String, password: String): AuthResult<User> {
        if (useDemoAuth) {
            return demoSignIn(email)
        }
        return executeAuthCall {
            val response = authApi.signIn(SignInRequest(email = email, password = password))
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenStorage.saveToken(body.token, body.user.email)
                AuthResult.Success(body.user)
            } else {
                val error = parseError(response.code(), response.errorBody()?.string())
                if (isInvalidCredentials(response.code(), error)) {
                    AuthResult.Error(
                        "Wrong email or password",
                        mapOf(
                            "email" to listOf("Wrong email or password"),
                            "password" to listOf("Wrong email or password")
                        )
                    )
                } else {
                    error
                }
            }
        }
    }

    suspend fun signUp(
        studentNumber: String,
        firstName: String,
        lastName: String,
        course: String,
        yearLevel: Int,
        email: String,
        password: String
    ): AuthResult<User> {
        if (useDemoAuth) {
            return demoSignUp(firstName, lastName, email)
        }
        return executeAuthCall {
            val request = SignUpRequest(
                studentNumber = studentNumber,
                firstName = firstName,
                lastName = lastName,
                course = course,
                yearLevel = yearLevel,
                email = email,
                password = password
            )
            val response = authApi.signUp(request)
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenStorage.saveToken(body.token, body.user.email)
                AuthResult.Success(body.user)
            } else {
                parseError(response.code(), response.errorBody()?.string())
            }
        }
    }

    private suspend fun demoSignIn(email: String): AuthResult<User> {
        if (email.isBlank()) {
            return AuthResult.Error("Please enter your email")
        }
        val user = User(
            id = 1,
            name = "Demo User",
            email = email.trim(),
            role = "student",
            pointsBalance = 0,
            level = 1,
            totalXpEarned = 0,
            totalCompletedQuests = 0,
            profileImage = null
        )
        tokenStorage.saveToken("demo-token-${System.currentTimeMillis()}", user.email)
        return AuthResult.Success(user)
    }

    private suspend fun demoSignUp(firstName: String, lastName: String, email: String): AuthResult<User> {
        if (email.isBlank()) return AuthResult.Error("Please enter your email")
        if (firstName.isBlank() || lastName.isBlank()) return AuthResult.Error("Please enter your name")
        val name = listOf(firstName.trim(), lastName.trim()).joinToString(" ")
        val user = User(
            id = 1,
            name = name,
            email = email.trim(),
            role = "student",
            pointsBalance = 0,
            level = 1,
            totalXpEarned = 0,
            totalCompletedQuests = 0,
            profileImage = null
        )
        tokenStorage.saveToken("demo-token-${System.currentTimeMillis()}", user.email)
        return AuthResult.Success(user)
    }

    suspend fun signOut(): AuthResult<Unit> {
        return try {
            val token = tokenStorage.token.first()
            if (!token.isNullOrBlank()) {
                try {
                    authApi.signOut("Bearer $token")
                } catch (_: Exception) {
                    // Best-effort: clear local state even if server call fails
                }
            }
            tokenStorage.clear()
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            tokenStorage.clear()
            AuthResult.Success(Unit)
        }
    }

    private suspend fun <T> executeAuthCall(block: suspend () -> AuthResult<T>): AuthResult<T> {
        return try {
            block()
        } catch (e: IOException) {
            AuthResult.NetworkError(e)
        } catch (e: HttpException) {
            parseError(e.code(), e.response()?.errorBody()?.string())
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Unknown error")
        }
    }

    private fun isInvalidCredentials(code: Int, error: AuthResult.Error): Boolean {
        if (code == 401) return true
        val emailErrors = error.fieldErrors?.get("email") ?: return false
        return emailErrors.any { it.contains("credential", ignoreCase = true) || it.contains("incorrect", ignoreCase = true) }
    }

    private fun parseError(code: Int, body: String?): AuthResult.Error {
        val defaultMessage = when (code) {
            401 -> "Wrong email or password"
            else -> "Request failed ($code)"
        }
        val message = body?.let { parseMessage(it) } ?: defaultMessage
        val errors = body?.let { parseFieldErrors(it) }
        return AuthResult.Error(message, errors)
    }

    private fun parseMessage(json: String): String? {
        return try {
            val gson = com.google.gson.Gson()
            val obj = gson.fromJson(json, com.campusgomobile.data.model.ApiErrorBody::class.java)
            obj.message
        } catch (_: Exception) {
            null
        }
    }

    private fun parseFieldErrors(json: String): Map<String, List<String>>? {
        return try {
            val gson = com.google.gson.Gson()
            val obj = gson.fromJson(json, com.campusgomobile.data.model.ApiErrorBody::class.java)
            obj.errors
        } catch (_: Exception) {
            null
        }
    }
}
