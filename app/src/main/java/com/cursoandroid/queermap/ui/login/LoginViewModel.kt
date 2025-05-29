package com.cursoandroid.queermap.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithEmailUseCase
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithFacebookUseCase
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithGoogleUseCase
import com.cursoandroid.queermap.ui.forgotpassword.ForgotPasswordValidator.isValidEmail
import com.cursoandroid.queermap.ui.forgotpassword.ForgotPasswordValidator.isValidPassword
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginWithEmailUseCase: LoginWithEmailUseCase,
    private val loginWithFacebookUseCase: LoginWithFacebookUseCase,
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    private val _event = MutableSharedFlow<LoginEvent>()
    val event = _event.asSharedFlow()

    fun loginWithEmail(email: String, password: String) {
        updateUiState(isLoading = true)
        viewModelScope.launch {
            if (!isValidEmail(email)) {
                updateEmailInvalid()
                return@launch
            }
            if (!isValidPassword(password)) {
                updatePasswordInvalid()
                return@launch
            }
            handleEmailLogin(email, password)
        }
    }

    private fun updateEmailInvalid() {
        _uiState.value = LoginUiState(isEmailInvalid = true)
    }

    private fun updatePasswordInvalid() {
        _uiState.value = LoginUiState(isPasswordInvalid = true)
    }

    private suspend fun handleEmailLogin(email: String, password: String) {
        val result = loginWithEmailUseCase(email, password)
        result.fold(
            onSuccess = {
                _uiState.value = LoginUiState(isSuccess = true)
                _event.emit(LoginEvent.NavigateToHome)
            },
            onFailure = { error ->
                val errorMessage = mapErrorToMessage(error)
                _uiState.value = LoginUiState(errorMessage = errorMessage)
                _event.emit(LoginEvent.ShowMessage(errorMessage))
            }
        )
    }

    fun loginWithGoogle(idToken: String) {
        performThirdPartyLogin { loginWithGoogleUseCase(idToken) }
    }

    fun loginWithFacebook(token: String) {
        performThirdPartyLogin { loginWithFacebookUseCase(token) }
    }

    private fun performThirdPartyLogin(loginAction: suspend () -> Result<Any>) {
        updateUiState(isLoading = true)
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
                    val userProfileExistsResult = authRepository.verifyUserInFirestore(currentUser.uid)
                    userProfileExistsResult.fold(
                        onSuccess = { exists ->
                            if (exists) {
                                // El perfil ya existe en Firestore, el usuario está completamente registrado en la app
                                _uiState.value = LoginUiState(isSuccess = true)
                                _event.emit(LoginEvent.NavigateToHome)
                            } else {
                                // El usuario se autenticó con Firebase, pero NO tiene perfil en Firestore
                                // Debe ir a la pantalla de registro de perfil
                                _uiState.value = LoginUiState(isSuccess = true)

                                // *** ¡ESTE ES EL CAMBIO CLAVE EN EL VIEWMODEL! ***
                                // Ahora emitimos los argumentos directamente en el evento,
                                // SIN crear un objeto NavDirections aquí.
                                _event.emit(LoginEvent.NavigateToSignupWithArgs(
                                    socialUserEmail = currentUser.email,
                                    socialUserName = currentUser.displayName,
                                    isSocialLoginFlow = true
                                ))
                                _event.emit(LoginEvent.ShowMessage("Completa tu perfil para continuar"))
                            }
                        },
                        onFailure = { error ->
                            val errorMessage = error.message ?: "Error al verificar perfil de usuario."
                            _uiState.value = LoginUiState(errorMessage = errorMessage)
                            _event.emit(LoginEvent.ShowMessage(errorMessage))
                        }
                    )
                } else {
                    val errorMessage = "Error: Usuario autenticado nulo después del login social."
                    _uiState.value = LoginUiState(errorMessage = errorMessage)
                    _event.emit(LoginEvent.ShowMessage(errorMessage))
                }
            },
            onFailure = {
                val errorMessage = it.message ?: "Error inesperado"
                _uiState.value = LoginUiState(errorMessage = errorMessage)
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
            updateCredentials(email, password)
        }
    }

    private fun updateCredentials(email: String?, password: String?) {
        if (!email.isNullOrBlank() && !password.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(email = email, password = password)
        }
    }

    private fun updateUiState(isLoading: Boolean = false) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading)
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