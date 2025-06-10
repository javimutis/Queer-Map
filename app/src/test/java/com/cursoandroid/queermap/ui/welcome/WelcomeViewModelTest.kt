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
        viewModel = WelcomeViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState is false initially`() = testScope.runTest {
        assertFalse(viewModel.uiState.value)
    }

    @Test
    fun `uiState becomes true after 2000ms delay`() = testScope.runTest {
        assertFalse(viewModel.uiState.value)
        advanceTimeBy(1000)
        assertFalse(viewModel.uiState.value)
        advanceTimeBy(1000)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value)
    }

    @Test
    fun `onNavigated resets uiState to false`() = testScope.runTest {
        val job = launch {
            viewModel.uiState.collect {}
        }
        advanceTimeBy(2000)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value)

        viewModel.onNavigated()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value)
        job.cancel()
    }
}