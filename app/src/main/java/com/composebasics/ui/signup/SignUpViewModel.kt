package com.composebasics.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.composebasics.data.repository.AuthRepository
import com.composebasics.ui.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val isLoading: Boolean = false,
    val signedUp: Boolean = false,
)

class SignUpViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    fun onNameChange(v: String) = _state.update { it.copy(name = v, nameError = null) }
    fun onEmailChange(v: String) = _state.update { it.copy(email = v, emailError = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, passwordError = null) }
    fun onConfirmChange(v: String) = _state.update { it.copy(confirmPassword = v, confirmError = null) }

    fun submit() {
        val s = _state.value
        if (s.isLoading) return
        val nameError = Validators.nameError(s.name)
        val emailError = Validators.emailError(s.email)
        val passwordError = Validators.passwordError(s.password)
        val confirmError = Validators.confirmPasswordError(s.password, s.confirmPassword)
        if (listOf(nameError, emailError, passwordError, confirmError).any { it != null }) {
            _state.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmError = confirmError,
                )
            }
            return
        }
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val ok = auth.register(s.name, s.email, s.password)
            _state.update {
                if (ok) it.copy(isLoading = false, signedUp = true)
                else it.copy(isLoading = false, emailError = "An account with this email already exists")
            }
        }
    }
}
