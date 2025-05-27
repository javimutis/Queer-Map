package com.cursoandroid.queermap.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursoandroid.queermap.ui.forgotpassword.ForgotPasswordValidator.isValidEmail
import com.cursoandroid.queermap.ui.forgotpassword.ForgotPasswordValidator.isValidPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState

    private val _event = MutableSharedFlow<SignUpEvent>()
    val event = _event.asSharedFlow()

    fun onEvent(event: SignUpEvent) {
        when (event) {
            is SignUpEvent.OnNameChanged -> {
                _uiState.update { it.copy(name = event.name) }
            }

            is SignUpEvent.OnEmailChanged -> {
                _uiState.update { it.copy(email = event.email, isEmailInvalid = false) }
            }

            is SignUpEvent.OnPasswordChanged -> {
                _uiState.update { it.copy(password = event.password, isPasswordInvalid = false) }
            }

            is SignUpEvent.OnConfirmPasswordChanged -> {
                _uiState.update { it.copy(confirmPassword = event.confirmPassword, doPasswordsMismatch = false) }
            }

            SignUpEvent.OnRegisterClicked -> {
                onSignupClicked()
            }

            SignUpEvent.NavigateBack -> TODO()
            SignUpEvent.NavigateToHome -> TODO()
            is SignUpEvent.ShowMessage -> TODO()
        }
    }

    private fun onSignupClicked() {
        viewModelScope.launch {
            val email = _uiState.value.email ?: ""
            val password = _uiState.value.password ?: ""
            val confirmPassword = _uiState.value.confirmPassword ?: ""

            if (!isValidEmail(email)) {
                _uiState.update { it.copy(isEmailInvalid = true) }
                return@launch
            }

            if (!isValidPassword(password)) {
                _uiState.update { it.copy(isPasswordInvalid = true) }
                return@launch
            }

            if (password != confirmPassword) {
                _uiState.update { it.copy(doPasswordsMismatch = true) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            delay(1000)
            _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            _event.emit(SignUpEvent.NavigateToHome)
        }
    }

    fun onBackPressed() {
        viewModelScope.launch {
            _event.emit(SignUpEvent.NavigateBack)

        }
    }
}
