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

class LoginWithFacebookUseCaseTest {

    // Mock relajado del repositorio de autenticación, lo que nos permite
    // simular su comportamiento sin una implementación real.
    @RelaxedMockK
    private lateinit var authRepository: AuthRepository

    // La instancia del caso de uso que se va a probar.
    private lateinit var loginWithFacebookUseCase: LoginWithFacebookUseCase

    @Before
    fun setUp() {
        // Inicializa los mocks para esta suite de pruebas.
        MockKAnnotations.init(this)

        // Instancia el caso de uso inyectándole el mock del repositorio.
        loginWithFacebookUseCase = LoginWithFacebookUseCase(authRepository)
    }

    @Test
    fun `when firebase auth with facebook succeeds then return success with user`() = runTest {
        // Given: Se prepara un token de acceso de Facebook y se crea el usuario esperado.
        val accessToken = "facebook_access_token_12345"
        val expectedUser = User(
            id = "uidFb456",
            email = "fb@example.com",
            username = "fbusername",
            name = "Facebook User",
            birthday = "02/02/1999"
        )

        // Se configura el mock: cuando se llame a `firebaseAuthWithFacebook` con el token
        // simulado, debe retornar un resultado de éxito con el usuario.
        coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns success(expectedUser)

        // When: Se ejecuta el caso de uso con el token.
        val result = loginWithFacebookUseCase(accessToken)

        // Then: Se verifica que el resultado sea el esperado (un resultado de éxito).
        assertEquals(success(expectedUser), result)
    }

    @Test
    fun `when firebase auth with facebook fails then return failure with exception`() = runTest {
        // Given: Se prepara un token y se simula una excepción de autenticación.
        val accessToken = "invalid_facebook_token"
        val expectedException = Exception("Facebook authentication failed: Token is invalid")

        // Se configura el mock para que, cuando se llame al método,
        // retorne un resultado de fallo con la excepción esperada.
        coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns failure(expectedException)

        // When: Se ejecuta el caso de uso con el token inválido.
        val result = loginWithFacebookUseCase(accessToken)

        // Then: Se verifica que el resultado sea el esperado (un resultado de fallo
        // con la excepción correspondiente).
        assertEquals(failure<User>(expectedException), result)
    }
}
