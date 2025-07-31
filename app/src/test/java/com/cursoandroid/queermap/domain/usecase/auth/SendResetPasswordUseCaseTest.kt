package com.cursoandroid.queermap.domain.usecase.auth

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
import java.io.IOException
import java.lang.Exception

class SendResetPasswordUseCaseTest {

    // Mock relajado del repositorio de autenticación, lo que nos permite simular su comportamiento.
    @RelaxedMockK
    private lateinit var authRepository: AuthRepository

    // La instancia del caso de uso que vamos a probar.
    private lateinit var sendResetPasswordUseCase: SendResetPasswordUseCase

    @Before
    fun setUp() {
        // Inicializa los mocks para esta suite de pruebas.
        MockKAnnotations.init(this)
        // Instancia el caso de uso inyectándole el mock del repositorio.
        sendResetPasswordUseCase = SendResetPasswordUseCase(authRepository)
    }

    @Test
    fun `when send reset password email succeeds then return success`() = runTest {
        // Given: Se simula que el repositorio completa el envío de email exitosamente.
        val email = "test@example.com"
        // Se configura el mock para que, cuando se llame al método, retorne un resultado de éxito.
        coEvery { authRepository.sendResetPasswordEmail(email) } returns success(Unit)

        // When: Se ejecuta el caso de uso.
        val result = sendResetPasswordUseCase(email)

        // Then: Se verifica que el resultado sea exitoso con Unit.
        assertEquals(success(Unit), result)
    }

    @Test
    fun `when send reset password email fails then return failure with exception`() = runTest {
        // Given: Se simula que el repositorio falla al enviar el email.
        val email = "test@example.com"
        val expectedException = IOException("Network error during password reset")
        // Se configura el mock para que retorne un resultado de fallo con la excepción esperada.
        coEvery { authRepository.sendResetPasswordEmail(email) } returns failure(expectedException)

        // When: Se ejecuta el caso de uso.
        val result = sendResetPasswordUseCase(email)

        // Then: Se verifica que el resultado sea un fallo con la misma excepción.
        assertEquals(failure<Unit>(expectedException), result)
    }
}
