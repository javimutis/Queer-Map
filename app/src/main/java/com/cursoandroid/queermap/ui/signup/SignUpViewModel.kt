package com.cursoandroid.queermap.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursoandroid.queermap.ui.forgotpassword.ForgotPasswordValidator.isValidEmail
import com.cursoandroid.queermap.ui.forgotpassword.ForgotPasswordValidator.isValidPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState

    private val _event = MutableSharedFlow<SignUpEvent>()
    val event = _event.asSharedFlow()

    fun onSignupClicked(email: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            if (!isValidEmail(email)) {
                _uiState.value = SignUpUiState(isEmailInvalid = true)
                return@launch
            }

            if (!isValidPassword(password)) {
                _uiState.value = SignUpUiState(isPasswordInvalid = true)
                return@launch
            }

            if (password != confirmPassword) {
                _uiState.value = SignUpUiState(doPasswordsMismatch = true)
                return@launch
            }

            // Simulación de registro exitoso
            _uiState.value = SignUpUiState(isLoading = true)
            kotlinx.coroutines.delay(1000)
            _uiState.value = SignUpUiState(isSuccess = true)
            _event.emit(SignUpEvent.NavigateToHome)
        }
    }

    fun onBackPressed() {
        viewModelScope.launch {
            _event.emit(SignUpEvent.NavigateBack)
        }
    }
}
