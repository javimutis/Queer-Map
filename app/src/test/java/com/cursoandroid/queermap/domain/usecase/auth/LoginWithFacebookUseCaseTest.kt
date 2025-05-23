package com.cursoandroid.queermap.domain.usecase.auth

import com.cursoandroid.queermap.domain.repository.AuthRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LoginWithFacebookUseCaseTest {

    @RelaxedMockK
    private lateinit var authRepository: AuthRepository

    private lateinit var loginWithFacebookUseCase: LoginWithFacebookUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        loginWithFacebookUseCase = LoginWithFacebookUseCase(authRepository)
    }

    @Test
    fun `when facebook login succeeds then return true`() = runTest {
        // Given
        val token = "valid_facebook_token"
        coEvery { authRepository.firebaseAuthWithFacebook(token) } returns Result.success(true)

        // When
        val result = loginWithFacebookUseCase(token)

        // Then
        assertEquals(Result.success(true), result)
    }
    @Test
    fun `when facebook login fails then return error`() = runTest {
        // Given
        val token = "valid_facebook_token"
        val expectedException = Exception("Login failed")
        coEvery { authRepository.firebaseAuthWithFacebook(token) } returns Result.failure(expectedException)

        // When
        val result = loginWithFacebookUseCase(token)

        // Then
        assertEquals(Result.failure<Boolean>(expectedException), result)
    }
}