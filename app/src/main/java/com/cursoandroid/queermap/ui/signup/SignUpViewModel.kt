package com.cursoandroid.queermap.ui.signup

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.domain.usecase.auth.CreateUserUseCase
import com.cursoandroid.queermap.domain.usecase.auth.RegisterWithFacebookUseCase
import com.cursoandroid.queermap.domain.usecase.auth.RegisterWithGoogleUseCase
import com.facebook.CallbackManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
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
    private val facebookCallbackManager: CallbackManager,
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth,
    private val signUpValidator: SignUpValidator
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
            facebookSignInDataSource.accessTokenChannel.collectLatest { result ->
                if (_uiState.value.isSocialLoginFlow) {
                    _uiState.update { it.copy(isLoading = false) }
                    _event.emit(SignUpEvent.ShowMessage("Ya autenticado socialmente. Completa tu perfil."))
                    return@collectLatest
                }

                if (result.isSuccess) {
                    handleFacebookAuthWithFirebase(result.getOrThrow())
                } else {
                    val exception = result.exceptionOrNull()
                    _uiState.update { it.copy(isLoading = false) }
                    val errorMessage = exception?.message ?: "Inicio de sesión con Facebook fallido."
                    _event.emit(SignUpEvent.ShowMessage(errorMessage))
                }
            }
        }
    }

    fun setSocialLoginData(isSocialLogin: Boolean, email: String?, name: String?) {
        _uiState.update { currentState ->
            currentState.copy(
                isSocialLoginFlow = isSocialLogin,
                email = email ?: currentState.email,
                fullName = name ?: currentState.fullName
            )
        }
    }

    fun onEvent(event: SignUpEvent) {
        when (event) {
            is SignUpEvent.OnUsernameChanged -> _uiState.update { it.copy(username = event.username) }
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
            is SignUpEvent.OnBirthdayChanged -> _uiState.update { it.copy(birthday = event.birthday, isBirthdayInvalid = false) }
            SignUpEvent.OnRegisterClicked -> {
                if (_uiState.value.isSocialLoginFlow) {
                    completeUserProfile()
                } else {
                    onSignupClicked()
                }
            }
            SignUpEvent.OnGoogleSignUpClicked -> handleGoogleSignUpClicked()
            SignUpEvent.OnFacebookSignUpClicked -> handleFacebookSignUpClicked()
            is SignUpEvent.OnGoogleSignInResult -> {
                if (!_uiState.value.isSocialLoginFlow) {
                    handleGoogleSignInResult(event.data)
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    viewModelScope.launch {
                        _event.emit(SignUpEvent.ShowMessage("Ya autenticado socialmente. Completa tu perfil."))
                    }
                }
            }
            is SignUpEvent.OnFacebookActivityResult -> {
                if (!_uiState.value.isSocialLoginFlow) {
                    facebookCallbackManager.onActivityResult(
                        event.requestCode,
                        event.resultCode,
                        event.data
                    )
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    viewModelScope.launch {
                        _event.emit(SignUpEvent.ShowMessage("Ya autenticado socialmente. Completa tu perfil."))
                    }
                }
            }
            // AÑADIDO: 'else' branch para hacer el 'when' exhaustivo
            else -> Unit
        }
    }

    fun onBackPressed() {
        viewModelScope.launch {
            _event.emit(SignUpEvent.NavigateBack)
        }
    }

    private fun onSignupClicked() {
        viewModelScope.launch {
            val email = _uiState.value.email.orEmpty()
            val password = _uiState.value.password.orEmpty()
            val confirmPassword = _uiState.value.confirmPassword.orEmpty()
            val fullName = _uiState.value.fullName.orEmpty()
            val username = _uiState.value.username.orEmpty()
            val birthday = _uiState.value.birthday.orEmpty()

            _uiState.update {
                it.copy(
                    isEmailInvalid = false,
                    isPasswordInvalid = false,
                    doPasswordsMismatch = false,
                    isBirthdayInvalid = false,
                    errorMessage = null
                )
            }

            if (!signUpValidator.isValidEmail(email)) {
                _uiState.update { it.copy(isEmailInvalid = true) }
                _event.emit(SignUpEvent.ShowMessage("Ingresa un email válido."))
                return@launch
            }
            if (!signUpValidator.isValidPassword(password)) {
                _uiState.update { it.copy(isPasswordInvalid = true) }
                _event.emit(SignUpEvent.ShowMessage("La contraseña debe tener al menos 8 caracteres."))
                return@launch
            }
            if (password != confirmPassword) {
                _uiState.update { it.copy(doPasswordsMismatch = true) }
                _event.emit(SignUpEvent.ShowMessage("Las contraseñas no coinciden."))
                return@launch
            }
            if (!signUpValidator.isValidUsername(username)) {
                _event.emit(SignUpEvent.ShowMessage("El nombre de usuario no puede estar vacío."))
                return@launch
            }
            if (!signUpValidator.isValidFullName(fullName)) {
                _event.emit(SignUpEvent.ShowMessage("El nombre completo no puede estar vacío."))
                return@launch
            }
            if (!signUpValidator.isValidBirthday(birthday)) {
                _uiState.update { it.copy(isBirthdayInvalid = true) }
                _event.emit(SignUpEvent.ShowMessage("Ingresa una fecha de nacimiento válida."))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            val newUser = User(
                id = null,
                name = fullName,
                username = username,
                email = email,
                birthday = birthday
            )

            createUserUseCase(newUser, password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    _event.emit(SignUpEvent.NavigateToHome)
                    _event.emit(SignUpEvent.ShowMessage("Registro exitoso. ¡Bienvenido/a!"))
                }
                .onFailure { exception ->
                    val errorMessage = when (exception) {
                        is FirebaseAuthUserCollisionException -> "El correo electrónico ya está registrado."
                        is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil. Usa una combinación de letras, números y símbolos."
                        is FirebaseAuthInvalidCredentialsException -> "El formato del correo electrónico es inválido."
                        else -> "Error de registro: ${exception.message ?: "desconocido"}."
                    }
                    _uiState.update { it.copy(isLoading = false, isSuccess = false) }
                    _event.emit(SignUpEvent.ShowMessage(errorMessage))
                }
        }
    }

    private fun completeUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                _uiState.update { it.copy(isLoading = false) }
                _event.emit(SignUpEvent.ShowMessage("Usuario no autenticado para completar el perfil."))
                return@launch
            }

            val uid = currentUser.uid
            val fullName = _uiState.value.fullName.orEmpty()
            val username = _uiState.value.username.orEmpty()
            val birthday = _uiState.value.birthday.orEmpty()

            if (!signUpValidator.isValidUsername(username)) {
                _uiState.update { it.copy(isLoading = false) }
                _event.emit(SignUpEvent.ShowMessage("El nombre de usuario no puede estar vacío."))
                return@launch
            }
            if (!signUpValidator.isValidFullName(fullName)) {
                _uiState.update { it.copy(isLoading = false) }
                _event.emit(SignUpEvent.ShowMessage("El nombre completo no puede estar vacío."))
                return@launch
            }
            if (!signUpValidator.isValidBirthday(birthday)) {
                _uiState.update { it.copy(isLoading = false, isBirthdayInvalid = true) }
                _event.emit(SignUpEvent.ShowMessage("Ingresa una fecha de nacimiento válida."))
                return@launch
            }

            val updatedUser = User(
                id = uid,
                name = fullName,
                username = username,
                email = currentUser.email,
                birthday = birthday
            )

            authRepository.updateUserProfile(uid, updatedUser)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    _event.emit(SignUpEvent.NavigateToHome)
                    _event.emit(SignUpEvent.ShowMessage("Perfil completado exitosamente."))
                }
                .onFailure { exception ->
                    val errorMessage = exception.message ?: "Error al completar el perfil."
                    _uiState.update { it.copy(isLoading = false, isSuccess = false) }
                    _event.emit(SignUpEvent.ShowMessage(errorMessage))
                }
        }
    }

    private fun handleGoogleSignUpClicked() {
        viewModelScope.launch {
            if (_uiState.value.isSocialLoginFlow) {
                _event.emit(SignUpEvent.ShowMessage("Ya autenticado socialmente. Completa tu perfil."))
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val signInIntent = googleSignInDataSource.getSignInIntent()
            _launchGoogleSignIn.emit(signInIntent)
        }
    }

    private fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            googleSignInDataSource.handleSignInResult(data)
                .onSuccess { idToken ->
                    registerWithGoogleUseCase(idToken)
                        .onSuccess {
                            _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                            _event.emit(SignUpEvent.NavigateToHome)
                            _event.emit(SignUpEvent.ShowMessage("Registro con Google exitoso. ¡Bienvenido/a!"))
                        }
                        .onFailure { exception ->
                            val errorMessage = when (exception) {
                                is FirebaseAuthUserCollisionException -> "El correo electrónico ya está registrado con otra cuenta."
                                else -> exception.message ?: "Autenticación de Google con Firebase fallida."
                            }
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
            if (_uiState.value.isSocialLoginFlow) {
                _event.emit(SignUpEvent.ShowMessage("Ya autenticado socialmente. Completa tu perfil."))
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            _event.emit(SignUpEvent.ShowMessage("Iniciando sesión con Facebook..."))
        }
    }

    private suspend fun handleFacebookAuthWithFirebase(accessToken: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        registerWithFacebookUseCase(accessToken)
            .onSuccess {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                _event.emit(SignUpEvent.NavigateToHome)
                _event.emit(SignUpEvent.ShowMessage("Registro con Facebook exitoso. ¡Bienvenido/a!"))
            }
            .onFailure { exception ->
                val errorMessage = when (exception) {
                    is FirebaseAuthUserCollisionException -> "El correo electrónico ya está registrado con otra cuenta."
                    else -> exception.message ?: "Autenticación de Facebook con Firebase fallida."
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
                _event.emit(SignUpEvent.ShowMessage(errorMessage))
            }
    }
}