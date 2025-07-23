package com.cursoandroid.queermap.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithEmailUseCase
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithFacebookUseCase
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithGoogleUseCase
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.common.InputValidator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    internal val loginWithEmailUseCase: LoginWithEmailUseCase,
    internal val loginWithFacebookUseCase: LoginWithFacebookUseCase,
    internal val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth,
    private val signUpValidator: InputValidator
) : ViewModel() {

    internal val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    internal val _event = MutableSharedFlow<LoginEvent>()
    val event: SharedFlow<LoginEvent> = _event.asSharedFlow()

    fun loginWithEmail(email: String, password: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isEmailInvalid = false,
            isPasswordInvalid = false,
            errorMessage = null,
            isSuccess = false
        )

        viewModelScope.launch {
            if (!signUpValidator.isValidEmail(email)) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isEmailInvalid = true
                )
                _event.emit(LoginEvent.ShowMessage("Por favor ingresa un email válido"))
                return@launch
            }
            if (!signUpValidator.isValidPassword(password)) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isPasswordInvalid = true
                )
                _event.emit(LoginEvent.ShowMessage("La contraseña debe tener al menos 6 caracteres"))
                return@launch
            }
            handleEmailLogin(email, password)
        }
    }

    private suspend fun handleEmailLogin(email: String, password: String) {
        val result = loginWithEmailUseCase(email, password)
        result.fold(
            onSuccess = {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                _event.emit(LoginEvent.NavigateToHome)
                _event.emit(LoginEvent.ShowMessage("Inicio de sesión exitoso"))
            },
            onFailure = { error ->
                val errorMessage = mapErrorToMessage(error)
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = errorMessage)
                _event.emit(LoginEvent.ShowMessage(errorMessage))
            }
        )
    }

    fun loginWithGoogle(idToken: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isEmailInvalid = false,
            isPasswordInvalid = false,
            errorMessage = null,
            isSuccess = false
        )
        performThirdPartyLogin { loginWithGoogleUseCase(idToken) }
    }

    fun loginWithFacebook(token: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isEmailInvalid = false,
            isPasswordInvalid = false,
            errorMessage = null,
            isSuccess = false
        )
        performThirdPartyLogin { loginWithFacebookUseCase(token) }
    }

    private fun performThirdPartyLogin(loginAction: suspend () -> Result<Any>) {
        viewModelScope.launch {
            val result = loginAction()
            handleThirdPartyResult(result)
        }
    }

    private suspend fun handleThirdPartyResult(result: Result<Any>) {
        result.fold(
            onSuccess = {
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    val userProfileExistsResult =
                        authRepository.verifyUserInFirestore(currentUser.uid)
                    userProfileExistsResult.fold(
                        onSuccess = { exists ->
                            if (exists) {
                                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                                _event.emit(LoginEvent.NavigateToHome)
                                _event.emit(LoginEvent.ShowMessage("Inicio de sesión social exitoso"))
                            } else {
                                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                                _event.emit(
                                    LoginEvent.NavigateToSignupWithArgs(
                                        socialUserEmail = currentUser.email,
                                        socialUserName = currentUser.displayName,
                                        isSocialLoginFlow = true
                                    )
                                )
                                _event.emit(LoginEvent.ShowMessage("Completa tu perfil para continuar"))
                            }
                        },
                        onFailure = { error ->
                            val errorMessage = error.message ?: "Error al verificar perfil de usuario."
                            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = errorMessage)
                            _event.emit(LoginEvent.ShowMessage(errorMessage))
                        }
                    )
                } else {
                    val errorMessage = "Error: Usuario autenticado nulo después del login social."
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = errorMessage)
                    _event.emit(LoginEvent.ShowMessage(errorMessage))
                }
            },
            onFailure = { error ->
                val errorMessage = error.message ?: "Error inesperado"
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = errorMessage)
                _event.emit(LoginEvent.ShowMessage(errorMessage))
            }
        )
    }

    fun onForgotPasswordClicked() {
        sendEvent(LoginEvent.NavigateToForgotPassword)
    }

    fun onBackPressed() {
        sendEvent(LoginEvent.NavigateBack)
    }

    private fun sendEvent(event: LoginEvent) {
        viewModelScope.launch { _event.emit(event) }
    }

    fun saveUserCredentials(email: String, password: String) {
        viewModelScope.launch {
            authRepository.saveCredentials(email, password)
        }
    }

    fun loadUserCredentials() {
        viewModelScope.launch {
            val (email, password) = authRepository.loadSavedCredentials()
            // Only update if credentials are not null or blank
            if (!email.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(email = email)
            }
            if (!password.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(password = password)
            }
        }
    }

    private fun mapErrorToMessage(error: Throwable): String {
        return when (error) {
            is IOException -> "Error de red. Por favor, revisa tu conexión"
            is FirebaseAuthInvalidCredentialsException -> "Credenciales inválidas. Email o contraseña incorrectos."
            is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con este email."
            else -> "Error inesperado. Intenta de nuevo más tarde"
        }
    }
}