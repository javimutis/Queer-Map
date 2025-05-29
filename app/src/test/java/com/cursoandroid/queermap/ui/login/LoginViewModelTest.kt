package com.cursoandroid.queermap.ui.login

import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithEmailUseCase
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithFacebookUseCase
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithGoogleUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

// Anotación para indicar que se utilizan APIs experimentales de Coroutines
@ExperimentalCoroutinesApi
// Clase de prueba para LoginViewModel
class LoginViewModelTest {

    // Mocks de las dependencias inyectadas en el ViewModel
    @MockK
    private lateinit var loginWithEmailUseCase: LoginWithEmailUseCase
    @MockK
    private lateinit var loginWithFacebookUseCase: LoginWithFacebookUseCase
    @MockK
    private lateinit var loginWithGoogleUseCase: LoginWithGoogleUseCase
    @MockK
    private lateinit var authRepository: AuthRepository
    @MockK
    private lateinit var firebaseAuth: FirebaseAuth
    @MockK
    private lateinit var firebaseUser: FirebaseUser // Mock para simular FirebaseAuth.currentUser

    // Instancia del ViewModel a probar
    private lateinit var viewModel: LoginViewModel

    // Dispatcher de prueba para controlar la ejecución de corrutinas
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // Configuración inicial antes de cada prueba
    @Before
    fun setUp() {
        MockKAnnotations.init(this) // Inicializa todos los mocks anotados
        // Establece el dispatcher principal de corrutinas para pruebas
        Dispatchers.setMain(testDispatcher)
        // Instancia el ViewModel con las dependencias mockeadas
        viewModel = LoginViewModel(
            loginWithEmailUseCase,
            loginWithFacebookUseCase,
            loginWithGoogleUseCase,
            authRepository,
            firebaseAuth
        )
    }

    // Limpieza después de cada prueba
    @After
    fun tearDown() {
        Dispatchers.resetMain() // Restablece el dispatcher principal
        clearAllMocks() // Limpia el estado de todos los mocks
    }

    // --- Tests para loginWithEmail ---

    // Prueba de login con email exitoso
    @Test
    fun `loginWithEmail emits success state and navigates to home on successful login`() = testScope.runTest {
        // Given: Define el escenario
        val email = "test@example.com"
        val password = "password123"
        val user = User("uid123", email, "testuser", "Test User", "01/01/2000")
        // Mockea la respuesta del UseCase de login con email
        coEvery { loginWithEmailUseCase(email, password) } returns Result.success(user)

        // Prepara para recolectar eventos del ViewModel *antes* de que ocurran
        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch { // Lanza una corrutina para recolectar eventos
            viewModel.event.toList(emittedEvents)
        }

        // When: Invoca la función a probar
        viewModel.loginWithEmail(email, password)
        advanceUntilIdle() // Avanza el dispatcher hasta completar todas las corrutinas

        // Then: Verifica el estado de UI y el evento emitido
        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isSuccess)
        assertEquals(false, uiState.isLoading)
        assertEquals(null, uiState.errorMessage)

        // Verifica que el evento esperado fue emitido
        assertTrue(emittedEvents.isNotEmpty())
        val emittedEvent = emittedEvents.first()
        assertTrue(emittedEvent is LoginEvent.NavigateToHome)

