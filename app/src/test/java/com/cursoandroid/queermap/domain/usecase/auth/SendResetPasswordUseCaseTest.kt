package com.cursoandroid.queermap.domain.usecase.auth

import com.cursoandroid.queermap.domain.repository.AuthRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SendResetPasswordUseCaseTest {

    @RelaxedMockK
    private lateinit var authRepository: AuthRepository

    private lateinit var sendResetPasswordUseCase: SendResetPasswordUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        sendResetPasswordUseCase = SendResetPasswordUseCase(authRepository)
    }

    @Test
    fun `when password reset succeeds then return success`() = runTest {
        // Given
        val email = "test@example.com"
        coEvery { authRepository.sendPasswordResetEmail(email) } returns Result.success(Unit)

        // When
        val result = sendResetPasswordUseCase(email)

        // Then
        assertEquals(Result.success(Unit), result)
    }
    @Test
    fun `when password reset fails then return error`() = runTest {
        // Given
        val email = "test@example.com"
        val expectedException = Exception("Reset failed")
        coEvery { authRepository.sendPasswordResetEmail(email) } returns Result.failure(expectedException)

        // When
        val result = sendResetPasswordUseCase(email)

        // Then
        assertEquals(Result.failure<Unit>(expectedException), result)
    }

}