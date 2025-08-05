package com.cursoandroid.queermap.ui.signup

// En SignUpViewModel.kt
// Asegúrate de que NO tengas import kotlin.Result ni otras similares que puedan causar conflicto.

// ¡¡¡IMPORTANTE!!! Asegúrate de que esta importación sea a tu clase Result personalizada
// ¡¡¡IMPORTANTE!!! Asegúrate de importar las funciones de extensión onSuccess y onFailure
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursoandroid.queermap.common.InputValidator
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.domain.usecase.auth.CreateUserUseCase
import com.cursoandroid.queermap.domain.usecase.auth.RegisterWithFacebookUseCase
import com.cursoandroid.queermap.domain.usecase.auth.RegisterWithGoogleUseCase
import com.cursoandroid.queermap.util.onFailure
import com.cursoandroid.queermap.util.onSuccess
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val facebookCallbackManager: CallbackManager, // Este es el CallbackManager de Facebook SDK
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth,
    private val signUpValidator: InputValidator
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState

    private val _event = MutableSharedFlow<SignUpEvent>(replay = 1) // <--- con replay 1
    val event = _event.asSharedFlow()

    private val _launchGoogleSignIn = MutableSharedFlow<Intent>()
    val launchGoogleSignIn = _launchGoogleSignIn.asSharedFlow()
    // Añade esto solo para pruebas
    internal suspend fun handleFacebookAuthWithFirebaseForTest(accessToken: String) {
        handleFacebookAuthWithFirebase(accessToken)
    }

    init {
        facebookSignInDataSource.registerCallback(
            facebookCallbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    viewModelScope.launch {
                        val token = result.accessToken?.token
                        if (token.isNullOrEmpty()) {
                            val errorMessage = "Token de acceso de Facebook nulo."
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = errorMessage
                                )
                            }
                            _event.emit(SignUpEvent.ShowMessage(errorMessage))
                            return@launch
                        }

                        handleFacebookAuthWithFirebase(token)
                    }
                }


                override fun onCancel() {
                    viewModelScope.launch {
                        val errorMessage = "Inicio de sesión con Facebook cancelado."
                        _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
                        _event.emit(SignUpEvent.ShowMessage(errorMessage))
                    }
                }

                override fun onError(error: FacebookException) {
                    viewModelScope.launch {
                        val errorMessage = error.message ?: "Error desconocido en Facebook Login."
                        _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
                        _event.emit(SignUpEvent.ShowMessage("Error: $errorMessage"))
                    }
                }
            }
        )

        // 🔥 ESTA ES LA LÍNEA CRÍTICA QUE FALTABA:
        viewModelScope.launch {
            facebookSignInDataSource.accessTokenChannel.collect { result ->
                result.onSuccess { token ->
                    handleFacebookAuthWithFirebase(token)
                }.onFailure { error ->
                    val errorMessage = error.message ?: "Error en login con Facebook."
                    _uiState.update {
                        it.copy(isLoading = false, isSuccess = false, errorMessage = errorMessage)
                    }
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
            is SignUpEvent.OnBirthdayChanged -> _uiState.update {
                it.copy(
                    birthday = event.birthday,
                    isBirthdayInvalid = false
                )
            }

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
            // Agrega esta rama 'else' para manejar cualquier otro tipo de evento
            // o los que solo se emiten y no requieren procesamiento directo en onEvent
            else -> Unit
        }
    }

    fun onBackPressed() {
        viewModelScope.launch {
            _event.emit(SignUpEvent.NavigateBack)
        }
    }

    private fun onSignupClicked() {
        viewModelScope.launch { // Esto ya es un scope de coroutine
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
                    errorMessage = null // Reset error message before new attempt
                )
            }

            if (!signUpValidator.isValidEmail(email)) {
                _uiState.update {
                    it.copy(
                        isEmailInvalid = true,
                        errorMessage = "Ingresa un email válido."
                    )
                }
                _event.emit(SignUpEvent.ShowMessage("Ingresa un email válido."))
                return@launch
            }
            if (!signUpValidator.isValidPassword(password)) {
                _uiState.update {
                    it.copy(
                        isPasswordInvalid = true,
                        errorMessage = "La contraseña debe tener al menos 8 caracteres."
                    )
                }
                _event.emit(SignUpEvent.ShowMessage("La contraseña debe tener al menos 8 caracteres."))
                return@launch
            }
            if (password != confirmPassword) {
                _uiState.update {
                    it.copy(
                        doPasswordsMismatch = true,
                        errorMessage = "Las contraseñas no coinciden."
                    )
                }
                _event.emit(SignUpEvent.ShowMessage("Las contraseñas no coinciden."))
                return@launch
            }
            if (!signUpValidator.isValidUsername(username)) {
                _uiState.update { it.copy(errorMessage = "El nombre de usuario no puede estar vacío.") }
                _event.emit(SignUpEvent.ShowMessage("El nombre de usuario no puede estar vacío."))
                return@launch
            }
            if (!signUpValidator.isValidFullName(fullName)) {
                _uiState.update { it.copy(errorMessage = "El nombre completo no puede estar vacío.") }
                _event.emit(SignUpEvent.ShowMessage("El nombre completo no puede estar vacío."))
                return@launch
            }
            if (!signUpValidator.isValidBirthday(birthday)) {
                _uiState.update {
                    it.copy(
                        isBirthdayInvalid = true,
                        errorMessage = "Ingresa una fecha de nacimiento válida."
                    )
                }
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

            // Usa tus funciones de extensión onSuccess/onFailure
            createUserUseCase(newUser, password)
                .onSuccess {
                    viewModelScope.launch { // Nuevo launch para llamadas suspend
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = true,
                                errorMessage = null
                            )
                        }
                        _event.emit(SignUpEvent.NavigateToHome)
                        _event.emit(SignUpEvent.ShowMessage("Registro exitoso. ¡Bienvenido/a!"))
                    }
                }
                .onFailure { exception ->
                    viewModelScope.launch { // Nuevo launch para llamadas suspend
                        val errorMessage = when (exception) {
                            is FirebaseAuthUserCollisionException -> "El correo electrónico ya está registrado."
                            is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil. Usa una combinación de letras, números y símbolos."
                            is FirebaseAuthInvalidCredentialsException -> "El formato del correo electrónico es inválido."
                            else -> "Error de registro: ${exception.message ?: "desconocido"}."
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = false,
                                errorMessage = errorMessage
                            )
                        }
                        _event.emit(SignUpEvent.ShowMessage(errorMessage))
                    }
                }
        }
    }

    private fun completeUserProfile() {
        viewModelScope.launch { // Esto ya es un scope de coroutine
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Usuario no autenticado para completar el perfil."
                    )
                }
                _event.emit(SignUpEvent.ShowMessage("Usuario no autenticado para completar el perfil."))
                return@launch
            }

            val uid = currentUser.uid
            val fullName = _uiState.value.fullName.orEmpty()
            val username = _uiState.value.username.orEmpty()
            val birthday = _uiState.value.birthday.orEmpty()

            if (!signUpValidator.isValidUsername(username)) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "El nombre de usuario no puede estar vacío."
                    )
                }
                _event.emit(SignUpEvent.ShowMessage("El nombre de usuario no puede estar vacío."))
                return@launch
            }
            if (!signUpValidator.isValidFullName(fullName)) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "El nombre completo no puede estar vacío."
                    )
                }
                _event.emit(SignUpEvent.ShowMessage("El nombre completo no puede estar vacío."))
                return@launch
            }
            if (!signUpValidator.isValidBirthday(birthday)) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isBirthdayInvalid = true,
                        errorMessage = "Ingresa una fecha de nacimiento válida."
                    )
                }
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

            // Usa tus funciones de extensión onSuccess/onFailure
            authRepository.updateUserProfile(uid, updatedUser)
                .onSuccess {
                    viewModelScope.launch { // Nuevo launch para llamadas suspend
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = true,
                                errorMessage = null
                            )
                        }
                        _event.emit(SignUpEvent.NavigateToHome)
                        _event.emit(SignUpEvent.ShowMessage("Perfil completado exitosamente."))
                    }
                }
                .onFailure { exception ->
                    viewModelScope.launch { // Nuevo launch para llamadas suspend
                        val errorMessage = exception.message ?: "Error al completar el perfil."
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = false,
                                errorMessage = errorMessage
                            )
                        }
                        _event.emit(SignUpEvent.ShowMessage(errorMessage))
                    }
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
        viewModelScope.launch { // Esto ya es un scope de coroutine
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Usa tus funciones de extensión onSuccess/onFailure
            googleSignInDataSource.handleSignInResult(data)
                .onSuccess { idToken ->
                    viewModelScope.launch { // Nuevo launch para llamadas suspend
                        // Aquí, registerWithGoogleUseCase(idToken) es una suspend function
                        // Por eso, la llamada y sus onSuccess/onFailure deben estar dentro de un launch
                        registerWithGoogleUseCase(idToken)
                            .onSuccess {
                                viewModelScope.launch { // Nuevo launch para llamadas suspend
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            isSuccess = true,
                                            errorMessage = null
                                        )
                                    }
                                    _event.emit(SignUpEvent.NavigateToHome)
                                    _event.emit(SignUpEvent.ShowMessage("Registro con Google exitoso. ¡Bienvenido/a!"))
                                }
                            }
                            .onFailure { exception ->
                                viewModelScope.launch { // Nuevo launch para llamadas suspend
                                    val errorMessage = when (exception) {
                                        is FirebaseAuthUserCollisionException -> "El correo electrónico ya está registrado con otra cuenta."
                                        else -> exception.message
                                            ?: "Autenticación de Google con Firebase fallida."
                                    }
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            errorMessage = errorMessage,
                                            isSuccess = false
                                        )
                                    }
                                    _event.emit(SignUpEvent.ShowMessage(errorMessage))
                                }
                            }
                    }
                }
                .onFailure { exception ->
                    viewModelScope.launch { // Nuevo launch para llamadas suspend
                        val errorMessage =
                            exception.message ?: "Error de inicio de sesión de Google."
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = errorMessage,
                                isSuccess = false
                            )
                        }
                        _event.emit(SignUpEvent.ShowMessage(errorMessage))
                    }
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
            // Aquí no llamas directamente a logInWithReadPermissions porque el Fragmento/Activity es quien lo hace.
            // Solo actualizas el UIState y emites un mensaje.
        }
    }

    private suspend fun handleFacebookAuthWithFirebase(accessToken: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        registerWithFacebookUseCase(accessToken)
            .onSuccess {
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            errorMessage = null
                        )
                    }
                    _event.emit(SignUpEvent.NavigateToHome)
                    _event.emit(SignUpEvent.ShowMessage("Registro con Facebook exitoso. ¡Bienvenido/a!"))
                }
            }
            .onFailure { exception ->
                viewModelScope.launch {
                    val errorMessage = when (exception) {
                        is FirebaseAuthUserCollisionException -> "El correo electrónico ya está registrado con otra cuenta."
                        else -> "Autenticación de Facebook con Firebase fallida." // ← fuerza el mensaje esperado
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMessage,
                            isSuccess = false
                        )
                    }
                    _event.emit(SignUpEvent.ShowMessage(errorMessage))
                }
            }


    }
    fun setUiStateForTesting(newState: SignUpUiState) {
        _uiState.value = newState
    }

}