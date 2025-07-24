package com.cursoandroid.queermap.ui.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursoandroid.queermap.domain.usecase.auth.SendResetPasswordUseCase
import com.cursoandroid.queermap.util.Result // IMPORTANT: Import your custom Result
import com.cursoandroid.queermap.util.exceptionOrNull // Import your custom exceptionOrNull
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val sendResetPasswordUseCase: SendResetPasswordUseCase,
    private val forgotPasswordValidator: ForgotPasswordValidator // Inyectar validador
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState

    private val _events = Channel<ForgotPasswordEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow() // Eventos de una sola vez (mensajes, navegación)

    fun sendPasswordReset(email: String) {
        // Validar email antes de proceder
        if (!forgotPasswordValidator.isValidEmail(email)) {
            viewModelScope.launch {
                _events.send(ForgotPasswordEvent.ShowMessage("Ingrese un correo válido"))
            }
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true) // Iniciar carga

        viewModelScope.launch {
            val result = sendResetPasswordUseCase(email)
            _uiState.value = _uiState.value.copy(isLoading = false) // Finalizar carga

            // Usa 'when' para manejar tu Result sellado
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isSuccess = true) // Actualizar estado de éxito
                    _events.send(ForgotPasswordEvent.ShowMessage("Se ha enviado un correo de restablecimiento de contraseña."))
                    _events.send(ForgotPasswordEvent.NavigateBack) // Navegar al terminar
                }
                is Result.Failure -> {
                    // Accede directamente a la propiedad 'exception' de tu clase Result.Failure
                    val errorMessage = when (val exception = result.exception) {
                        is FirebaseAuthInvalidUserException -> "No hay ninguna cuenta registrada con este correo electrónico."
                        is FirebaseAuthInvalidCredentialsException -> "El formato del correo electrónico es inválido."
                        else -> "Ocurrió un error inesperado. Intenta de nuevo más tarde."
                    }
                    _events.send(ForgotPasswordEvent.ShowMessage(errorMessage))
                    _uiState.value = _uiState.value.copy(isSuccess = false) // Asegurar que no está en éxito
                }
            }
        }
    }
}

// Clase sellada para eventos de una sola vez
sealed class ForgotPasswordEvent {
    object NavigateBack : ForgotPasswordEvent()
    data class ShowMessage(val message: String) : ForgotPasswordEvent()
}