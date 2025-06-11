package com.cursoandroid.queermap.ui.signup

import android.content.Intent
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
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@ExperimentalCoroutinesApi
class SignUpViewModelTest {

    @MockK
    private lateinit var createUserUseCase: CreateUserUseCase
    @MockK
    private lateinit var registerWithGoogleUseCase: RegisterWithGoogleUseCase
    @MockK
    private lateinit var registerWithFacebookUseCase: RegisterWithFacebookUseCase
    @MockK
    private lateinit var googleSignInDataSource: GoogleSignInDataSource
    @MockK
    private lateinit var facebookSignInDataSource: FacebookSignInDataSource
    @MockK
    private lateinit var facebookCallbackManager: CallbackManager
    @MockK
    private lateinit var authRepository: AuthRepository
    @MockK
    private lateinit var firebaseAuth: FirebaseAuth
    @MockK
    private lateinit var firebaseUser: FirebaseUser
    @MockK
    private lateinit var signUpValidator: SignUpValidator

    private lateinit var viewModel: SignUpViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        every { signUpValidator.isValidEmail(any()) } returns true
        every { signUpValidator.isValidPassword(any()) } returns true
        every { signUpValidator.isStrongPassword(any()) } returns true
        every { signUpValidator.isValidUsername(any()) } returns true
        every { signUpValidator.isValidFullName(any()) } returns true
        every { signUpValidator.isValidBirthday(any()) } returns true

        every { facebookSignInDataSource.accessTokenChannel } returns flowOf()

        every { facebookSignInDataSource.registerCallback(any()) } just runs

        viewModel = SignUpViewModel(
            createUserUseCase,
            registerWithGoogleUseCase,
            registerWithFacebookUseCase,
            googleSignInDataSource,
            facebookSignInDataSource,
            facebookCallbackManager,
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

    // --- Tests para el manejo de eventos de UI ---

    @Test
    fun `onEvent OnUsernameChanged updates uiState username`() = testScope.runTest {
        val newUsername = "test_user"
        viewModel.onEvent(SignUpEvent.OnUsernameChanged(newUsername))
        val uiState = viewModel.uiState.first()
        assertEquals(newUsername, uiState.username)
    }

    @Test
    fun `onEvent OnEmailChanged updates uiState email and resets isEmailInvalid`() = testScope.runTest {
        val newEmail = "test@example.com"
        viewModel.onEvent(SignUpEvent.OnEmailChanged(newEmail))
        val uiState = viewModel.uiState.first()
        assertEquals(newEmail, uiState.email)
        assertFalse(uiState.isEmailInvalid)
    }

    @Test
    fun `onEvent OnPasswordChanged updates uiState password and resets isPasswordInvalid`() = testScope.runTest {
        val newPassword = "newPassword123"
        viewModel.onEvent(SignUpEvent.OnPasswordChanged(newPassword))
        val uiState = viewModel.uiState.first()
        assertEquals(newPassword, uiState.password)
        assertFalse(uiState.isPasswordInvalid)
    }

    @Test
    fun `onEvent OnConfirmPasswordChanged updates uiState confirmPassword and resets doPasswordsMismatch`() = testScope.runTest {
        val newConfirmPassword = "newPassword123"
        viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged(newConfirmPassword))
        val uiState = viewModel.uiState.first()
        assertEquals(newConfirmPassword, uiState.confirmPassword)
        assertFalse(uiState.doPasswordsMismatch)
    }

    @Test
    fun `onEvent OnFullNameChanged updates uiState fullName`() = testScope.runTest {
        val newFullName = "Test User Full"
        viewModel.onEvent(SignUpEvent.OnFullNameChanged(newFullName))
        val uiState = viewModel.uiState.first()
        assertEquals(newFullName, uiState.fullName)
    }

