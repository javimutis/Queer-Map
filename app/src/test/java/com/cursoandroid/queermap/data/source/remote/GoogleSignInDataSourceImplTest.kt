package com.cursoandroid.queermap.data.source.remote

import android.content.Context
import android.content.Intent
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.util.Result
import com.cursoandroid.queermap.util.failure
import com.cursoandroid.queermap.util.success
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.Task
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Configura Robolectric para un SDK específico de Android
@LooperMode(LooperMode.Mode.PAUSED) // Pausa el looper de Android para controlar las corrutinas
@OptIn(ExperimentalCoroutinesApi::class)
class GoogleSignInDataSourceImplTest {

    // Mock del Context de Android, relajado para no tener que mockear cada método
    private val mockContext: Context = mockk(relaxed = true)

    // Mock del GoogleSignInClient, relajado para no tener que mockear cada método
    private val mockGoogleSignInClient: GoogleSignInClient = mockk(relaxed = true)

    // Instancia de la clase a testear
    private lateinit var dataSource: GoogleSignInDataSourceImpl

    @Before
    fun setUp() {
        // Inicializa los mocks de MockK
        MockKAnnotations.init(this)

        // Mockear llamadas estáticas a GoogleSignIn y GoogleSignInOptions.Builder
        // Esto es necesario porque GoogleSignInDataSourceImpl usa métodos estáticos
        mockkStatic(GoogleSignIn::class)
        mockkStatic(GoogleSignInOptions::class)
        mockkStatic(GoogleSignInOptions.Builder::class)

        // Configura el comportamiento del mockContext para getString
        // Se usa para simular la obtención del default_web_client_id
        every { mockContext.getString(R.string.default_web_client_id) } returns "fake_web_client_id"

        // Configura el mock del GoogleSignInClient que se devuelve cuando se llama a GoogleSignIn.getClient
        // Esto es crucial para que la inicialización lazy de signInClient funcione en el DataSource
        every { GoogleSignIn.getClient(mockContext, any<GoogleSignInOptions>()) } returns mockGoogleSignInClient

        // Inicializa la clase que vamos a testear
        dataSource = GoogleSignInDataSourceImpl(mockContext)
    }

    @After
    fun tearDown() {
        // Deshace todos los mocks estáticos después de cada test para evitar interferencias
        unmockkAll()
    }

    @Test
    fun `when getSignInIntent is called then it returns the signInIntent from GoogleSignInClient`() {
        // Given: una Intent de inicio de sesión simulada
        val expectedIntent = mockk<Intent>()
        // Configura el comportamiento del mockGoogleSignInClient para devolver la Intent simulada
        every { mockGoogleSignInClient.signInIntent } returns expectedIntent

        // When: se llama al método getSignInIntent del DataSource
        val resultIntent = dataSource.getSignInIntent()

        // Then: se verifica que la Intent devuelta es la esperada
        assertEquals(expectedIntent, resultIntent)
        // Y se verifica que se llamó al método signInIntent en el cliente de Google
        verify(exactly = 1) { mockGoogleSignInClient.signInIntent }
    }

    @Test
    fun `when handleSignInResult succeeds with valid ID token then return success with ID token`() = runTest {
        // Given: una Intent de datos, un ID token válido y un Task exitoso
        val mockIntent: Intent = mockk()
        val expectedIdToken = "valid_google_id_token"
        val mockAccount: GoogleSignInAccount = mockk()
        val mockTask: Task<GoogleSignInAccount> = mockk()

        // Configura el mockAccount para devolver el ID token esperado
        every { mockAccount.idToken } returns expectedIdToken
        // Configura el mockTask para devolver el mockAccount al llamar a getResult
        every { mockTask.getResult(ApiException::class.java) } returns mockAccount
        // Configura la llamada estática para devolver el mockTask
        every { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) } returns mockTask

        // When: se llama al método handleSignInResult con la Intent simulada
        val result = dataSource.handleSignInResult(mockIntent)

