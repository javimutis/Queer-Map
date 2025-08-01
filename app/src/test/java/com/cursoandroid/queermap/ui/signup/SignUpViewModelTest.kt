package com.cursoandroid.queermap.ui.signup

import android.content.Intent
import com.cursoandroid.queermap.common.InputValidator
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.domain.usecase.auth.CreateUserUseCase
import com.cursoandroid.queermap.domain.usecase.auth.RegisterWithFacebookUseCase
import com.cursoandroid.queermap.domain.usecase.auth.RegisterWithGoogleUseCase
import com.cursoandroid.queermap.util.Result
import com.cursoandroid.queermap.util.failure
import com.cursoandroid.queermap.util.success
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    private lateinit var signUpValidator: InputValidator

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
        every { facebookSignInDataSource.registerCallback(any(), any()) } just runs

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

    // Tests para el manejo de eventos de UI
    @Test
    fun `when OnUsernameChanged then uiState username is updated`() = testScope.runTest {
        // Given: A new username
        val newUsername = "test_user"

        // When: OnUsernameChanged event is sent
        viewModel.onEvent(SignUpEvent.OnUsernameChanged(newUsername))
        advanceUntilIdle() // Ensure coroutines complete

        // Then: The uiState's username should be updated
        val uiState = viewModel.uiState.first()
        assertEquals(newUsername, uiState.username)
    }


    @Test
    fun `when OnEmailChanged then uiState email is updated and isEmailInvalid is reset`() =
        testScope.runTest {
            // Given: A new email
            val newEmail = "test@example.com"

            // When: OnEmailChanged event is sent
            viewModel.onEvent(SignUpEvent.OnEmailChanged(newEmail))
            advanceUntilIdle() // Ensure coroutines complete

            // Then: The uiState's email should be updated and isEmailInvalid should be false
            val uiState = viewModel.uiState.first()
            assertEquals(newEmail, uiState.email)
            assertFalse(uiState.isEmailInvalid)
        }

    @Test
    fun `when OnPasswordChanged then uiState password is updated and isPasswordInvalid is reset`() =
        testScope.runTest {
            // Given: A new password
            val newPassword = "newPassword123"

            // When: OnPasswordChanged event is sent
            viewModel.onEvent(SignUpEvent.OnPasswordChanged(newPassword))
            advanceUntilIdle() // Ensure coroutines complete

            // Then: The uiState's password should be updated and isPasswordInvalid should be false
            val uiState = viewModel.uiState.first()
            assertEquals(newPassword, uiState.password)
            assertFalse(uiState.isPasswordInvalid)
        }

    @Test
    fun `when OnConfirmPasswordChanged then uiState confirmPassword is updated and doPasswordsMismatch is reset`() =
        testScope.runTest {
            // Given: A new confirm password
            val newConfirmPassword = "newPassword123"

            // When: OnConfirmPasswordChanged event is sent
            viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged(newConfirmPassword))
            advanceUntilIdle() // Ensure coroutines complete

            // Then: The uiState's confirmPassword should be updated and doPasswordsMismatch should be false
            val uiState = viewModel.uiState.first()
            assertEquals(newConfirmPassword, uiState.confirmPassword)
            assertFalse(uiState.doPasswordsMismatch)
        }

    @Test
    fun `when OnFullNameChanged then uiState fullName is updated`() = testScope.runTest {
        // Given: A new full name
        val newFullName = "Test User Full"

        // When: OnFullNameChanged event is sent
        viewModel.onEvent(SignUpEvent.OnFullNameChanged(newFullName))
        advanceUntilIdle() // Ensure coroutines complete

        // Then: The uiState's fullName should be updated
        val uiState = viewModel.uiState.first()
        assertEquals(newFullName, uiState.fullName)
    }

    @Test
    fun `when OnBirthdayChanged then uiState birthday is updated and isBirthdayInvalid is reset`() =
        testScope.runTest {
            // Given: A new birthday
            val newBirthday = "01/01/2000"

            // When: OnBirthdayChanged event is sent
            viewModel.onEvent(SignUpEvent.OnBirthdayChanged(newBirthday))
            advanceUntilIdle() // Ensure coroutines complete

            // Then: The uiState's birthday should be updated and isBirthdayInvalid should be false
            val uiState = viewModel.uiState.first()
            assertEquals(newBirthday, uiState.birthday)
            assertFalse(uiState.isBirthdayInvalid)
        }


    @Test
    fun `when FacebookCallback onSuccess and accessToken is empty then error message is emitted`() =
        testScope.runTest {
            val messages = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { messages.add(it) }
            }

            val callbackSlot = slot<FacebookCallback<LoginResult>>()
            verify { facebookSignInDataSource.registerCallback(any(), capture(callbackSlot)) }

            val loginResultMock = mockk<LoginResult>()
            val accessTokenMock = mockk<AccessToken>()

            every { accessTokenMock.token } returns "" // Token vacío
            every { loginResultMock.accessToken } returns accessTokenMock

            callbackSlot.captured.onSuccess(loginResultMock)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals("Token de acceso de Facebook nulo.", viewModel.uiState.value.errorMessage)
            assertTrue(messages.contains(SignUpEvent.ShowMessage("Token de acceso de Facebook nulo.")))

            job.cancel()
        }


    @Test
    fun `when FacebookCallback onCancel then cancellation message is emitted`() =
        testScope.runTest {
            val messages = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { messages.add(it) }
            }

            val callbackSlot = slot<FacebookCallback<LoginResult>>()
            verify { facebookSignInDataSource.registerCallback(any(), capture(callbackSlot)) }

            callbackSlot.captured.onCancel()

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(
                "Inicio de sesión con Facebook cancelado.",
                viewModel.uiState.value.errorMessage
            )
            assertTrue(messages.contains(SignUpEvent.ShowMessage("Inicio de sesión con Facebook cancelado.")))

            job.cancel()
        }

    @Test
    fun `when FacebookCallback onError then error message is emitted`() = testScope.runTest {
        val messages = mutableListOf<SignUpEvent>()
        val job = launch {
            viewModel.event.collect { messages.add(it) }
        }

        val callbackSlot = slot<FacebookCallback<LoginResult>>()
        verify { facebookSignInDataSource.registerCallback(any(), capture(callbackSlot)) }

        val facebookException = FacebookException("Facebook error message")
        callbackSlot.captured.onError(facebookException)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Facebook error message", viewModel.uiState.value.errorMessage)
        assertTrue(messages.contains(SignUpEvent.ShowMessage("Error: Facebook error message")))

        job.cancel()
    }

    @Test
    fun `when facebookSignInDataSource accessTokenChannel emits success then handleFacebookAuthWithFirebase is called`() =
        testScope.runTest {
            val accessToken = "mockFacebookAccessToken"
            // Usar tu función auxiliar 'success'
            val flow = flowOf(success(accessToken))
            every { facebookSignInDataSource.accessTokenChannel } returns flow
            // Mockeamos que registerWithFacebookUseCase retorne un éxito para simular el flujo completo
            // FIX: Explicitly specify the type parameter for `mockk()` to ensure type inference for `success<T>()`
            coEvery { registerWithFacebookUseCase(accessToken) } returns success(mockk<User>())

            // Re-initialize ViewModel to ensure the init block's collect is triggered
            // FIX: Corrected constructor parameter order to match SignUpViewModel
            viewModel = SignUpViewModel(
                createUserUseCase,
                registerWithGoogleUseCase, // Correct order
                registerWithFacebookUseCase, // Correct order
                googleSignInDataSource, // Correct order
                facebookSignInDataSource, // Correct order
                facebookCallbackManager, // Correct order
                authRepository, // Correct order
                firebaseAuth, // Correct order
                signUpValidator // Correct order
            )

            val messages = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { messages.add(it) }
            }

            advanceUntilIdle() // Allow the collect block in init to run

            // Verificamos que registerWithFacebookUseCase fue llamado con el token esperado
            coVerify { registerWithFacebookUseCase(accessToken) }
            assertFalse(viewModel.uiState.value.isLoading)
            assertTrue(viewModel.uiState.value.isSuccess)
            assertTrue(messages.contains(SignUpEvent.NavigateToHome))
            assertTrue(messages.contains(SignUpEvent.ShowMessage("Registro con Facebook exitoso. ¡Bienvenido/a!")))

            job.cancel()
        }

    @Test
    fun `when facebookSignInDataSource accessTokenChannel emits failure then errorMessage is set`() =
        testScope.runTest {
            val error = Exception("Failed Facebook access token")
            // FIX: Específicamos el tipo genérico <String> para la función failure,
            // ya que el canal de acceso de Facebook maneja tokens de tipo String.
            val flow = flowOf(failure<String>(error, error.message))
            every { facebookSignInDataSource.accessTokenChannel } returns flow

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

            val messages = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { messages.add(it) }
            }

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.isSuccess)
            assertEquals("Failed Facebook access token", viewModel.uiState.value.errorMessage)
            assertTrue(messages.contains(SignUpEvent.ShowMessage("Failed Facebook access token")))

            job.cancel()
        }


    @Test
    fun `when handleFacebookAuthWithFirebase fails with FirebaseAuthUserCollisionException then correct message is shown`() =
        testScope.runTest {
            val accessToken = "mockAccessToken"
            val exception = mockk<FirebaseAuthUserCollisionException> {
                every { message } returns "Email already in use with Facebook"
            }

            // Mockeamos que registerWithFacebookUseCase falle con esta excepción
            coEvery { registerWithFacebookUseCase(accessToken) } returns failure(
                exception,
                exception.message
            )

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            // Simular el callback onSuccess de Facebook SDK para que se llame a handleFacebookAuthWithFirebase
            val callbackSlot = slot<FacebookCallback<LoginResult>>()
            verify { facebookSignInDataSource.registerCallback(any(), capture(callbackSlot)) }

            val loginResultMock = mockk<LoginResult>()
            val accessTokenMock = mockk<AccessToken>()
            every { accessTokenMock.token } returns accessToken // Aseguramos que el token no sea nulo
            every { loginResultMock.accessToken } returns accessTokenMock

            callbackSlot.captured.onSuccess(loginResultMock)

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.isSuccess)
            assertEquals(
                "El correo electrónico ya está registrado con otra cuenta.",
                viewModel.uiState.value.errorMessage
            )
            assertTrue(events.contains(SignUpEvent.ShowMessage("El correo electrónico ya está registrado con otra cuenta.")))

            job.cancel()
        }

    @Test
    fun `when handleFacebookAuthWithFirebase fails with generic exception then correct message is shown`() =
        testScope.runTest {
            val accessToken = "mockAccessToken"
            val exception = Exception("Generic Facebook auth error")
            // Mockeamos que registerWithFacebookUseCase falle con esta excepción genérica
            coEvery { registerWithFacebookUseCase(accessToken) } returns failure(
                exception,
                exception.message
            )

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            // Simular el callback onSuccess de Facebook SDK para que se llame a handleFacebookAuthWithFirebase
            val callbackSlot = slot<FacebookCallback<LoginResult>>()
            verify { facebookSignInDataSource.registerCallback(any(), capture(callbackSlot)) }

            val loginResultMock = mockk<LoginResult>()
            val accessTokenMock = mockk<AccessToken>()
            every { accessTokenMock.token } returns accessToken // Aseguramos que el token no sea nulo
            every { loginResultMock.accessToken } returns accessTokenMock

            callbackSlot.captured.onSuccess(loginResultMock)

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.isSuccess)
            assertEquals(
                "Autenticación de Facebook con Firebase fallida.",
                viewModel.uiState.value.errorMessage
            )
            assertTrue(events.contains(SignUpEvent.ShowMessage("Autenticación de Facebook con Firebase fallida.")))

            job.cancel()
        }

    // Tests para registro con email

    @Test
    fun `when OnRegisterClicked with valid data then user is registered successfully and navigates to home`() =
        testScope.runTest {
            // Given: Valid user registration data
            val email = "test@example.com"
            val password = "password123"
            viewModel.onEvent(SignUpEvent.OnEmailChanged(email))
            viewModel.onEvent(SignUpEvent.OnPasswordChanged(password))
            viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged(password))
            viewModel.onEvent(SignUpEvent.OnFullNameChanged("Full Name"))
            viewModel.onEvent(SignUpEvent.OnUsernameChanged("user"))
            viewModel.onEvent(SignUpEvent.OnBirthdayChanged("01/01/2000"))
            advanceUntilIdle()

            // Given: createUserUseCase returns success
            coEvery { createUserUseCase(any(), any()) } returns success(Unit)

            val emittedEvents = mutableListOf<SignUpEvent>()
            val job = launch { viewModel.event.toList(emittedEvents) }

            // When: OnRegisterClicked event is sent
            viewModel.onEvent(SignUpEvent.OnRegisterClicked)
            advanceUntilIdle()

            // Then: The UI state reflects success, createUserUseCase is called, and navigation/message events are emitted
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
    fun `when OnRegisterClicked with invalid email then an error message is emitted`() =
        testScope.runTest {
            // Given: Invalid email and other valid user data
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

            // When: OnRegisterClicked event is sent
            viewModel.onEvent(SignUpEvent.OnRegisterClicked)
            advanceUntilIdle()

            // Then: The UI state reflects email invalidity, createUserUseCase is not called, and an error message is emitted
            val uiState = viewModel.uiState.first()
            assertTrue(uiState.isEmailInvalid)
            assertFalse(uiState.isLoading)
            coVerify(exactly = 0) { createUserUseCase(any(), any()) }
            assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Ingresa un email válido." })
            job.cancel()
        }

    @Test
    fun `when OnRegisterClicked with short password then an error message is emitted`() =
        testScope.runTest {
            // Given: A short password and other valid user data
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

            // When: OnRegisterClicked event is sent
            viewModel.onEvent(SignUpEvent.OnRegisterClicked)
            advanceUntilIdle()

            // Then: The UI state reflects password invalidity, createUserUseCase is not called, and an error message is emitted
            val uiState = viewModel.uiState.first()
            assertTrue(uiState.isPasswordInvalid)
            assertFalse(uiState.isLoading)
            coVerify(exactly = 0) { createUserUseCase(any(), any()) }
            assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "La contraseña debe tener al menos 8 caracteres." })
            job.cancel()
        }

    @Test
    fun `when OnRegisterClicked with mismatched passwords then an error message is emitted`() =
        testScope.runTest {
            // Given: Mismatched passwords and other valid user data
            viewModel.onEvent(SignUpEvent.OnEmailChanged("test@example.com"))
            viewModel.onEvent(SignUpEvent.OnPasswordChanged("password123"))
            viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged("passwordIncorrect"))
            viewModel.onEvent(SignUpEvent.OnFullNameChanged("Full Name"))
            viewModel.onEvent(SignUpEvent.OnUsernameChanged("user"))
            viewModel.onEvent(SignUpEvent.OnBirthdayChanged("01/01/2000"))
            advanceUntilIdle()

            val emittedEvents = mutableListOf<SignUpEvent>()
            val job = launch { viewModel.event.toList(emittedEvents) }

            // When: OnRegisterClicked event is sent
            viewModel.onEvent(SignUpEvent.OnRegisterClicked)
            advanceUntilIdle()

            // Then: The UI state reflects password mismatch, createUserUseCase is not called, and an error message is emitted
            val uiState = viewModel.uiState.first()
            assertTrue(uiState.doPasswordsMismatch)
            assertFalse(uiState.isLoading)
            coVerify(exactly = 0) { createUserUseCase(any(), any()) }
            assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Las contraseñas no coinciden." })
            job.cancel()
        }

    @Test
    fun `when OnRegisterClicked with existing email then a specific error message is emitted`() =
        testScope.runTest {
            // Given: Valid user data but createUserUseCase returns a FirebaseAuthUserCollisionException
            val email = "existing@example.com"
            val password = "password123"
            val exception = mockk<FirebaseAuthUserCollisionException>()
            every { exception.message } returns "The email address is already in use by another account."
            coEvery { createUserUseCase(any(), any()) } returns failure(exception)
            viewModel.onEvent(SignUpEvent.OnEmailChanged(email))
            viewModel.onEvent(SignUpEvent.OnPasswordChanged(password))
            viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged(password))
            viewModel.onEvent(SignUpEvent.OnFullNameChanged("Full Name"))
            viewModel.onEvent(SignUpEvent.OnUsernameChanged("user"))
            viewModel.onEvent(SignUpEvent.OnBirthdayChanged("01/01/2000"))
            advanceUntilIdle()

            val emittedEvents = mutableListOf<SignUpEvent>()
            val job = launch { viewModel.event.toList(emittedEvents) }

            // When: OnRegisterClicked event is sent
            viewModel.onEvent(SignUpEvent.OnRegisterClicked)
            advanceUntilIdle()

            // Then: The UI state reflects failure and a specific error message about existing email is emitted
            val uiState = viewModel.uiState.first()
            assertFalse(uiState.isSuccess)
            assertFalse(uiState.isLoading)
            assertEquals("El correo electrónico ya está registrado.", uiState.errorMessage)
            assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "El correo electrónico ya está registrado." })
            job.cancel()
        }

    @Test
    fun `when OnRegisterClicked with weak password then a specific error message is emitted`() =
        testScope.runTest {
            // Given: Valid email but a weak password, and createUserUseCase returns a FirebaseAuthWeakPasswordException
            val email = "new@example.com"
            val weakPassword = "123"
            val exception = mockk<FirebaseAuthWeakPasswordException>()
            every { exception.message } returns "The password is too weak."
            coEvery { createUserUseCase(any(), any()) } returns failure(exception)
            viewModel.onEvent(SignUpEvent.OnEmailChanged(email))
            viewModel.onEvent(SignUpEvent.OnPasswordChanged(weakPassword))
            viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged(weakPassword))
            viewModel.onEvent(SignUpEvent.OnFullNameChanged("Full Name"))
            viewModel.onEvent(SignUpEvent.OnUsernameChanged("user"))
            viewModel.onEvent(SignUpEvent.OnBirthdayChanged("01/01/2000"))
            advanceUntilIdle()

            val emittedEvents = mutableListOf<SignUpEvent>()
            val job = launch { viewModel.event.toList(emittedEvents) }

            // When: OnRegisterClicked event is sent
            viewModel.onEvent(SignUpEvent.OnRegisterClicked)
            advanceUntilIdle()

            // Then: The UI state reflects failure and a specific error message about weak password is emitted
            val uiState = viewModel.uiState.first()
            assertFalse(uiState.isSuccess)
            assertFalse(uiState.isLoading)
            assertEquals(
                "La contraseña es demasiado débil. Usa una combinación de letras, números y símbolos.",
                uiState.errorMessage
            )
            assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "La contraseña es demasiado débil. Usa una combinación de letras, números y símbolos." })
            job.cancel()
        }

    // Tests para registro Social (Google)

    @Test
    fun `when setSocialLoginData is called then uiState is updated correctly`() =
        testScope.runTest {
            val initialUiState = viewModel.uiState.value
            assertFalse(initialUiState.isSocialLoginFlow)
            assertEquals(null, initialUiState.email)
            assertEquals(null, initialUiState.fullName)

            val testEmail = "social@example.com"
            val testName = "Social User"
            viewModel.setSocialLoginData(true, testEmail, testName)

            assertEquals(true, viewModel.uiState.value.isSocialLoginFlow)
            assertEquals(testEmail, viewModel.uiState.value.email)
            assertEquals(testName, viewModel.uiState.value.fullName)

            // Test with nulls, should retain existing values if not null previously
            viewModel.setSocialLoginData(true, null, null)
            assertEquals(testEmail, viewModel.uiState.value.email)
            assertEquals(testName, viewModel.uiState.value.fullName)

            // Test with nulls when initial state has nulls
            viewModel.setSocialLoginData(false, null, null)
            assertEquals(false, viewModel.uiState.value.isSocialLoginFlow)
            assertEquals(
                testEmail,
                viewModel.uiState.value.email
            ) // Should remain if not explicitly cleared
            assertEquals(
                testName,
                viewModel.uiState.value.fullName
            ) // Should remain if not explicitly cleared
        }


    @Test
    fun `when OnGoogleSignUpClicked then Google Sign-In intent is launched`() = testScope.runTest {
        // Given: A mocked Google Sign-In intent
        val signInIntent = mockk<Intent>()
        coEvery { googleSignInDataSource.getSignInIntent() } returns signInIntent

        val launchedIntents = mutableListOf<Intent>()
        val job = launch { viewModel.launchGoogleSignIn.toList(launchedIntents) }

        // When: OnGoogleSignUpClicked event is sent
        viewModel.onEvent(SignUpEvent.OnGoogleSignUpClicked)
        advanceUntilIdle()

        // Then: The Google Sign-In intent should be launched
        assertTrue(launchedIntents.isNotEmpty())
        assertEquals(signInIntent, launchedIntents.first())
        job.cancel()
    }

    @Test
    fun `when OnGoogleSignInResult is successful then user is registered with Google and navigates to home`() =
        testScope.runTest {
            // Given: A successful Google sign-in result and mock Firebase user
            val intentData = mockk<Intent>()
            val idToken = "google_id_token"
            every { firebaseAuth.currentUser } returns firebaseUser
            every { firebaseUser.uid } returns "googleUid"
            every { firebaseUser.email } returns "google@example.com"
            every { firebaseUser.displayName } returns "Google User"
            coEvery { googleSignInDataSource.handleSignInResult(intentData) } returns success(
                idToken
            )
            coEvery { registerWithGoogleUseCase(idToken) } returns success(
                User("googleUid", "Google User", null, "google@example.com", null)
            )

            val emittedEvents = mutableListOf<SignUpEvent>()
            val job = launch { viewModel.event.toList(emittedEvents) }

            // When: OnGoogleSignInResult event is sent
            viewModel.onEvent(SignUpEvent.OnGoogleSignInResult(intentData))
            advanceUntilIdle()

            // Then: The UI state reflects success, registerWithGoogleUseCase is called, and navigation/message events are emitted
            val uiState = viewModel.uiState.first()
            assertTrue(uiState.isSuccess)
            assertFalse(uiState.isLoading)
            coVerify(exactly = 1) { registerWithGoogleUseCase(idToken) }
            assertTrue(emittedEvents.any { it is SignUpEvent.NavigateToHome })
            assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Registro con Google exitoso. ¡Bienvenido/a!" })
            job.cancel()
        }

    @Test
    fun `when OnGoogleSignInResult fails then an error message is emitted`() = testScope.runTest {
        // Given: A failed Google sign-in result
        val intentData = mockk<Intent>()
        val exception = Exception("Google sign-in error")
        coEvery { googleSignInDataSource.handleSignInResult(intentData) } returns failure(exception)

        val emittedEvents = mutableListOf<SignUpEvent>()
        val job = launch { viewModel.event.toList(emittedEvents) }

        // When: OnGoogleSignInResult event is sent
        viewModel.onEvent(SignUpEvent.OnGoogleSignInResult(intentData))
        advanceUntilIdle()

        // Then: The UI state reflects failure and an error message is emitted
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isSuccess)
        assertFalse(uiState.isLoading)
        assertEquals("Google sign-in error", uiState.errorMessage)
        assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Google sign-in error" })
        job.cancel()
    }

    @Test
    fun `when OnGoogleSignInResult leads to Firebase registration failure then a specific error message is emitted`() =
        testScope.runTest {
            // Given: Successful Google sign-in but registerWithGoogleUseCase returns a FirebaseAuthUserCollisionException
            val intentData = mockk<Intent>()
            val idToken = "google_id_token"
            val exception = mockk<FirebaseAuthUserCollisionException>()
            every { exception.message } returns "The email address is already in use by another account."
            coEvery { googleSignInDataSource.handleSignInResult(intentData) } returns success(
                idToken
            )
            coEvery { registerWithGoogleUseCase(idToken) } returns failure(exception)

            val emittedEvents = mutableListOf<SignUpEvent>()
            val job = launch { viewModel.event.toList(emittedEvents) }

            // When: OnGoogleSignInResult event is sent
            viewModel.onEvent(SignUpEvent.OnGoogleSignInResult(intentData))
            advanceUntilIdle()

            // Then: The UI state reflects failure and a specific error message about existing account is emitted
            val uiState = viewModel.uiState.first()
            assertFalse(uiState.isSuccess)
            assertFalse(uiState.isLoading)
            assertEquals(
                "El correo electrónico ya está registrado con otra cuenta.",
                uiState.errorMessage
            )
            assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "El correo electrónico ya está registrado con otra cuenta." })
            job.cancel()
        }

