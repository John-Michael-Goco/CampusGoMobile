package com.campusgomobile.data.auth

import com.campusgomobile.data.api.AuthApi
import com.campusgomobile.data.model.Achievement
import com.campusgomobile.data.model.AchievementsResponse
import com.campusgomobile.data.model.ActivityLogEntry
import com.campusgomobile.data.model.ActivityLogResponse
import com.campusgomobile.data.model.ActivityPagination
import com.campusgomobile.data.model.LeaderboardEntry
import com.campusgomobile.data.model.LeaderboardResponse
import com.campusgomobile.data.model.ChangePasswordRequest
import com.campusgomobile.data.model.PointTransaction
import com.campusgomobile.data.model.ProfileUpdateResponse
import com.campusgomobile.data.model.SignInRequest
import com.campusgomobile.data.model.SignUpRequest
import com.campusgomobile.data.model.StudentSearchResponse
import com.campusgomobile.data.model.TransferRequest
import com.campusgomobile.data.model.TransferResponse
import com.campusgomobile.data.model.TransferStudent
import com.campusgomobile.data.model.TransactionsPagination
import com.campusgomobile.data.model.TransactionsResponse
import com.campusgomobile.data.model.User
import com.campusgomobile.data.network.NetworkModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate

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

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

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
                _currentUser.value = body.user
                AuthResult.Success(body.user)
            } else {
                parseError(response.code(), response.errorBody()?.string())
            }
        }
    }

    suspend fun getUser(): AuthResult<User> {
        if (useDemoAuth) {
            val email = tokenStorage.userEmail.first() ?: ""
            when (val result = demoSignIn(email)) {
                is AuthResult.Success -> {
                    _currentUser.value = result.data
                    return result
                }
                else -> return result
            }
        }
        val token = tokenStorage.token.first() ?: return AuthResult.Error("Not signed in")
        if (token.isBlank()) return AuthResult.Error("Not signed in")
        return executeAuthCall {
            val client = NetworkModule.createAuthenticatedClient(token)
            val userApi = NetworkModule.createUserApi(client)
            val response = userApi.getUser()
            if (response.isSuccessful) {
                val user = response.body()!!
                _currentUser.value = user
                AuthResult.Success(user)
            } else {
                parseError(response.code(), response.errorBody()?.string())
            }
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String, confirmation: String): AuthResult<Unit> {
        if (useDemoAuth) {
            return AuthResult.Success(Unit)
        }
        val token = tokenStorage.token.first() ?: return AuthResult.Error("Not signed in")
        if (token.isBlank()) return AuthResult.Error("Not signed in")
        return executeAuthCall {
            val client = NetworkModule.createAuthenticatedClient(token)
            val userApi = NetworkModule.createUserApi(client)
            val request = ChangePasswordRequest(
                currentPassword = currentPassword,
                password = newPassword,
                passwordConfirmation = confirmation
            )
            val response = userApi.changePassword(request)
            if (response.isSuccessful) {
                AuthResult.Success(Unit)
            } else {
                parseError(response.code(), response.errorBody()?.string())
            }
        }
    }

    suspend fun updateProfileImage(imagePart: MultipartBody.Part?, remove: Boolean): AuthResult<User> {
        if (useDemoAuth) {
            val current = _currentUser.value
            if (current != null) return AuthResult.Success(current)
            val email = tokenStorage.userEmail.first() ?: ""
            val u = User(1, "Demo User", email, "student", 0, 1, 0, 0, null, null)
            _currentUser.value = u
            return AuthResult.Success(u)
        }
        val token = tokenStorage.token.first() ?: return AuthResult.Error("Not signed in")
        if (token.isBlank()) return AuthResult.Error("Not signed in")
        if (imagePart == null && !remove) return AuthResult.Error("No changes")
        return executeAuthCall {
            val client = NetworkModule.createAuthenticatedClient(token)
            val userApi = NetworkModule.createUserApi(client)
            val removeBody = if (remove) "1".toRequestBody("text/plain".toMediaTypeOrNull()) else null
            val response = userApi.updateProfile(profile_image = imagePart, removeProfileImage = removeBody)
            if (response.isSuccessful) {
                val body = response.body()
                val user = body?.user
                if (user != null) _currentUser.value = user
                AuthResult.Success(user ?: _currentUser.value!!)
            } else {
                parseError(response.code(), response.errorBody()?.string())
            }
        }
    }

    suspend fun getLeaderboard(period: String): AuthResult<LeaderboardResponse> {
        if (useDemoAuth) {
            return AuthResult.Success(demoLeaderboard(period))
        }
        val token = tokenStorage.token.first() ?: return AuthResult.Error("Not signed in")
        if (token.isBlank()) return AuthResult.Error("Not signed in")
        return executeAuthCall {
            val client = NetworkModule.createAuthenticatedClient(token)
            val api = NetworkModule.createLeaderboardApi(client)
            val response = api.getLeaderboard(period)
            if (response.isSuccessful) {
                AuthResult.Success(response.body()!!)
            } else {
                parseError(response.code(), response.errorBody()?.string())
            }
        }
    }

    private fun demoLeaderboard(period: String): LeaderboardResponse = LeaderboardResponse(
        entries = listOf(
            LeaderboardEntry(1, 1, "Top Player", 450),
            LeaderboardEntry(2, 2, "Second Best", 380),
            LeaderboardEntry(3, 3, "Third Place", 320),
            LeaderboardEntry(4, 4, "Demo User", 85)
        ),
        period = period,
        periods = listOf("today", "week", "month", "semester", "overall"),
        valueLabel = if (period == "overall") "Total XP earned" else "Points gained",
        myRank = 4,
        myValue = 85
    )

    suspend fun getAchievements(): AuthResult<AchievementsResponse> {
        if (useDemoAuth) {
            return AuthResult.Success(demoAchievements())
        }
        val token = tokenStorage.token.first() ?: return AuthResult.Error("Not signed in")
        if (token.isBlank()) return AuthResult.Error("Not signed in")
        return executeAuthCall {
            val client = NetworkModule.createAuthenticatedClient(token)
            val api = NetworkModule.createAchievementsApi(client)
            val response = api.getAchievements()
            if (response.isSuccessful) {
                AuthResult.Success(response.body()!!)
            } else {
                parseError(response.code(), response.errorBody()?.string())
            }
        }
    }

    private fun demoAchievements(): AchievementsResponse = AchievementsResponse(
        achievements = listOf(
            Achievement(1, "First Quest", "Complete your first quest.", "quest_count", 1, null, true, "2026-03-10 14:30:00"),
            Achievement(2, "Level 5", "Reach level 5.", "level", 5, null, false, null),
            Achievement(3, "Quest Master", "Complete 10 quests.", "quest_count", 10, null, false, null),
            Achievement(4, "Early Bird", "Complete a quest before 9 AM.", "complete_quest", 1, null, true, "2026-03-09 08:15:00")
        )
    )

    suspend fun getActivity(
        page: Int = 1,
        perPage: Int = 20,
        action: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null
    ): AuthResult<ActivityLogResponse> {
        if (useDemoAuth) {
            return AuthResult.Success(demoActivityLog(page, perPage, action, dateFrom, dateTo))
        }
        val token = tokenStorage.token.first() ?: return AuthResult.Error("Not signed in")
        if (token.isBlank()) return AuthResult.Error("Not signed in")
        return executeAuthCall {
            val client = NetworkModule.createAuthenticatedClient(token)
            val api = NetworkModule.createActivityApi(client)
            val response = api.getActivity(page = page, perPage = perPage, action = action, dateFrom = dateFrom, dateTo = dateTo)
            if (response.isSuccessful) {
                AuthResult.Success(response.body()!!)
            } else {
                parseError(response.code(), response.errorBody()?.string())
            }
        }
    }

    private fun demoActivityLog(
        page: Int,
        perPage: Int,
        action: String?,
        dateFrom: String?,
        dateTo: String?
    ): ActivityLogResponse {
        val all = listOf(
            ActivityLogEntry(1, "quest_joined", "Campus Tour Quest", "Joined quest", "2026-03-12 09:15:00"),
            ActivityLogEntry(2, "achievement_earned", "First Quest", "Earned achievement", "2026-03-11 16:45:00"),
            ActivityLogEntry(3, "store_redeem", "Free Coffee Coupon", "Redeemed item", "2026-03-11 14:20:00"),
            ActivityLogEntry(4, "quest_stage_submitted", "Campus Tour Quest - Stage 1", "Submitted stage", "2026-03-10 11:30:00"),
            ActivityLogEntry(5, "item_used", "Free Coffee Coupon", "Used item", "2026-03-10 10:00:00"),
            ActivityLogEntry(6, "auth_signin", null, "Signed in", "2026-03-10 08:00:00")
        )
        var filtered = if (action.isNullOrBlank()) all else all.filter { it.actionKey.startsWith(action) }
        if (!dateFrom.isNullOrBlank() || !dateTo.isNullOrBlank()) {
            val from = dateFrom?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val to = dateTo?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            filtered = filtered.filter { entry ->
                val entryDate = runCatching {
                    entry.timestamp.take(10).let { LocalDate.parse(it) }
                }.getOrNull() ?: return@filter true
                (from == null || !entryDate.isBefore(from)) && (to == null || !entryDate.isAfter(to))
            }
        }
        val start = (page - 1) * perPage
        val pageItems = filtered.drop(start).take(perPage)
        val total = filtered.size
        val lastPage = if (total == 0) 1 else (total + perPage - 1) / perPage
        return ActivityLogResponse(
            activity = pageItems,
            pagination = ActivityPagination(
                currentPage = page,
                perPage = perPage,
                total = total,
                lastPage = lastPage
            )
        )
    }

    suspend fun getTransactions(
        page: Int = 1,
        perPage: Int = 20,
        type: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null
    ): AuthResult<TransactionsResponse> {
        if (useDemoAuth) {
            return AuthResult.Success(demoTransactions(page, perPage, type, dateFrom, dateTo))
        }
        val token = tokenStorage.token.first() ?: return AuthResult.Error("Not signed in")
        if (token.isBlank()) return AuthResult.Error("Not signed in")
        return executeAuthCall {
            val client = NetworkModule.createAuthenticatedClient(token)
            val api = NetworkModule.createTransactionsApi(client)
            val response = api.getTransactions(page = page, perPage = perPage, type = type, dateFrom = dateFrom, dateTo = dateTo)
            if (response.isSuccessful) {
                AuthResult.Success(response.body()!!)
            } else {
                parseError(response.code(), response.errorBody()?.string())
            }
        }
    }

    suspend fun searchStudent(studentId: String): AuthResult<TransferStudent> {
        if (useDemoAuth) {
            val trimmed = studentId.trim()
            if (trimmed.isBlank()) return AuthResult.Error("Enter a student number")
            return AuthResult.Success(
                TransferStudent(
                    id = 5,
                    name = "Doe, John",
                    email = "john@example.com",
                    schoolId = trimmed,
                    firstName = "John",
                    lastName = "Doe",
                    course = "BSCS",
                    yearLevel = 2,
                    section = "2A",
                    pointsBalance = 80
                )
            )
        }
        val token = tokenStorage.token.first() ?: return AuthResult.Error("Not signed in")
        if (token.isBlank()) return AuthResult.Error("Not signed in")
        val trimmed = studentId.trim()
        if (trimmed.isBlank()) return AuthResult.Error("Enter a student number")
        return executeAuthCall {
            val client = NetworkModule.createAuthenticatedClient(token)
            val api = NetworkModule.createPointsTransferApi(client)
            val response = api.searchStudent(trimmed)
            when {
                response.isSuccessful -> response.body()?.student?.let { AuthResult.Success(it) }
                    ?: AuthResult.Error("Student not found")
                else -> parseError(response.code(), response.errorBody()?.string())
            }
        }
    }

    suspend fun transferPoints(toUserId: Int, amount: Int): AuthResult<TransferResponse> {
        if (useDemoAuth) {
            val user = _currentUser.value ?: return AuthResult.Error("Not signed in")
            val newBalance = (user.pointsBalance ?: 0) - amount
            _currentUser.value = user.copy(pointsBalance = newBalance.coerceAtLeast(0))
            return AuthResult.Success(
                TransferResponse(
                    message = "Points transferred successfully.",
                    pointsBalance = newBalance.coerceAtLeast(0),
                    transferred = com.campusgomobile.data.model.TransferredInfo(toUserId, "Doe, John", amount)
                )
            )
        }
        val token = tokenStorage.token.first() ?: return AuthResult.Error("Not signed in")
        if (token.isBlank()) return AuthResult.Error("Not signed in")
        return executeAuthCall {
            val client = NetworkModule.createAuthenticatedClient(token)
            val api = NetworkModule.createPointsTransferApi(client)
            val response = api.transfer(TransferRequest(toUserId = toUserId, amount = amount))
            when {
                response.isSuccessful -> {
                    val body = response.body()!!
                    _currentUser.value = _currentUser.value?.copy(pointsBalance = body.pointsBalance)
                    AuthResult.Success(body)
                }
                else -> parseError(response.code(), response.errorBody()?.string())
            }
        }
    }

    private fun demoTransactions(
        page: Int,
        perPage: Int,
        type: String?,
        dateFrom: String?,
        dateTo: String?
    ): TransactionsResponse {
        val all = listOf(
            PointTransaction(1, 50, "quest_reward", "Quest reward", null, "2026-03-12 09:15:00"),
            PointTransaction(2, -25, "store_redeem", "Store redeem", "3", "2026-03-11 14:20:00"),
            PointTransaction(3, 30, "quest_reward", "Quest reward", null, "2026-03-10 16:00:00"),
            PointTransaction(4, 10, "transfer_in", "Transfer in", "5", "2026-03-10 12:00:00"),
            PointTransaction(5, -10, "transfer_out", "Transfer out", "5", "2026-03-10 11:55:00"),
            PointTransaction(6, -5, "buy_in", "Quest buy-in", "1", "2026-03-09 10:00:00")
        )
        var filtered = if (type.isNullOrBlank()) all else all.filter { it.transactionType == type }
        if (!dateFrom.isNullOrBlank() || !dateTo.isNullOrBlank()) {
            val from = dateFrom?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val to = dateTo?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            filtered = filtered.filter { entry ->
                val entryDate = runCatching { LocalDate.parse(entry.createdAt.take(10)) }.getOrNull() ?: return@filter true
                (from == null || !entryDate.isBefore(from)) && (to == null || !entryDate.isAfter(to))
            }
        }
        val start = (page - 1) * perPage
        val pageItems = filtered.drop(start).take(perPage)
        val total = filtered.size
        val lastPage = if (total == 0) 1 else (total + perPage - 1) / perPage
        return TransactionsResponse(
            transactions = pageItems,
            pointsBalance = 150,
            pagination = TransactionsPagination(currentPage = page, perPage = perPage, total = total, lastPage = lastPage)
        )
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
            _currentUser.value = null
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            tokenStorage.clear()
            _currentUser.value = null
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
