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

// IMPORTANTE: Asegúrate de importar tu clase Result personalizada y sus helpers
import com.cursoandroid.queermap.util.Result
import com.cursoandroid.queermap.util.success
import com.cursoandroid.queermap.util.failure
import java.lang.Exception // Asegúrate de que Exception también esté importado si lo usas directamente

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
        } returns success(expectedUser) // Usa tu función helper 'success'

        // When: se invoca el caso de uso con los datos
        val result = loginWithEmailUseCase(email, password)

        // Then: se espera un resultado exitoso con el usuario esperado
        assertEquals(success(expectedUser), result) // Usa tu función helper 'success'
    }

    @Test
    fun `when login fails then return error`() = runTest {
        // Given: datos de login y simulación de error en autenticación
        val email = "test@example.com"
        val password = "password"
        val expectedException = Exception("Login failed")
        coEvery {
            authRepository.loginWithEmailAndPassword(email, password)
        } returns failure(expectedException) // Usa tu función helper 'failure'

        // When: se ejecuta el caso de uso con las credenciales
        val result = loginWithEmailUseCase(email, password)

        // Then: se espera un resultado fallido con la excepción correspondiente
        assertEquals(failure<User>(expectedException), result) // Usa tu función helper 'failure'
    }
}