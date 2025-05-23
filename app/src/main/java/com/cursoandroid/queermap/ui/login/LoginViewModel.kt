package com.cursoandroid.queermap.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithEmailUseCase
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithFacebookUseCase
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithGoogleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginWithEmailUseCase: LoginWithEmailUseCase,
    private val loginWithFacebookUseCase: LoginWithFacebookUseCase,
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    private val _event = MutableSharedFlow<LoginEvent>()
    val event = _event.asSharedFlow()

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            val result = loginWithEmailUseCase(email, password)
            result.fold(
                onSuccess = {
                    _uiState.value = LoginUiState(isSuccess = true)
                    _event.emit(LoginEvent.NavigateToHome)
                },
                onFailure = {
                    _uiState.value = LoginUiState(errorMessage = it.message)
                    _event.emit(LoginEvent.ShowMessage(it.message ?: "Error inesperado"))
                }
            )
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            val result = loginWithGoogleUseCase(idToken)
            result.fold(
                onSuccess = {
                    _uiState.value = LoginUiState(isSuccess = true)
                    _event.emit(LoginEvent.NavigateToHome)
                },
                onFailure = {
                    _uiState.value = LoginUiState(errorMessage = it.message)
                    _event.emit(LoginEvent.ShowMessage(it.message ?: "Error en login con Google"))
                }
            )
        }
    }

    fun loginWithFacebook(token: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            val result = loginWithFacebookUseCase(token)
            result.fold(
                onSuccess = {
                    _uiState.value = LoginUiState(isSuccess = true)
                    _event.emit(LoginEvent.NavigateToHome)
                },
                onFailure = {
                    _uiState.value = LoginUiState(errorMessage = it.message)
                    _event.emit(LoginEvent.ShowMessage(it.message ?: "Error en login con Facebook"))
                }
            )
        }
    }

    fun onForgotPasswordClicked() {
        viewModelScope.launch { _event.emit(LoginEvent.NavigateToForgotPassword) }
    }

    fun onBackPressed() {
        viewModelScope.launch { _event.emit(LoginEvent.NavigateBack) }
    }
    fun saveUserCredentials(email: String, password: String) {
        viewModelScope.launch {
            authRepository.saveCredentials(email, password)
        }
    }

    fun loadUserCredentials() {
        viewModelScope.launch {
            val (email, password) = authRepository.loadSavedCredentials()
            // Manejar las credenciales cargadas para actualizar UI o estados
        }
    }
}
