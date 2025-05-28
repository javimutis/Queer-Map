// ui/signup/SignUpViewModel.kt
package com.cursoandroid.queermap.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.usecase.auth.CreateUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val createUserUseCase: CreateUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState

    private val _event = MutableSharedFlow<SignUpEvent>()
    val event = _event.asSharedFlow()

    fun onEvent(event: SignUpEvent) {
        when (event) {
            is SignUpEvent.OnUserChanged -> _uiState.update { it.copy(user = event.user) }
            is SignUpEvent.OnEmailChanged -> _uiState.update { it.copy(email = event.email, isEmailInvalid = false) }
            is SignUpEvent.OnPasswordChanged -> _uiState.update { it.copy(password = event.password, isPasswordInvalid = false) }
            is SignUpEvent.OnConfirmPasswordChanged -> _uiState.update {
                it.copy(
                    confirmPassword = event.confirmPassword,
                    doPasswordsMismatch = false
                )
            }
            is SignUpEvent.OnFullNameChanged -> _uiState.update { it.copy(fullName = event.fullName) }
            is SignUpEvent.OnBirthdayChanged -> _uiState.update { it.copy(birthday = event.birthday) }
            SignUpEvent.OnRegisterClicked -> onSignupClicked()
            // Los eventos de navegación y ShowMessage son emitidos, no manejados por onEvent
            SignUpEvent.NavigateBack, SignUpEvent.NavigateToHome, is SignUpEvent.ShowMessage -> Unit
        }
    }

    private fun onSignupClicked() {
        viewModelScope.launch {
            val email = _uiState.value.email
            val password = _uiState.value.password ?: ""
            val confirmPassword = _uiState.value.confirmPassword ?: ""
            val fullName = _uiState.value.fullName ?: ""
            val user = _uiState.value.user ?: ""
            val birthday = _uiState.value.birthday ?: ""

            // Validaciones
            if (email.isNullOrBlank() || !SignUpValidator.isValidEmail(email)) {
                _uiState.update { it.copy(isEmailInvalid = true, errorMessage = "Please enter a valid email.") }
                return@launch
            }
            if (!SignUpValidator.isValidPassword(password)) {
                _uiState.update { it.copy(isPasswordInvalid = true, errorMessage = "Password must be at least 8 characters long.") }
                return@launch
            }
            if (password != confirmPassword) {
                _uiState.update { it.copy(doPasswordsMismatch = true, errorMessage = "Passwords do not match.") }
                return@launch
            }
            if (!SignUpValidator.isValidBirthday(birthday)) {
                _uiState.update { it.copy(isBirthdayInvalid = true, errorMessage = "Please enter a valid birthday.") }
                return@launch
            }
            if (!SignUpValidator.isValidUser(user)) {
                _event.emit(SignUpEvent.ShowMessage("Username cannot be empty."))
                return@launch
            }
            if (!SignUpValidator.isValidFullName(fullName)) {
                _event.emit(SignUpEvent.ShowMessage("Full name cannot be empty."))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val newUser = User(
                id = null,
                name = fullName,
                username = user,
                email = email,
                birthday = birthday
            )

            val result = createUserUseCase(newUser, password)
            result
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    _event.emit(SignUpEvent.NavigateToHome) // Emitir evento de navegación
                }
                .onFailure { exception ->
                    val errorMessage = exception.message ?: "Unknown registration error."
                    _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
                    _event.emit(SignUpEvent.ShowMessage(errorMessage)) // Emitir mensaje de error
                }
        }
    }

    fun onBackPressed() {
        viewModelScope.launch {
            _event.emit(SignUpEvent.NavigateBack) // Emitir evento de navegación
        }
    }
}