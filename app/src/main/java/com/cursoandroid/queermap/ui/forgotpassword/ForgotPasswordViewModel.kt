package com.cursoandroid.queermap.ui.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursoandroid.queermap.domain.usecase.SendResetPasswordUseCase
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val sendResetPasswordUseCase: SendResetPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.value = ForgotPasswordUiState(isLoading = true)
            val result = sendResetPasswordUseCase(email)
            _uiState.value = if (result.isSuccess) {
                ForgotPasswordUiState(isSuccess = true, message = "Correo enviado")
            } else {
                val exception = result.exceptionOrNull()
                val errorMessage = when (exception) {
                    is FirebaseAuthInvalidUserException -> "No hay ninguna cuenta con ese correo"
                    is FirebaseAuthInvalidCredentialsException -> "Correo inválido"
                    else -> exception?.localizedMessage ?: "Ocurrió un error"
                }
                ForgotPasswordUiState(message = errorMessage)
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
