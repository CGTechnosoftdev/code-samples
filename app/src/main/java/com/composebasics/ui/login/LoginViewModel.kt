package com.composebasics.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.composebasics.data.repository.AuthRepository
import com.composebasics.ui.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val formError: String? = null,
    val isLoading: Boolean = false,
    val loggedIn: Boolean = false,
)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(v: String) = _state.update { it.copy(email = v, emailError = null, formError = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, passwordError = null, formError = null) }

    fun login() {
        val s = _state.value
        if (s.isLoading) return
        val emailError = Validators.emailError(s.email)
        val passwordError = if (s.password.isEmpty()) "Password is required" else null
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        _state.update { it.copy(isLoading = true, formError = null) }
        viewModelScope.launch {
            val ok = auth.login(s.email, s.password)
            _state.update {
                if (ok) it.copy(isLoading = false, loggedIn = true)
                else it.copy(isLoading = false, formError = "Invalid email or password")
            }
        }
    }
}
