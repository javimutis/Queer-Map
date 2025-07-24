package com.cursoandroid.queermap.ui.login

// IMPORTANTE: Asegúrate de que TODAS estas importaciones estén presentes y sean correctas.
// Estas son las importaciones clave para tu clase Result personalizada y sus extensiones.
import com.cursoandroid.queermap.common.InputValidator
import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithEmailUseCase
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithFacebookUseCase
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithGoogleUseCase
import com.cursoandroid.queermap.util.failure
import com.cursoandroid.queermap.util.success
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException


@ExperimentalCoroutinesApi
class LoginViewModelTest {

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
    private lateinit var firebaseUser: FirebaseUser

    @MockK
    private lateinit var signUpValidator: InputValidator

    private lateinit var viewModel: LoginViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        // AÑADIDO: Mockear los métodos de SignUpValidator para que siempre retornen true por defecto
        // Esto es importante porque LoginViewModel usa isValidPassword.
        // Si tuvieras más validaciones, las mockearías aquí también.
        every { signUpValidator.isValidEmail(any()) } returns true
        every { signUpValidator.isValidPassword(any()) } returns true
        every { signUpValidator.isValidUsername(any()) } returns true
        every { signUpValidator.isValidFullName(any()) } returns true
        every { signUpValidator.isValidBirthday(any()) } returns true


        viewModel = LoginViewModel(
            loginWithEmailUseCase,
            loginWithFacebookUseCase,
            loginWithGoogleUseCase,
            authRepository,
            firebaseAuth,
            signUpValidator
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    // --- Tests para loginWithEmail ---

    @Test
    fun `loginWithEmail emits success state and navigates to home on successful login`() =
        testScope.runTest {
            val email = "test@example.com"
            val password = "password123"
            val user = User("uid123", email, "testuser", "Test User", "01/01/2000")
            // Usando tu función helper 'success'
            coEvery { loginWithEmailUseCase(email, password) } returns success(user)

            val emittedEvents = mutableListOf<LoginEvent>()
            val job = launch {
                viewModel.event.toList(emittedEvents)
            }

            viewModel.loginWithEmail(email, password)
            advanceUntilIdle()


            val uiState = viewModel.uiState.first()
            assertTrue(uiState.isSuccess)
            assertEquals(false, uiState.isLoading)
            assertEquals(null, uiState.errorMessage)

            assertTrue(emittedEvents.isNotEmpty())
            val emittedEvent = emittedEvents.first()
            assertTrue(emittedEvent is LoginEvent.NavigateToHome)

            job.cancel()
        }

    @Test
    fun `loginWithEmail emits email invalid state if email is not valid`() = testScope.runTest {
        val invalidEmail = "invalid-email"
        val password = "password123"
        // Mockear que isValidEmail retorna false para este test específico
        every { signUpValidator.isValidEmail(invalidEmail) } returns false


        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        viewModel.loginWithEmail(invalidEmail, password)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isEmailInvalid)
        assertEquals(false, uiState.isLoading)
        assertEquals(null, uiState.errorMessage)

        coVerify(exactly = 0) { loginWithEmailUseCase(any(), any()) }

        assertTrue(emittedEvents.isNotEmpty())
        val emittedEvent = emittedEvents.first()
        assertTrue(emittedEvent is LoginEvent.ShowMessage)
        assertEquals(
            "Por favor ingresa un email válido",
            (emittedEvent as LoginEvent.ShowMessage).message
        )
        job.cancel()
    }

    @Test
    fun `loginWithEmail emits password invalid state if password is too short`() =
        testScope.runTest {
            val email = "test@example.com"
            val shortPassword = "short"
            // Mockear que isValidPassword retorna false para este test específico
            every { signUpValidator.isValidPassword(shortPassword) } returns false


            val emittedEvents = mutableListOf<LoginEvent>()
            val job = launch {
                viewModel.event.toList(emittedEvents)
            }

            viewModel.loginWithEmail(email, shortPassword)
            advanceUntilIdle()

            val uiState = viewModel.uiState.first()
            assertTrue(uiState.isPasswordInvalid)
            assertEquals(false, uiState.isLoading)
            assertEquals(null, uiState.errorMessage)

            coVerify(exactly = 0) { loginWithEmailUseCase(any(), any()) }
            assertTrue(emittedEvents.isNotEmpty())
            val emittedEvent = emittedEvents.first()
            assertTrue(emittedEvent is LoginEvent.ShowMessage)
            assertEquals(
                "La contraseña debe tener al menos 6 caracteres",
                (emittedEvent as LoginEvent.ShowMessage).message
            )
            job.cancel()
        }

