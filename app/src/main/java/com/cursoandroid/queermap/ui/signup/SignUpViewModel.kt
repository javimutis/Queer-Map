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
            is SignUpEvent.OnUserChanged -> {
                _uiState.update { it.copy(user = event.user) }
            }
            is SignUpEvent.OnEmailChanged -> {
                _uiState.update { it.copy(email = event.email, isEmailInvalid = false) }
            }
            is SignUpEvent.OnPasswordChanged -> {
                _uiState.update { it.copy(password = event.password, isPasswordInvalid = false) }
            }
            is SignUpEvent.OnConfirmPasswordChanged -> {
                _uiState.update {
                    it.copy(
                        confirmPassword = event.confirmPassword,
                        doPasswordsMismatch = false
                    )
                }
            }
            is SignUpEvent.OnFullNameChanged -> {
                _uiState.update { it.copy(fullName = event.fullName) }
            }
            is SignUpEvent.OnBirthdayChanged -> {
                _uiState.update { it.copy(birthday = event.birthday) }
            }
            SignUpEvent.OnRegisterClicked -> {
                onSignupClicked()
            }
            SignUpEvent.NavigateBack -> { /* Handled in Fragment observeEvents */ }
            SignUpEvent.NavigateToHome -> { /* Handled in Fragment observeEvents */ }
            is SignUpEvent.ShowMessage -> { /* Handled in Fragment observeEvents */ }
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

            // **IMPORTANTE**: Validar que el email NO sea nulo y que sea válido
            if (email.isNullOrBlank() || !SignUpValidator.isValidEmail(email)) {
                _uiState.update { it.copy(isEmailInvalid = true, errorMessage = "Por favor, ingresa un email válido.") }
                return@launch
            }

            if (!SignUpValidator.isValidPassword(password)) {
                _uiState.update { it.copy(isPasswordInvalid = true, errorMessage = "La contraseña debe tener al menos 8 caracteres.") }
                return@launch
            }

            if (password != confirmPassword) {
                _uiState.update { it.copy(doPasswordsMismatch = true, errorMessage = "Las contraseñas no coinciden.") }
                return@launch
            }
            if (!SignUpValidator.isValidBirthday(birthday)) {
                _uiState.update { it.copy(isBirthdayInvalid = true, errorMessage = "Por favor, ingresa una fecha de nacimiento válida.") }
                return@launch
            }

            if (!SignUpValidator.isValidUser(user)) {
                _event.emit(SignUpEvent.ShowMessage("El nombre de usuario no puede estar vacío."))
                return@launch
            }

            if (!SignUpValidator.isValidFullName(fullName)) {
                _event.emit(SignUpEvent.ShowMessage("El nombre completo no puede estar vacío."))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Crear el objeto User para pasarlo al UseCase.
            // Asegúrate de que el email no sea nulo aquí, porque ya lo validaste.
            val newUser = User(
                id = null, // El ID se generará en el repositorio (Firebase UID)
                name = fullName,
                username = user,
                email = email, // Email ya validado y no nulo
                birthday = birthday
            )

            val result = createUserUseCase(newUser, password)
            result
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    _event.emit(SignUpEvent.NavigateToHome)
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = exception.message) }
                    _event.emit(SignUpEvent.ShowMessage(exception.message ?: "Error desconocido durante el registro."))
                }
        }
    }

    fun onBackPressed() {
        viewModelScope.launch {
            _event.emit(SignUpEvent.NavigateBack)
        }
    }
}