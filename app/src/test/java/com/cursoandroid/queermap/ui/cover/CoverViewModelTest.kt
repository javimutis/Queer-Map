package com.cursoandroid.queermap.ui.cover

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import io.mockk.Runs
import io.mockk.just
import com.cursoandroid.queermap.util.IdlingResourceProvider
import io.mockk.every

@ExperimentalCoroutinesApi
class CoverViewModelTest {

    // Dispatcher de prueba para controlar corrutinas
    private val testDispatcher = StandardTestDispatcher()

    // Mock del IdlingResourceProvider
    private lateinit var mockIdlingResourceProvider: IdlingResourceProvider

    // Se ejecuta antes de cada test
    @Before
    fun setup() {
        // Establece el dispatcher principal para pruebas
        Dispatchers.setMain(testDispatcher)

        // Inicializa el mock
        mockIdlingResourceProvider = mockk {
            // Configurar que las llamadas a increment/decrement no hagan nada en el mock
            every { increment() } just Runs
            every { decrement() } just Runs
        }
    }

    // Se ejecuta después de cada test
    @After
    fun tearDown() {
        // Restablece el dispatcher principal
        Dispatchers.resetMain()
        // Limpiar el mock después de cada test
        clearMocks(mockIdlingResourceProvider)
    }

    // Test: ViewModel inicializa título oculto y llama a increment
    @Test
    fun `when viewmodel is initialized then title is hidden and increment is called`() =
        runTest(testDispatcher) {
            // Pasa el mock al constructor del ViewModel
            val viewModel = CoverViewModel(mockIdlingResourceProvider)

            // Verifica estado inicial
            assertFalse(viewModel.uiState.first().showTitle)

            // Verifica que se llamó a increment en el mock
            verify(exactly = 1) { mockIdlingResourceProvider.increment() }
        }

    // Test: Título se muestra después del delay y llama a decrement
    @Test
    fun `when delay finishes then title is shown and decrement is called`() =
        runTest(testDispatcher) {
            // Pasa el mock al constructor del ViewModel
            val viewModel = CoverViewModel(mockIdlingResourceProvider)

            // Avanza el tiempo virtual
            advanceUntilIdle()

            // Verifica estado final
            assertTrue(viewModel.uiState.first().showTitle)

            // Verifica que se llamó a decrement en el mock
            verify(exactly = 1) { mockIdlingResourceProvider.decrement() }
        }

    // Test: Clic en login actualiza estado
    @Test
    fun `when login button clicked then navigate to login is true`() = runTest {
        // Pasa el mock al constructor del ViewModel
        val viewModel = CoverViewModel(mockIdlingResourceProvider)

        // Llama a la función del ViewModel
        viewModel.onLoginClicked()

        // Verifica estado de navegación
        assertTrue(viewModel.uiState.first().navigateToLogin)
        assertFalse(viewModel.uiState.first().navigateToSignUp)
    }

    // Test: Clic en signup actualiza estado
    @Test
    fun `when signup button clicked then navigate to signup is true`() = runTest {
        // Pasa el mock al constructor del ViewModel
        val viewModel = CoverViewModel(mockIdlingResourceProvider)

        // Llama a la función del ViewModel
        viewModel.onSignUpClicked()

        // Verifica estado de navegación
        assertFalse(viewModel.uiState.first().navigateToLogin)
        assertTrue(viewModel.uiState.first().navigateToSignUp)
    }

    // Test: Estado de navegación se resetea
    @Test
    fun `when navigated then navigation state is reset`() = runTest {
        // Pasa el mock al constructor del ViewModel
        val viewModel = CoverViewModel(mockIdlingResourceProvider)

        // Simula clic y navegación
        viewModel.onLoginClicked()
        viewModel.onNavigated()

        // Verifica reseteo
        assertFalse(viewModel.uiState.first().navigateToLogin)
        assertFalse(viewModel.uiState.first().navigateToSignUp)

        // Otro caso con signup
        viewModel.onSignUpClicked()
        viewModel.onNavigated()
        assertFalse(viewModel.uiState.first().navigateToLogin)
        assertFalse(viewModel.uiState.first().navigateToSignUp)
    }
}