package com.cursoandroid.queermap.domain.usecase.auth

import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.domain.model.User
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LoginWithEmailUseCaseTest {

    @RelaxedMockK
    private lateinit var authRepository: AuthRepository

    private lateinit var loginWithEmailUseCase: LoginWithEmailUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        loginWithEmailUseCase = LoginWithEmailUseCase(authRepository)
    }

    @Test
    fun `when login succeeds then return user`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password"
        val expectedUser = User("123", email)
        coEvery {
            authRepository.loginWithEmailAndPassword(
                email,
                password
            )
        } returns Result.success(expectedUser)

        // When
        val result = loginWithEmailUseCase(email, password)

        // Then
        assertEquals(Result.success(expectedUser), result)
    }
    @Test
    fun `when login fails then return error`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password"
        val expectedException = Exception("Login failed")
        coEvery { authRepository.loginWithEmailAndPassword(email, password) } returns Result.failure(expectedException)

        // When
        val result = loginWithEmailUseCase(email, password)

        // Then
        assertEquals(Result.failure<User>(expectedException), result)
    }
}