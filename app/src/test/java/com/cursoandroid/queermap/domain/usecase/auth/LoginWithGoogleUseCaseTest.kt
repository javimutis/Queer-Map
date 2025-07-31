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

class LoginWithGoogleUseCaseTest {

    // Simula el comportamiento del repositorio de autenticación
    @RelaxedMockK
    private lateinit var authRepository: AuthRepository

    // La instancia del caso de uso que se va a probar.
    private lateinit var loginWithGoogleUseCase: LoginWithGoogleUseCase

    @Before
    fun setUp() {
        // Inicializa las anotaciones de MockK para esta suite de pruebas.
        MockKAnnotations.init(this)

        // Asigna el mock al caso de uso para que use la versión simulada del repositorio.
        loginWithGoogleUseCase = LoginWithGoogleUseCase(authRepository)
    }

    @Test
    fun `when firebase auth with google succeeds then return success with user`() = runTest {
        // Given: Se define un token de Google válido y un usuario esperado.
        val idToken = "google_id_token_xyz"
        val expectedUser = User(
            id = "uid123Google",
            email = "test@google.com",
            username = "googleuser",
            name = "Google User",
            birthday = "01/01/2000"
        )

        // Se configura el mock: cuando se llame a `firebaseAuthWithGoogle` con el token
        // simulado, debe retornar un resultado de éxito con el usuario.
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns success(expectedUser)

        // When: Se ejecuta el caso de uso con el token.
        val result = loginWithGoogleUseCase(idToken)

        // Then: Se verifica que el resultado sea el esperado (un resultado de éxito).
        assertEquals(success(expectedUser), result)
    }

    @Test
    fun `when firebase auth with google fails then return failure with exception`() = runTest {
        // Given: Se prepara un token y se simula una excepción de autenticación.
        val idToken = "invalid_google_id_token"
        val expectedException = Exception("Google authentication failed: The token is expired")

        // Se configura el mock para que, cuando se llame al método,
        // retorne un resultado de fallo con la excepción esperada.
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns failure(expectedException)

        // When: Se ejecuta el caso de uso con el token inválido.
        val result = loginWithGoogleUseCase(idToken)

        // Then: Se verifica que el resultado sea el esperado (un resultado de fallo
        // con la excepción correspondiente).
        assertEquals(failure<User>(expectedException), result)
    }
}