    @Test
    fun `loginWithEmail emits error message on failed login with general exception`() =
        testScope.runTest {
            val email = "test@example.com"
            val password = "password123"
            val exception = Exception("Network error")
            // Usando tu función helper 'failure'
            coEvery { loginWithEmailUseCase(email, password) } returns failure(exception)

            val emittedEvents = mutableListOf<LoginEvent>()
            val job = launch {
                viewModel.event.toList(emittedEvents)
            }

            viewModel.loginWithEmail(email, password)
            advanceUntilIdle()

            val uiState = viewModel.uiState.first()
            assertEquals("Error inesperado. Intenta de nuevo más tarde", uiState.errorMessage)
            assertEquals(false, uiState.isSuccess)
            assertEquals(false, uiState.isLoading)

            assertTrue(emittedEvents.isNotEmpty())
            val emittedEvent = emittedEvents.first()
            assertTrue(emittedEvent is LoginEvent.ShowMessage)
            assertEquals(
                "Error inesperado. Intenta de nuevo más tarde",
                (emittedEvent as LoginEvent.ShowMessage).message
            )
            job.cancel()
        }

    @Test
    fun `loginWithEmail emits specific error message for FirebaseAuthInvalidCredentialsException`() =
        testScope.runTest {
            val email = "test@example.com"
            val password = "password123"

            val mockedException = mockk<FirebaseAuthInvalidCredentialsException>()
            every { mockedException.errorCode } returns "auth/wrong-password"
            every { mockedException.message } returns "Bad credentials"

            // Usando tu función helper 'failure'
            coEvery { loginWithEmailUseCase(email, password) } returns failure(mockedException)

            val emittedEvents = mutableListOf<LoginEvent>()
            val job = launch {
                viewModel.event.toList(emittedEvents)
            }

            viewModel.loginWithEmail(email, password)
            advanceUntilIdle()

            val uiState = viewModel.uiState.first()
            assertEquals(
                "Credenciales inválidas. Email o contraseña incorrectos.",
                uiState.errorMessage
            )

            assertTrue(emittedEvents.isNotEmpty())
            val emittedEvent = emittedEvents.first()
            assertTrue(emittedEvent is LoginEvent.ShowMessage)
            assertEquals(
                "Credenciales inválidas. Email o contraseña incorrectos.",
                (emittedEvent as LoginEvent.ShowMessage).message
            )
            job.cancel()
        }

    // --- Tests para loginWithGoogle (y login social en general) ---

    @Test
    fun `loginWithGoogle navigates to mapFragment if user profile exists in Firestore`() =
        testScope.runTest {
            val idToken = "google_id_token"
            val userUid = "googleUid123"
            // Usando tu función helper 'success'
            coEvery { loginWithGoogleUseCase(idToken) } returns success(
                User(
                    userUid,
                    "email@example.com",
                    null,
                    null,
                    null
                )
            )
            every { firebaseAuth.currentUser } returns firebaseUser
            every { firebaseUser.uid } returns userUid
            // Usando tu función helper 'success' para verifyUserInFirestore
            coEvery { authRepository.verifyUserInFirestore(userUid) } returns success(true)

            val emittedEvents = mutableListOf<LoginEvent>()
            val job = launch {
                viewModel.event.toList(emittedEvents)
            }

            viewModel.loginWithGoogle(idToken)
            advanceUntilIdle()

            val uiState = viewModel.uiState.first()
            assertTrue(uiState.isSuccess)
            assertEquals(false, uiState.isLoading)

            assertTrue(emittedEvents.isNotEmpty())
            val emittedEvent = emittedEvents.first()
            assertTrue(emittedEvent is LoginEvent.NavigateToHome)
            job.cancel()
        }

