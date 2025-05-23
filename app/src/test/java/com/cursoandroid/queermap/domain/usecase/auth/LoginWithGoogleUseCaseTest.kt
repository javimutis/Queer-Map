package com.cursoandroid.queermap.domain.usecase.auth

import com.cursoandroid.queermap.domain.repository.AuthRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LoginWithGoogleUseCaseTest {

    @RelaxedMockK
    private lateinit var authRepository: AuthRepository

    private lateinit var loginWithGoogleUseCase: LoginWithGoogleUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        loginWithGoogleUseCase = LoginWithGoogleUseCase(authRepository)
    }

    @Test
    fun `when google login succeeds then return true`() = runTest {
        // Given
        val idToken = "valid_google_token"
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns Result.success(true)

        // When
        val result = loginWithGoogleUseCase(idToken)

        // Then
        assertEquals(Result.success(true), result)
    }
    @Test
    fun `when google login fails then return error`() = runTest {
        // Given
        val idToken = "valid_google_token"
        val expectedException = Exception("Login failed")
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns Result.failure(expectedException)

        // When
        val result = loginWithGoogleUseCase(idToken)

        // Then
        assertEquals(Result.failure<Boolean>(expectedException), result)
    }

}