package com.cursoandroid.queermap.ui.welcome

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class WelcomeViewModelTest {

    private lateinit var viewModel: WelcomeViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // El ViewModel se instancia aquí, lo que dispara el 'init' y el 'delay'.
        // Sin embargo, las corrutinas no se ejecutarán hasta que 'advanceTimeBy' o 'advanceUntilIdle' sean llamados.
        viewModel = WelcomeViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState is false initially`() = testScope.runTest {
        // En este punto, el delay en el init del ViewModel ha comenzado,
        // pero como no hemos avanzado el tiempo, el estado inicial sigue siendo false.
        assertFalse(viewModel.uiState.value)
    }

    @Test
    fun `uiState becomes true after 2000ms delay`() = testScope.runTest {
        // Lanza una corrutina para recolectar el estado
        val states = mutableListOf<Boolean>()
        val job = launch {
            // Este collect también se inicia, pero sin avanzar el tiempo, aún no emite nada
            viewModel.uiState.collect { states.add(it) }
        }

        // Después de lanzar el collect, necesitamos dar tiempo para que la primera emisión (false) ocurra.
        // Aquí no queremos avanzar el tiempo del delay en el init, solo que el StateFlow emita su primer valor.
        // 'runTest' y 'launch' ya manejan esto de forma síncrona en el dispatcher de prueba.
        // Si no se recolecta inmediatamente, es porque el colector no tiene un `scope` o `dispatcher` para ello.
        // Debería recolectar el valor inicial de `false` al momento de suscribirse.

        // Si `states` está vacío aquí, es porque la recolección no es instantánea.
        // `advanceUntilIdle` al principio de este test (después de launch) es el que causa el problema,
        // porque completa el delay del init.

        // La mejor manera de probar el DELAY específicamente es así:

        // 1. Verificar el estado actual ANTES de avanzar el tiempo:
        assertFalse(viewModel.uiState.value) // Debería ser false justo después de la inicialización

        // 2. Avanzar el tiempo por menos del delay:
        advanceTimeBy(1000)
        // El estado debería seguir siendo false
        assertFalse(viewModel.uiState.value) // El ViewModel aún no debería haber cambiado el estado

        // 3. Avanzar el tiempo hasta completar el delay:
        advanceTimeBy(1000) // Total 2000ms
        // Asegura que todas las corrutinas (incluyendo la del delay) se ejecuten.
        advanceUntilIdle()
        // El estado debería haber cambiado a true
        assertTrue(viewModel.uiState.value)

        // Limpia el job de recolección si lo hubieras usado, pero para este test no es necesario el `states.add(it)`
        job.cancel()
    }


    @Test
    fun `onNavigated resets uiState to false`() = testScope.runTest {
        // Primero, hacemos que uiState sea true (simulando la navegación)
        val job = launch {
            viewModel.uiState.collect {} // Necesitamos un colector activo
        }
        advanceTimeBy(2000) // Avanza el tiempo para que uiState se vuelva true
        advanceUntilIdle() // Asegura que la emisión de `true` haya sido procesada

        assertTrue(viewModel.uiState.value) // Verificamos que ahora es true

        // Llama a la función que debería resetear el estado
        viewModel.onNavigated()
        advanceUntilIdle() // Asegura que todas las corrutinas se ejecuten

        // Verifica que uiState ha vuelto a ser false
        assertFalse(viewModel.uiState.value)
        job.cancel()
    }
}