    @Test
    fun `loginWithGoogle navigates to signup if user profile does not exist in Firestore`() =
        testScope.runTest {
            val idToken = "google_id_token"
            val userUid = "googleUid456"
            val userEmail = "social@example.com"
            val userName = "Social User"
            // Usando tu función helper 'success'
            coEvery { loginWithGoogleUseCase(idToken) } returns success(
                User(
                    userUid,
                    userEmail,
                    null,
                    userName,
                    null
                )
            )
            every { firebaseAuth.currentUser } returns firebaseUser
            every { firebaseUser.uid } returns userUid
            every { firebaseUser.email } returns userEmail
            every { firebaseUser.displayName } returns userName
            // Usando tu función helper 'success' para verifyUserInFirestore
            coEvery { authRepository.verifyUserInFirestore(userUid) } returns success(false)

            val emittedEvents = mutableListOf<LoginEvent>()
            val job = launch {
                viewModel.event.toList(emittedEvents)
            }
            viewModel.loginWithGoogle(idToken)
            advanceUntilIdle()

            val uiState = viewModel.uiState.first()
            assertTrue(uiState.isSuccess)
            assertEquals(false, uiState.isLoading)

            assertTrue(emittedEvents.size >= 2)
            val navigateEvent = emittedEvents.first()
            assertTrue(navigateEvent is LoginEvent.NavigateToSignupWithArgs)

            assertEquals(
                userEmail,
                (navigateEvent as LoginEvent.NavigateToSignupWithArgs).socialUserEmail
            )
            assertEquals(
                userName,
                (navigateEvent as LoginEvent.NavigateToSignupWithArgs).socialUserName
            )
            assertEquals(
                true,
                (navigateEvent as LoginEvent.NavigateToSignupWithArgs).isSocialLoginFlow
            )

            val messageEvent = emittedEvents[1]
            assertTrue(messageEvent is LoginEvent.ShowMessage)
            assertEquals(
                "Completa tu perfil para continuar",
                (messageEvent as LoginEvent.ShowMessage).message
            )

            job.cancel()
        }

    @Test
    fun `loginWithGoogle emits error message if current user is null after successful social login`() =
        testScope.runTest {
            val idToken = "google_id_token"
            // Usando tu función helper 'success'
            coEvery { loginWithGoogleUseCase(idToken) } returns success(
                User(
                    "anyUid",
                    "email@example.com",
                    null,
                    null,
                    null
                )
            )
            every { firebaseAuth.currentUser } returns null

            val emittedEvents = mutableListOf<LoginEvent>()
            val job = launch {
                viewModel.event.toList(emittedEvents)
            }

            viewModel.loginWithGoogle(idToken)
            advanceUntilIdle()

            val uiState = viewModel.uiState.first()
            assertEquals(
                "Error: Usuario autenticado nulo después del login social.",
                uiState.errorMessage
            )
            assertEquals(false, uiState.isLoading)

            assertTrue(emittedEvents.isNotEmpty())
            val emittedEvent = emittedEvents.first()
            assertTrue(emittedEvent is LoginEvent.ShowMessage)
            assertEquals(
                "Error: Usuario autenticado nulo después del login social.",
                (emittedEvent as LoginEvent.ShowMessage).message
            )
            job.cancel()
        }

    @Test
    fun `loginWithGoogle emits error message on social login failure`() = testScope.runTest {
        val idToken = "google_id_token"
        val exception = Exception("Google login failed")
        // Usando tu función helper 'failure'
        coEvery { loginWithGoogleUseCase(idToken) } returns failure(exception)

        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        viewModel.loginWithGoogle(idToken)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals("Google login failed", uiState.errorMessage)
        assertEquals(false, uiState.isLoading)

        assertTrue(emittedEvents.isNotEmpty())
        val emittedEvent = emittedEvents.first()
        assertTrue(emittedEvent is LoginEvent.ShowMessage)
        assertEquals("Google login failed", (emittedEvent as LoginEvent.ShowMessage).message)
        job.cancel()
    }

    @Test
    fun `loginWithGoogle emits error message if verifying user in Firestore fails`() =
        testScope.runTest {
            val idToken = "google_id_token"
            val userUid = "someUid"
            val verificationException = Exception("Firestore verification failed")
            // Usando tu función helper 'success'
            coEvery { loginWithGoogleUseCase(idToken) } returns success(
                User(
                    userUid,
                    "email@example.com",
                    null,
                    null,
                    null
                )
            )
            every { firebaseAuth.currentUser } returns firebaseUser
            every { firebaseUser.uid } returns userUid
            // Usando tu función helper 'failure'
            coEvery { authRepository.verifyUserInFirestore(userUid) } returns failure(
                verificationException
            )

            val emittedEvents = mutableListOf<LoginEvent>()
            val job = launch {
                viewModel.event.toList(emittedEvents)
            }

            viewModel.loginWithGoogle(idToken)
            advanceUntilIdle()

            val uiState = viewModel.uiState.first()
            assertEquals("Firestore verification failed", uiState.errorMessage)
            assertEquals(false, uiState.isLoading)

            assertTrue(emittedEvents.isNotEmpty())
            val emittedEvent = emittedEvents.first()
            assertTrue(emittedEvent is LoginEvent.ShowMessage)
            assertEquals(
                "Firestore verification failed",
                (emittedEvent as LoginEvent.ShowMessage).message
            )
            job.cancel()
        }

