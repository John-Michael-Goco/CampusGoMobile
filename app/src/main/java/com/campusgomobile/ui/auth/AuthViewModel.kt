package com.campusgomobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusgomobile.data.auth.AuthRepository
import com.campusgomobile.data.auth.AuthResult
import com.campusgomobile.data.model.AchievementsResponse
import com.campusgomobile.data.model.ActivityLogResponse
import com.campusgomobile.data.model.LeaderboardResponse
import com.campusgomobile.data.model.TransactionsResponse
import com.campusgomobile.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val fieldErrors: Map<String, List<String>>? = null,
    val isLoggedIn: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = authRepository.currentUser

    private val _leaderboardState = MutableStateFlow<LeaderboardUiState>(LeaderboardUiState())
    val leaderboardState: StateFlow<LeaderboardUiState> = _leaderboardState.asStateFlow()

    data class LeaderboardUiState(
        val data: LeaderboardResponse? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _achievementsState = MutableStateFlow<AchievementsUiState>(AchievementsUiState())
    val achievementsState: StateFlow<AchievementsUiState> = _achievementsState.asStateFlow()

    data class AchievementsUiState(
        val data: AchievementsResponse? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    )

    fun refreshAchievements(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _achievementsState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.getAchievements()) {
                is AuthResult.Success ->
                    _achievementsState.update {
                        it.copy(data = result.data, isLoading = false, error = null)
                    }
                is AuthResult.Error ->
                    _achievementsState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                is AuthResult.NetworkError ->
                    _achievementsState.update {
                        it.copy(isLoading = false, error = "Network error: ${result.cause.message}")
                    }
            }
        }
    }

    private val _activityState = MutableStateFlow<ActivityUiState>(ActivityUiState())
    val activityState: StateFlow<ActivityUiState> = _activityState.asStateFlow()

    data class ActivityUiState(
        val data: ActivityLogResponse? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    )

    fun refreshActivity(
        page: Int = 1,
        perPage: Int = 20,
        action: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        silent: Boolean = false
    ) {
        viewModelScope.launch {
            if (!silent) _activityState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.getActivity(page = page, perPage = perPage, action = action, dateFrom = dateFrom, dateTo = dateTo)) {
                is AuthResult.Success ->
                    _activityState.update {
                        it.copy(data = result.data, isLoading = false, error = null)
                    }
                is AuthResult.Error ->
                    _activityState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                is AuthResult.NetworkError ->
                    _activityState.update {
                        it.copy(isLoading = false, error = "Network error: ${result.cause.message}")
                    }
            }
        }
    }

    private val _transactionsState = MutableStateFlow<TransactionsUiState>(TransactionsUiState())
    val transactionsState: StateFlow<TransactionsUiState> = _transactionsState.asStateFlow()

    data class TransactionsUiState(
        val data: TransactionsResponse? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    )

    fun refreshTransactions(
        page: Int = 1,
        perPage: Int = 20,
        type: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        silent: Boolean = false
    ) {
        viewModelScope.launch {
            if (!silent) _transactionsState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.getTransactions(page = page, perPage = perPage, type = type, dateFrom = dateFrom, dateTo = dateTo)) {
                is AuthResult.Success ->
                    _transactionsState.update {
                        it.copy(data = result.data, isLoading = false, error = null)
                    }
                is AuthResult.Error ->
                    _transactionsState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                is AuthResult.NetworkError ->
                    _transactionsState.update {
                        it.copy(isLoading = false, error = "Network error: ${result.cause.message}")
                    }
            }
        }
    }

    fun refreshLeaderboard(period: String, silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _leaderboardState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.getLeaderboard(period)) {
                is AuthResult.Success ->
                    _leaderboardState.update {
                        it.copy(data = result.data, isLoading = false, error = null)
                    }
                is AuthResult.Error ->
                    _leaderboardState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                is AuthResult.NetworkError ->
                    _leaderboardState.update {
                        it.copy(isLoading = false, error = "Network error: ${result.cause.message}")
                    }
            }
        }
    }

    init {
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { loggedIn ->
                _uiState.update { it.copy(isLoggedIn = loggedIn) }
                if (loggedIn) refreshUser()
            }
        }
    }

    fun refreshUser() {
        viewModelScope.launch {
            when (authRepository.getUser()) {
                is AuthResult.Success -> { /* currentUser updated by repository */ }
                is AuthResult.Error -> { /* keep existing user if any */ }
                is AuthResult.NetworkError -> { /* keep existing user if any */ }
            }
        }
    }

    // Edit profile: password change & profile image
    data class EditProfileState(
        val passwordLoading: Boolean = false,
        val passwordError: String? = null,
        val passwordSuccess: Boolean = false,
        val profileImageLoading: Boolean = false,
        val profileImageError: String? = null,
        val profileImageSuccess: Boolean = false
    )
    private val _editProfileState = MutableStateFlow(EditProfileState())
    val editProfileState: StateFlow<EditProfileState> = _editProfileState.asStateFlow()

    fun changePassword(currentPassword: String, newPassword: String, confirmation: String) {
        viewModelScope.launch {
            _editProfileState.update {
                it.copy(passwordLoading = true, passwordError = null, passwordSuccess = false)
            }
            when (val result = authRepository.changePassword(currentPassword, newPassword, confirmation)) {
                is AuthResult.Success ->
                    _editProfileState.update {
                        it.copy(passwordLoading = false, passwordError = null, passwordSuccess = true)
                    }
                is AuthResult.Error ->
                    _editProfileState.update {
                        it.copy(passwordLoading = false, passwordError = result.message, passwordSuccess = false)
                    }
                is AuthResult.NetworkError ->
                    _editProfileState.update {
                        it.copy(
                            passwordLoading = false,
                            passwordError = "Network error: ${result.cause.message}",
                            passwordSuccess = false
                        )
                    }
            }
        }
    }

    fun updateProfileImage(imagePart: okhttp3.MultipartBody.Part?, remove: Boolean) {
        viewModelScope.launch {
            _editProfileState.update {
                it.copy(profileImageLoading = true, profileImageError = null, profileImageSuccess = false)
            }
            when (val result = authRepository.updateProfileImage(imagePart, remove)) {
                is AuthResult.Success -> {
                    _editProfileState.update {
                        it.copy(profileImageLoading = false, profileImageError = null, profileImageSuccess = true)
                    }
                }
                is AuthResult.Error ->
                    _editProfileState.update {
                        it.copy(profileImageLoading = false, profileImageError = result.message, profileImageSuccess = false)
                    }
                is AuthResult.NetworkError ->
                    _editProfileState.update {
                        it.copy(
                            profileImageLoading = false,
                            profileImageError = "Network error: ${result.cause.message}",
                            profileImageSuccess = false
                        )
                    }
            }
        }
    }

    fun clearEditProfileMessages() {
        _editProfileState.update {
            it.copy(
                passwordError = null,
                passwordSuccess = false,
                profileImageError = null,
                profileImageSuccess = false
            )
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, fieldErrors = null)
            }
            when (val result = authRepository.signIn(email, password)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = null, isLoggedIn = true)
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            fieldErrors = result.fieldErrors
                        )
                    }
                }
                is AuthResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Network error: ${result.cause.message}"
                        )
                    }
                }
            }
        }
    }

    fun signUp(
        studentNumber: String,
        firstName: String,
        lastName: String,
        course: String,
        yearLevel: Int,
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, fieldErrors = null)
            }
            when (val result = authRepository.signUp(
                studentNumber = studentNumber,
                firstName = firstName,
                lastName = lastName,
                course = course,
                yearLevel = yearLevel,
                email = email,
                password = password
            )) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = null, isLoggedIn = true)
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            fieldErrors = result.fieldErrors
                        )
                    }
                }
                is AuthResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Network error: ${result.cause.message}"
                        )
                    }
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.signOut()
            _uiState.update {
                it.copy(isLoading = false, isLoggedIn = false, errorMessage = null)
            }
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(errorMessage = null, fieldErrors = null)
        }
    }
}
