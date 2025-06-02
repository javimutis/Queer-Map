package com.cursoandroid.queermap.ui.login

// Importa los Directions generados por Safe Args
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule // <-- Importación corregida
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import com.cursoandroid.queermap.domain.model.User // Importar User
import com.facebook.AccessToken
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class LoginFragmentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val instantTaskExecutorRule = InstantTaskExecutorRule() // Para LiveData y Corrutinas

    @Inject
    lateinit var googleSignInDataSource: GoogleSignInDataSource

    @Inject
    lateinit var facebookSignInDataSource: FacebookSignInDataSource

    private lateinit var mockNavController: NavController
    private lateinit var mockLoginViewModel: LoginViewModel // Mockear el ViewModel directamente

    // Cana de eventos para Facebook que controlaremos en el test
    private lateinit var testFacebookAccessTokenChannel: MutableSharedFlow<Result<String>>

    @Before
    fun setUp() {
        hiltRule.inject()
        clearAllMocks()

        // Inicializar el canal de eventos de Facebook para cada test
        testFacebookAccessTokenChannel = MutableSharedFlow(extraBufferCapacity = 1) // Añadir un buffer para trySend

        // Configuramos el comportamiento por defecto de los DataSources inyectados
        // Mockeamos el getter de accessTokenChannel para que devuelva nuestro canal de test
        every { facebookSignInDataSource.accessTokenChannel } returns testFacebookAccessTokenChannel
        every { facebookSignInDataSource.registerCallback(any()) } just Runs
        every { facebookSignInDataSource.logInWithReadPermissions(any(), any()) } just Runs

        coEvery { googleSignInDataSource.getSignInIntent() } returns mockk(relaxed = true)
        coEvery { googleSignInDataSource.handleSignInResult(any()) } returns mockk(relaxed = true)
    }

    // --- Tests para la UI y el estado inicial ---

    @Test
    fun loginFragment_isDisplayed_onLaunch() {
        launchFragmentInContainer<LoginFragment>(themeResId = R.style.Theme_QueerMap)
        onView(withId(R.id.btnLogin)).check(matches(isDisplayed()))
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()))
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun loginFragment_loadsSavedCredentials_onLaunch() {
        launchFragmentInContainer<LoginFragment>(themeResId = R.style.Theme_QueerMap)
        onView(withId(R.id.etEmail)).check(matches(withText("")))
        onView(withId(R.id.etPassword)).check(matches(withText("")))
    }

    // --- Tests de Login con Email/Contraseña ---

    @Test
    fun loginFragment_showsSnackbar_forInvalidEmail() {
        launchFragmentAndSetNavController()

        onView(withId(R.id.etEmail)).perform(typeText("invalid-email"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        onView(withText("Por favor ingresa un email válido")).check(matches(isDisplayed()))
    }

    @Test
    fun loginFragment_showsSnackbar_forShortPassword() {
        launchFragmentAndSetNavController()

        onView(withId(R.id.etEmail)).perform(typeText("test@example.com"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText("short"), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        onView(withText("La contraseña debe tener al menos 6 caracteres")).check(matches(isDisplayed()))
    }

    @Test
    fun loginFragment_showsProgressBar_onLoginAttempt() {
        val fragmentScenario = launchFragmentAndSetNavController()

        // Mockeamos el método loginWithEmail del ViewModel para simular una operación que tarda
        // y que el ViewModel entra en estado de carga.
        coEvery { mockLoginViewModel.loginWithEmail(any(), any()) } coAnswers {
            // Simula que el ViewModel actualiza su estado a isLoading = true
            mockLoginViewModel._uiState.value = LoginUiState(isLoading = true)
            // No hacemos nada más para que el estado de carga persista brevemente
            // y Espresso pueda verificarlo.
            // Luego, para que el test no se quede colgado, simulamos un éxito o fallo.
            // Para este test, simplemente lo dejamos en isLoading para la aserción inicial.
            // En un test real, se usaría un TestDispatcher para controlar el tiempo.
        }

        onView(withId(R.id.etEmail)).perform(typeText("test@example.com"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        // Verifica que el ProgressBar se muestre
        onView(withId(R.id.progressBar)).check(matches(isDisplayed()))

        // Para que el test termine, podemos simular que el ViewModel sale del estado de carga
        // Esto se puede hacer en un `after` o en un `coVerify` que simule el final de la operación.
        // Por la forma en que está estructurado, el test pasará si el ProgressBar se muestra.
        // La limpieza se hace en el `tearDown` general.
    }

    @Test
    fun loginFragment_navigatesToHome_onSuccessfulEmailLogin() = runBlocking {
        val fragmentScenario = launchFragmentAndSetNavController()

        // Mockeamos el método loginWithEmail del ViewModel para simular un éxito
        coEvery { mockLoginViewModel.loginWithEmail(any(), any()) } coAnswers {
            // Simula el ViewModel emitiendo el evento de navegación y actualizando el estado de UI
            mockLoginViewModel._event.emit(LoginEvent.NavigateToHome)
            mockLoginViewModel._uiState.value = LoginUiState(isSuccess = true, isLoading = false)
        }

        onView(withId(R.id.etEmail)).perform(typeText("valid@example.com"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        // Verificamos que se llamó a la navegación a MapFragment
        verify(exactly = 1) { mockNavController.navigate(R.id.action_loginFragment_to_mapFragment) }
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun loginFragment_showsSnackbar_onFailedEmailLogin() = runBlocking {
        val fragmentScenario = launchFragmentAndSetNavController()

        val errorMessage = "Credenciales incorrectas"
        // Mockeamos el método loginWithEmail del ViewModel para simular un fallo
        coEvery { mockLoginViewModel.loginWithEmail(any(), any()) } coAnswers {
            // Simula el ViewModel emitiendo un mensaje de error y actualizando el estado de UI
            mockLoginViewModel._event.emit(LoginEvent.ShowMessage(errorMessage))
            mockLoginViewModel._uiState.value = LoginUiState(errorMessage = errorMessage, isLoading = false)
        }

        onView(withId(R.id.etEmail)).perform(typeText("invalid@example.com"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText("wrongpass"), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        onView(withText(errorMessage)).check(matches(isDisplayed()))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    // --- Tests de Login con Google ---

    @Test
    fun loginFragment_startsGoogleSignInFlow() {
        launchFragmentAndSetNavController()

        val mockIntent: Intent = mockk(relaxed = true)
        coEvery { googleSignInDataSource.getSignInIntent() } returns mockIntent

        onView(withId(R.id.btnGoogleSignIn)).perform(click())

        coVerify(exactly = 1) { googleSignInDataSource.getSignInIntent() }
    }

    @Test
    fun loginFragment_navigatesToHome_onSuccessfulGoogleLoginExistingUser() = runBlocking {
        val fragmentScenario = launchFragmentAndSetNavController()

        val idToken = "some_google_id_token"
        val mockIntentData = mockk<Intent>(relaxed = true)
        val mockResult = Result.success(idToken)

        coEvery { googleSignInDataSource.handleSignInResult(mockIntentData) } returns mockResult

        // Mockeamos el método loginWithGoogle del ViewModel
        coEvery { mockLoginViewModel.loginWithGoogle(idToken) } coAnswers {
            mockLoginViewModel._event.emit(LoginEvent.NavigateToHome)
            mockLoginViewModel._uiState.value = LoginUiState(isSuccess = true, isLoading = false)
        }

        fragmentScenario.onFragment { fragment ->
            // Llamamos a la función interna handleGoogleSignInResult directamente
            fragment.handleGoogleSignInResult(mockIntentData)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        verify(exactly = 1) { mockNavController.navigate(R.id.action_loginFragment_to_mapFragment) }
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun loginFragment_navigatesToSignup_onSuccessfulGoogleLoginNewUser() = runBlocking {
        val fragmentScenario = launchFragmentAndSetNavController()

        val idToken = "some_google_id_token"
        val userEmail = "newuser@example.com"
        val userName = "New Google User"
        val mockIntentData = mockk<Intent>(relaxed = true)
        val mockResult = Result.success(idToken)

        coEvery { googleSignInDataSource.handleSignInResult(mockIntentData) } returns mockResult

        // Mockeamos el método loginWithGoogle del ViewModel
        coEvery { mockLoginViewModel.loginWithGoogle(idToken) } coAnswers {
            mockLoginViewModel._event.emit(LoginEvent.NavigateToSignupWithArgs(userEmail, userName, true))
            mockLoginViewModel._uiState.value = LoginUiState(isSuccess = true, isLoading = false)
        }

        fragmentScenario.onFragment { fragment ->
            fragment.handleGoogleSignInResult(mockIntentData)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val expectedDirections = LoginFragmentDirections.actionLoginFragmentToSignupFragment(
            socialUserEmail = userEmail,
            socialUserName = userName,
            isSocialLoginFlow = true
        )
        verify(exactly = 1) { mockNavController.navigate(expectedDirections) }
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun loginFragment_showsSnackbar_onGoogleLoginFailure() = runBlocking {
        val fragmentScenario = launchFragmentAndSetNavController()

        val errorMessage = "Google sign-in failed"
        val mockIntentData = mockk<Intent>(relaxed = true)
        val mockResult = Result.failure<String>(Exception(errorMessage))

        coEvery { googleSignInDataSource.handleSignInResult(mockIntentData) } returns mockResult

        // Mockeamos el método loginWithGoogle del ViewModel (aunque no debería llamarse si handleSignInResult falla)
        coEvery { mockLoginViewModel.loginWithGoogle(any()) } coAnswers {
            mockLoginViewModel._event.emit(LoginEvent.ShowMessage(errorMessage))
            mockLoginViewModel._uiState.value = LoginUiState(errorMessage = errorMessage, isLoading = false)
        }

        fragmentScenario.onFragment { fragment ->
            fragment.handleGoogleSignInResult(mockIntentData)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        onView(withText("Error en Sign-In: $errorMessage")).check(matches(isDisplayed()))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    // --- Tests de Login con Facebook ---

    @Test
    fun loginFragment_startsFacebookLoginFlow() {
        launchFragmentAndSetNavController()

        onView(withId(R.id.btnFacebookLogin)).perform(click())

        verify(exactly = 1) { facebookSignInDataSource.logInWithReadPermissions(any(), any()) }
    }

    @Test
    fun loginFragment_navigatesToHome_onSuccessfulFacebookLoginExistingUser() = runBlocking {
        val fragmentScenario = launchFragmentAndSetNavController()

        val accessToken = mockk<AccessToken>(relaxed = true)

        // Mockeamos el método loginWithFacebook del ViewModel
        coEvery { mockLoginViewModel.loginWithFacebook(accessToken.token) } coAnswers {
            mockLoginViewModel._event.emit(LoginEvent.NavigateToHome)
            mockLoginViewModel._uiState.value = LoginUiState(isSuccess = true, isLoading = false)
        }

        fragmentScenario.onFragment { fragment ->
            runBlocking {
                // Simulamos la emisión de un token por el canal de Facebook DataSource
                testFacebookAccessTokenChannel.emit(Result.success(accessToken.token)) // Usar testFacebookAccessTokenChannel
            }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        verify(exactly = 1) { mockNavController.navigate(R.id.action_loginFragment_to_mapFragment) }
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun loginFragment_navigatesToSignup_onSuccessfulFacebookLoginNewUser() = runBlocking {
        val fragmentScenario = launchFragmentAndSetNavController()

        val accessToken = mockk<AccessToken>(relaxed = true)
        val userEmail = "fb_new@example.com"
        val userName = "New Facebook User"

        // Mockeamos el método loginWithFacebook del ViewModel
        coEvery { mockLoginViewModel.loginWithFacebook(accessToken.token) } coAnswers {
            mockLoginViewModel._event.emit(LoginEvent.NavigateToSignupWithArgs(userEmail, userName, true))
            mockLoginViewModel._uiState.value = LoginUiState(isSuccess = true, isLoading = false)
        }

        fragmentScenario.onFragment { fragment ->
            runBlocking {
                testFacebookAccessTokenChannel.emit(Result.success(accessToken.token)) // Usar testFacebookAccessTokenChannel
            }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val expectedDirections = LoginFragmentDirections.actionLoginFragmentToSignupFragment(
            socialUserEmail = userEmail,
            socialUserName = userName,
            isSocialLoginFlow = true
        )
        verify(exactly = 1) { mockNavController.navigate(expectedDirections) }
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun loginFragment_showsSnackbar_onFacebookLoginFailure() = runBlocking {
        val fragmentScenario = launchFragmentAndSetNavController()

        val errorMessage = "Facebook login failed"
        val mockException = Exception(errorMessage)

        // Mockeamos el método loginWithFacebook del ViewModel
        coEvery { mockLoginViewModel.loginWithFacebook(any()) } coAnswers {
            mockLoginViewModel._event.emit(LoginEvent.ShowMessage(errorMessage))
            mockLoginViewModel._uiState.value = LoginUiState(errorMessage = errorMessage, isLoading = false)
        }

        fragmentScenario.onFragment { fragment ->
            runBlocking {
                testFacebookAccessTokenChannel.emit(Result.failure(mockException)) // Usar testFacebookAccessTokenChannel
            }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        onView(withText("Error: $errorMessage")).check(matches(isDisplayed()))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    // --- Tests de Navegación de Botones (Forgot Password, Back, Sign Up) ---

    @Test
    fun loginFragment_navigatesToForgotPassword_onClick() {
        launchFragmentAndSetNavController()

        onView(withId(R.id.tvForgotPassword)).perform(click())

        verify(exactly = 1) { mockNavController.navigate(R.id.action_loginFragment_to_forgotPasswordFragment) }
    }

    @Test
    fun loginFragment_navigatesBack_onClick() {
        launchFragmentAndSetNavController()

        onView(withId(R.id.ivBack)).perform(click())

        verify(exactly = 1) { mockNavController.popBackStack() }
    }

    @Test
    fun loginFragment_navigatesToSignUp_onClick() {
        launchFragmentAndSetNavController()

        onView(withId(R.id.tvSignUpBtn)).perform(click())

        verify(exactly = 1) { mockNavController.navigate(R.id.action_loginFragment_to_signupFragment) }
    }

    // --- Funciones Auxiliares ---

    private fun launchFragmentAndSetNavController(): androidx.fragment.app.testing.FragmentScenario<LoginFragment> {
        mockNavController = mockk(relaxed = true)
        mockLoginViewModel = mockk(relaxed = true) // Creamos un mock del ViewModel

        val fragmentScenario =
            launchFragmentInContainer<LoginFragment>(themeResId = R.style.Theme_QueerMap)
        fragmentScenario.moveToState(Lifecycle.State.STARTED)

        fragmentScenario.onFragment { fragment ->
            // Establecemos el mock NavController en el Fragment
            Navigation.setViewNavController(fragment.requireView(), mockNavController)

            // Usamos Reflection para inyectar nuestro mockViewModel en el Fragment
            // Esto es necesario porque 'by viewModels()' no permite inyectar un mock directamente.
            val viewModelField = LoginFragment::class.java.getDeclaredField("viewModel")
            viewModelField.isAccessible = true
            viewModelField.set(fragment, mockLoginViewModel)
        }
        return fragmentScenario
    }
}