    // --- Tests para loginWithFacebook ---

    @Test
    fun `loginWithFacebook navigates to home if user profile exists in Firestore`() =
        testScope.runTest {
            val accessToken = "facebook_access_token"
            val userUid = "facebookUid123"
            // Usando tu función helper 'success'
            coEvery { loginWithFacebookUseCase(accessToken) } returns success(
                User(
                    userUid,
                    "fb_email@example.com",
                    null,
                    null,
                    null
                )
            )
            every { firebaseAuth.currentUser } returns firebaseUser
            every { firebaseUser.uid } returns userUid
            // Usando tu función helper 'success'
            coEvery { authRepository.verifyUserInFirestore(userUid) } returns success(true)

            val emittedEvents = mutableListOf<LoginEvent>()
            val job = launch {
                viewModel.event.toList(emittedEvents)
            }

            viewModel.loginWithFacebook(accessToken)
            advanceUntilIdle()

            val uiState = viewModel.uiState.first()
            assertTrue(uiState.isSuccess)
            assertEquals(false, uiState.isLoading)

            assertTrue(emittedEvents.isNotEmpty())
            val emittedEvent = emittedEvents.first()
            assertTrue(emittedEvent is LoginEvent.NavigateToHome)
            job.cancel()
        }

    @Test
    fun `loginWithFacebook navigates to signup if user profile does not exist in Firestore`() =
        testScope.runTest {
            val accessToken = "facebook_access_token"
            val userUid = "facebookUid456"
            val userEmail = "fb_social@example.com"
            val userName = "Facebook User"
            // Usando tu función helper 'success'
            coEvery { loginWithFacebookUseCase(accessToken) } returns success(
                User(
                    userUid,
                    userEmail,
                    null,
                    userName,
                    null
                )
            )
            every { firebaseAuth.currentUser } returns firebaseUser
            every { firebaseUser.uid } returns userUid
            every { firebaseUser.email } returns userEmail
            every { firebaseUser.displayName } returns userName

            // Usando tu función helper 'success'
            coEvery { authRepository.verifyUserInFirestore(userUid) } returns success(false)

            val emittedEvents = mutableListOf<LoginEvent>()
            val job = launch {
                viewModel.event.toList(emittedEvents)
            }

            viewModel.loginWithFacebook(accessToken)
            advanceUntilIdle()

            val uiState = viewModel.uiState.first()
            assertTrue(uiState.isSuccess)
            assertEquals(false, uiState.isLoading)

            assertTrue(emittedEvents.size >= 2)
            val navigateEvent = emittedEvents.first()
            assertTrue(navigateEvent is LoginEvent.NavigateToSignupWithArgs)

            assertEquals(
                userEmail,
                (navigateEvent as LoginEvent.NavigateToSignupWithArgs).socialUserEmail
            )
            assertEquals(
                userName,
                (navigateEvent as LoginEvent.NavigateToSignupWithArgs).socialUserName
            )
            assertEquals(
                true,
                (navigateEvent as LoginEvent.NavigateToSignupWithArgs).isSocialLoginFlow
            )

            val messageEvent = emittedEvents[1]
            assertTrue(messageEvent is LoginEvent.ShowMessage)
            assertEquals(
                "Completa tu perfil para continuar",
                (messageEvent as LoginEvent.ShowMessage).message
            )

            job.cancel()
        }

    @Test
    fun `loginWithFacebook emits error message on social login failure`() = testScope.runTest {
        val accessToken = "facebook_access_token"
        val exception = Exception("Facebook login failed")
        // Usando tu función helper 'failure'
        coEvery { loginWithFacebookUseCase(accessToken) } returns failure(exception)

        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        viewModel.loginWithFacebook(accessToken)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals("Facebook login failed", uiState.errorMessage)
        assertEquals(false, uiState.isLoading)

        assertTrue(emittedEvents.isNotEmpty())
        val emittedEvent = emittedEvents.first()
        assertTrue(emittedEvent is LoginEvent.ShowMessage)
        assertEquals("Facebook login failed", (emittedEvent as LoginEvent.ShowMessage).message)
        job.cancel()
    }

