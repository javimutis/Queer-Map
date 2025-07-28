package com.cursoandroid.queermap.data.source.remote

import androidx.fragment.app.Fragment
import com.cursoandroid.queermap.util.exceptionOrNull
import com.cursoandroid.queermap.util.getOrNull
import com.cursoandroid.queermap.util.isFailure
import com.cursoandroid.queermap.util.isSuccess
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test


class FacebookSignInDataSourceImplTest {

    // Instancia de la clase a testear
    private lateinit var facebookSignInDataSource: FacebookSignInDataSourceImpl

    private lateinit var mockCallbackManager: CallbackManager
    private lateinit var mockFragment: Fragment


    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true) // Esto sí está correcto

        mockCallbackManager = mockk()
        mockFragment = mockk()

        mockkObject(LoginManager)
        every { LoginManager.getInstance().registerCallback(any(), any()) } returns Unit
        every {
            LoginManager.getInstance().logInWithReadPermissions(
                ofType(androidx.fragment.app.Fragment::class),
                ofType(List::class) as List<String>
            )
        } returns Unit


        facebookSignInDataSource = FacebookSignInDataSourceImpl()
    }


    @After
    fun tearDown() {
        // Desmockear el objeto estático LoginManager después de cada test
        unmockkObject(LoginManager) // Correct capitalization
        clearAllMocks() // Limpia todos los mocks para evitar interferencias entre tests
    }

    @Test
    fun `registerCallback registers the FacebookCallback with LoginManager`() {
        // Given: Slot para capturar el callback
        val callbackSlot = slot<FacebookCallback<LoginResult>>()

        every {
            LoginManager.getInstance().registerCallback(mockCallbackManager, capture(callbackSlot))
        } returns Unit

        // When
        facebookSignInDataSource.registerCallback(mockCallbackManager, mockk(relaxed = true))

        // Then
        verify(exactly = 1) {
            LoginManager.getInstance().registerCallback(mockCallbackManager, callbackSlot.captured)
        }
    }


    @Test
    fun `logInWithReadPermissions calls LoginManager with correct fragment and permissions`() {
        // Given: Una lista de permisos
        val permissions = listOf("email", "public_profile")

        // When: Se llama a logInWithReadPermissions
        facebookSignInDataSource.logInWithReadPermissions(mockFragment, permissions)

        // Then: Se verifica que LoginManager.getInstance().logInWithReadPermissions fue llamado
        verify(exactly = 1) {
            LoginManager.getInstance().logInWithReadPermissions(mockFragment, permissions)
        }
    }

    @Test
    fun `accessTokenChannel emits success when FacebookCallback onSuccess is triggered`() = runTest {
        // Given: Un slot para capturar el FacebookCallback
        val callbackSlot = slot<FacebookCallback<LoginResult>>()

        // Cuando se llama a registerCallback, captura el FacebookCallback
        every {
            LoginManager.getInstance().registerCallback(any(), capture(callbackSlot))
        } returns Unit

        // Registra un callback (esto capturará el callback en callbackSlot)
        facebookSignInDataSource.registerCallback(mockCallbackManager, mockk(relaxed = true))

        // Mocks para simular un LoginResult exitoso
        val testAccessToken = "test_facebook_access_token"
        val mockAccessToken: AccessToken = mockk {
            every { token } returns testAccessToken
        }
        val mockLoginResult: LoginResult = mockk {
            every { accessToken } returns mockAccessToken
        }

        // Simula directamente el callback (¡sin launch!)
        callbackSlot.captured.onSuccess(mockLoginResult)

        // Then: Verifica que el canal emitió un Result.Success con el token correcto
        val result = facebookSignInDataSource.accessTokenChannel.first()
        assertTrue(result.isSuccess())
        assertEquals(testAccessToken, result.getOrNull())
    }


    @Test
    fun `accessTokenChannel emits failure when FacebookCallback onCancel is triggered`() = runTest {
        // Given: Un slot para capturar el FacebookCallback
        val callbackSlot = slot<FacebookCallback<LoginResult>>()

        // Cuando se llama a registerCallback, captura el FacebookCallback
        every {
            LoginManager.getInstance().registerCallback(any(), capture(callbackSlot))
        } returns Unit

        // Registra un callback
        facebookSignInDataSource.registerCallback(mockCallbackManager, mockk(relaxed = true))

        // Lanza una corrutina para recolectar el primer valor del canal
        val job = launch {
            // When: Se simula una llamada onCancel en el callback capturado
            callbackSlot.captured.onCancel()
        }

        // Then: Verifica que el canal emitió un Result.Failure
        val result = facebookSignInDataSource.accessTokenChannel.first()
        assertTrue(result.isFailure())
        assertNotNull(result.exceptionOrNull())
        assertEquals("Inicio de sesión cancelado.", result.exceptionOrNull()?.message)

        job.cancel()
    }

    @Test
    fun `accessTokenChannel emits failure when FacebookCallback onError is triggered`() = runTest {
        // Given: Un slot para capturar el FacebookCallback y una excepción
        val callbackSlot = slot<FacebookCallback<LoginResult>>()
        val testException = FacebookException("Something went wrong with Facebook login")

        // Cuando se llama a registerCallback, captura el FacebookCallback
        every {
            LoginManager.getInstance().registerCallback(any(), capture(callbackSlot))
        } returns Unit

        // Registra un callback
        facebookSignInDataSource.registerCallback(mockCallbackManager, mockk(relaxed = true))

        // Lanza una corrutina para recolectar el primer valor del canal
        val job = launch {
            // When: Se simula una llamada onError en el callback capturado
            callbackSlot.captured.onError(testException)
        }

        // Then: Verifica que el canal emitió un Result.Failure con la excepción correcta
        val result = facebookSignInDataSource.accessTokenChannel.first()
        assertTrue(result.isFailure())
        assertEquals(testException, result.exceptionOrNull())
        assertEquals("Something went wrong with Facebook login", result.exceptionOrNull()?.message)

        job.cancel()
    }

    @Test
    fun `accessTokenChannel emits failure when FacebookCallback onSuccess is triggered but accessToken token is empty`() = runTest {
        val callbackSlot = slot<FacebookCallback<LoginResult>>()

        every {
            LoginManager.getInstance().registerCallback(any(), capture(callbackSlot))
        } returns Unit

        facebookSignInDataSource.registerCallback(mockCallbackManager, mockk(relaxed = true))

        val mockAccessToken: AccessToken = mockk()
        every { mockAccessToken.token } returns "" // No null, pero sí vacío

        val mockLoginResult: LoginResult = mockk()
        every { mockLoginResult.accessToken } returns mockAccessToken

        callbackSlot.captured.onSuccess(mockLoginResult)

        val result = facebookSignInDataSource.accessTokenChannel.first()
        assertTrue(result.isFailure())
        assertEquals("Token de acceso de Facebook nulo.", result.exceptionOrNull()?.message)
    }


}
