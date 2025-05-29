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

    // Mock relajado del repositorio de autenticación para simular comportamientos
    @RelaxedMockK
    private lateinit var authRepository: AuthRepository

    // Instancia del caso de uso que se probará
    private lateinit var loginWithEmailUseCase: LoginWithEmailUseCase

    @Before
    fun setUp() {
        // Inicializa los mocks anotados en la clase
        MockKAnnotations.init(this)
        // Crea instancia del use case con el mock de repositorio
        loginWithEmailUseCase = LoginWithEmailUseCase(authRepository)
    }

    @Test
    fun `when login succeeds then return user`() = runTest {
        // Given: datos de login válidos y respuesta exitosa simulada
        val email = "test@example.com"
        val password = "password"
        val expectedUser = User("123", email)
        coEvery {
            authRepository.loginWithEmailAndPassword(email, password)
        } returns Result.success(expectedUser)

        // When: se invoca el caso de uso con los datos
        val result = loginWithEmailUseCase(email, password)

        // Then: se espera un resultado exitoso con el usuario esperado
        assertEquals(Result.success(expectedUser), result)
    }

    @Test
    fun `when login fails then return error`() = runTest {
        // Given: datos de login y simulación de error en autenticación
        val email = "test@example.com"
        val password = "password"
        val expectedException = Exception("Login failed")
        coEvery { authRepository.loginWithEmailAndPassword(email, password) } returns Result.failure(expectedException)

        // When: se ejecuta el caso de uso con las credenciales
        val result = loginWithEmailUseCase(email, password)

        // Then: se espera un resultado fallido con la excepción correspondiente
        assertEquals(Result.failure<User>(expectedException), result)
    }
}