    // --- Tests para onForgotPasswordClicked y onBackPressed ---

    @Test
    fun `onForgotPasswordClicked emits NavigateToForgotPassword event`() = testScope.runTest {
        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        viewModel.onForgotPasswordClicked()
        advanceUntilIdle()

        assertTrue(emittedEvents.isNotEmpty())
        val emittedEvent = emittedEvents.first()
        assertTrue(emittedEvent is LoginEvent.NavigateToForgotPassword)
        job.cancel()
    }

    @Test
    fun `onBackPressed emits NavigateBack event`() = testScope.runTest {
        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        viewModel.onBackPressed()
        advanceUntilIdle()

        assertTrue(emittedEvents.isNotEmpty())
        val emittedEvent = emittedEvents.first()
        assertTrue(emittedEvent is LoginEvent.NavigateBack)
        job.cancel()
    }

    // --- Tests para saveUserCredentials y loadUserCredentials ---

    @Test
    fun `saveUserCredentials calls authRepository saveCredentials`() = testScope.runTest {
        val email = "save@example.com"
        val password = "savedPassword"
        coJustRun { authRepository.saveCredentials(email, password) }

        viewModel.saveUserCredentials(email, password)
        advanceUntilIdle()

        coVerify(exactly = 1) { authRepository.saveCredentials(email, password) }
    }

    @Test
    fun `loadUserCredentials updates uiState with loaded credentials`() = testScope.runTest {
        val savedEmail = "loaded@example.com"
        val savedPassword = "loadedPassword"
        coEvery { authRepository.loadSavedCredentials() } returns (savedEmail to savedPassword)

        viewModel.loadUserCredentials()
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals(savedEmail, uiState.email)
        assertEquals(savedPassword, uiState.password)
    }

    @Test
    fun `loadUserCredentials does not update uiState if credentials are null`() =
        testScope.runTest {
            coEvery { authRepository.loadSavedCredentials() } returns (null to null)

            viewModel.loadUserCredentials()
            advanceUntilIdle()

            val uiState = viewModel.uiState.first()
            assertEquals(null, uiState.email)
            assertEquals(null, uiState.password)
        }

    @Test
    fun `loadUserCredentials does not update uiState if credentials are blank`() =
        testScope.runTest {
            coEvery { authRepository.loadSavedCredentials() } returns (" " to "")

            viewModel.loadUserCredentials()
            advanceUntilIdle()

            val uiState = viewModel.uiState.first()
            assertEquals(null, uiState.email)
            assertEquals(null, uiState.password)
        }

    @Test
    fun `handleThirdPartyResult emits error if verifying user in Firestore fails (Facebook)`() =
        testScope.runTest {
            val accessToken = "facebook_access_token"
            val userUid = "someFacebookUid"
            val verificationException = Exception("Error de red al verificar perfil en Firestore")

            // Usando tu función helper 'success'
            coEvery { loginWithFacebookUseCase(accessToken) } returns success(
                User(
                    userUid,
                    "fb_email@example.com",
                    null,
                    null,
                    null
                )
            )
            every { firebaseAuth.currentUser } returns firebaseUser
            every { firebaseUser.uid } returns userUid
            // Simulate authRepository.verifyUserInFirestore returning a failure, using your helper
            coEvery { authRepository.verifyUserInFirestore(userUid) } returns failure(
                verificationException
            )

            val emittedEvents = mutableListOf<LoginEvent>()
            val job = launch {
                viewModel.event.toList(emittedEvents)
            }

            viewModel.loginWithFacebook(accessToken)
            advanceUntilIdle()

            val uiState = viewModel.uiState.first()
            assertEquals("Error de red al verificar perfil en Firestore", uiState.errorMessage)
            assertEquals(false, uiState.isLoading)

            assertTrue(emittedEvents.isNotEmpty())
            val emittedEvent = emittedEvents.first()
            assertTrue(emittedEvent is LoginEvent.ShowMessage)
            assertEquals(
                "Error de red al verificar perfil en Firestore",
                (emittedEvent as LoginEvent.ShowMessage).message
            )
            job.cancel()
        }