    @Test
    fun `onEvent OnBirthdayChanged updates uiState birthday and resets isBirthdayInvalid`() = testScope.runTest {
        val newBirthday = "01/01/2000"
        viewModel.onEvent(SignUpEvent.OnBirthdayChanged(newBirthday))
        val uiState = viewModel.uiState.first()
        assertEquals(newBirthday, uiState.birthday)
        assertFalse(uiState.isBirthdayInvalid)
    }

    // --- Tests para registro con email ---

    @Test
    fun `OnRegisterClicked for non-social flow registers user successfully`() = testScope.runTest {
        val email = "test@example.com"
        val password = "password123"
        val user = User(id = "uid123", email = email, username = "user", name = "Full Name", birthday = "01/01/2000")

        viewModel.onEvent(SignUpEvent.OnEmailChanged(email))
        viewModel.onEvent(SignUpEvent.OnPasswordChanged(password))
        viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged(password))
        viewModel.onEvent(SignUpEvent.OnFullNameChanged("Full Name"))
        viewModel.onEvent(SignUpEvent.OnUsernameChanged("user"))
        viewModel.onEvent(SignUpEvent.OnBirthdayChanged("01/01/2000"))
        advanceUntilIdle()

        coEvery { createUserUseCase(any(), any()) } returns Result.success(Unit)

        val emittedEvents = mutableListOf<SignUpEvent>()
        val job = launch { viewModel.event.toList(emittedEvents) }

