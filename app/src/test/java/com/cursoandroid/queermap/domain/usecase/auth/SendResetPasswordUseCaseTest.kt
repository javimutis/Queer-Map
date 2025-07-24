package com.cursoandroid.queermap.domain.usecase.auth

import com.cursoandroid.queermap.domain.repository.AuthRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.lang.Exception // Asegúrate de que Exception también esté importado si lo usas directamente

// IMPORTANTE: Asegúrate de importar tu clase Result personalizada y sus helpers
import com.cursoandroid.queermap.util.Result
import com.cursoandroid.queermap.util.success
import com.cursoandroid.queermap.util.failure

class SendResetPasswordUseCaseTest {

    @RelaxedMockK
    private lateinit var authRepository: AuthRepository // Mock del repositorio de autenticación

    private lateinit var sendResetPasswordUseCase: SendResetPasswordUseCase // Caso de uso a testear

    @Before
    fun setUp() {
        MockKAnnotations.init(this) // Inicializa anotaciones de MockK
        sendResetPasswordUseCase = SendResetPasswordUseCase(authRepository) // Inyección del mock en el use case
    }

    @Test
    fun `when send reset password email succeeds then return success`() = runTest {
        // Given: Se simula envío exitoso de correo de reseteo
        val email = "test@example.com"
        // Usa tu función helper 'success'
        coEvery { authRepository.sendResetPasswordEmail(email) } returns success(Unit)

        // When: Se ejecuta el caso de uso
        val result = sendResetPasswordUseCase(email)

        // Then: Se espera resultado exitoso
        // Usa tu función helper 'success'
        assertEquals(success(Unit), result)
    }

    @Test
    fun `when send reset password email fails then return failure with exception`() = runTest {
        // Given: Se simula error de red al enviar el correo
        val email = "test@example.com"
        val expectedException = IOException("Network error during password reset")
        // Usa tu función helper 'failure'
        coEvery { authRepository.sendResetPasswordEmail(email) } returns failure(expectedException)

        // When: Se ejecuta el caso de uso
        val result = sendResetPasswordUseCase(email)

        // Then: Se espera resultado con excepción
        // Usa tu función helper 'failure'
        assertEquals(failure<Unit>(expectedException), result)
    }
}