        job.cancel() // Cancela el job de recolección de eventos
    }

    // Prueba de login con email inválido
    @Test
    fun `loginWithEmail emits email invalid state if email is not valid`() = testScope.runTest {
        // Given: Define un email inválido
        val invalidEmail = "invalid-email"
        val password = "password123"

        // Prepara para recolectar eventos, aunque se espera que no haya
        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        // When: Invoca la función
        viewModel.loginWithEmail(invalidEmail, password)
        advanceUntilIdle()

        // Then: Verifica el estado de UI para email inválido
        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isEmailInvalid)
        assertEquals(false, uiState.isLoading)
        assertEquals(null, uiState.errorMessage)

        // Verifica que el UseCase no fue llamado
        coVerify(exactly = 0) { loginWithEmailUseCase(any(), any()) }

        // Verifica que NO se emitieron eventos de navegación
        assertTrue(emittedEvents.isEmpty())
        job.cancel()
    }

    // Prueba de login con contraseña inválida
    @Test
    fun `loginWithEmail emits password invalid state if password is too short`() = testScope.runTest {
        // Given: Define una contraseña corta
        val email = "test@example.com"
        val shortPassword = "short"

        // Prepara para recolectar eventos
        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        // When: Invoca la función
        viewModel.loginWithEmail(email, shortPassword)
        advanceUntilIdle()

        // Then: Verifica el estado de UI para contraseña inválida
        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isPasswordInvalid)
        assertEquals(false, uiState.isLoading)
        assertEquals(null, uiState.errorMessage)

        // Verifica que el UseCase no fue llamado
        coVerify(exactly = 0) { loginWithEmailUseCase(any(), any()) }
        assertTrue(emittedEvents.isEmpty())
        job.cancel()
    }

    // Prueba de login con error general
    @Test
    fun `loginWithEmail emits error message on failed login with general exception`() = testScope.runTest {
        // Given: Define una excepción general
        val email = "test@example.com"
        val password = "password123"
        val exception = Exception("Network error")
        // Mockea el UseCase para devolver un fallo
        coEvery { loginWithEmailUseCase(email, password) } returns Result.failure(exception)

        // Prepara para recolectar eventos
        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        // When: Invoca la función
        viewModel.loginWithEmail(email, password)
        advanceUntilIdle()

        // Then: Verifica el estado de UI con el mensaje de error
        val uiState = viewModel.uiState.first()
        assertEquals("Error inesperado. Intenta de nuevo más tarde", uiState.errorMessage)
        assertEquals(false, uiState.isSuccess)
        assertEquals(false, uiState.isLoading)

        // Verifica el evento de mostrar mensaje
        assertTrue(emittedEvents.isNotEmpty())
        val emittedEvent = emittedEvents.first()
        assertTrue(emittedEvent is LoginEvent.ShowMessage)
        assertEquals("Error inesperado. Intenta de nuevo más tarde", (emittedEvent as LoginEvent.ShowMessage).message)
        job.cancel()
    }

    // Prueba de login con credenciales inválidas de Firebase
    @Test
    fun `loginWithEmail emits specific error message for FirebaseAuthInvalidCredentialsException`() = testScope.runTest {
        // Given: Define una excepción específica de Firebase
        val email = "test@example.com"
        val password = "password123"

        // Mockear la excepción de Firebase en lugar de instanciarla directamente
        val mockedException = mockk<FirebaseAuthInvalidCredentialsException>()
        every { mockedException.errorCode } returns "auth/wrong-password" // Mockeamos propiedades si son necesarias
        every { mockedException.message } returns "Bad credentials" // Mockeamos el mensaje

        // Mockea el UseCase para devolver un fallo con la excepción específica mockeada
        coEvery { loginWithEmailUseCase(email, password) } returns Result.failure(mockedException)

        // Prepara para recolectar eventos
        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        // When: Invoca la función
        viewModel.loginWithEmail(email, password)
        advanceUntilIdle()

        // Then: Verifica el estado de UI con el mensaje de error específico
        val uiState = viewModel.uiState.first()
        assertEquals("Credenciales inválidas. Email o contraseña incorrectos.", uiState.errorMessage)

        // Verifica el evento de mostrar mensaje
        assertTrue(emittedEvents.isNotEmpty())
        val emittedEvent = emittedEvents.first()
        assertTrue(emittedEvent is LoginEvent.ShowMessage)
        assertEquals("Credenciales inválidas. Email o contraseña incorrectos.", (emittedEvent as LoginEvent.ShowMessage).message)
        job.cancel()
    }

    // --- Tests para loginWithGoogle (y login social en general) ---

    // Prueba de login con Google exitoso y usuario existente en Firestore
    @Test
    fun `loginWithGoogle navigates to mapFragment if user profile exists in Firestore`() = testScope.runTest {
        // Given: Define un token y un usuario de Firebase
        val idToken = "google_id_token"
        val userUid = "googleUid123"
        coEvery { loginWithGoogleUseCase(idToken) } returns Result.success(User(userUid, "email@example.com", null, null, null))
        // Mockea currentUser de FirebaseAuth
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns userUid
        // Mockea que el perfil del usuario ya existe en Firestore
        coEvery { authRepository.verifyUserInFirestore(userUid) } returns Result.success(true)

        // Prepara para recolectar eventos
        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        // When: Invoca la función
        viewModel.loginWithGoogle(idToken)
        advanceUntilIdle()

        // Then: Verifica el estado de UI y el evento de navegación a Home
        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isSuccess)
        assertEquals(false, uiState.isLoading)

        // Verifica el evento de navegación a Home
        assertTrue(emittedEvents.isNotEmpty())
        val emittedEvent = emittedEvents.first()
        assertTrue(emittedEvent is LoginEvent.NavigateToHome)
        job.cancel()
    }

    // Prueba de login con Google exitoso pero perfil no existente en Firestore
    @Test
    fun `loginWithGoogle navigates to signup if user profile does not exist in Firestore`() = testScope.runTest {
        // Given: Define un token, usuario de Firebase con email y display name
        val idToken = "google_id_token"
        val userUid = "googleUid456"
        val userEmail = "social@example.com"
        val userName = "Social User"
        coEvery { loginWithGoogleUseCase(idToken) } returns Result.success(User(userUid, userEmail, null, userName, null))
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns userUid
        every { firebaseUser.email } returns userEmail
        every { firebaseUser.displayName } returns userName
        // Mockea que el perfil del usuario NO existe en Firestore
        coEvery { authRepository.verifyUserInFirestore(userUid) } returns Result.success(false)

        // Recolecta todos los eventos emitidos (NavigateToSignupWithArgs y ShowMessage)
        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch { // Lanza una corrutina para recolectar eventos
            viewModel.event.toList(emittedEvents)
        }
        // When: Invoca la función
        viewModel.loginWithGoogle(idToken)
        advanceUntilIdle() // Asegura que el flujo de eventos se complete

        // Then: Verifica el estado de UI y los eventos de navegación y mensaje
        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isSuccess)
        assertEquals(false, uiState.isLoading)

        // Los eventos se recolectan en el orden de emisión
        assertTrue(emittedEvents.size >= 2) // Esperamos al menos 2 eventos: Navigate y ShowMessage
        val navigateEvent = emittedEvents.first()
        assertTrue(navigateEvent is LoginEvent.NavigateToSignupWithArgs)

        // *** ¡ESTE ES EL CAMBIO CLAVE EN EL TEST! ***
        // Ahora verificamos las propiedades directamente del evento LoginEvent.NavigateToSignupWithArgs
        assertEquals(userEmail, (navigateEvent as LoginEvent.NavigateToSignupWithArgs).socialUserEmail)
        assertEquals(userName, (navigateEvent as LoginEvent.NavigateToSignupWithArgs).socialUserName)
        assertEquals(true, (navigateEvent as LoginEvent.NavigateToSignupWithArgs).isSocialLoginFlow)

        val messageEvent = emittedEvents[1]
        assertTrue(messageEvent is LoginEvent.ShowMessage)
        assertEquals("Completa tu perfil para continuar", (messageEvent as LoginEvent.ShowMessage).message)

        job.cancel() // Cancela el job de recolección de eventos
    }


    // Prueba cuando el currentUser de Firebase es nulo después de login social exitoso
    @Test
    fun `loginWithGoogle emits error message if current user is null after successful social login`() = testScope.runTest {
        // Given: Mockea el UseCase de Google exitoso, pero currentUser nulo
        val idToken = "google_id_token"
        coEvery { loginWithGoogleUseCase(idToken) } returns Result.success(User("anyUid", "email@example.com", null, null, null))
        every { firebaseAuth.currentUser } returns null // Simula un currentUser nulo

        // Prepara para recolectar eventos
        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        // When: Invoca la función
        viewModel.loginWithGoogle(idToken)
        advanceUntilIdle()

        // Then: Verifica el estado de UI con el mensaje de error y el evento
        val uiState = viewModel.uiState.first()
        assertEquals("Error: Usuario autenticado nulo después del login social.", uiState.errorMessage)
        assertEquals(false, uiState.isLoading)

        // Verifica el evento de mostrar mensaje
        assertTrue(emittedEvents.isNotEmpty())
        val emittedEvent = emittedEvents.first()
        assertTrue(emittedEvent is LoginEvent.ShowMessage)
        assertEquals("Error: Usuario autenticado nulo después del login social.", (emittedEvent as LoginEvent.ShowMessage).message)
        job.cancel()
    }

    // Prueba de login social con fallo en el UseCase
    @Test
    fun `loginWithGoogle emits error message on social login failure`() = testScope.runTest {
        // Given: Define una excepción de fallo en el UseCase de Google
        val idToken = "google_id_token"
        val exception = Exception("Google login failed")
        coEvery { loginWithGoogleUseCase(idToken) } returns Result.failure(exception)

        // Prepara para recolectar eventos
        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        // When: Invoca la función
        viewModel.loginWithGoogle(idToken)
        advanceUntilIdle()

        // Then: Verifica el estado de UI con el mensaje de error y el evento
        val uiState = viewModel.uiState.first()
        assertEquals("Google login failed", uiState.errorMessage)
        assertEquals(false, uiState.isLoading)

        // Verifica el evento de mostrar mensaje
        assertTrue(emittedEvents.isNotEmpty())
        val emittedEvent = emittedEvents.first()
        assertTrue(emittedEvent is LoginEvent.ShowMessage)
        assertEquals("Google login failed", (emittedEvent as LoginEvent.ShowMessage).message)
        job.cancel()
    }

    // Prueba de login social con fallo al verificar usuario en Firestore
    @Test
    fun `loginWithGoogle emits error message if verifying user in Firestore fails`() = testScope.runTest {
        // Given: Mockea UseCase de Google exitoso, pero fallo en verificación de Firestore
        val idToken = "google_id_token"
        val userUid = "someUid"
        val verificationException = Exception("Firestore verification failed")
        coEvery { loginWithGoogleUseCase(idToken) } returns Result.success(User(userUid, "email@example.com", null, null, null))
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns userUid
        // Mockea que la verificación en Firestore falla
        coEvery { authRepository.verifyUserInFirestore(userUid) } returns Result.failure(verificationException)

        // Prepara para recolectar eventos
        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        // When: Invoca la función
        viewModel.loginWithGoogle(idToken)
        advanceUntilIdle()

        // Then: Verifica el estado de UI con el mensaje de error y el evento
        val uiState = viewModel.uiState.first()
        assertEquals("Firestore verification failed", uiState.errorMessage)
        assertEquals(false, uiState.isLoading)

        // Verifica el evento de mostrar mensaje
        assertTrue(emittedEvents.isNotEmpty())
        val emittedEvent = emittedEvents.first()
        assertTrue(emittedEvent is LoginEvent.ShowMessage)
        assertEquals("Firestore verification failed", (emittedEvent as LoginEvent.ShowMessage).message)
        job.cancel()
    }

    // --- Tests para loginWithFacebook ---
    // (Lógica idéntica a loginWithGoogle, solo cambian los mocks de UseCase y token)

    // Prueba de login con Facebook exitoso y usuario existente en Firestore
    @Test
    fun `loginWithFacebook navigates to home if user profile exists in Firestore`() = testScope.runTest {
        // Given: Define un token y un usuario de Firebase para Facebook
        val accessToken = "facebook_access_token"
        val userUid = "facebookUid123"
        coEvery { loginWithFacebookUseCase(accessToken) } returns Result.success(User(userUid, "fb_email@example.com", null, null, null))
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns userUid
        coEvery { authRepository.verifyUserInFirestore(userUid) } returns Result.success(true)

        // Prepara para recolectar eventos
        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        // When: Invoca la función
        viewModel.loginWithFacebook(accessToken)
        advanceUntilIdle()

        // Then: Verifica el estado de UI y el evento de navegación a Home
        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isSuccess)
        assertEquals(false, uiState.isLoading)

        // Verifica el evento de navegación a Home
        assertTrue(emittedEvents.isNotEmpty())
        val emittedEvent = emittedEvents.first()
        assertTrue(emittedEvent is LoginEvent.NavigateToHome)
        job.cancel()
    }

    // Prueba de login con Facebook exitoso pero perfil no existente en Firestore
    @Test
    fun `loginWithFacebook navigates to signup if user profile does not exist in Firestore`() = testScope.runTest {
        // Given: Define un token y usuario de Firebase con email y display name para Facebook
        val accessToken = "facebook_access_token"
        val userUid = "facebookUid456"
        val userEmail = "fb_social@example.com"
        val userName = "Facebook User"
        coEvery { loginWithFacebookUseCase(accessToken) } returns Result.success(User(userUid, userEmail, null, userName, null))
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns userUid
        every { firebaseUser.email } returns userEmail
        every { firebaseUser.displayName } returns userName
        coEvery { authRepository.verifyUserInFirestore(userUid) } returns Result.success(false)

        // Recolecta todos los eventos emitidos (NavigateToSignupWithArgs y ShowMessage)
        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        // When: Invoca la función
        viewModel.loginWithFacebook(accessToken)
        advanceUntilIdle()

        // Then: Verifica el estado de UI y los eventos de navegación y mensaje
        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isSuccess)
        assertEquals(false, uiState.isLoading)

        // Los eventos se recolectan en el orden de emisión
        assertTrue(emittedEvents.size >= 2) // Esperamos al menos 2 eventos: Navigate y ShowMessage
        val navigateEvent = emittedEvents.first()
        assertTrue(navigateEvent is LoginEvent.NavigateToSignupWithArgs)
        val directions = (navigateEvent as LoginEvent.NavigateToSignupWithArgs).directions
        assertEquals(userEmail, directions.arguments.getString("socialUserEmail"))
        assertEquals(userName, directions.arguments.getString("socialUserName"))
        assertEquals(true, directions.arguments.getBoolean("isSocialLoginFlow"))

        val messageEvent = emittedEvents[1]
        assertTrue(messageEvent is LoginEvent.ShowMessage)
        assertEquals("Completa tu perfil para continuar", (messageEvent as LoginEvent.ShowMessage).message)

        job.cancel()
    }

    // Prueba de login social de Facebook con fallo en el UseCase
    @Test
    fun `loginWithFacebook emits error message on social login failure`() = testScope.runTest {
        // Given: Define una excepción de fallo en el UseCase de Facebook
        val accessToken = "facebook_access_token"
        val exception = Exception("Facebook login failed")
        coEvery { loginWithFacebookUseCase(accessToken) } returns Result.failure(exception)

        // Prepara para recolectar eventos
        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        // When: Invoca la función
        viewModel.loginWithFacebook(accessToken)
        advanceUntilIdle()

        // Then: Verifica el estado de UI con el mensaje de error y el evento
        val uiState = viewModel.uiState.first()
        assertEquals("Facebook login failed", uiState.errorMessage)
        assertEquals(false, uiState.isLoading)

        // Verifica el evento de mostrar mensaje
        assertTrue(emittedEvents.isNotEmpty())
        val emittedEvent = emittedEvents.first()
        assertTrue(emittedEvent is LoginEvent.ShowMessage)
        assertEquals("Facebook login failed", (emittedEvent as LoginEvent.ShowMessage).message)
        job.cancel()
    }

    // --- Tests para onForgotPasswordClicked y onBackPressed ---

    // Prueba de navegación a la pantalla de "Olvidé mi contraseña"
    @Test
    fun `onForgotPasswordClicked emits NavigateToForgotPassword event`() = testScope.runTest {
        // Prepara para recolectar eventos
        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        // When: Invoca la función
        viewModel.onForgotPasswordClicked()
        advanceUntilIdle()

        // Then: Verifica que se emitió el evento de navegación correcto
        assertTrue(emittedEvents.isNotEmpty())
        val emittedEvent = emittedEvents.first()
        assertTrue(emittedEvent is LoginEvent.NavigateToForgotPassword)
        job.cancel()
    }

    // Prueba de navegación hacia atrás
    @Test
    fun `onBackPressed emits NavigateBack event`() = testScope.runTest {
        // Prepara para recolectar eventos
        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        // When: Invoca la función
        viewModel.onBackPressed()
        advanceUntilIdle()

        // Then: Verifica que se emitió el evento de navegación de retroceso
        assertTrue(emittedEvents.isNotEmpty())
        val emittedEvent = emittedEvents.first()
        assertTrue(emittedEvent is LoginEvent.NavigateBack)
        job.cancel()
    }

    // --- Tests para saveUserCredentials y loadUserCredentials ---

    // Prueba de guardado de credenciales
    @Test
    fun `saveUserCredentials calls authRepository saveCredentials`() = testScope.runTest {
        // Given: Define las credenciales
        val email = "save@example.com"
        val password = "savedPassword"
        // Mockea la llamada al repositorio, sin retorno
        coJustRun { authRepository.saveCredentials(email, password) }

        // When: Invoca la función
        viewModel.saveUserCredentials(email, password)
        advanceUntilIdle()

        // Then: Verifica que el método del repositorio fue llamado
        coVerify(exactly = 1) { authRepository.saveCredentials(email, password) }
    }

    // Prueba de carga de credenciales exitosa
    @Test
    fun `loadUserCredentials updates uiState with loaded credentials`() = testScope.runTest {
        // Given: Define las credenciales a cargar
        val savedEmail = "loaded@example.com"
        val savedPassword = "loadedPassword"
        // Mockea el repositorio para devolver las credenciales guardadas
        coEvery { authRepository.loadSavedCredentials() } returns (savedEmail to savedPassword)

        // When: Invoca la función
        viewModel.loadUserCredentials()
        advanceUntilIdle()

        // Then: Verifica que el uiState se actualizó con las credenciales
        val uiState = viewModel.uiState.first()
        assertEquals(savedEmail, uiState.email)
        assertEquals(savedPassword, uiState.password)
    }

    // Prueba de carga de credenciales con valores nulos o en blanco
    @Test
    fun `loadUserCredentials does not update uiState if credentials are null or blank`() = testScope.runTest {
        // Given: Mockea el repositorio para devolver credenciales nulas
        coEvery { authRepository.loadSavedCredentials() } returns (null to null)

        // When: Invoca la función
        viewModel.loadUserCredentials()
        advanceUntilIdle()

        // Then: Verifica que el uiState no se modificó (permanecen null por defecto)
        val uiState = viewModel.uiState.first()
        assertEquals(null, uiState.email)
        assertEquals(null, uiState.password)

        // Given: Mockea el repositorio para devolver credenciales en blanco
        coEvery { authRepository.loadSavedCredentials() } returns (" " to "")
        // When: Invoca la función de nuevo
        viewModel.loadUserCredentials()
        advanceUntilIdle()
        // Then: Verifica que el uiState sigue sin modificarse
        val uiStateBlank = viewModel.uiState.first()
        assertEquals(null, uiStateBlank.email)
        assertEquals(null, uiStateBlank.password)
    }
}