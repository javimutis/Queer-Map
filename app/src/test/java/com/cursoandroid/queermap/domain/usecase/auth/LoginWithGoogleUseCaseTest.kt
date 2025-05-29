package com.cursoandroid.queermap.domain.usecase.auth

import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.lang.Exception

class LoginWithGoogleUseCaseTest {

    // Simula el repositorio de autenticación
    @RelaxedMockK
    private lateinit var authRepository: AuthRepository

    // Use case bajo prueba
    private lateinit var loginWithGoogleUseCase: LoginWithGoogleUseCase

    @Before
    fun setUp() {
        // Inicializa las anotaciones de MockK
        MockKAnnotations.init(this)

        // Asigna el mock al use case
        loginWithGoogleUseCase = LoginWithGoogleUseCase(authRepository)
    }

    @Test
    fun `when firebase auth with google succeeds then return success with user`() = runTest {
        // Given: token válido y mock que retorna un usuario exitosamente
        val idToken = "google_id_token"
        val expectedUser = User("uid123", "test@example.com", "username", "Test User", "01/01/2000")
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns Result.success(expectedUser)

        // When: se ejecuta el use case con el token
        val result = loginWithGoogleUseCase(idToken)

        // Then: se espera un resultado exitoso con el usuario esperado
        assertEquals(Result.success(expectedUser), result)
    }

    @Test
    fun `when firebase auth with google fails then return failure with exception`() = runTest {
        // Given: token válido y mock que retorna una excepción
        val idToken = "google_id_token"
        val expectedException = Exception("Google authentication failed")
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns Result.failure(expectedException)

        // When: se ejecuta el use case con el token
        val result = loginWithGoogleUseCase(idToken)

        // Then: se espera un resultado fallido con la excepción
        assertEquals(Result.failure<User>(expectedException), result)
    }
}
