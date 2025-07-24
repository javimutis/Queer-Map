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
import java.lang.Exception // Asegúrate de que Exception también esté importado si lo usas directamente

// IMPORTANTE: Asegúrate de importar tu clase Result personalizada y sus helpers
import com.cursoandroid.queermap.util.Result
import com.cursoandroid.queermap.util.success
import com.cursoandroid.queermap.util.failure


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
            User("uidFb456", "fb@example.com", "fbusername", "Facebook User", "02/02/1999")
        // Usa tu función helper 'success'
        coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns success(expectedUser)

        // When: se ejecuta el use case con el token de Facebook
        val result = loginWithFacebookUseCase(accessToken)

        // Then: se espera resultado exitoso con los datos del usuario
        // Usa tu función helper 'success'
        assertEquals(success(expectedUser), result)
    }

    @Test
    fun `when firebase auth with facebook fails then return failure with exception`() = runTest {
        // Given: token válido y se mockea una excepción al autenticar
        val accessToken = "facebook_access_token"
        val expectedException = Exception("Facebook authentication failed")
        // Usa tu función helper 'failure'
        coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns failure(expectedException)

        // When: se ejecuta el use case con el token de Facebook
        val result = loginWithFacebookUseCase(accessToken)

        // Then: se espera resultado fallido con la excepción recibida
        // Usa tu función helper 'failure'
        assertEquals(failure<User>(expectedException), result)
    }
}