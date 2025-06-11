package com.cursoandroid.queermap.ui.login

import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithEmailUseCase
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithFacebookUseCase
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithGoogleUseCase
import com.cursoandroid.queermap.ui.signup.SignUpValidator // Importar SignUpValidator
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
    @MockK // AÑADIDO: Mock para SignUpValidator
    private lateinit var signUpValidator: SignUpValidator

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
            signUpValidator // AÑADIDO: Pasar el mock al constructor
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    // --- Tests para loginWithEmail ---

    @Test
    fun `loginWithEmail emits success state and navigates to home on successful login`() = testScope.runTest {
        val email = "test@example.com"
        val password = "password123"
        val user = User("uid123", email, "testuser", "Test User", "01/01/2000")
        coEvery { loginWithEmailUseCase(email, password) } returns Result.success(user)

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

        assertTrue(emittedEvents.isEmpty())
        job.cancel()
    }

    @Test
    fun `loginWithEmail emits password invalid state if password is too short`() = testScope.runTest {
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
        assertTrue(emittedEvents.isEmpty())
        job.cancel()
    }

    @Test
    fun `loginWithEmail emits error message on failed login with general exception`() = testScope.runTest {
        val email = "test@example.com"
        val password = "password123"
        val exception = Exception("Network error")
        coEvery { loginWithEmailUseCase(email, password) } returns Result.failure(exception)

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
        assertEquals("Error inesperado. Intenta de nuevo más tarde", (emittedEvent as LoginEvent.ShowMessage).message)
        job.cancel()
    }

    @Test
    fun `loginWithEmail emits specific error message for FirebaseAuthInvalidCredentialsException`() = testScope.runTest {
        val email = "test@example.com"
        val password = "password123"

        val mockedException = mockk<FirebaseAuthInvalidCredentialsException>()
        every { mockedException.errorCode } returns "auth/wrong-password"
        every { mockedException.message } returns "Bad credentials"

        coEvery { loginWithEmailUseCase(email, password) } returns Result.failure(mockedException)

        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        viewModel.loginWithEmail(email, password)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals("Credenciales inválidas. Email o contraseña incorrectos.", uiState.errorMessage)

        assertTrue(emittedEvents.isNotEmpty())
        val emittedEvent = emittedEvents.first()
        assertTrue(emittedEvent is LoginEvent.ShowMessage)
        assertEquals("Credenciales inválidas. Email o contraseña incorrectos.", (emittedEvent as LoginEvent.ShowMessage).message)
        job.cancel()
    }

    // --- Tests para loginWithGoogle (y login social en general) ---

    @Test
    fun `loginWithGoogle navigates to mapFragment if user profile exists in Firestore`() = testScope.runTest {
        val idToken = "google_id_token"
        val userUid = "googleUid123"
        coEvery { loginWithGoogleUseCase(idToken) } returns Result.success(User(userUid, "email@example.com", null, null, null))
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns userUid
        coEvery { authRepository.verifyUserInFirestore(userUid) } returns Result.success(true)

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
    fun `loginWithGoogle navigates to signup if user profile does not exist in Firestore`() = testScope.runTest {
        val idToken = "google_id_token"
        val userUid = "googleUid456"
        val userEmail = "social@example.com"
        val userName = "Social User"
        coEvery { loginWithGoogleUseCase(idToken) } returns Result.success(User(userUid, userEmail, null, userName, null))
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns userUid
        every { firebaseUser.email } returns userEmail
        every { firebaseUser.displayName } returns userName
        coEvery { authRepository.verifyUserInFirestore(userUid) } returns Result.success(false)

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

        assertEquals(userEmail, (navigateEvent as LoginEvent.NavigateToSignupWithArgs).socialUserEmail)
        assertEquals(userName, (navigateEvent as LoginEvent.NavigateToSignupWithArgs).socialUserName)
        assertEquals(true, (navigateEvent as LoginEvent.NavigateToSignupWithArgs).isSocialLoginFlow)

        val messageEvent = emittedEvents[1]
        assertTrue(messageEvent is LoginEvent.ShowMessage)
        assertEquals("Completa tu perfil para continuar", (messageEvent as LoginEvent.ShowMessage).message)

        job.cancel()
    }

    @Test
    fun `loginWithGoogle emits error message if current user is null after successful social login`() = testScope.runTest {
        val idToken = "google_id_token"
        coEvery { loginWithGoogleUseCase(idToken) } returns Result.success(User("anyUid", "email@example.com", null, null, null))
        every { firebaseAuth.currentUser } returns null

        val emittedEvents = mutableListOf<LoginEvent>()
        val job = launch {
            viewModel.event.toList(emittedEvents)
        }

        viewModel.loginWithGoogle(idToken)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals("Error: Usuario autenticado nulo después del login social.", uiState.errorMessage)
        assertEquals(false, uiState.isLoading)

        assertTrue(emittedEvents.isNotEmpty())
        val emittedEvent = emittedEvents.first()
        assertTrue(emittedEvent is LoginEvent.ShowMessage)
        assertEquals("Error: Usuario autenticado nulo después del login social.", (emittedEvent as LoginEvent.ShowMessage).message)
        job.cancel()
    }

    @Test
    fun `loginWithGoogle emits error message on social login failure`() = testScope.runTest {
        val idToken = "google_id_token"
        val exception = Exception("Google login failed")
        coEvery { loginWithGoogleUseCase(idToken) } returns Result.failure(exception)

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
    fun `loginWithGoogle emits error message if verifying user in Firestore fails`() = testScope.runTest {
        val idToken = "google_id_token"
        val userUid = "someUid"
        val verificationException = Exception("Firestore verification failed")
        coEvery { loginWithGoogleUseCase(idToken) } returns Result.success(User(userUid, "email@example.com", null, null, null))
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns userUid
        coEvery { authRepository.verifyUserInFirestore(userUid) } returns Result.failure(verificationException)

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
        assertEquals("Firestore verification failed", (emittedEvent as LoginEvent.ShowMessage).message)
        job.cancel()
    }

    // --- Tests para loginWithFacebook ---

    @Test
    fun `loginWithFacebook navigates to home if user profile exists in Firestore`() = testScope.runTest {
        val accessToken = "facebook_access_token"
        val userUid = "facebookUid123"
        coEvery { loginWithFacebookUseCase(accessToken) } returns Result.success(User(userUid, "fb_email@example.com", null, null, null))
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns userUid
        coEvery { authRepository.verifyUserInFirestore(userUid) } returns Result.success(true)

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
    fun `loginWithFacebook navigates to signup if user profile does not exist in Firestore`() = testScope.runTest {
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

        assertEquals(userEmail, (navigateEvent as LoginEvent.NavigateToSignupWithArgs).socialUserEmail)
        assertEquals(userName, (navigateEvent as LoginEvent.NavigateToSignupWithArgs).socialUserEmail) // CORREGIR: debe ser socialUserName
        assertEquals(true, (navigateEvent as LoginEvent.NavigateToSignupWithArgs).isSocialLoginFlow)

        val messageEvent = emittedEvents[1]
        assertTrue(messageEvent is LoginEvent.ShowMessage)
        assertEquals("Completa tu perfil para continuar", (messageEvent as LoginEvent.ShowMessage).message)

        job.cancel()
    }

    @Test
    fun `loginWithFacebook emits error message on social login failure`() = testScope.runTest {
        val accessToken = "facebook_access_token"
        val exception = Exception("Facebook login failed")
        coEvery { loginWithFacebookUseCase(accessToken) } returns Result.failure(exception)

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
    fun `loadUserCredentials does not update uiState if credentials are null or blank`() = testScope.runTest {
        coEvery { authRepository.loadSavedCredentials() } returns (null to null)

        viewModel.loadUserCredentials()
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals(null, uiState.email)
        assertEquals(null, uiState.password)

        coEvery { authRepository.loadSavedCredentials() } returns (" " to "")
        viewModel.loadUserCredentials()
        advanceUntilIdle()
        val uiStateBlank = viewModel.uiState.first()
        assertEquals(null, uiStateBlank.email)
        assertEquals(null, uiStateBlank.password)
    }
}