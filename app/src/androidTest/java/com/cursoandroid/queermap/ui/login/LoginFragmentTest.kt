package com.cursoandroid.queermap.ui.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
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
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
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
import com.cursoandroid.queermap.util.EspressoIdlingResource
import com.cursoandroid.queermap.util.MainDispatcherRule
import com.cursoandroid.queermap.util.Result
import com.facebook.AccessToken
import com.facebook.AuthenticationToken
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.TimeoutException

// --- UTILIDADES DE ESPRESSO PERSONALIZADAS (sin cambios) ---

fun waitForViewToBeClickable(): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
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

fun waitUntilVisibleAndEnabledAndCompletelyDisplayed(): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return allOf(isDisplayed(), isEnabled(), isCompletelyDisplayed())
        }

        override fun getDescription(): String {
            return "Espera hasta que la vista esté visible, habilitada y completamente dibujada"
        }

        override fun perform(uiController: UiController, view: View) {
            val timeout = 5000L
            val interval = 50L
            var waited = 0L
            while (!constraints.matches(view) && waited < timeout) {
                uiController.loopMainThreadForAtLeast(interval)
                waited += interval
            }
            if (!constraints.matches(view)) {
                throw PerformException.Builder()
                    .withActionDescription(this.description)
                    .withViewDescription(view.toString())
                    .withCause(TimeoutException("La vista no estaba lista en $timeout ms."))
                    .build()
            }
        }
    }
}

fun withDecorView(matcher: Matcher<View>): Matcher<View> {
    return object : org.hamcrest.TypeSafeMatcher<View>() {
        override fun describeTo(description: org.hamcrest.Description) {
            matcher.describeTo(description)
        }

        override fun matchesSafely(item: View): Boolean {
            return matcher.matches(item.rootView.findViewById<View>(android.R.id.content))
        }
    }
}


/* Clase de Test del Fragmento de Login */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
class LoginFragmentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val mainDispatcherRule = MainDispatcherRule()

    // @BindValue asegura que este mock sea proporcionado cuando Hilt inyecte LoginViewModel
    @BindValue
    @JvmField
    val mockLoginViewModel: LoginViewModel = mockk(relaxed = true)

    @BindValue
    @JvmField
    val mockInputValidator: InputValidator = mockk(relaxed = true)

    private lateinit var mockGoogleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var mockGoogleSignInDataSource: GoogleSignInDataSource
    private lateinit var mockFacebookSignInDataSource: FacebookSignInDataSource

    private lateinit var activityScenario: ActivityScenario<HiltTestActivity>
    private lateinit var mockNavController: TestNavHostController

    private lateinit var uiStateFlow: MutableStateFlow<LoginUiState>
    private lateinit var eventFlow: MutableSharedFlow<LoginEvent>

    private lateinit var activityDecorView: View

    private val FAKE_GOOGLE_ID_TOKEN = "fake_google_id_token"
    private lateinit var mockGoogleSignInResultIntent: Intent

    private lateinit var facebookCallbackSlot: CapturingSlot<FacebookCallback<LoginResult>>

    @Before
    fun setUp() {
        hiltRule.inject()

        Intents.init()

        clearAllMocks()

        uiStateFlow = MutableStateFlow(LoginUiState())
        eventFlow = MutableSharedFlow()

        every { mockLoginViewModel.uiState } returns uiStateFlow
        every { mockLoginViewModel.event } returns eventFlow

        mockGoogleSignInDataSource = mockk(relaxed = true)
        mockFacebookSignInDataSource = mockk(relaxed = true)

        every { mockGoogleSignInDataSource.getSignInIntent() } returns Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://example.com/oauth")
        )

        mockGoogleSignInLauncher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)
        mockGoogleSignInResultIntent = mockk<Intent>(relaxed = true)

        every { mockGoogleSignInLauncher.launch(any()) } answers {
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()
            activityScenario.onActivity { activity ->
                mainDispatcherRule.testScope.launch {
                    val fragment =
                        activity.supportFragmentManager.findFragmentById(android.R.id.content) as? LoginFragment
                    fragment?.handleGoogleSignInResult(mockGoogleSignInResultIntent)
                }
            }
        }

        facebookCallbackSlot = CapturingSlot()

        every {
            mockFacebookSignInDataSource.registerCallback(
                any(),
                capture(facebookCallbackSlot)
            )
        } just Runs

        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)

        mockNavController = TestNavHostController(ApplicationProvider.getApplicationContext())

        activityScenario = ActivityScenario.launch(HiltTestActivity::class.java)

        activityScenario.onActivity { activity ->
            mockNavController.setGraph(R.navigation.nav_graph)
            mockNavController.setCurrentDestination(R.id.loginFragment)

            val fragment = LoginFragment() // Hilt will provide the mock ViewModel to this instance

            activity.supportFragmentManager.beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow()

            val currentFragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content) as LoginFragment
            Navigation.setViewNavController(currentFragment.requireView(), mockNavController)

            activityDecorView = activity.window.decorView

            // REMOVIDO: No necesitas asignar testViewModel aquí. Hilt lo manejará.
            // currentFragment.testViewModel = mockLoginViewModel

            // Mantén las asignaciones para otros mocks que no son ViewModels
            currentFragment.testGoogleSignInLauncher = mockGoogleSignInLauncher
            currentFragment.testGoogleSignInDataSource = mockGoogleSignInDataSource
            currentFragment.testFacebookSignInDataSource = mockFacebookSignInDataSource
            currentFragment.testCallbackManager = mockk(relaxed = true)
        }
        Espresso.onIdle()
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        Intents.release()
        if (this::activityScenario.isInitialized) {
            activityScenario.close()
        }
        clearAllMocks()
    }
