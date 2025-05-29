package com.cursoandroid.queermap.ui.login

import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithEmailUseCase
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithFacebookUseCase
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithGoogleUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

@ExperimentalCoroutinesApi
class LoginViewModelTest {

    // Mocks de las dependencias
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

    // El ViewModel a testear
    private lateinit var viewModel: LoginViewModel

    // Dispatcher de prueba para controlar las corrutinas
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(
            loginWithEmailUseCase,
            loginWithFacebookUseCase,
            loginWithGoogleUseCase,
            authRepository,
            firebaseAuth
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
            // Given
            val email = "test@example.com"
            val password = "password123"
            val user = User("uid123", email, "testuser", "Test User", "01/01/2000")
            coEvery { loginWithEmailUseCase(email, password) } returns Result.success(user)

            // When
            viewModel.loginWithEmail(email, password)
            advanceUntilIdle()

            // Then
            // Verifica el estado de UI
            val uiState = viewModel.uiState.first()
            assertTrue(uiState.isSuccess)
            assertEquals(false, uiState.isLoading)
            assertEquals(null, uiState.errorMessage)

            // Verifica que el evento de navegación se haya emitido
            val emittedEvent = viewModel.event.first()
            assertTrue(emittedEvent is LoginEvent.NavigateToHome)
        }

}