    @Test
    fun `loginWithFacebook emits error message if verifying user in Firestore fails`() =
        testScope.runTest {
            val accessToken = "facebook_access_token"
            val userUid = "someFacebookUid"
            val verificationException =
                Exception("Error de red al verificar perfil en Firestore") // Usa un mensaje específico para este test

            // 1. Mockear que loginWithFacebookUseCase tiene éxito y devuelve un usuario
            // Usando tu función helper 'success'
            coEvery { loginWithFacebookUseCase(accessToken) } returns success(
                User(
                    userUid,
                    "fb_email@example.com",
                    null,
                    null,
                    null
                )
            )

            // 2. Mockear que firebaseAuth.currentUser NO es nulo y devuelve el mock de FirebaseUser
            every { firebaseAuth.currentUser } returns firebaseUser
            every { firebaseUser.uid } returns userUid

            // 3. Simular que authRepository.verifyUserInFirestore retorna un fallo
            // Usando tu función helper 'failure'
            coEvery { authRepository.verifyUserInFirestore(userUid) } returns failure(
                verificationException
            )

            val emittedEvents = mutableListOf<LoginEvent>()
            val job = launch {
                viewModel.event.toList(emittedEvents)
            }

            // Ejecutar la función a testear
            viewModel.loginWithFacebook(accessToken)
            advanceUntilIdle() // Esperar a que las corrutinas terminen

            // Verificar el estado de la UI
            val uiState = viewModel.uiState.first()
            assertEquals(false, uiState.isLoading)
            // El errorMessage debe ser el mensaje de la excepción de verificación
            assertEquals(verificationException.message, uiState.errorMessage)

            // Verificar que se emitió el evento ShowMessage
            assertTrue(emittedEvents.isNotEmpty())
            val emittedEvent = emittedEvents.first()
            assertTrue(emittedEvent is LoginEvent.ShowMessage)
            // El mensaje del evento debe coincidir con el errorMessage
            assertEquals(
                verificationException.message,
                (emittedEvent as LoginEvent.ShowMessage).message
            )

            // Verificaciones adicionales (opcional, pero buena práctica)
            coVerify(exactly = 1) { loginWithFacebookUseCase(accessToken) }
            verify(exactly = 1) { firebaseAuth.currentUser }
            coVerify(exactly = 1) { authRepository.verifyUserInFirestore(userUid) }

            job.cancel() // Cancelar la recolección de eventos
        }


    @Test
    fun `mapErrorToMessage returns network error message for IOException`() = testScope.runTest {
        val email = "test@example.com"
        val password = "password123"
        val exception = IOException("No internet connection")
        // Usando tu función helper 'failure'
        coEvery { loginWithEmailUseCase(email, password) } returns failure(exception)

        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        viewModel.loginWithEmail(email, password)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals("Error de red. Por favor, revisa tu conexión", uiState.errorMessage)
        assertEquals(false, uiState.isSuccess)
        assertEquals(false, uiState.isLoading)

        assertTrue(emittedEvents.isNotEmpty())
        val emittedEvent = emittedEvents.first()
        assertTrue(emittedEvent is LoginEvent.ShowMessage)
        assertEquals(
            "Error de red. Por favor, revisa tu conexión",
            (emittedEvent as LoginEvent.ShowMessage).message
        )
        job.cancel()
    }

    @Test
    fun `mapErrorToMessage returns account collision message for FirebaseAuthUserCollisionException`() =
        testScope.runTest {
            val email = "test@example.com"
            val password = "password123"

            val mockedException = mockk<FirebaseAuthUserCollisionException>()
            every { mockedException.errorCode } returns "auth/email-already-in-use"
            every { mockedException.message } returns "The email address is already in use by another account."

            // Usando tu función helper 'failure'
            coEvery { loginWithEmailUseCase(email, password) } returns failure(mockedException)

            val emittedEvents = mutableListOf<LoginEvent>()
            val job = launch {
                viewModel.event.toList(emittedEvents)
            }

            viewModel.loginWithEmail(email, password)
            advanceUntilIdle()

            val uiState = viewModel.uiState.first()
            assertEquals("Ya existe una cuenta con este email.", uiState.errorMessage)
            assertEquals(false, uiState.isSuccess)
            assertEquals(false, uiState.isLoading)

            assertTrue(emittedEvents.isNotEmpty())
            val emittedEvent = emittedEvents.first()
            assertTrue(emittedEvent is LoginEvent.ShowMessage)
            assertEquals(
                "Ya existe una cuenta con este email.",
                (emittedEvent as LoginEvent.ShowMessage).message
            )
            job.cancel()
        }

}