package com.cursoandroid.queermap.ui.forgotpassword

// IMPORTANTE: Asegúrate de importar tu clase Result personalizada y sus helpers
// También las extensiones si las usas directamente en el ViewModel o en las aserciones
import com.cursoandroid.queermap.domain.usecase.auth.SendResetPasswordUseCase
import com.cursoandroid.queermap.util.failure
import com.cursoandroid.queermap.util.success
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test


@ExperimentalCoroutinesApi
class ForgotPasswordViewModelTest {

    @MockK
    private lateinit var sendResetPasswordUseCase: SendResetPasswordUseCase

    @MockK
    private lateinit var forgotPasswordValidator: ForgotPasswordValidator

    private lateinit var viewModel: ForgotPasswordViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = ForgotPasswordViewModel(sendResetPasswordUseCase, forgotPasswordValidator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `sendPasswordReset emits success state and events on successful reset`() =
        testScope.runTest {
            val email = "test@example.com"
            // Validator: email válido
            coEvery { forgotPasswordValidator.isValidEmail(email) } returns true
            // UseCase: éxito, usando tu función helper 'success'
            coEvery { sendResetPasswordUseCase(email) } returns success(Unit)

            val emittedEvents = mutableListOf<ForgotPasswordEvent>()
            val job = launch {
                viewModel.events.toList(emittedEvents)
            }

            viewModel.sendPasswordReset(email)
            advanceUntilIdle()

            val uiState = viewModel.uiState.first()
            // UI: éxito, no cargando
            assertTrue(uiState.isSuccess) // Usando la extensión isSuccess de tu Result
            assertFalse(uiState.isLoading)

            // Eventos: mensaje, navegación
            assertEquals(2, emittedEvents.size)
            assertTrue(emittedEvents[0] is ForgotPasswordEvent.ShowMessage)
            assertEquals(
                "Se ha enviado un correo de restablecimiento de contraseña.",
                (emittedEvents[0] as ForgotPasswordEvent.ShowMessage).message
            )
            assertTrue(emittedEvents[1] is ForgotPasswordEvent.NavigateBack)

            // Verificaciones
            coVerify(exactly = 1) { forgotPasswordValidator.isValidEmail(email) }
            coVerify(exactly = 1) { sendResetPasswordUseCase(email) }

            job.cancel()
        }

    @Test
    fun `sendPasswordReset emits error event if email is invalid`() = testScope.runTest {
        val email = "invalid-email"
        // Validator: email inválido
        coEvery { forgotPasswordValidator.isValidEmail(email) } returns false

        val emittedEvents = mutableListOf<ForgotPasswordEvent>()
        val job = launch {
            viewModel.events.toList(emittedEvents)
        }

        viewModel.sendPasswordReset(email)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        // UI: sin cambio de carga, no éxito
        assertFalse(uiState.isLoading)
        assertFalse(uiState.isSuccess) // Usando la extensión isSuccess de tu Result

        // Eventos: solo mensaje de error
        assertEquals(1, emittedEvents.size)
        assertTrue(emittedEvents.first() is ForgotPasswordEvent.ShowMessage)
        assertEquals(
            "Ingrese un correo válido",
            (emittedEvents.first() as ForgotPasswordEvent.ShowMessage).message
        )

        // Verificaciones: no se llama al UseCase
        coVerify(exactly = 1) { forgotPasswordValidator.isValidEmail(email) }
        coVerify(exactly = 0) { sendResetPasswordUseCase(any()) }

        job.cancel()
    }

    @Test
    fun `sendPasswordReset emits specific error message for FirebaseAuthInvalidUserException`() =
        testScope.runTest {
            val email = "no_registered@example.com"
            // Mockear la excepción de Firebase en lugar de instanciarla directamente
            val mockedException = mockk<FirebaseAuthInvalidUserException>()
            // Si necesitas verificar alguna propiedad específica de la excepción, móckela aquí
            // Por ejemplo, si el ViewModel usara exception.errorCode o exception.message
            // coEvery { mockedException.errorCode } returns "ERROR_USER_NOT_FOUND"

            // Validator: email válido
            coEvery { forgotPasswordValidator.isValidEmail(email) } returns true
            // UseCase: fallo por usuario no encontrado, usando la excepción mockeada y tu función helper 'failure'
            coEvery { sendResetPasswordUseCase(email) } returns failure(mockedException)

            val emittedEvents = mutableListOf<ForgotPasswordEvent>()
            val job = launch {
                viewModel.events.toList(emittedEvents)
            }

            viewModel.sendPasswordReset(email)
            advanceUntilIdle()

            val uiState = viewModel.uiState.first()
            // UI: no cargando, no éxito
            assertFalse(uiState.isLoading)
            assertFalse(uiState.isSuccess) // Usando la extensión isSuccess de tu Result

            // Eventos: solo mensaje de error específico
            assertEquals(1, emittedEvents.size)
            assertTrue(emittedEvents.first() is ForgotPasswordEvent.ShowMessage)
            assertEquals(
                "No hay ninguna cuenta registrada con este correo electrónico.",
                (emittedEvents.first() as ForgotPasswordEvent.ShowMessage).message
            )

            // Verificaciones
            coVerify(exactly = 1) { forgotPasswordValidator.isValidEmail(email) }
            coVerify(exactly = 1) { sendResetPasswordUseCase(email) }

            job.cancel()
        }

    @Test
    fun `sendPasswordReset emits specific error message for FirebaseAuthInvalidCredentialsException`() =
        testScope.runTest {
            val email = "bad_format"
            // Mockear la excepción de Firebase en lugar de instanciarla directamente
            val mockedException = mockk<FirebaseAuthInvalidCredentialsException>()
            // Si necesitas verificar alguna propiedad específica de la excepción, móckela aquí
            // coEvery { mockedException.errorCode } returns "ERROR_INVALID_EMAIL"

            // Validator: email válido (simulamos que pasó la validación inicial del Fragment)
            coEvery { forgotPasswordValidator.isValidEmail(email) } returns true
            // UseCase: fallo por credenciales inválidas (formato), usando la excepción mockeada y tu función helper 'failure'
            coEvery { sendResetPasswordUseCase(email) } returns failure(mockedException)

            val emittedEvents = mutableListOf<ForgotPasswordEvent>()
            val job = launch {
                viewModel.events.toList(emittedEvents)
            }

            viewModel.sendPasswordReset(email)
            advanceUntilIdle()

            val uiState = viewModel.uiState.first()
            // UI: no cargando, no éxito
            assertFalse(uiState.isLoading)
            assertFalse(uiState.isSuccess) // Usando la extensión isSuccess de tu Result

            // Eventos: solo mensaje de error específico
            assertEquals(1, emittedEvents.size)
            assertTrue(emittedEvents.first() is ForgotPasswordEvent.ShowMessage)
            assertEquals(
                "El formato del correo electrónico es inválido.",
                (emittedEvents.first() as ForgotPasswordEvent.ShowMessage).message
            )

            // Verificaciones
            coVerify(exactly = 1) { forgotPasswordValidator.isValidEmail(email) }
            coVerify(exactly = 1) { sendResetPasswordUseCase(email) }

            job.cancel()
        }

    @Test
    fun `sendPasswordReset emits general error message for other exceptions`() = testScope.runTest {
        val email = "test@example.com"
        val exception = Exception("Network error")
        // Validator: email válido
        coEvery { forgotPasswordValidator.isValidEmail(email) } returns true
        // UseCase: fallo genérico, usando tu función helper 'failure'
        coEvery { sendResetPasswordUseCase(email) } returns failure(exception)

        val emittedEvents = mutableListOf<ForgotPasswordEvent>()
        val job = launch {
            viewModel.events.toList(emittedEvents)
        }

        viewModel.sendPasswordReset(email)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        // UI: no cargando, no éxito
        assertFalse(uiState.isLoading)
        assertFalse(uiState.isSuccess) // Usando la extensión isSuccess de tu Result

        // Eventos: solo mensaje de error genérico
        assertEquals(1, emittedEvents.size)
        assertTrue(emittedEvents.first() is ForgotPasswordEvent.ShowMessage)
        assertEquals(
            "Ocurrió un error inesperado. Intenta de nuevo más tarde.",
            (emittedEvents.first() as ForgotPasswordEvent.ShowMessage).message
        )

        // Verificaciones
        coVerify(exactly = 1) { forgotPasswordValidator.isValidEmail(email) }
        coVerify(exactly = 1) { sendResetPasswordUseCase(email) }

        job.cancel()
    }
}