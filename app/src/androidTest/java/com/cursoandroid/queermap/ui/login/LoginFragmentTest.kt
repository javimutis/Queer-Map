package com.cursoandroid.queermap.ui.login

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cursoandroid.queermap.HiltTestActivity
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.common.InputValidator
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import com.cursoandroid.queermap.ui.forgotpassword.ForgotPasswordFragment
import com.cursoandroid.queermap.ui.map.MapFragment
import com.cursoandroid.queermap.ui.signup.SignUpFragment
import com.cursoandroid.queermap.util.EspressoIdlingResource
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import org.junit.Assert.assertTrue
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.activity.result.ActivityResult // Necesario para crear el resultado de la actividad


// Custom ViewAction para esperar a que una vista sea clickable
fun waitForViewToBeClickable(): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): org.hamcrest.Matcher<View> {
            return allOf(isDisplayed(), isEnabled(), isClickable())
        }

        override fun getDescription(): String {
            return "Espera que la vista esté visible, habilitada y lista para click"
        }

        override fun perform(uiController: UiController, view: View) {
            val timeout = 5000L
            val interval = 100L
            var waited = 0L
            while (!(view.isShown && view.isClickable && view.isEnabled) && waited < timeout) {
                uiController.loopMainThreadForAtLeast(interval)
                waited += interval
            }
            if (!(view.isShown && view.isClickable && view.isEnabled)) {
                throw PerformException.Builder()
                    .withViewDescription("View with id ${view.id}")
                    .withCause(TimeoutException("La vista no estaba lista para click en $timeout ms"))
                    .build()
            }
        }
    }
}


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
class LoginFragmentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    // BindValue para inyectar un mock de LoginViewModel
    @BindValue
    @JvmField
    val mockLoginViewModel: LoginViewModel = mockk(relaxed = true)

    // BindValue para inyectar un mock de InputValidator
    @BindValue
    @JvmField
    val mockInputValidator: InputValidator = mockk(relaxed = true)

    // Custom FragmentFactory para asegurar que el LoginFragment reciba el mock de ViewModel
    @BindValue
    @JvmField
    val fragmentFactory: FragmentFactory = object : FragmentFactory() {
        override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
            return when (className) {
                LoginFragment::class.java.name -> LoginFragment().apply {
                    testViewModel =
                        mockLoginViewModel // Asigna el mock al testViewModel del fragment
                }
                // Añade otros fragments si son parte del grafo de navegación y necesarios para el test
                // Por ejemplo, para navegación a otras pantallas
                SignUpFragment::class.java.name -> SignUpFragment()
                ForgotPasswordFragment::class.java.name -> ForgotPasswordFragment()
                MapFragment::class.java.name -> MapFragment()
                else -> super.instantiate(classLoader, className)
            }
        }
    }

    // Inyección de los mocks de DataSources, también controlados por Hilt
    @Inject
    lateinit var mockGoogleSignInDataSource: GoogleSignInDataSource

    @Inject
    lateinit var mockFacebookSignInDataSource: FacebookSignInDataSource

    // Flows para controlar el estado y eventos del ViewModel mock
    private lateinit var uiStateFlow: MutableStateFlow<LoginUiState>
    private lateinit var eventFlow: MutableSharedFlow<LoginEvent>
    private lateinit var accessTokenChannelFlow: MutableSharedFlow<Result<String>>

    // Configuracion de coroutines para pruebas
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = UnconfinedTestDispatcher(testScheduler)

    // Variables para Espresso y Navigation Testing
    private lateinit var activityDecorView: View
    private lateinit var activityScenario: ActivityScenario<HiltTestActivity>
    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher) // Establece el dispatcher principal para coroutines
        hiltRule.inject() // Inyecta las dependencias, incluyendo los mocks @BindValue

        // Inicializa Intents antes de cada test que interactúe con actividades
        Intents.init()

        // Inicializa los flows con un estado inicial
        uiStateFlow = MutableStateFlow(LoginUiState())
        eventFlow = MutableSharedFlow()
        accessTokenChannelFlow = MutableSharedFlow()

        clearAllMocks() // Limpia todos los mocks antes de cada test para evitar interferencias

        // Stub del comportamiento del mockLoginViewModel
        every { mockLoginViewModel.uiState } returns uiStateFlow
        every { mockLoginViewModel.event } returns eventFlow

        // Stub para GoogleSignInDataSource: getSignInIntent debe retornar un Intent válido
        every { mockGoogleSignInDataSource.getSignInIntent() } returns Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://example.com/oauth")
        )
        // handleSignInResult será mockeado en el test específico para devolver el ID token
        coEvery { mockGoogleSignInDataSource.handleSignInResult(any()) } returns Result.success("fake_google_id_token")


        // Stub del comportamiento general del mockLoginViewModel
        coEvery { mockLoginViewModel.loginWithEmail(any(), any()) } coAnswers { /* do nothing */ }
        // loginWithGoogle será mockeado en el test específico
        coEvery { mockLoginViewModel.loginWithGoogle(any()) } coAnswers { /* will be defined in specific test */ }
        coEvery { mockLoginViewModel.loginWithFacebook(any()) } coAnswers { /* do nothing */ }
        coEvery { mockLoginViewModel.onForgotPasswordClicked() } coAnswers { /* do nothing */ }
        coEvery { mockLoginViewModel.onBackPressed() } coAnswers { /* do nothing */ }
        coEvery {
            mockLoginViewModel.saveUserCredentials(
                any(),
                any()
            )
        } coAnswers { /* do nothing */ }
        coEvery { mockLoginViewModel.loadUserCredentials() } coAnswers { /* do nothing */ } // Llamado en onViewCreated

        // Stub para InputValidator
        every { mockInputValidator.isValidEmail(any()) } returns true
        every { mockInputValidator.isValidPassword(any()) } returns true
        every { mockInputValidator.isValidFullName(any()) } returns true
        every { mockInputValidator.isValidUsername(any()) } returns true
        every { mockInputValidator.isStrongPassword(any()) } returns true
        every { mockInputValidator.isValidBirthday(any()) } returns true

        // Stub para FacebookSignInDataSource
        every { mockFacebookSignInDataSource.accessTokenChannel } returns accessTokenChannelFlow
        every { mockFacebookSignInDataSource.registerCallback(any()) } answers { /* do nothing */ }
        every {
            mockFacebookSignInDataSource.logInWithReadPermissions(
                any(),
                any()
            )
        } answers { /* do nothing */ }

        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)

        navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        activityScenario = ActivityScenario.launch(HiltTestActivity::class.java)

        activityScenario.onActivity { activity ->
            activity.supportFragmentManager.fragmentFactory = fragmentFactory

            val fragment = fragmentFactory.instantiate(
                activity.classLoader,
                LoginFragment::class.java.name
            ) as LoginFragment

            fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                viewLifecycleOwner?.let {
                    navController.setGraph(R.navigation.nav_graph)
                    navController.setCurrentDestination(R.id.loginFragment)

                    fragment.view?.post {
                        Navigation.setViewNavController(fragment.requireView(), navController)
                    }
                }
            }

            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment, null)
                .commitNow()

            // Asegura que todas las coroutines iniciales del fragmento se completen
            testScheduler.advanceUntilIdle()
            activityDecorView = activity.window.decorView
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // Restablece el dispatcher principal
        IdlingRegistry.getInstance()
            .unregister(EspressoIdlingResource.countingIdlingResource) // Desregistra el IdlingResource

        // Libera Intents después de cada test
        Intents.release()

        if (this::activityScenario.isInitialized) {
            activityScenario.close() // Cierra el escenario de la actividad
        }

        // Restablece el estado de los flows para el siguiente test
        if (this::uiStateFlow.isInitialized) {
            uiStateFlow.value = LoginUiState()
        }

        testScheduler.advanceUntilIdle() // Asegura que todas las coroutines pendientes se completen
    }
    @Test
    fun when_google_sign_in_button_clicked_and_result_is_success_then_navigates_to_home() =
        runTest(testDispatcher) {
            // Arrange
            val fakeIdToken = "some_google_id_token"
            val successMessage = "Inicio de sesión con Google exitoso"

            // Prepara el Intent y ActivityResult que simularán el resultado del inicio de sesión de Google
            val resultData =
                Intent() // Puedes agregar datos reales si tu handleSignInResult los lee del Intent
            val activityResult = ActivityResult(Activity.RESULT_OK, resultData)

            // Define el comportamiento de intending: cuando se lance un Intent con esta acción/datos, responde con activityResult
            intending(hasAction(Intent.ACTION_VIEW) and hasData(Uri.parse("https://example.com/oauth")))
                .respondWith(activityResult)

            // Mockear el método handleSignInResult de GoogleSignInDataSource para que devuelva el token falso.
            // Esto se llamará cuando el ActivityResultLauncher procese el resultado mockeado.
            coEvery { mockGoogleSignInDataSource.handleSignInResult(any()) } returns Result.success(
                fakeIdToken
            )

            // Define el comportamiento del mockLoginViewModel.loginWithGoogle
            // Esto se ejecutará cuando el fragmento (a través de handleGoogleSignInResult) llame a loginWithGoogle
            coEvery { mockLoginViewModel.loginWithGoogle(fakeIdToken) } coAnswers {
                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = true))
                testScheduler.advanceTimeBy(100) // Simula un pequeño retraso
                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = false, isSuccess = true))
                eventFlow.emit(LoginEvent.NavigateToHome)
                eventFlow.emit(LoginEvent.ShowMessage(successMessage))
            }

            // Act
            // Realiza un click en el botón de Google Sign-In
            onView(withId(R.id.btnGoogleSignIn)).perform(click())

            // Asegura que Espresso y todas las coroutines activadas por el click y el resultado del Intent se ejecuten
            Espresso.onIdle() // Espera a que el hilo principal de Espresso esté inactivo
            testScheduler.advanceUntilIdle() // Avanza el tiempo para que todas las coroutines pendientes finalicen

            // Assert
            // Verifica que el Intent de inicio de sesión de Google fue lanzado
            intended(hasAction(Intent.ACTION_VIEW) and hasData(Uri.parse("https://example.com/oauth")))

            // Verifica que handleSignInResult fue llamado en el mock de GoogleSignInDataSource
            coVerify(exactly = 1) { mockGoogleSignInDataSource.handleSignInResult(any()) }
            // Verifica que loginWithGoogle fue llamado en el mock de LoginViewModel con el token esperado
            coVerify(exactly = 1) { mockLoginViewModel.loginWithGoogle(fakeIdToken) }

            // Verifica que el Snackbar con el mensaje de éxito se muestre
            onView(withText(successMessage))
                .inRoot(withDecorView(not(`is`(activityDecorView)))) // Verifica en una ventana diferente para el Snackbar
                .check(matches(isDisplayed()))

            // Verifica que la barra de progreso no esté visible
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))

            // Verifica la navegación a la pantalla de inicio (MapFragment)
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.mapFragment)
        }

    @Test
    fun a_very_basic_test_to_check_setup() {
        onView(withId(R.id.tvTitle)).check(matches(isDisplayed()))
    }

    @Test
    fun when_login_fragment_is_launched_then_all_ui_elements_are_displayed() {
        onView(withId(R.id.tvTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.etEmailLogin)).check(matches(isDisplayed()))
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()))
        onView(withId(R.id.btnLogin)).check(matches(isDisplayed()))
        onView(withId(R.id.btnGoogleSignIn)).check(matches(isDisplayed()))
        onView(withId(R.id.btnFacebookLogin)).check(matches(isDisplayed()))
        onView(withId(R.id.tvForgotPassword)).check(matches(isDisplayed()))
        onView(withId(R.id.tvSignUpBtn)).check(matches(isDisplayed()))
        onView(withId(R.id.ivBack)).check(matches(isDisplayed()))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun when_email_field_is_typed_then_text_is_updated() {
        onView(withId(R.id.etEmailLogin)).perform(typeText("test@example.com"), closeSoftKeyboard())
        onView(withId(R.id.etEmailLogin)).check(matches(withText("test@example.com")))
    }

    @Test
    fun when_password_field_is_typed_then_text_is_updated() {
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).check(matches(withText("password123")))
    }

    @Test
    fun when_valid_credentials_are_entered_and_login_clicked_then_navigates_to_home() =
        runTest(testDispatcher) {
            val email = "valid@example.com"
            val password = "validpassword"

            every { mockInputValidator.isValidEmail(email) } returns true
            every { mockInputValidator.isValidPassword(password) } returns true

            coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = true))
                testScheduler.advanceTimeBy(100)
                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = false, isSuccess = true))
                eventFlow.emit(LoginEvent.NavigateToHome)
            }

            onView(withId(R.id.etEmailLogin)).perform(typeText(email))
            onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(click())

            testScheduler.advanceUntilIdle()

            assertThat(navController.currentDestination?.id).isEqualTo(R.id.mapFragment)
        }

    @Test
    fun when_invalid_email_is_entered_then_shows_error_message() = runTest(testDispatcher) {
        val email = "invalid-email"
        val password = "validpassword"

        every { mockInputValidator.isValidEmail(email) } returns false
        every { mockInputValidator.isValidPassword(password) } returns true

        coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
            uiStateFlow.emit(uiStateFlow.value.copy(isLoading = false, isEmailInvalid = true))
            eventFlow.emit(LoginEvent.ShowMessage("Por favor ingresa un email válido"))
        }
        onView(withId(R.id.etEmailLogin)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        testScheduler.advanceUntilIdle()

        onView(withText("Por favor ingresa un email válido"))
            .check(matches(isDisplayed()))

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun when_invalid_password_is_entered_then_shows_error_message() = runTest(testDispatcher) {
        val email = "valid@example.com"
        val password = "short"

        every { mockInputValidator.isValidEmail(email) } returns true
        every { mockInputValidator.isValidPassword(password) } returns false

        coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
            uiStateFlow.emit(
                uiStateFlow.value.copy(
                    isLoading = false,
                    isPasswordInvalid = true
                )
            )
            eventFlow.emit(LoginEvent.ShowMessage("La contraseña debe tener al menos 6 caracteres"))
        }

        onView(withId(R.id.etEmailLogin)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        testScheduler.advanceUntilIdle()

        onView(withText("La contraseña debe tener al menos 6 caracteres"))
            .check(matches(isDisplayed()))

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun when_login_fails_with_network_error_then_shows_specific_message() =
        runTest(testDispatcher) {
            val email = "test@example.com"
            val password = "password123"
            val errorMessage = "Error de red. Por favor, revisa tu conexión"

            every { mockInputValidator.isValidEmail(email) } returns true
            every { mockInputValidator.isValidPassword(password) } returns true

            coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = true))
                testScheduler.advanceTimeBy(100)
                uiStateFlow.emit(
                    uiStateFlow.value.copy(
                        isLoading = false,
                        errorMessage = errorMessage
                    )
                )
                eventFlow.emit(LoginEvent.ShowMessage(errorMessage))
            }

            onView(withId(R.id.etEmailLogin)).perform(typeText(email))
            onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(click())

            coVerify(exactly = 1) { mockLoginViewModel.loginWithEmail(email, password) }

            testScheduler.advanceUntilIdle()

            onView(withText(errorMessage)).check(matches(isDisplayed()))

            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        }

    //passed
    @Test
    fun when_google_sign_in_button_is_clicked_then_getSignInIntent_is_called() =
        runTest(testDispatcher) {
            uiStateFlow.emit(uiStateFlow.value.copy(isLoading = false))
            testScheduler.advanceUntilIdle()

            onView(withId(R.id.btnGoogleSignIn)).perform(scrollTo(), closeSoftKeyboard())

            testScheduler.advanceTimeBy(500)

            onView(withId(R.id.btnGoogleSignIn))
                .perform(waitForViewToBeClickable(), click())

            testScheduler.advanceUntilIdle()

            coVerify(exactly = 1) { mockGoogleSignInDataSource.getSignInIntent() }
        }
}
//aqui va el test que etsamos probando