//passed
    @Test
    fun when_login_fragment_is_launched_all_essential_ui_elements_are_displayed() {
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


    /* Pruebas de Interacción de Usuario y Actualizaciones de UI */
//passed
    @Test
    fun when_typing_in_email_field_text_is_updated() {
        onView(withId(R.id.etEmailLogin)).perform(typeText("test@example.com"), closeSoftKeyboard())
        onView(withId(R.id.etEmailLogin)).check(matches(withText("test@example.com")))
    }

    @Test
    fun when_typing_in_password_field_text_is_updated() {
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).check(matches(withText("password123")))
    }

    @Test
    fun when_login_loads_credentials_email_and_password_fields_are_updated() = runTest {
        val savedEmail = "saved@example.com"
        val savedPassword = "savedPassword123"

        coEvery { mockLoginViewModel.loadUserCredentials() } coAnswers {
            uiStateFlow.emit(uiStateFlow.value.copy(email = savedEmail, password = savedPassword))

        }

        advanceUntilIdle()

        onView(withId(R.id.etEmailLogin)).check(matches(withText(savedEmail)))
        onView(withId(R.id.etPassword)).check(matches(withText(savedPassword)))

        coVerify(exactly = 1) { mockLoginViewModel.loadUserCredentials() }
    }
    /* Pruebas de Interacción del Botón de Login (Email/Password) */

    @Test
    fun when_login_button_is_clicked_loginWithEmail_is_called_with_correct_data_and_navigates_to_home_on_success() =
        runTest {
            val email = "valid@example.com"
            val password = "validpassword"

            coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = true))
                this@runTest.advanceUntilIdle()
                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = false, isSuccess = true))
                eventFlow.emit(LoginEvent.NavigateToHome)
                eventFlow.emit(LoginEvent.ShowMessage("Inicio de sesión exitoso"))
            }

            every { mockNavController.navigate(R.id.action_loginFragment_to_mapFragment) } just Runs

            onView(withId(R.id.etEmailLogin)).perform(typeText(email))
            onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(click())

            advanceUntilIdle()

            coVerify(exactly = 1) { mockLoginViewModel.loginWithEmail(email, password) }

            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))

            assertThat(mockNavController.currentDestination?.id).isEqualTo(R.id.loginFragment)
            coVerify(exactly = 1) { mockNavController.navigate(R.id.action_loginFragment_to_mapFragment) }
        }

    @Test
    fun when_login_button_is_clicked_and_email_is_invalid_error_message_is_shown() = runTest {
        val email = "invalid-email"
        val password = "validpassword"
        val errorMessage = "Por favor ingresa un email válido"

        coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
            uiStateFlow.emit(uiStateFlow.value.copy(isEmailInvalid = true))
            eventFlow.emit(LoginEvent.ShowMessage(errorMessage))
        }

        onView(withId(R.id.etEmailLogin)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        advanceUntilIdle()

        coVerify(exactly = 1) { mockLoginViewModel.loginWithEmail(email, password) }

        onView(withText(errorMessage))
            .inRoot(withDecorView(not(activityDecorView)))
            .check(matches(isDisplayed()))

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun when_login_button_is_clicked_and_password_is_invalid_error_message_is_shown() = runTest {
        val email = "valid@example.com"
        val password = "short"
        val errorMessage = "La contraseña debe tener al menos 6 caracteres"

        coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
            uiStateFlow.emit(uiStateFlow.value.copy(isPasswordInvalid = true))
            eventFlow.emit(LoginEvent.ShowMessage(errorMessage))
        }

        onView(withId(R.id.etEmailLogin)).perform(typeText(email))
        onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        advanceUntilIdle()

        coVerify(exactly = 1) { mockLoginViewModel.loginWithEmail(email, password) }

        onView(withText(errorMessage))
            .inRoot(withDecorView(not(activityDecorView)))
            .check(matches(isDisplayed()))

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun when_login_fails_due_to_general_error_error_message_is_shown() = runTest {
        val email = "test@example.com"
        val password = "password123"
        val errorMessage = "Error inesperado. Intenta de nuevo más tarde"

        coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
            uiStateFlow.emit(uiStateFlow.value.copy(isLoading = true))
            this@runTest.advanceUntilIdle()
            uiStateFlow.emit(uiStateFlow.value.copy(isLoading = false, errorMessage = errorMessage))
            eventFlow.emit(LoginEvent.ShowMessage(errorMessage))
        }

        onView(withId(R.id.etEmailLogin)).perform(typeText(email))
        onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        advanceUntilIdle()

        coVerify(exactly = 1) { mockLoginViewModel.loginWithEmail(email, password) }

        onView(withText(errorMessage))
            .inRoot(withDecorView(not(activityDecorView)))
            .check(matches(isDisplayed()))

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    /* Pruebas de Interacción de Login Social (Google) */

    @Test
    fun when_google_button_is_clicked_launcher_is_invoked_and_navigates_to_home_on_success() =
        runTest {
            val idToken = FAKE_GOOGLE_ID_TOKEN
            val successMessage = "Inicio de sesión social exitoso"

            coEvery { mockGoogleSignInDataSource.handleSignInResult(mockGoogleSignInResultIntent) } returns Result.Success(
                idToken
            )

            coEvery { mockLoginViewModel.loginWithGoogle(idToken) } coAnswers {
                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = true))
                this@runTest.advanceUntilIdle()
                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = false, isSuccess = true))
                eventFlow.emit(LoginEvent.NavigateToHome)
                eventFlow.emit(LoginEvent.ShowMessage(successMessage))
            }

            every { mockNavController.navigate(R.id.action_loginFragment_to_mapFragment) } just Runs

            onView(withId(R.id.btnGoogleSignIn)).perform(click())

            advanceUntilIdle()

            coVerify(exactly = 1) { mockGoogleSignInLauncher.launch(any()) }
            coVerify(exactly = 1) {
                mockGoogleSignInDataSource.handleSignInResult(
                    mockGoogleSignInResultIntent
                )
            }
            coVerify(exactly = 1) { mockLoginViewModel.loginWithGoogle(idToken) }

            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
            assertThat(mockNavController.currentDestination?.id).isEqualTo(R.id.loginFragment)
            coVerify(exactly = 1) { mockNavController.navigate(R.id.action_loginFragment_to_mapFragment) }
            onView(withText(successMessage))
                .inRoot(withDecorView(not(activityDecorView)))
                .check(matches(isDisplayed()))
        }

    @Test
    fun when_google_login_is_for_new_user_navigates_to_signup_with_args() = runTest {
        val idToken = "some_new_google_id_token"
        val socialEmail = "new.user@example.com"
        val socialName = "New Google User"
        val messageForNewUser = "Completa tu perfil para continuar"

        coEvery { mockGoogleSignInDataSource.handleSignInResult(mockGoogleSignInResultIntent) } returns Result.Success(
            idToken
        )

        coEvery { mockLoginViewModel.loginWithGoogle(idToken) } coAnswers {
            uiStateFlow.emit(uiStateFlow.value.copy(isLoading = false, isSuccess = true))
            eventFlow.emit(
                LoginEvent.NavigateToSignupWithArgs(
                    socialUserEmail = socialEmail,
                    socialUserName = socialName,
                    isSocialLoginFlow = true
                )
            )
            eventFlow.emit(LoginEvent.ShowMessage(messageForNewUser))
        }

        val navArgsSlot = slot<Bundle>()
        every {
            mockNavController.navigate(
                R.id.action_loginFragment_to_signupFragment,
                capture(navArgsSlot)
            )
        } just Runs

        onView(withId(R.id.btnGoogleSignIn)).perform(click())

        advanceUntilIdle()

        coVerify(exactly = 1) { mockGoogleSignInLauncher.launch(any()) }
        coVerify(exactly = 1) {
            mockGoogleSignInDataSource.handleSignInResult(
                mockGoogleSignInResultIntent
            )
        }
        coVerify(exactly = 1) { mockLoginViewModel.loginWithGoogle(idToken) }

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))

        assertThat(mockNavController.currentDestination?.id).isEqualTo(R.id.loginFragment)
        coVerify(exactly = 1) {
            mockNavController.navigate(
                R.id.action_loginFragment_to_signupFragment,
                any()
            )
        }
        assertThat(navArgsSlot.captured.getString("socialUserEmail")).isEqualTo(socialEmail)
        assertThat(navArgsSlot.captured.getString("socialUserName")).isEqualTo(socialName)
        assertThat(navArgsSlot.captured.getBoolean("isSocialLoginFlow")).isTrue()

        onView(withText(messageForNewUser))
            .inRoot(withDecorView(not(activityDecorView)))
            .check(matches(isDisplayed()))
    }

    @Test
    fun when_google_login_fails_error_message_is_shown() = runTest {
        val exceptionMessage = "Error de autenticación de Google"
        val expectedDisplayMessage =
            "Error en Sign-In: $exceptionMessage"

        coEvery { mockGoogleSignInDataSource.handleSignInResult(mockGoogleSignInResultIntent) } returns Result.Failure(
            Exception(exceptionMessage)
        )

        coEvery { eventFlow.emit(LoginEvent.ShowMessage(expectedDisplayMessage)) } just Runs

        onView(withId(R.id.btnGoogleSignIn)).perform(click())

        advanceUntilIdle()

        coVerify(exactly = 1) { mockGoogleSignInLauncher.launch(any()) }
        coVerify(exactly = 1) {
            mockGoogleSignInDataSource.handleSignInResult(
                mockGoogleSignInResultIntent
            )
        }
        coVerify(exactly = 0) { mockLoginViewModel.loginWithGoogle(any()) }

        onView(withText(expectedDisplayMessage))
            .inRoot(withDecorView(not(activityDecorView)))
            .check(matches(isDisplayed()))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    /* Pruebas de Interacción de Login Social (Facebook) */

    @Test
    fun when_facebook_button_is_clicked_logInWithReadPermissions_is_called() = runTest {
        onView(withId(R.id.btnFacebookLogin))
            .perform(waitUntilVisibleAndEnabledAndCompletelyDisplayed(), click())

        advanceUntilIdle()

        coVerify(exactly = 1) {
            mockFacebookSignInDataSource.logInWithReadPermissions(
                any(),
                listOf("email", "public_profile")
            )
        }
    }

    @Test
    fun when_facebook_access_token_is_received_loginWithFacebook_is_called_and_navigates_to_home() =
        runTest {
            val accessTokenString = "facebook_access_token_simulated"
            val successMessage = "Inicio de sesión con Facebook exitoso"

            every { mockFacebookSignInDataSource.logInWithReadPermissions(any(), any()) } just Runs

            coEvery { mockLoginViewModel.loginWithFacebook(accessTokenString) } coAnswers {
                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = true))
                this@runTest.advanceUntilIdle()
                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = false, isSuccess = true))
                eventFlow.emit(LoginEvent.NavigateToHome)
                eventFlow.emit(LoginEvent.ShowMessage(successMessage))
            }

            every { mockNavController.navigate(R.id.action_loginFragment_to_mapFragment) } just Runs

            onView(withId(R.id.btnFacebookLogin)).perform(click())

            coVerify(exactly = 1) {
                mockFacebookSignInDataSource.logInWithReadPermissions(any(), any())
            }

            val mockAccessToken = mockk<AccessToken>(relaxed = true) {
                every { token } returns accessTokenString
            }
            val mockAuthenticationToken = mockk<AuthenticationToken>(relaxed = true) {
                every { token } returns "mock_auth_token"
            }
            val mockLoginResult = LoginResult(
                accessToken = mockAccessToken,
                authenticationToken = mockAuthenticationToken,
                recentlyGrantedPermissions = setOf("email", "public_profile"),
                recentlyDeniedPermissions = emptySet()
            )

            // Invoca directamente el callback capturado para simular el éxito de Facebook
            facebookCallbackSlot.captured.onSuccess(mockLoginResult)

            advanceUntilIdle()

            coVerify(exactly = 1) { mockLoginViewModel.loginWithFacebook(accessTokenString) }
            onView(withText(successMessage))
                .inRoot(withDecorView(not(activityDecorView)))
                .check(matches(isDisplayed()))
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
            assertThat(mockNavController.currentDestination?.id).isEqualTo(R.id.loginFragment)
            coVerify(exactly = 1) { mockNavController.navigate(R.id.action_loginFragment_to_mapFragment) }
        }

    @Test
    fun when_facebook_login_is_cancelled_error_message_is_shown() = runTest {
        val errorMessage = "Inicio de sesión con Facebook cancelado."

        every { mockFacebookSignInDataSource.logInWithReadPermissions(any(), any()) } just Runs

        // Configura que el ViewModel emita el mensaje de error cuando se le indique (a través del callback)
        // No hay un coEvery para un 'LoginEvent.ShowMessage' específico en el ViewModel
        // porque la lógica del error se manejará directamente en el callback capturado.

        onView(withId(R.id.btnFacebookLogin)).perform(click())
        advanceUntilIdle() // Asegura que el clic y el `logInWithReadPermissions` se procesen

        // Simula la cancelación del login de Facebook invocando el método onCancel del callback capturado
        facebookCallbackSlot.captured.onCancel()
        advanceUntilIdle() // Permite que el mensaje de error se emita y se muestre en la UI

        // Verificaciones
        // Verifica que el método logInWithReadPermissions fue llamado
        coVerify(exactly = 1) {
            mockFacebookSignInDataSource.logInWithReadPermissions(
                any(),
                any()
            )
        }

        // Verifica que no se llamó a loginWithFacebook (porque fue cancelado)
        coVerify(exactly = 0) { mockLoginViewModel.loginWithFacebook(any()) }

        // Verifica que el mensaje de error se mostró
        onView(withText(errorMessage))
            .inRoot(withDecorView(not(activityDecorView)))
            .check(matches(isDisplayed()))

        // Asegura que la barra de progreso no esté visible
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun when_facebook_login_fails_due_to_error_error_message_is_shown() = runTest {
        val exceptionMessage = "Error de conexión de red"
        val errorMessage = "Error: $exceptionMessage" // Como el fragmento lo construye

        every { mockFacebookSignInDataSource.logInWithReadPermissions(any(), any()) } just Runs

        onView(withId(R.id.btnFacebookLogin)).perform(click())
        advanceUntilIdle()

        // Simula un error en el login de Facebook
        val facebookException = FacebookException(exceptionMessage)
        facebookCallbackSlot.captured.onError(facebookException)
        advanceUntilIdle()

        // Verificaciones
        coVerify(exactly = 1) {
            mockFacebookSignInDataSource.logInWithReadPermissions(
                any(),
                any()
            )
        }
        coVerify(exactly = 0) { mockLoginViewModel.loginWithFacebook(any()) } // No se llama a loginWithFacebook en caso de error

        onView(withText(errorMessage))
            .inRoot(withDecorView(not(activityDecorView)))
            .check(matches(isDisplayed()))

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }


    /* Pruebas de Navegación */

    @Test
    fun when_forgot_password_is_clicked_navigates_to_forgot_password_fragment() = runTest {
        every { mockLoginViewModel.onForgotPasswordClicked() } coAnswers {
            eventFlow.emit(LoginEvent.NavigateToForgotPassword)
        }
        every { mockNavController.navigate(R.id.action_loginFragment_to_forgotPasswordFragment) } just Runs

        onView(withId(R.id.tvForgotPassword)).perform(click())
        advanceUntilIdle()

        coVerify(exactly = 1) { mockLoginViewModel.onForgotPasswordClicked() }
        assertThat(mockNavController.currentDestination?.id).isEqualTo(R.id.loginFragment)
        coVerify(exactly = 1) { mockNavController.navigate(R.id.action_loginFragment_to_forgotPasswordFragment) }
    }

    @Test
    fun when_back_button_is_clicked_navigates_back() = runTest {
        every { mockLoginViewModel.onBackPressed() } coAnswers {
            eventFlow.emit(LoginEvent.NavigateBack)
        }
        every { mockNavController.popBackStack() } returns true

        onView(withId(R.id.ivBack)).perform(click())
        advanceUntilIdle()

        coVerify(exactly = 1) { mockLoginViewModel.onBackPressed() }
        coVerify(exactly = 1) { mockNavController.popBackStack() }
    }

    @Test
    fun when_sign_up_button_is_clicked_navigates_to_signup_fragment() {
        every { mockNavController.navigate(R.id.action_loginFragment_to_signupFragment) } just Runs

        onView(withId(R.id.tvSignUpBtn)).perform(click())

        assertThat(mockNavController.currentDestination?.id).isEqualTo(R.id.loginFragment)
        verify(exactly = 1) { mockNavController.navigate(R.id.action_loginFragment_to_signupFragment) }
    }
}