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

// IMPORTANTE: Asegúrate de importar tu clase Result personalizada y sus helpers
import com.cursoandroid.queermap.util.Result
import com.cursoandroid.queermap.util.success
import com.cursoandroid.queermap.util.failure

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
        // Usa tu función helper 'success'
        coEvery { authRepository.registerUser(user, password) } returns success(Unit)

        // When: se ejecuta el caso de uso con usuario y contraseña
        val result = createUserUseCase(user, password)

        // Then: se espera resultado exitoso sin datos (Unit)
        // Usa tu función helper 'success'
        assertEquals(success(Unit), result)
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
        // Usa tu función helper 'failure'
        coEvery { authRepository.registerUser(user, password) } returns failure(expectedException)

        // When: se ejecuta el caso de uso con usuario y contraseña
        val result = createUserUseCase(user, password)

        // Then: se espera resultado fallido con la excepción esperada
        // Usa tu función helper 'failure'
        assertEquals(failure<Unit>(expectedException), result)
    }
}