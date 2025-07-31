package com.cursoandroid.queermap.domain.usecase.auth

import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.domain.model.User
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

class LoginWithEmailUseCaseTest {

    // Se usa @RelaxedMockK para crear un mock del AuthRepository.
    // Un mock relajado devuelve valores predeterminados para las funciones no configuradas.
    @RelaxedMockK
    private lateinit var authRepository: AuthRepository

    // La instancia del caso de uso que vamos a probar.
    private lateinit var loginWithEmailUseCase: LoginWithEmailUseCase

    @Before
    fun setUp() {
        // Inicializa todos los mocks anotados en la clase.
        MockKAnnotations.init(this)
        // Crea la instancia de la clase de caso de uso, inyectando el mock del repositorio.
        loginWithEmailUseCase = LoginWithEmailUseCase(authRepository)
    }

    @Test
    fun `when login succeeds then return user`() = runTest {
        // Given: Se definen los datos de prueba y se configura el mock.
        val email = "test@example.com"
        val password = "password123"
        val expectedUser = User(id = "user-123", email = email, username = "testuser")

        // Se configura el comportamiento del mock: cuando se llame a loginWithEmailAndPassword
        // con los datos de prueba, debe retornar un resultado de éxito con el usuario esperado.
        coEvery {
            authRepository.loginWithEmailAndPassword(email, password)
        } returns success(expectedUser)

        // When: Se ejecuta la función que se está probando.
        val result = loginWithEmailUseCase(email, password)

        // Then: Se verifica que el resultado sea exactamente el resultado de éxito esperado.
        assertEquals(success(expectedUser), result)
    }

    @Test
    fun `when login fails then return error`() = runTest {
        // Given: Se definen los datos de prueba para un escenario de fallo.
        val email = "fail@example.com"
        val password = "wrongpassword"
        val expectedException = Exception("Authentication failed with invalid credentials")

        // Se configura el mock para que devuelva un resultado de fallo
        // con la excepción esperada.
        coEvery {
            authRepository.loginWithEmailAndPassword(email, password)
        } returns failure(expectedException)

        // When: Se ejecuta la función que se está probando.
        val result = loginWithEmailUseCase(email, password)

        // Then: Se verifica que el resultado sea exactamente el resultado de fallo esperado.
        // Se usa el helper <User> para que el compilador sepa el tipo esperado.
        assertEquals(failure<User>(expectedException), result)
    }
}
