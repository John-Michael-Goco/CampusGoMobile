package com.campusgomobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusgomobile.data.auth.AuthRepository
import com.campusgomobile.data.auth.AuthResult
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

    init {
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { loggedIn ->
                _uiState.update { it.copy(isLoggedIn = loggedIn) }
            }
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