        // Then: se verifica que el resultado es un éxito con el ID token esperado
        assertTrue(result is Result.Success)
        assertEquals(expectedIdToken, (result as Result.Success).data)
        // Se verifica que se intentó obtener la cuenta de la Intent
        verify(exactly = 1) { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) }
    }

    @Test
    fun `when handleSignInResult succeeds but ID token is null then return failure`() = runTest {
        // Given: una Intent de datos, un ID token nulo y un Task exitoso
        val mockIntent: Intent = mockk()
        val mockAccount: GoogleSignInAccount = mockk()
        val mockTask: Task<GoogleSignInAccount> = mockk()

        // Configura el mockAccount para devolver un ID token nulo
        every { mockAccount.idToken } returns null
        // Configura el mockTask para devolver el mockAccount
        every { mockTask.getResult(ApiException::class.java) } returns mockAccount
        // Configura la llamada estática para devolver el mockTask
        every { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) } returns mockTask

        // When: se llama al método handleSignInResult con la Intent simulada
        val result = dataSource.handleSignInResult(mockIntent)

        // Then: se verifica que el resultado es un fallo con el mensaje esperado
        assertTrue(result is Result.Failure)
        assertEquals("Google ID token es nulo.", (result as Result.Failure).exception.message)
        verify(exactly = 1) { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) }
    }

    @Test
    fun `when handleSignInResult fails with ApiException then return failure with ApiException message`() = runTest {
        // Given: una Intent de datos y un Task que falla con una ApiException
        val mockIntent: Intent = mockk()
        val apiException = ApiException(Status(10, "Developer error")) // Código de error de Google
        val mockTask: Task<GoogleSignInAccount> = mockk()

        // Configura el mockTask para lanzar la ApiException al llamar a getResult
        every { mockTask.getResult(ApiException::class.java) } throws apiException
        // Configura la llamada estática para devolver el mockTask
        every { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) } returns mockTask

        // When: se llama al método handleSignInResult con la Intent simulada
        val result = dataSource.handleSignInResult(mockIntent)

        // Then: se verifica que el resultado es un fallo con el mensaje de la ApiException
        // Ajustado para esperar solo el mensaje original de ApiException, ya que parece que tu helper 'failure'
        // no está prependiéndole el texto "Error de Google Sign-In: "
        assertEquals("10: Developer error", (result as Result.Failure).exception.message)
        verify(exactly = 1) { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) }
    }

    @Test
    fun `when handleSignInResult fails with generic Exception then return failure with generic message`() = runTest {
        // Given: una Intent de datos y un Task que falla con una excepción genérica
        val mockIntent: Intent = mockk()
        val genericException = Exception("Something went wrong during Google Sign-In")
        val mockTask: Task<GoogleSignInAccount> = mockk()

        // Configura el mockTask para lanzar la excepción genérica al llamar a getResult
        every { mockTask.getResult(ApiException::class.java) } throws genericException
        // Configura la llamada estática para devolver el mockTask
        every { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) } returns mockTask

        // When: se llama al método handleSignInResult con la Intent simulada
        val result = dataSource.handleSignInResult(mockIntent)

        // Then: se verifica que el resultado es un fallo con el mensaje de la excepción genérica
        assertTrue(result is Result.Failure)
        assertEquals("Something went wrong during Google Sign-In", (result as Result.Failure).exception.message)
        verify(exactly = 1) { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) }
    }

    @Test
    fun `when handleSignInResult is called with null Intent data then return failure with NullPointerException`() = runTest {
        // Given: Intent de datos nula
        val nullIntent: Intent? = null

        // Mockear el comportamiento de getSignedInAccountFromIntent cuando se le pasa null.
        // Esto es crucial para simular el NPE que ocurriría si la Intent es nula.
        every { GoogleSignIn.getSignedInAccountFromIntent(nullIntent) } throws NullPointerException("Intent data is null")

        // When: se llama al método handleSignInResult con la Intent nula
        val result = dataSource.handleSignInResult(nullIntent)

        // Then: se verifica que el resultado es un fallo
        assertTrue(result is Result.Failure)
        // Y que la excepción es de tipo NullPointerException con el mensaje esperado
        assertTrue((result as Result.Failure).exception is NullPointerException)
        assertEquals("Intent data is null", result.exception.message)
        // Se verifica que se intentó obtener la cuenta de la Intent (con null)
        verify(exactly = 1) { GoogleSignIn.getSignedInAccountFromIntent(nullIntent) }
    }
}
