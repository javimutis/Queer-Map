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

class LoginWithFacebookUseCaseTest {

    // Mock relajado del repositorio de autenticación
    @RelaxedMockK
    private lateinit var authRepository: AuthRepository

    // Use case que se está testeando
    private lateinit var loginWithFacebookUseCase: LoginWithFacebookUseCase

    @Before
    fun setUp() {
        // Inicializa los mocks de MockK
        MockKAnnotations.init(this)

        // Inyección del mock en el use case
        loginWithFacebookUseCase = LoginWithFacebookUseCase(authRepository)
    }

    @Test
    fun `when firebase auth with facebook succeeds then return success with user`() = runTest {
        // Given: token válido y se mockea retorno exitoso con un usuario
        val accessToken = "facebook_access_token"
        val expectedUser =
            User("uidFb456", "fbuser@example.com", "fbusername", "Facebook User", "02/02/1999")
        coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns Result.success(
            expectedUser
        )

        // When: se ejecuta el use case con el token de Facebook
        val result = loginWithFacebookUseCase(accessToken)

        // Then: se espera resultado exitoso con los datos del usuario
        assertEquals(Result.success(expectedUser), result)
    }

    @Test
    fun `when firebase auth with facebook fails then return failure with exception`() = runTest {
        // Given: token válido y se mockea una excepción al autenticar
        val accessToken = "facebook_access_token"
        val expectedException = Exception("Facebook authentication failed")
        coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns Result.failure(
            expectedException
        )

        // When: se ejecuta el use case con el token de Facebook
        val result = loginWithFacebookUseCase(accessToken)

        // Then: se espera resultado fallido con la excepción recibida
        assertEquals(Result.failure<User>(expectedException), result)
    }
}