// Tests para registro Social (Facebook)


    @Test
    fun `when OnFacebookSignUpClicked then a Starting Facebook login message is emitted`() =
        testScope.runTest {
            val emittedEvents = mutableListOf<SignUpEvent>()
            val job = launch { viewModel.event.toList(emittedEvents) }

            // When: OnFacebookSignUpClicked event is sent
            viewModel.onEvent(SignUpEvent.OnFacebookSignUpClicked)
            advanceUntilIdle()

            // Then: The UI state reflects loading and a message about starting Facebook login is emitted
            val uiState = viewModel.uiState.first()
            assertTrue(uiState.isLoading)
            assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Iniciando sesión con Facebook..." })
            job.cancel()
        }

    @Test
    fun `when Facebook accessTokenChannel emits success then user is registered with Facebook and navigates to home`() =
        testScope.runTest {
            // Given: Facebook access token channel emits a success result and mock Firebase user
            val accessToken = "facebook_access_token"
            every { firebaseAuth.currentUser } returns firebaseUser
            every { firebaseUser.uid } returns "fbUid"
            every { firebaseUser.email } returns "fb@example.com"
            every { firebaseUser.displayName } returns "FB User"
            every { facebookSignInDataSource.accessTokenChannel } returns flowOf(success(accessToken))
            coEvery { registerWithFacebookUseCase(accessToken) } returns success(
                User("fbUid", "FB User", null, "fb@example.com", null)
            )

            // Re-initialize ViewModel to pick up the mocked accessTokenChannel
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

            // When: Coroutine advances and accessTokenChannel emits
            advanceUntilIdle()

            // Then: The UI state reflects success, registerWithFacebookUseCase is called, and navigation/message events are emitted
            val uiState = viewModel.uiState.first()
            assertTrue(uiState.isSuccess)
            assertFalse(uiState.isLoading)
            coVerify(exactly = 1) { registerWithFacebookUseCase(accessToken) }
            assertTrue(emittedEvents.any { it is SignUpEvent.NavigateToHome })
            assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Registro con Facebook exitoso. ¡Bienvenido/a!" })
            job.cancel()
        }

    @Test
    fun `when Facebook accessTokenChannel emits failure then an error message is emitted`() =
        runTest {
            // Given: Facebook access token channel emits a failure result
            val exception = Exception("Facebook login failed")
            every { facebookSignInDataSource.accessTokenChannel } returns flowOf(failure(exception))

            // Re-initialize ViewModel to pick up the mocked accessTokenChannel
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

            // When: Coroutine advances and accessTokenChannel emits a failure
            withTimeout(5000) { // Add a timeout to prevent infinite wait if event is not emitted
                while (!emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Facebook login failed" }) {
                    kotlinx.coroutines.yield()
                }
            }

            // Then: The UI state reflects failure and an error message is emitted
            val uiState = viewModel.uiState.value
            assertFalse(uiState.isSuccess)
            assertFalse(uiState.isLoading)
            assertEquals("Facebook login failed", uiState.errorMessage)
            assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Facebook login failed" })
            job.cancel()
        }