//    @Test
//    fun when_google_sign_in_result_is_success_for_new_user_then_navigates_to_signup_with_args() =
//        runTest(testDispatcher) {
//            val intentData = mockk<Intent>()
//            val idToken = "some_google_id_token"
//            val socialEmail = "new_google@example.com"
//            val socialName = "New Google User"
//
//            coEvery { mockGoogleSignInDataSource.handleSignInResult(intentData) } returns Result.success(
//                idToken
//            )
//            coEvery { mockLoginViewModel.loginWithGoogle(idToken) } coAnswers {
//                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = true))
//                testScheduler.advanceTimeBy(100)
//                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = false, isSuccess = true))
//                eventFlow.emit(
//                    LoginEvent.NavigateToSignupWithArgs(
//                        socialUserEmail = socialEmail,
//                        socialUserName = socialName,
//                        isSocialLoginFlow = true
//                    )
//                )
//                eventFlow.emit(LoginEvent.ShowMessage("Completa tu perfil para continuar"))
//            }
//
//            activityScenario.onActivity { activity ->
//                val fragment =
//                    activity.supportFragmentManager.findFragmentById(android.R.id.content) as LoginFragment
//                fragment.handleGoogleSignInResult(intentData)
//            }
//            testScheduler.advanceUntilIdle()
//
//            coVerify(exactly = 1) { mockGoogleSignInDataSource.handleSignInResult(intentData) }
//            coVerify(exactly = 1) { mockLoginViewModel.loginWithGoogle(idToken) }
//
//            onView(withText("Completa tu perfil para continuar"))
//                .inRoot(withDecorView(not(`is`(activityDecorView))))
//                .check(matches(isDisplayed()))
//            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
//
//            assertThat(navController.currentDestination?.id).isEqualTo(R.id.signupFragment)
//            val args = navController.backStack.last().arguments
//            assertThat(args?.getString("socialUserEmail")).isEqualTo(socialEmail)
//            assertThat(args?.getString("socialUserName")).isEqualTo(socialName)
//            assertThat(args?.getBoolean("isSocialLoginFlow")).isTrue()
//        }
//
//
//    @Test
//    fun when_google_sign_in_result_is_failure_then_shows_error_message() = runTest(testDispatcher) {
//        val intentData = mockk<Intent>()
//        val exceptionMessage = "Fallo de Google"
//        val expectedSnackbarMessage = "Error en Sign-In: $exceptionMessage"
//
//        coEvery { mockGoogleSignInDataSource.handleSignInResult(intentData) } returns Result.failure(
//            Exception(exceptionMessage)
//        )
//
//        activityScenario.onActivity { activity ->
//            val fragment =
//                activity.supportFragmentManager.findFragmentById(android.R.id.content) as LoginFragment
//            fragment.handleGoogleSignInResult(intentData)
//        }
//        testScheduler.advanceUntilIdle()
//
//        coVerify(exactly = 1) { mockGoogleSignInDataSource.handleSignInResult(intentData) }
//        coVerify(exactly = 0) { mockLoginViewModel.loginWithGoogle(any()) }
//
//        onView(withText(expectedSnackbarMessage))
//            .inRoot(withDecorView(not(`is`(activityDecorView))))
//            .check(matches(isDisplayed()))
//        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
//    }
//
//
//    @Test
//    fun when_facebook_login_button_is_clicked_then_logInWithReadPermissions_is_called() =
//        runTest(testDispatcher) {
//            uiStateFlow.emit(uiStateFlow.value.copy(isLoading = false))
//            testScheduler.advanceUntilIdle()
//
//            // Eliminar Thread.sleep(2000)
//
//            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
//
//            onView(withId(R.id.btnFacebookLogin)).perform(scrollTo(), waitUntilVisibleAndEnabledAndCompletelyDisplayed(), click())
//            testScheduler.advanceUntilIdle()
//
//            coVerify(exactly = 1) {
//                mockFacebookSignInDataSource.logInWithReadPermissions(
//                    any(),
//                    listOf("email", "public_profile")
//                )
//            }
//        }
//
//    @Test
//    fun when_facebook_accessTokenChannel_success_then_registers_user_and_navigates_to_home() =
//        runTest(testDispatcher) {
//            val accessToken = "facebook_access_token"
//
//            coEvery { mockLoginViewModel.loginWithFacebook(accessToken) } coAnswers {
//                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = true))
//                testScheduler.advanceTimeBy(100)
//                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = false, isSuccess = true))
//                eventFlow.emit(LoginEvent.NavigateToHome)
//                eventFlow.emit(LoginEvent.ShowMessage("Inicio de sesión con Facebook exitoso"))
//            }
//
//            launch(testDispatcher) {
//                accessTokenChannelFlow.emit(Result.success(accessToken))
//            }
//            testScheduler.advanceUntilIdle()
//
//            coVerify(exactly = 1) { mockLoginViewModel.loginWithFacebook(accessToken) }
//
//            onView(withText("Inicio de sesión con Facebook exitoso"))
//                .inRoot(withDecorView(not(`is`(activityDecorView))))
//                .check(matches(isDisplayed()))
//            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
//            assertThat(navController.currentDestination?.id).isEqualTo(R.id.mapFragment)
//        }
//
//
//    @Test
//    fun when_facebook_accessTokenChannel_failure_then_shows_error_message() =
//        runTest(testDispatcher) {
//            val exceptionMessage = "Facebook login failed"
//            val expectedSnackbarMessage = "Error: $exceptionMessage"
//
//            launch(testDispatcher) {
//                accessTokenChannelFlow.emit(Result.failure(Exception(exceptionMessage)))
//            }
//            testScheduler.advanceUntilIdle()
//
//            coVerify(exactly = 0) { mockLoginViewModel.loginWithFacebook(any()) }
//
//            onView(withText(expectedSnackbarMessage))
//                .inRoot(withDecorView(not(`is`(activityDecorView))))
//                .check(matches(isDisplayed()))
//            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
//        }
//
//
//    @Test
//    fun when_forgot_password_is_clicked_then_navigates_to_ForgotPasswordFragment() =
//        runTest(testDispatcher) {
//            coEvery { mockLoginViewModel.onForgotPasswordClicked() } coAnswers {
//                eventFlow.emit(LoginEvent.NavigateToForgotPassword)
//            }
//
//            onView(withId(R.id.tvForgotPassword)).perform(click())
//            testScheduler.advanceUntilIdle()
//
//            coVerify(exactly = 1) { mockLoginViewModel.onForgotPasswordClicked() }
//            assertThat(navController.currentDestination?.id).isEqualTo(R.id.forgotPasswordFragment)
//        }
//
//
//    @Test
//    fun when_back_button_is_clicked_then_navigates_back() = runTest(testDispatcher) {
//        navController.navigate(R.id.mapFragment)
//        testScheduler.advanceUntilIdle()
//        assertThat(navController.currentDestination?.id).isEqualTo(R.id.mapFragment)
//
//        coEvery { mockLoginViewModel.onBackPressed() } coAnswers {
//            eventFlow.emit(LoginEvent.NavigateBack)
//        }
//
//        onView(withId(R.id.ivBack)).perform(click())
//        testScheduler.advanceUntilIdle()
//
//        coVerify(exactly = 1) { mockLoginViewModel.onBackPressed() }
//        assertThat(navController.currentDestination?.id).isEqualTo(R.id.loginFragment)
//    }
