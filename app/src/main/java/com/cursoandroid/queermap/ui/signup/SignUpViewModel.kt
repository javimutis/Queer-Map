package com.cursoandroid.queermap.ui.signup

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.usecase.auth.CreateUserUseCase
import com.cursoandroid.queermap.domain.usecase.auth.RegisterWithFacebookUseCase
import com.cursoandroid.queermap.domain.usecase.auth.RegisterWithGoogleUseCase
import com.facebook.CallbackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest // Keep this import for consistent Flow collection
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val RC_GOOGLE_SIGN_IN = 9001

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val createUserUseCase: CreateUserUseCase,
    private val registerWithGoogleUseCase: RegisterWithGoogleUseCase,
    private val registerWithFacebookUseCase: RegisterWithFacebookUseCase,
    private val googleSignInDataSource: GoogleSignInDataSource,
    private val facebookSignInDataSource: FacebookSignInDataSource,
    private val facebookCallbackManager: CallbackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState

    private val _event = MutableSharedFlow<SignUpEvent>()
    val event = _event.asSharedFlow()

    private val _launchGoogleSignIn = MutableSharedFlow<Intent>()
    val launchGoogleSignIn = _launchGoogleSignIn.asSharedFlow()

    init {
        facebookSignInDataSource.registerCallback(facebookCallbackManager)

        viewModelScope.launch {
            // Use collectLatest for Facebook access token channel to handle new emissions
            facebookSignInDataSource.accessTokenChannel.collectLatest { result: kotlin.Result<String> ->
                if (result.isSuccess) {
                    val accessToken = result.getOrThrow()
                    handleFacebookAuthWithFirebase(accessToken)
                } else {
                    val exception = result.exceptionOrNull()
                    _uiState.update { it.copy(isLoading = false) }
                    _event.emit(
                        SignUpEvent.ShowMessage(
                            exception?.message ?: "Inicio de sesión con Facebook fallido."
                        )
                    )
                }
            }
        }
    }

    fun onEvent(event: SignUpEvent) {
        when (event) {
            is SignUpEvent.OnUserChanged -> _uiState.update { it.copy(user = event.user) }
            is SignUpEvent.OnEmailChanged -> _uiState.update {
                it.copy(
                    email = event.email,
                    isEmailInvalid = false
                )
            }

            is SignUpEvent.OnPasswordChanged -> _uiState.update {
                it.copy(
                    password = event.password,
                    isPasswordInvalid = false
                )
            }

            is SignUpEvent.OnConfirmPasswordChanged -> _uiState.update {
                it.copy(
                    confirmPassword = event.confirmPassword,
                    doPasswordsMismatch = false
                )
            }

            is SignUpEvent.OnFullNameChanged -> _uiState.update { it.copy(fullName = event.fullName) }
            is SignUpEvent.OnBirthdayChanged -> _uiState.update { it.copy(birthday = event.birthday) }
            SignUpEvent.OnRegisterClicked -> onSignupClicked()

            SignUpEvent.OnGoogleSignUpClicked -> handleGoogleSignUpClicked()
            SignUpEvent.OnFacebookSignUpClicked -> handleFacebookSignUpClicked()
            is SignUpEvent.OnGoogleSignInResult -> handleGoogleSignInResult(event.data)
            is SignUpEvent.OnFacebookActivityResult -> facebookCallbackManager.onActivityResult(
                event.requestCode,
                event.resultCode,
                event.data
            )
            // SignUpEvent.NavigateBack, SignUpEvent.NavigateToHome, is SignUpEvent.ShowMessage are handled by sendEvent
            else -> Unit // Handle any other unhandled events if necessary
        }
    }

    // Move onBackPressed to be a top-level function in ViewModel
    fun onBackPressed() {
        viewModelScope.launch {
            _event.emit(SignUpEvent.NavigateBack)
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

            if (email.isNullOrBlank() || !SignUpValidator.isValidEmail(email)) {
                _uiState.update {
                    it.copy(
                        isEmailInvalid = true,
                        errorMessage = "Por favor, ingresa un email válido."
                    )
                }
                return@launch
            }
            if (!SignUpValidator.isValidPassword(password)) {
                _uiState.update {
                    it.copy(
                        isPasswordInvalid = true,
                        errorMessage = "La contraseña debe tener al menos 8 caracteres."
                    )
                }
                return@launch
            }
            if (password != confirmPassword) {
                _uiState.update {
                    it.copy(
                        doPasswordsMismatch = true,
                        errorMessage = "Las contraseñas no coinciden."
                    )
                }
                return@launch
            }
            if (!SignUpValidator.isValidBirthday(birthday)) {
                _uiState.update {
                    it.copy(
                        isBirthdayInvalid = true,
                        errorMessage = "Por favor, ingresa una fecha de nacimiento válida."
                    )
                }
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
                    _event.emit(SignUpEvent.NavigateToHome)
                }
                .onFailure { exception ->
                    val errorMessage = exception.message ?: "Error de registro desconocido."
                    _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
                    _event.emit(SignUpEvent.ShowMessage(errorMessage))
                }
        }
    }

    private fun handleGoogleSignUpClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val signInIntent = googleSignInDataSource.getSignInIntent()
            _launchGoogleSignIn.emit(signInIntent)
        }
    }

    private fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = googleSignInDataSource.handleSignInResult(data)
            result
                .onSuccess { idToken ->
                    registerWithGoogleUseCase(idToken)
                        .onSuccess {
                            _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                            _event.emit(SignUpEvent.NavigateToHome)
                        }
                        .onFailure { exception ->
                            val errorMessage =
                                exception.message ?: "Autenticación de Google con Firebase fallida."
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = errorMessage
                                )
                            }
                            _event.emit(SignUpEvent.ShowMessage(errorMessage))
                        }
                }
                .onFailure { exception ->
                    val errorMessage = exception.message ?: "Error de inicio de sesión de Google."
                    _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
                    _event.emit(SignUpEvent.ShowMessage(errorMessage))
                }
        }
    }

    private fun handleFacebookSignUpClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            _event.emit(SignUpEvent.ShowMessage("Iniciando sesión con Facebook..."))
            // The actual Facebook login is triggered by the Fragment, and the result is handled by the channel observer in init {}.
        }
    }

    private suspend fun handleFacebookAuthWithFirebase(accessToken: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        registerWithFacebookUseCase(accessToken)
            .onSuccess {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                _event.emit(SignUpEvent.NavigateToHome)
            }
            .onFailure { exception ->
                val errorMessage =
                    exception.message ?: "Autenticación de Facebook con Firebase fallida."
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
                _event.emit(SignUpEvent.ShowMessage(errorMessage))
            }
    }
}