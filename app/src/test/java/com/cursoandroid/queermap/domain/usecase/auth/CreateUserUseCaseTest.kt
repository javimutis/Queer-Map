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

class CreateUserUseCaseTest {

    // Mock relajado del repositorio de autenticación
    @RelaxedMockK
    private lateinit var authRepository: AuthRepository

    // Caso de uso para crear usuario
    private lateinit var createUserUseCase: CreateUserUseCase

    @Before
    fun setUp() {
        // Inicializa mocks para pruebas
        MockKAnnotations.init(this)

        // Instancia el caso de uso con el mock del repositorio
        createUserUseCase = CreateUserUseCase(authRepository)
    }

    @Test
    fun `when user registration succeeds then return success`() = runTest {
        // Given: usuario válido y mock que simula registro exitoso
        val user = User(
            id = "newUserId",
            email = "newuser@example.com",
            username = "new_user",
            name = "New User",
            birthday = "05/10/2000"
        )
        val password = "securePassword123"
        coEvery { authRepository.registerUser(user, password) } returns Result.success(Unit)

        // When: se ejecuta el caso de uso con usuario y contraseña
        val result = createUserUseCase(user, password)

        // Then: se espera resultado exitoso sin datos (Unit)
        assertEquals(Result.success(Unit), result)
    }

    @Test
    fun `when user registration fails then return failure with exception`() = runTest {
        // Given: usuario válido y mock que simula fallo con excepción
        val user = User(
            id = "newUserId",
            email = "failuser@example.com",
            username = "fail_user",
            name = "Fail User",
            birthday = "01/01/1990"
        )
        val password = "anotherPassword"
        val expectedException = Exception("Registration failed due to network error")
        coEvery { authRepository.registerUser(user, password) } returns Result.failure(expectedException)

        // When: se ejecuta el caso de uso con usuario y contraseña
        val result = createUserUseCase(user, password)

        // Then: se espera resultado fallido con la excepción esperada
        assertEquals(Result.failure<Unit>(expectedException), result)
    }
}
