package com.composebasics.ui.forgot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.composebasics.data.repository.AuthRepository
import com.composebasics.ui.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val email: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val isLoading: Boolean = false,
    val success: Boolean = false,
)

class ForgotPasswordViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun onEmailChange(v: String) = _state.update { it.copy(email = v, emailError = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(newPassword = v, passwordError = null) }
    fun onConfirmChange(v: String) = _state.update { it.copy(confirmPassword = v, confirmError = null) }

    fun submit() {
        val s = _state.value
        if (s.isLoading) return
        val emailError = Validators.emailError(s.email)
        val passwordError = Validators.passwordError(s.newPassword)
        val confirmError = Validators.confirmPasswordError(s.newPassword, s.confirmPassword)
        if (emailError != null || passwordError != null || confirmError != null) {
            _state.update {
                it.copy(emailError = emailError, passwordError = passwordError, confirmError = confirmError)
            }
            return
        }
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val ok = auth.resetPassword(s.email, s.newPassword)
            _state.update {
                if (ok) it.copy(isLoading = false, success = true)
                else it.copy(isLoading = false, emailError = "No account found with this email")
            }
        }
    }
}