// Tests para completar perfil (flujo social)


    @Test
    fun `when OnRegisterClicked for social flow then user profile is completed successfully and navigates to home`() =
        testScope.runTest {
            // Given: Social login data is set and valid username/birthday are provided
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

            // Given: authRepository.updateUserProfile returns success
            val updatedUser = User(
                id = uid,
                name = socialName,
                username = username,
                email = socialEmail,
                birthday = birthday
            )
            coEvery { authRepository.updateUserProfile(uid, updatedUser) } returns success(Unit)

            val emittedEvents = mutableListOf<SignUpEvent>()
            val job = launch { viewModel.event.toList(emittedEvents) }

            // When: OnRegisterClicked event is sent
            viewModel.onEvent(SignUpEvent.OnRegisterClicked)
            advanceUntilIdle()

            // Then: The UI state reflects success, updateUserProfile is called, and navigation/message events are emitted
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
    fun `when completeUserProfile is called with null currentUser then an error message is emitted`() =
        testScope.runTest {
            // Given: currentUser is null
            every { firebaseAuth.currentUser } returns null
            viewModel.setSocialLoginData(true, "social@example.com", "Social User")
            viewModel.onEvent(SignUpEvent.OnUsernameChanged("user"))
            viewModel.onEvent(SignUpEvent.OnBirthdayChanged("01/01/2000"))
            advanceUntilIdle()

            val emittedEvents = mutableListOf<SignUpEvent>()
            val job = launch { viewModel.event.toList(emittedEvents) }

            // When: OnRegisterClicked event is sent (which triggers completeUserProfile internally for social flow)
            viewModel.onEvent(SignUpEvent.OnRegisterClicked)
            advanceUntilIdle()

            // Then: The UI state reflects failure and an error message about unauthenticated user is emitted
            val uiState = viewModel.uiState.first()
            assertFalse(uiState.isLoading)
            assertEquals("Usuario no autenticado para completar el perfil.", uiState.errorMessage)
            assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Usuario no autenticado para completar el perfil." })
            job.cancel()
        }

    @Test
    fun `when completeUserProfile handles updateProfile failure then an error message is emitted`() =
        testScope.runTest {
            // Given: Social login data and valid profile data, but authRepository.updateUserProfile returns a failure
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

            val updatedUser = User(
                id = uid,
                name = socialName,
                username = username,
                email = socialEmail,
                birthday = birthday
            )
            coEvery {
                authRepository.updateUserProfile(
                    uid,
                    updatedUser
                )
            } returns failure(exception)

            val emittedEvents = mutableListOf<SignUpEvent>()
            val job = launch { viewModel.event.toList(emittedEvents) }

            // When: OnRegisterClicked event is sent (triggering completeUserProfile)
            viewModel.onEvent(SignUpEvent.OnRegisterClicked)
            advanceUntilIdle()

            // Then: The UI state reflects failure and a network error message is emitted
            val uiState = viewModel.uiState.first()
            assertFalse(uiState.isSuccess)
            assertFalse(uiState.isLoading)
            assertEquals("Network error", uiState.errorMessage)
            assertTrue(emittedEvents.any { it is SignUpEvent.ShowMessage && it.message == "Network error" })
            job.cancel()
        }

    @Test
    fun `when completeUserProfile and currentUser is null then error message is shown`() =
        testScope.runTest {
            viewModel.setSocialLoginData(true, "social@test.com", "Social User")
            every { firebaseAuth.currentUser } returns null // Mock null current user

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.OnRegisterClicked) // This triggers completeUserProfile

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(
                "Usuario no autenticado para completar el perfil.",
                viewModel.uiState.value.errorMessage
            )
            assertTrue(events.contains(SignUpEvent.ShowMessage("Usuario no autenticado para completar el perfil.")))

            job.cancel()
        }

    @Test
    fun `when completeUserProfile and username is invalid then error message is shown`() =
        testScope.runTest {
            viewModel.setSocialLoginData(true, "social@test.com", "Social User")
            // Usar FirebaseUser de Firebase SDK
            val mockFirebaseUser = mockk<FirebaseUser>()
            every { firebaseAuth.currentUser } returns mockFirebaseUser
            every { mockFirebaseUser.uid } returns "some_uid"
            every { mockFirebaseUser.email } returns "test@social.com"

            every { signUpValidator.isValidUsername(any()) } returns false

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.OnRegisterClicked)

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(
                "El nombre de usuario no puede estar vacío.",
                viewModel.uiState.value.errorMessage
            )
            assertTrue(events.contains(SignUpEvent.ShowMessage("El nombre de usuario no puede estar vacío.")))

            job.cancel()
        }

    @Test
    fun `when completeUserProfile and fullName is invalid then error message is shown`() =
        testScope.runTest {
            viewModel.setSocialLoginData(true, "social@test.com", "Social User")
            // Usar FirebaseUser de Firebase SDK
            val mockFirebaseUser = mockk<FirebaseUser>()
            every { firebaseAuth.currentUser } returns mockFirebaseUser
            every { mockFirebaseUser.uid } returns "some_uid"
            every { mockFirebaseUser.email } returns "test@social.com"

            every { signUpValidator.isValidUsername(any()) } returns true
            every { signUpValidator.isValidFullName(any()) } returns false

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.OnRegisterClicked)

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(
                "El nombre completo no puede estar vacío.",
                viewModel.uiState.value.errorMessage
            )
            assertTrue(events.contains(SignUpEvent.ShowMessage("El nombre completo no puede estar vacío.")))

            job.cancel()
        }


    @Test
    fun `when completeUserProfile and birthday is invalid then error message is shown`() =
        testScope.runTest {
            viewModel.setSocialLoginData(true, "social@test.com", "Social User")
            // Usar FirebaseUser de Firebase SDK
            val mockFirebaseUser = mockk<FirebaseUser>()
            every { firebaseAuth.currentUser } returns mockFirebaseUser
            every { mockFirebaseUser.uid } returns "some_uid"
            every { mockFirebaseUser.email } returns "test@social.com"

            every { signUpValidator.isValidUsername(any()) } returns true
            every { signUpValidator.isValidFullName(any()) } returns true
            every { signUpValidator.isValidBirthday(any()) } returns false

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.OnRegisterClicked)

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertTrue(viewModel.uiState.value.isBirthdayInvalid)
            assertEquals(
                "Ingresa una fecha de nacimiento válida.",
                viewModel.uiState.value.errorMessage
            )
            assertTrue(events.contains(SignUpEvent.ShowMessage("Ingresa una fecha de nacimiento válida.")))

            job.cancel()
        }

    @Test
    fun `when completeUserProfile succeeds then uiState is updated and navigates to home`() =
        testScope.runTest {
            viewModel.setSocialLoginData(true, "social@test.com", "Social User")
            // Usar FirebaseUser de Firebase SDK
            val mockFirebaseUser = mockk<FirebaseUser>()
            every { firebaseAuth.currentUser } returns mockFirebaseUser
            every { mockFirebaseUser.uid } returns "some_uid"
            every { mockFirebaseUser.email } returns "test@social.com"

            every { signUpValidator.isValidUsername(any()) } returns true
            every { signUpValidator.isValidFullName(any()) } returns true
            every { signUpValidator.isValidBirthday(any()) } returns true
            // Usar tu función auxiliar 'success'
            coEvery { authRepository.updateUserProfile(any(), any()) } returns success(Unit)

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.OnRegisterClicked)

            advanceUntilIdle()

            coVerify { authRepository.updateUserProfile(any(), any()) }
            assertFalse(viewModel.uiState.value.isLoading)
            assertTrue(viewModel.uiState.value.isSuccess)
            assertTrue(events.contains(SignUpEvent.NavigateToHome))
            assertTrue(events.contains(SignUpEvent.ShowMessage("Perfil completado exitosamente.")))

            job.cancel()
        }

    @Test
    fun `when completeUserProfile fails then uiState is updated with error and message is shown`() =
        testScope.runTest {
            viewModel.setSocialLoginData(true, "social@test.com", "Social User")
            // Usar FirebaseUser de Firebase SDK
            val mockFirebaseUser = mockk<FirebaseUser>()
            every { firebaseAuth.currentUser } returns mockFirebaseUser
            every { mockFirebaseUser.uid } returns "some_uid"
            every { mockFirebaseUser.email } returns "test@social.com"

            every { signUpValidator.isValidUsername(any()) } returns true
            every { signUpValidator.isValidFullName(any()) } returns true
            every { signUpValidator.isValidBirthday(any()) } returns true

            val exception = Exception("Profile update failed")
            // Usar tu función auxiliar 'failure'
            coEvery { authRepository.updateUserProfile(any(), any()) } returns failure(
                exception,
                exception.message
            )

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.OnRegisterClicked)

            advanceUntilIdle()

            coVerify { authRepository.updateUserProfile(any(), any()) }
            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.isSuccess)
            assertEquals("Profile update failed", viewModel.uiState.value.errorMessage)
            assertTrue(events.contains(SignUpEvent.ShowMessage("Profile update failed")))

            job.cancel()
        }


    // Otros tests (navegación, etc.)

    @Test
    fun `when onBackPressed then NavigateBack event is emitted`() = testScope.runTest {
        val emittedEvents = mutableListOf<SignUpEvent>()
        val job = launch { viewModel.event.toList(emittedEvents) }

        // When: onBackPressed is called
        viewModel.onBackPressed()
        advanceUntilIdle()

        // Then: A NavigateBack event should be emitted
        assertTrue(emittedEvents.any { it is SignUpEvent.NavigateBack })
        job.cancel()
    }

    @Test
    fun `when setSocialLoginData is called then uiState is correctly updated`() =
        testScope.runTest {
            // Given: Social login data
            val socialEmail = "social@example.com"
            val socialName = "Social Name"

            // When: setSocialLoginData is called
            viewModel.setSocialLoginData(true, socialEmail, socialName)
            advanceUntilIdle()

            // Then: The uiState reflects the social login flow and provided data
            val uiState = viewModel.uiState.first()
            assertTrue(uiState.isSocialLoginFlow)
            assertEquals(socialEmail, uiState.email)
            assertEquals(socialName, uiState.fullName)
        }

    @Test
    fun `when setSocialLoginData is called with nulls then existing uiState data is not overwritten`() =
        testScope.runTest {
            // Given: Initial email and full name are set in uiState
            viewModel.onEvent(SignUpEvent.OnEmailChanged("initial@example.com"))
            viewModel.onEvent(SignUpEvent.OnFullNameChanged("Initial Name"))
            advanceUntilIdle()

            // When: setSocialLoginData is called with null values for email and full name
            viewModel.setSocialLoginData(true, null, null)
            advanceUntilIdle()

            // Then: The uiState reflects social login flow, but email and full name remain unchanged
            val uiState = viewModel.uiState.first()
            assertTrue(uiState.isSocialLoginFlow)
            assertEquals("initial@example.com", uiState.email)
            assertEquals("Initial Name", uiState.fullName)
        }

    //Tests for onEvent


    @Test
    fun `when OnEmailChanged then uiState email is updated and isEmailInvalid is false`() =
        testScope.runTest {
            val newEmail = "new@example.com"
            viewModel.onEvent(SignUpEvent.OnEmailChanged(newEmail))
            assertEquals(newEmail, viewModel.uiState.value.email)
            assertFalse(viewModel.uiState.value.isEmailInvalid)
        }

    @Test
    fun `when OnPasswordChanged then uiState password is updated and isPasswordInvalid is false`() =
        testScope.runTest {
            val newPassword = "newpassword123"
            viewModel.onEvent(SignUpEvent.OnPasswordChanged(newPassword))
            assertEquals(newPassword, viewModel.uiState.value.password)
            assertFalse(viewModel.uiState.value.isPasswordInvalid)
        }

    @Test
    fun `when OnConfirmPasswordChanged then uiState confirmPassword is updated and doPasswordsMismatch is false`() =
        testScope.runTest {
            val newConfirmPassword = "newpassword123"
            viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged(newConfirmPassword))
            assertEquals(newConfirmPassword, viewModel.uiState.value.confirmPassword)
            assertFalse(viewModel.uiState.value.doPasswordsMismatch)
        }


    @Test
    fun `when OnBirthdayChanged then uiState birthday is updated and isBirthdayInvalid is false`() =
        testScope.runTest {
            val newBirthday = "01/01/1995"
            viewModel.onEvent(SignUpEvent.OnBirthdayChanged(newBirthday))
            assertEquals(newBirthday, viewModel.uiState.value.birthday)
            assertFalse(viewModel.uiState.value.isBirthdayInvalid)
        }

    @Test
    fun `when OnRegisterClicked and isSocialLoginFlow is true then completeUserProfile is called`() =
        testScope.runTest {
            viewModel.setSocialLoginData(true, null, null) // Set social login flow
            // Usar FirebaseUser de Firebase SDK
            val mockFirebaseUser = mockk<FirebaseUser>()
            every { firebaseAuth.currentUser } returns mockFirebaseUser
            every { mockFirebaseUser.uid } returns "some_uid"
            every { mockFirebaseUser.email } returns "test@social.com"
            // Usar tu función auxiliar 'success'
            coEvery { authRepository.updateUserProfile(any(), any()) } returns success(Unit)

            viewModel.onEvent(SignUpEvent.OnRegisterClicked)
            advanceUntilIdle()
            coVerify { authRepository.updateUserProfile(any(), any()) }
        }

    @Test
    fun `when OnRegisterClicked and isSocialLoginFlow is false then onSignupClicked is called`() =
        testScope.runTest {
            viewModel.setSocialLoginData(false, null, null) // Set standard signup flow

            // Mock dependencies for onSignupClicked to succeed
            every { signUpValidator.isValidEmail(any()) } returns true
            every { signUpValidator.isValidPassword(any()) } returns true
            every { signUpValidator.isValidUsername(any()) } returns true
            every { signUpValidator.isValidFullName(any()) } returns true
            every { signUpValidator.isValidBirthday(any()) } returns true

            // Usar FirebaseUser de Firebase SDK
            val mockUser = mockk<FirebaseUser>()
            // Usar tu función auxiliar 'success'
            // FIX: The createUserUseCase returns Result<Unit>, not Result<FirebaseUser>.
            // So, change success(mockUser) to success(Unit)
            coEvery { createUserUseCase(any(), any()) } returns success(Unit)

            viewModel.onEvent(SignUpEvent.OnRegisterClicked)
            advanceUntilIdle()
            coVerify { createUserUseCase(any(), any()) }
        }


    @Test
    fun `when OnGoogleSignInResult and isSocialLoginFlow is true then message is shown`() =
        testScope.runTest {
            viewModel.setSocialLoginData(true, null, null)

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.OnGoogleSignInResult(mockk()))

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertTrue(events.contains(SignUpEvent.ShowMessage("Ya autenticado socialmente. Completa tu perfil.")))

            job.cancel()
        }

    @Test
    fun `when OnFacebookActivityResult and isSocialLoginFlow is true then message is shown`() =
        testScope.runTest {
            viewModel.setSocialLoginData(true, null, null)

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.OnFacebookActivityResult(1, 1, null))

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertTrue(events.contains(SignUpEvent.ShowMessage("Ya autenticado socialmente. Completa tu perfil.")))

            job.cancel()
        }

    @Test
    fun `when onEvent is called with an unhandled event then nothing happens`() =
        testScope.runTest {
            val initialUiState = viewModel.uiState.value
            val events = mutableListOf<SignUpEvent>()

            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.NavigateToHome)

            advanceUntilIdle()

            // Asegura que el estado UI no cambió
            assertEquals(initialUiState, viewModel.uiState.value)

            // Asegura que no se emitieron eventos
            assertTrue(events.isEmpty())

            job.cancelAndJoin() // ✅ CIERRA el collect para evitar corrutina colgando
        }


