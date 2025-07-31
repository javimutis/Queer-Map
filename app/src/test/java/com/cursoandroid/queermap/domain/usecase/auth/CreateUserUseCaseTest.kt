package com.cursoandroid.queermap.domain.usecase.auth

import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.util.Result
import com.cursoandroid.queermap.util.failure
import com.cursoandroid.queermap.util.success
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.lang.Exception

class CreateUserUseCaseTest {

    // Mock relajado del repositorio de autenticación.
    // Esto nos permite simular el comportamiento de `AuthRepository` sin tener que
    // usar una implementación real.
    @RelaxedMockK
    private lateinit var authRepository: AuthRepository

    // La instancia del caso de uso que vamos a probar.
    private lateinit var createUserUseCase: CreateUserUseCase

    @Before
    fun setUp() {
        // Inicializa los mocks para esta suite de pruebas.
        MockKAnnotations.init(this)

        // Instancia el caso de uso, inyectándole el mock del repositorio.
        createUserUseCase = CreateUserUseCase(authRepository)
    }

    @Test
    fun `when user registration succeeds then return success`() = runTest {
        // Given: Prepara los datos de prueba.
        val user = User(
            id = "newUserId",
            email = "newuser@example.com",
            username = "new_user",
            name = "New User",
            birthday = "05/10/2000"
        )
        val password = "securePassword123"

        // Configura el mock para que, cuando se llame a `registerUser`,
        // devuelva un resultado de éxito con `Unit` (sin datos).
        coEvery { authRepository.registerUser(user, password) } returns success(Unit)

        // When: Ejecuta el caso de uso con los datos de prueba.
        val result = createUserUseCase(user, password)

        // Then: Comprueba que el resultado sea el esperado (un resultado de éxito).
        assertEquals(success(Unit), result)
    }

    @Test
    fun `when user registration fails then return failure with exception`() = runTest {
        // Given: Prepara los datos de prueba para un escenario de fallo.
        val user = User(
            id = "newUserId",
            email = "failuser@example.com",
            username = "fail_user",
            name = "Fail User",
            birthday = "01/01/1990"
        )
        val password = "anotherPassword"
        val expectedException = Exception("Registration failed due to network error")

        // Configura el mock para que, cuando se llame a `registerUser`,
        // lance una excepción y devuelva un resultado de fallo.
        coEvery { authRepository.registerUser(user, password) } returns failure(expectedException)

        // When: Ejecuta el caso de uso con los datos de prueba.
        val result = createUserUseCase(user, password)

        // Then: Comprueba que el resultado sea el esperado (un resultado de fallo
        // que contenga la excepción).
        assertEquals(failure<Unit>(expectedException), result)
    }
}