        viewModel.onEvent(SignUpEvent.OnRegisterClicked)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isSuccess)
        assertFalse(uiState.isLoading)
        assertNull(uiState.errorMessage)

        coVerify(exactly = 1) { createUserUseCase(any(), password) }
        assertTrue(emittedEvents.any { it is SignUpEvent.NavigateToHome })
        assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Registro exitoso. ¡Bienvenido/a!" })
        job.cancel()
    }

    @Test
    fun `OnRegisterClicked with invalid email emits error message`() = testScope.runTest {
        val invalidEmail = "invalid-email"
        every { signUpValidator.isValidEmail(invalidEmail) } returns false

        viewModel.onEvent(SignUpEvent.OnEmailChanged(invalidEmail))
        viewModel.onEvent(SignUpEvent.OnPasswordChanged("password123"))
        viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged("password123"))
        viewModel.onEvent(SignUpEvent.OnFullNameChanged("Full Name"))
        viewModel.onEvent(SignUpEvent.OnUsernameChanged("user"))
        viewModel.onEvent(SignUpEvent.OnBirthdayChanged("01/01/2000"))
        advanceUntilIdle()

        val emittedEvents = mutableListOf<SignUpEvent>()
        val job = launch { viewModel.event.toList(emittedEvents) }

        viewModel.onEvent(SignUpEvent.OnRegisterClicked)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isEmailInvalid)
        assertFalse(uiState.isLoading)

        coVerify(exactly = 0) { createUserUseCase(any(), any()) }
        assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Ingresa un email válido." })
        job.cancel()
    }

    @Test
    fun `OnRegisterClicked with short password emits error message`() = testScope.runTest {
        val shortPassword = "short"
        every { signUpValidator.isValidPassword(shortPassword) } returns false

        viewModel.onEvent(SignUpEvent.OnEmailChanged("test@example.com"))
        viewModel.onEvent(SignUpEvent.OnPasswordChanged(shortPassword))
        viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged(shortPassword))
        viewModel.onEvent(SignUpEvent.OnFullNameChanged("Full Name"))
        viewModel.onEvent(SignUpEvent.OnUsernameChanged("user"))
        viewModel.onEvent(SignUpEvent.OnBirthdayChanged("01/01/2000"))
        advanceUntilIdle()

        val emittedEvents = mutableListOf<SignUpEvent>()
        val job = launch { viewModel.event.toList(emittedEvents) }

        viewModel.onEvent(SignUpEvent.OnRegisterClicked)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isPasswordInvalid)
        assertFalse(uiState.isLoading)

        coVerify(exactly = 0) { createUserUseCase(any(), any()) }
        assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "La contraseña debe tener al menos 8 caracteres." })
        job.cancel()
    }

    @Test
    fun `OnRegisterClicked with mismatched passwords emits error message`() = testScope.runTest {
        viewModel.onEvent(SignUpEvent.OnEmailChanged("test@example.com"))
        viewModel.onEvent(SignUpEvent.OnPasswordChanged("password123"))
        viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged("passwordIncorrect"))
        viewModel.onEvent(SignUpEvent.OnFullNameChanged("Full Name"))
        viewModel.onEvent(SignUpEvent.OnUsernameChanged("user"))
        viewModel.onEvent(SignUpEvent.OnBirthdayChanged("01/01/2000"))
        advanceUntilIdle()

        val emittedEvents = mutableListOf<SignUpEvent>()
        val job = launch { viewModel.event.toList(emittedEvents) }

        viewModel.onEvent(SignUpEvent.OnRegisterClicked)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertTrue(uiState.doPasswordsMismatch)
        assertFalse(uiState.isLoading)

        coVerify(exactly = 0) { createUserUseCase(any(), any()) }
        assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Las contraseñas no coinciden." })
        job.cancel()
    }

    @Test
    fun `OnRegisterClicked with existing email emits specific error message`() = testScope.runTest {
        val email = "existing@example.com"
        val password = "password123"
        val exception = mockk<FirebaseAuthUserCollisionException>()
        every { exception.message } returns "The email address is already in use by another account."
        coEvery { createUserUseCase(any(), any()) } returns Result.failure(exception)

        viewModel.onEvent(SignUpEvent.OnEmailChanged(email))
        viewModel.onEvent(SignUpEvent.OnPasswordChanged(password))
        viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged(password))
        viewModel.onEvent(SignUpEvent.OnFullNameChanged("Full Name"))
        viewModel.onEvent(SignUpEvent.OnUsernameChanged("user"))
        viewModel.onEvent(SignUpEvent.OnBirthdayChanged("01/01/2000"))
        advanceUntilIdle()

        val emittedEvents = mutableListOf<SignUpEvent>()
        val job = launch { viewModel.event.toList(emittedEvents) }

        viewModel.onEvent(SignUpEvent.OnRegisterClicked)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isSuccess)
        assertFalse(uiState.isLoading)
        // La UIState.errorMessage debería ser actualizado por el ViewModel.
        // Si el ViewModel lo actualiza, esta aserción debería pasar.
        assertEquals("El correo electrónico ya está registrado.", uiState.errorMessage)

        assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "El correo electrónico ya está registrado." })
        job.cancel()
    }

    @Test
    fun `OnRegisterClicked with weak password emits specific error message`() = testScope.runTest {
        val email = "new@example.com"
        val weakPassword = "123"
        val exception = mockk<FirebaseAuthWeakPasswordException>()
        every { exception.message } returns "The password is too weak."
        coEvery { createUserUseCase(any(), any()) } returns Result.failure(exception)

        viewModel.onEvent(SignUpEvent.OnEmailChanged(email))
        viewModel.onEvent(SignUpEvent.OnPasswordChanged(weakPassword))
        viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged(weakPassword))
        viewModel.onEvent(SignUpEvent.OnFullNameChanged("Full Name"))
        viewModel.onEvent(SignUpEvent.OnUsernameChanged("user"))
        viewModel.onEvent(SignUpEvent.OnBirthdayChanged("01/01/2000"))
        advanceUntilIdle()

        val emittedEvents = mutableListOf<SignUpEvent>()
        val job = launch { viewModel.event.toList(emittedEvents) }

        viewModel.onEvent(SignUpEvent.OnRegisterClicked)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isSuccess)
        assertFalse(uiState.isLoading)
        assertEquals("La contraseña es demasiado débil. Usa una combinación de letras, números y símbolos.", uiState.errorMessage)

        assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "La contraseña es demasiado débil. Usa una combinación de letras, números y símbolos." })
        job.cancel()
    }

    // --- Tests para Login Social (Google) ---

    @Test
    fun `OnGoogleSignUpClicked launches Google Sign-In intent`() = testScope.runTest {
        val signInIntent = mockk<Intent>()
        coEvery { googleSignInDataSource.getSignInIntent() } returns signInIntent

        val launchedIntents = mutableListOf<Intent>()
        val job = launch { viewModel.launchGoogleSignIn.toList(launchedIntents) } // CORRECCIÓN: Usar 'launchedIntents'

        viewModel.onEvent(SignUpEvent.OnGoogleSignUpClicked)
        advanceUntilIdle()

        assertTrue(launchedIntents.isNotEmpty())
        assertEquals(signInIntent, launchedIntents.first())
        job.cancel()
    }

    @Test
    fun `OnGoogleSignInResult registers user with Google and navigates to home`() = testScope.runTest {
        val intentData = mockk<Intent>()
        val idToken = "google_id_token"
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "googleUid"
        every { firebaseUser.email } returns "google@example.com"
        every { firebaseUser.displayName } returns "Google User"

        coEvery { googleSignInDataSource.handleSignInResult(intentData) } returns Result.success(idToken)
        coEvery { registerWithGoogleUseCase(idToken) } returns Result.success(User("googleUid", "Google User", null, "google@example.com", null))
        // ELIMINADA LÍNEA: coEvery { authRepository.verifyUserInFirestore(firebaseUser.uid) } returns Result.success(true)


        val emittedEvents = mutableListOf<SignUpEvent>()
        val job = launch { viewModel.event.toList(emittedEvents) }

        viewModel.onEvent(SignUpEvent.OnGoogleSignInResult(intentData))
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isSuccess)
        assertFalse(uiState.isLoading)

        coVerify(exactly = 1) { registerWithGoogleUseCase(idToken) }
        // ELIMINADA LÍNEA: coVerify(exactly = 1) { authRepository.verifyUserInFirestore(firebaseUser.uid) }

        assertTrue(emittedEvents.any { it is SignUpEvent.NavigateToHome })
        assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Registro con Google exitoso. ¡Bienvenido/a!" })
        job.cancel()
    }

    @Test
    fun `OnGoogleSignInResult handles Google sign-in failure`() = testScope.runTest {
        val intentData = mockk<Intent>()
        val exception = Exception("Google sign-in error")
        coEvery { googleSignInDataSource.handleSignInResult(intentData) } returns Result.failure(exception)

        val emittedEvents = mutableListOf<SignUpEvent>()
        val job = launch { viewModel.event.toList(emittedEvents) }

        viewModel.onEvent(SignUpEvent.OnGoogleSignInResult(intentData))
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isSuccess)
        assertFalse(uiState.isLoading)
        assertEquals("Google sign-in error", uiState.errorMessage)

        assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Google sign-in error" })
        job.cancel()
    }

    @Test
    fun `OnGoogleSignInResult handles Firebase registration failure for Google`() = testScope.runTest {
        val intentData = mockk<Intent>()
        val idToken = "google_id_token"
        val exception = mockk<FirebaseAuthUserCollisionException>()
        every { exception.message } returns "The email address is already in use by another account."
        coEvery { googleSignInDataSource.handleSignInResult(intentData) } returns Result.success(idToken)
        coEvery { registerWithGoogleUseCase(idToken) } returns Result.failure(exception)

        val emittedEvents = mutableListOf<SignUpEvent>()
        val job = launch { viewModel.event.toList(emittedEvents) }

        viewModel.onEvent(SignUpEvent.OnGoogleSignInResult(intentData))
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isSuccess)
        assertFalse(uiState.isLoading)
        assertEquals("El correo electrónico ya está registrado con otra cuenta.", uiState.errorMessage)

        assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "El correo electrónico ya está registrado con otra cuenta." })
        job.cancel()
    }

    // --- Tests para Login Social (Facebook) ---

    @Test
    fun `OnFacebookSignUpClicked emits ShowMessage`() = testScope.runTest {
        val emittedEvents = mutableListOf<SignUpEvent>()
        val job = launch { viewModel.event.toList(emittedEvents) }

        viewModel.onEvent(SignUpEvent.OnFacebookSignUpClicked)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isLoading)
        assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Iniciando sesión con Facebook..." })
        job.cancel()
    }

    @Test
    fun `Facebook accessTokenChannel success registers user with Facebook and navigates to home`() = testScope.runTest {
        val accessToken = "facebook_access_token"
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "fbUid"
        every { firebaseUser.email } returns "fb@example.com"
        every { firebaseUser.displayName } returns "FB User"

        every { facebookSignInDataSource.accessTokenChannel } returns flowOf(Result.success(accessToken))
        coEvery { registerWithFacebookUseCase(accessToken) } returns Result.success(User("fbUid", "FB User", null, "fb@example.com", null))
        // ELIMINADA LÍNEA: coEvery { authRepository.verifyUserInFirestore(firebaseUser.uid) } returns Result.success(true)


        viewModel = SignUpViewModel(
            createUserUseCase,
            registerWithGoogleUseCase,
            registerWithFacebookUseCase,
            googleSignInDataSource,
            facebookSignInDataSource,
            facebookCallbackManager,
            authRepository,
            firebaseAuth,
            signUpValidator
        )

        val emittedEvents = mutableListOf<SignUpEvent>()
        val job = launch { viewModel.event.toList(emittedEvents) }

        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isSuccess)
        assertFalse(uiState.isLoading)

        coVerify(exactly = 1) { registerWithFacebookUseCase(accessToken) }
        // ELIMINADA LÍNEA: coVerify(exactly = 1) { authRepository.verifyUserInFirestore(firebaseUser.uid) }

        assertTrue(emittedEvents.any { it is SignUpEvent.NavigateToHome })
        assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Registro con Facebook exitoso. ¡Bienvenido/a!" })
        job.cancel()
    }

    @Test
    fun `Facebook accessTokenChannel failure emits error message`() = testScope.runTest {
        val exception = Exception("Facebook login failed")
        every { facebookSignInDataSource.accessTokenChannel } returns flowOf(Result.failure(exception))

        viewModel = SignUpViewModel(
            createUserUseCase,
            registerWithGoogleUseCase,
            registerWithFacebookUseCase,
            googleSignInDataSource,
            facebookSignInDataSource,
            facebookCallbackManager,
            authRepository,
            firebaseAuth,
            signUpValidator
        )

        val emittedEvents = mutableListOf<SignUpEvent>()
        val job = launch { viewModel.event.toList(emittedEvents) }

        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isSuccess)
        assertFalse(uiState.isLoading)
        assertEquals("Facebook login failed", uiState.errorMessage)

        assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Facebook login failed" })
        job.cancel()
    }

    // --- Tests para completar perfil (flujo social) ---

    @Test
    fun `OnRegisterClicked for social flow completes user profile successfully`() = testScope.runTest {
        val uid = "socialUid123"
        val socialEmail = "social@example.com"
        val socialName = "Social User"
        val username = "social_user_name"
        val birthday = "01/01/1990"

        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns uid
        every { firebaseUser.email } returns socialEmail
        every { firebaseUser.displayName } returns socialName

        viewModel.setSocialLoginData(true, socialEmail, socialName)
        viewModel.onEvent(SignUpEvent.OnUsernameChanged(username))
        viewModel.onEvent(SignUpEvent.OnBirthdayChanged(birthday))
        advanceUntilIdle()

        val updatedUser = User(id = uid, name = socialName, username = username, email = socialEmail, birthday = birthday)
        coEvery { authRepository.updateUserProfile(uid, updatedUser) } returns Result.success(Unit)

        val emittedEvents = mutableListOf<SignUpEvent>()
        val job = launch { viewModel.event.toList(emittedEvents) }

        viewModel.onEvent(SignUpEvent.OnRegisterClicked)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isSuccess)
        assertFalse(uiState.isLoading)
        assertNull(uiState.errorMessage)

        coVerify(exactly = 1) { authRepository.updateUserProfile(uid, updatedUser) }
        assertTrue(emittedEvents.any { it is SignUpEvent.NavigateToHome })
        assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Perfil completado exitosamente." })
        job.cancel()
    }

    @Test
    fun `completeUserProfile emits error if currentUser is null`() = testScope.runTest {
        every { firebaseAuth.currentUser } returns null
        viewModel.setSocialLoginData(true, "social@example.com", "Social User")
        viewModel.onEvent(SignUpEvent.OnUsernameChanged("user"))
        viewModel.onEvent(SignUpEvent.OnBirthdayChanged("01/01/2000"))
        advanceUntilIdle()

        val emittedEvents = mutableListOf<SignUpEvent>()
        val job = launch { viewModel.event.toList(emittedEvents) }

        viewModel.onEvent(SignUpEvent.OnRegisterClicked)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isLoading)
        assertEquals("Usuario no autenticado para completar el perfil.", uiState.errorMessage)

        assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Usuario no autenticado para completar el perfil." })
        job.cancel()
    }

    @Test
    fun `completeUserProfile handles updateProfile failure`() = testScope.runTest {
        val uid = "socialUid123"
        val socialEmail = "social@example.com"
        val socialName = "Social User"
        val username = "social_user_name"
        val birthday = "01/01/1990"
        val exception = IOException("Network error")

        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns uid
        every { firebaseUser.email } returns socialEmail
        every { firebaseUser.displayName } returns socialName

        viewModel.setSocialLoginData(true, socialEmail, socialName)
        viewModel.onEvent(SignUpEvent.OnUsernameChanged(username))
        viewModel.onEvent(SignUpEvent.OnBirthdayChanged(birthday))
        advanceUntilIdle()

        val updatedUser = User(id = uid, name = socialName, username = username, email = socialEmail, birthday = birthday)
        coEvery { authRepository.updateUserProfile(uid, updatedUser) } returns Result.failure(exception)

        val emittedEvents = mutableListOf<SignUpEvent>()
        val job = launch { viewModel.event.toList(emittedEvents) }

        viewModel.onEvent(SignUpEvent.OnRegisterClicked)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isSuccess)
        assertFalse(uiState.isLoading)
        assertEquals("Network error", uiState.errorMessage)

        assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Network error" })
        job.cancel()
    }

    // --- Otros tests (navegación, etc.) ---

    @Test
    fun `onBackPressed emits NavigateBack event`() = testScope.runTest {
        val emittedEvents = mutableListOf<SignUpEvent>()
        val job = launch { viewModel.event.toList(emittedEvents) }

        viewModel.onBackPressed()
        advanceUntilIdle()

        assertTrue(emittedEvents.any { it is SignUpEvent.NavigateBack })
        job.cancel()
    }

    @Test
    fun `setSocialLoginData correctly updates uiState`() = testScope.runTest {
        viewModel.setSocialLoginData(true, "social@example.com", "Social Name")
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isSocialLoginFlow)
        assertEquals("social@example.com", uiState.email)
        assertEquals("Social Name", uiState.fullName)
    }

    @Test
    fun `setSocialLoginData does not overwrite existing data if null is passed`() = testScope.runTest {
        viewModel.onEvent(SignUpEvent.OnEmailChanged("initial@example.com"))
        viewModel.onEvent(SignUpEvent.OnFullNameChanged("Initial Name"))
        advanceUntilIdle()

        viewModel.setSocialLoginData(true, null, null)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isSocialLoginFlow)
        assertEquals("initial@example.com", uiState.email)
        assertEquals("Initial Name", uiState.fullName)
    }
}