// --- Tests for onSignupClicked (Validation paths) ---

    @Test
    fun `when OnRegisterClicked and email is invalid then isEmailInvalid is true and message is shown`() =
        testScope.runTest {
            every { signUpValidator.isValidEmail(any()) } returns false

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.OnRegisterClicked)

            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isEmailInvalid)
            assertEquals("Ingresa un email válido.", viewModel.uiState.value.errorMessage)
            assertTrue(events.contains(SignUpEvent.ShowMessage("Ingresa un email válido.")))

            job.cancel()
        }

    @Test
    fun `when OnRegisterClicked and password is invalid then isPasswordInvalid is true and message is shown`() =
        testScope.runTest {
            every { signUpValidator.isValidEmail(any()) } returns true // Pass email validation
            every { signUpValidator.isValidPassword(any()) } returns false

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.OnRegisterClicked)

            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isPasswordInvalid)
            assertEquals(
                "La contraseña debe tener al menos 8 caracteres.",
                viewModel.uiState.value.errorMessage
            )
            assertTrue(events.contains(SignUpEvent.ShowMessage("La contraseña debe tener al menos 8 caracteres.")))

            job.cancel()
        }

    @Test
    fun `when OnRegisterClicked and passwords do not match then doPasswordsMismatch is true and message is shown`() =
        testScope.runTest {
            viewModel.onEvent(SignUpEvent.OnEmailChanged("test@example.com"))
            viewModel.onEvent(SignUpEvent.OnPasswordChanged("password123"))
            viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged("differentpassword"))

            every { signUpValidator.isValidEmail(any()) } returns true
            every { signUpValidator.isValidPassword(any()) } returns true

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.OnRegisterClicked)

            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.doPasswordsMismatch)
            assertEquals("Las contraseñas no coinciden.", viewModel.uiState.value.errorMessage)
            assertTrue(events.contains(SignUpEvent.ShowMessage("Las contraseñas no coinciden.")))

            job.cancel()
        }

    @Test
    fun `when OnRegisterClicked and username is invalid then errorMessage is set and message is shown`() =
        testScope.runTest {
            viewModel.onEvent(SignUpEvent.OnEmailChanged("test@example.com"))
            viewModel.onEvent(SignUpEvent.OnPasswordChanged("password123"))
            viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged("password123"))
            every { signUpValidator.isValidEmail(any()) } returns true
            every { signUpValidator.isValidPassword(any()) } returns true
            every { signUpValidator.isValidUsername(any()) } returns false

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.OnRegisterClicked)

            advanceUntilIdle()

            assertEquals(
                "El nombre de usuario no puede estar vacío.",
                viewModel.uiState.value.errorMessage
            )
            assertTrue(events.contains(SignUpEvent.ShowMessage("El nombre de usuario no puede estar vacío.")))

            job.cancel()
        }

    @Test
    fun `when OnRegisterClicked and fullName is invalid then errorMessage is set and message is shown`() =
        testScope.runTest {
            viewModel.onEvent(SignUpEvent.OnEmailChanged("test@example.com"))
            viewModel.onEvent(SignUpEvent.OnPasswordChanged("password123"))
            viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged("password123"))
            viewModel.onEvent(SignUpEvent.OnUsernameChanged("validuser"))
            every { signUpValidator.isValidEmail(any()) } returns true
            every { signUpValidator.isValidPassword(any()) } returns true
            every { signUpValidator.isValidUsername(any()) } returns true
            every { signUpValidator.isValidFullName(any()) } returns false

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.OnRegisterClicked)

            advanceUntilIdle()

            assertEquals(
                "El nombre completo no puede estar vacío.",
                viewModel.uiState.value.errorMessage
            )
            assertTrue(events.contains(SignUpEvent.ShowMessage("El nombre completo no puede estar vacío.")))

            job.cancel()
        }

    @Test
    fun `when OnRegisterClicked and birthday is invalid then isBirthdayInvalid is true and message is shown`() =
        testScope.runTest {
            viewModel.onEvent(SignUpEvent.OnEmailChanged("test@example.com"))
            viewModel.onEvent(SignUpEvent.OnPasswordChanged("password123"))
            viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged("password123"))
            viewModel.onEvent(SignUpEvent.OnUsernameChanged("validuser"))
            viewModel.onEvent(SignUpEvent.OnFullNameChanged("Valid Fullname"))
            every { signUpValidator.isValidEmail(any()) } returns true
            every { signUpValidator.isValidPassword(any()) } returns true
            every { signUpValidator.isValidUsername(any()) } returns true
            every { signUpValidator.isValidFullName(any()) } returns true
            every { signUpValidator.isValidBirthday(any()) } returns false

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.OnRegisterClicked)

            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isBirthdayInvalid)
            assertEquals(
                "Ingresa una fecha de nacimiento válida.",
                viewModel.uiState.value.errorMessage
            )
            assertTrue(events.contains(SignUpEvent.ShowMessage("Ingresa una fecha de nacimiento válida.")))

            job.cancel()
        }

    @Test
    fun `when createUserUseCase fails with FirebaseAuthUserCollisionException then correct message is shown`() =
        testScope.runTest {
            viewModel.onEvent(SignUpEvent.OnEmailChanged("test@example.com"))
            viewModel.onEvent(SignUpEvent.OnPasswordChanged("password123"))
            viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged("password123"))
            viewModel.onEvent(SignUpEvent.OnFullNameChanged("Test User"))
            viewModel.onEvent(SignUpEvent.OnUsernameChanged("testuser"))
            viewModel.onEvent(SignUpEvent.OnBirthdayChanged("01/01/2000"))

            val exception = mockk<FirebaseAuthUserCollisionException> {
                every { message } returns "Email already in use"
            }
            // Usar tu función auxiliar 'failure'
            coEvery { createUserUseCase(any(), any()) } returns failure(
                exception,
                exception.message
            )

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.OnRegisterClicked)

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.isSuccess)
            assertEquals(
                "El correo electrónico ya está registrado.",
                viewModel.uiState.value.errorMessage
            )
            assertTrue(events.contains(SignUpEvent.ShowMessage("El correo electrónico ya está registrado.")))

            job.cancel()
        }

    @Test
    fun `when createUserUseCase fails with FirebaseAuthWeakPasswordException then correct message is shown`() =
        testScope.runTest {
            viewModel.onEvent(SignUpEvent.OnEmailChanged("test@example.com"))
            viewModel.onEvent(SignUpEvent.OnPasswordChanged("weak"))
            viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged("weak"))
            viewModel.onEvent(SignUpEvent.OnFullNameChanged("Test User"))
            viewModel.onEvent(SignUpEvent.OnUsernameChanged("testuser"))
            viewModel.onEvent(SignUpEvent.OnBirthdayChanged("01/01/2000"))

            val exception = mockk<FirebaseAuthWeakPasswordException> {
                every { message } returns "Password is too weak"
            }

            val mockResult: Result<Unit> =
                Result.Failure(exception = exception, message = exception.message)
            coEvery { createUserUseCase(any(), any()) } returns mockResult

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.OnRegisterClicked)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.isSuccess)
            assertEquals(
                "La contraseña es demasiado débil. Usa una combinación de letras, números y símbolos.",
                viewModel.uiState.value.errorMessage
            )
            assertTrue(events.contains(SignUpEvent.ShowMessage("La contraseña es demasiado débil. Usa una combinación de letras, números y símbolos.")))

            job.cancel()
        }

    @Test
    fun `when createUserUseCase fails with FirebaseAuthInvalidCredentialsException then correct message is shown`() =
        testScope.runTest {
            viewModel.onEvent(SignUpEvent.OnEmailChanged("invalid-email"))
            viewModel.onEvent(SignUpEvent.OnPasswordChanged("password123"))
            viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged("password123"))
            viewModel.onEvent(SignUpEvent.OnFullNameChanged("Test User"))
            viewModel.onEvent(SignUpEvent.OnUsernameChanged("testuser"))
            viewModel.onEvent(SignUpEvent.OnBirthdayChanged("01/01/2000"))

            val exception = Exception("Invalid email format") // 🔁 reemplazo
            coEvery { createUserUseCase(any(), any()) } returns failure(
                exception,
                exception.message
            )

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.OnRegisterClicked)

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.isSuccess)
            assertEquals(
                "Error de registro: Invalid email format.",
                viewModel.uiState.value.errorMessage
            )
            assertTrue(events.contains(SignUpEvent.ShowMessage("Error de registro: Invalid email format.")))

            job.cancel()
        }


    //    --- Tests for handleGoogleSignInResult (Error handling) ---

    @Test
    fun `when registerWithGoogleUseCase fails with FirebaseAuthUserCollisionException then correct message is shown`() =
        testScope.runTest {
            val mockIntent = mockk<Intent>()
            val idToken = "some_id_token"

            coEvery { googleSignInDataSource.handleSignInResult(mockIntent) } returns success(
                idToken
            )

            val exception = object : Exception("Email already in use") {} // 🔁 reemplazo
            coEvery { registerWithGoogleUseCase(idToken) } returns failure(
                exception,
                exception.message
            )

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.OnGoogleSignInResult(mockIntent))

            advanceUntilIdle()

            // Este test debe ajustarse a lo que devuelve tu ViewModel con un Exception genérico
            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.isSuccess)
            assertEquals(
                "Email already in use",
                viewModel.uiState.value.errorMessage
            )
            assertTrue(events.contains(SignUpEvent.ShowMessage("Email already in use")))

            job.cancel()
        }


    @Test
    fun `when registerWithGoogleUseCase fails with generic exception then correct message is shown`() =
        testScope.runTest {
            val mockIntent = mockk<Intent>()
            val idToken = "some_id_token"

            coEvery { googleSignInDataSource.handleSignInResult(mockIntent) } returns success(
                idToken
            )

            val exception = Exception("Generic Google auth error")
            coEvery { registerWithGoogleUseCase(idToken) } returns failure(
                exception,
                exception.message
            )

            val events = mutableListOf<SignUpEvent>()
            val job = launch {
                viewModel.event.collect { events.add(it) }
            }

            viewModel.onEvent(SignUpEvent.OnGoogleSignInResult(mockIntent))

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.isSuccess)

            // ✅ Cambia el mensaje esperado
            assertEquals("Generic Google auth error", viewModel.uiState.value.errorMessage)
            assertTrue(events.contains(SignUpEvent.ShowMessage("Generic Google auth error")))

            job.cancel()
        }

    @Test
    fun `when handleGoogleSignUpClicked and not social login then emit launchGoogleIntent`() =
        testScope.runTest {
            // Given
            val mockIntent = mockk<Intent>()
            every { googleSignInDataSource.getSignInIntent() } returns mockIntent

            // Create a job to collect the emitted intent
            val resultDeferred = async { viewModel.launchGoogleSignIn.first() }

            // When
            viewModel.onEvent(SignUpEvent.OnGoogleSignUpClicked)

            // Then
            val emittedIntent = resultDeferred.await()
            assertEquals(mockIntent, emittedIntent)
        }


    @Test
    fun `when handleGoogleSignUpClicked and isSocialLoginFlow then emit message only`() =
        testScope.runTest {
            // Given
            viewModel.setSocialLoginData(true, null, null)

            // When
            viewModel.onEvent(SignUpEvent.OnGoogleSignUpClicked)
            advanceUntilIdle()

            // Then
            val event = viewModel.event.first()
            assertTrue(event is SignUpEvent.ShowMessage)
            assertEquals(
                "Ya autenticado socialmente. Completa tu perfil.",
                (event as SignUpEvent.ShowMessage).message
            )
        }

    @Test
    fun `when handleFacebookSignUpClicked and not social login then loading is true and message emitted`() =
        testScope.runTest {
            // When
            viewModel.onEvent(SignUpEvent.OnFacebookSignUpClicked)
            advanceUntilIdle()

            // Then
            val uiState = viewModel.uiState.first()
            assertTrue(uiState.isLoading)

            val event = viewModel.event.first()
            assertTrue(event is SignUpEvent.ShowMessage)
            assertEquals(
                "Iniciando sesión con Facebook...",
                (event as SignUpEvent.ShowMessage).message
            )
        }

    @Test
    fun `when handleFacebookSignUpClicked and isSocialLoginFlow then show already authenticated message`() =
        testScope.runTest {
            // Given
            viewModel.setSocialLoginData(true, null, null)

            // When
            viewModel.onEvent(SignUpEvent.OnFacebookSignUpClicked)
            advanceUntilIdle()

            // Then
            val event = viewModel.event.first()
            assertTrue(event is SignUpEvent.ShowMessage)
            assertEquals(
                "Ya autenticado socialmente. Completa tu perfil.",
                (event as SignUpEvent.ShowMessage).message
            )
        }

    @Test
    fun `when onSignupClicked and FirebaseAuthWeakPasswordException then show weak password message`() =
        testScope.runTest {
            // Given
            every { signUpValidator.isValidEmail(any()) } returns true
            every { signUpValidator.isValidPassword(any()) } returns true
            every { signUpValidator.isValidUsername(any()) } returns true
            every { signUpValidator.isValidFullName(any()) } returns true
            every { signUpValidator.isValidBirthday(any()) } returns true

            val weakException = mockk<FirebaseAuthWeakPasswordException>(relaxed = true)
            coEvery { createUserUseCase(any(), any()) } returns failure(weakException)

            viewModel.onEvent(SignUpEvent.OnEmailChanged("email@example.com"))
            viewModel.onEvent(SignUpEvent.OnPasswordChanged("password123"))
            viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged("password123"))
            viewModel.onEvent(SignUpEvent.OnUsernameChanged("username"))
            viewModel.onEvent(SignUpEvent.OnFullNameChanged("Full Name"))
            viewModel.onEvent(SignUpEvent.OnBirthdayChanged("2000-01-01"))

            // When
            viewModel.onEvent(SignUpEvent.OnRegisterClicked)
            advanceUntilIdle()

            // Then
            val event = viewModel.event.first()
            assertTrue(event is SignUpEvent.ShowMessage)
            assertEquals(
                "La contraseña es demasiado débil. Usa una combinación de letras, números y símbolos.",
                (event as SignUpEvent.ShowMessage).message
            )
        }


    @Test
    fun `when handleFacebookAuthWithFirebase returns FirebaseAuthUserCollisionException then show collision error`() = testScope.runTest {
        // Given
        val collisionException = mockk<FirebaseAuthUserCollisionException>(relaxed = true)
        coEvery { registerWithFacebookUseCase(any()) } returns failure(collisionException)

        viewModel.onEvent(SignUpEvent.OnFacebookSignUpClicked)
        advanceUntilIdle()

        viewModel.setSocialLoginData(false, null, null)

        // Llama al método suspendido internal
        viewModel.handleFacebookAuthWithFirebaseForTest("fake_token")
        advanceUntilIdle()

        // Then
        val event = viewModel.event.first()
        assertTrue(event is SignUpEvent.ShowMessage)
        assertEquals(
            "El correo electrónico ya está registrado con otra cuenta.",
            (event as SignUpEvent.ShowMessage).message
        )
    }
}


