package com.cursoandroid.queermap.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursoandroid.queermap.common.EmailValidator.isValidEmail // Mantener esta importación si EmailValidator.kt tiene un 'object'
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithEmailUseCase
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithFacebookUseCase
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithGoogleUseCase
// import com.cursoandroid.queermap.ui.signup.SignUpValidator.isValidPassword // ELIMINAR esta importación
import com.cursoandroid.queermap.ui.signup.SignUpValidator // IMPORTAR la clase
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
    private val signUpValidator: SignUpValidator
) : ViewModel() {

    internal val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> =
        _uiState.asStateFlow()

    internal val _event = MutableSharedFlow<LoginEvent>()
    val event: SharedFlow<LoginEvent> =
        _event.asSharedFlow()

    fun loginWithEmail(email: String, password: String) {
        updateUiState(isLoading = true)
        viewModelScope.launch {
            if (!isValidEmail(email)) {
                updateEmailInvalid()
                return@launch
            }
            if (!signUpValidator.isValidPassword(password)) {
                updatePasswordInvalid()
                return@launch
            }
            handleEmailLogin(email, password)
        }
    }

    private fun updateEmailInvalid() {
        _uiState.value = LoginUiState(isEmailInvalid = true)
        sendEvent(LoginEvent.ShowMessage("Por favor ingresa un email válido"))
    }

    private fun updatePasswordInvalid() {
        _uiState.value = LoginUiState(isPasswordInvalid = true)
        sendEvent(LoginEvent.ShowMessage("La contraseña debe tener al menos 6 caracteres"))
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
                    val userProfileExistsResult =
                        authRepository.verifyUserInFirestore(currentUser.uid)
                    userProfileExistsResult.fold(
                        onSuccess = { exists ->
                            if (exists) {
                                _uiState.value = LoginUiState(isSuccess = true)
                                _event.emit(LoginEvent.NavigateToHome)
                            } else {
                                _uiState.value = LoginUiState(isSuccess = true)
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
                            val errorMessage =
                                error.message ?: "Error al verificar perfil de usuario."
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

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}