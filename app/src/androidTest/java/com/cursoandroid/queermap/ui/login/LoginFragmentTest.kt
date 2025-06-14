package com.cursoandroid.queermap.ui.login

import android.content.Intent
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4

import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import com.cursoandroid.queermap.test.HiltTestActivity
import com.cursoandroid.queermap.util.EspressoIdlingResource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING) // Mantiene el orden para depuración, si es necesario
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
class LoginFragmentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    var activityRule = ActivityScenarioRule(HiltTestActivity::class.java)

    private lateinit var mockLoginViewModel: LoginViewModel
    private lateinit var mockGoogleSignInDataSource: GoogleSignInDataSource
    private lateinit var mockFacebookSignInDataSource: FacebookSignInDataSource

    private val uiStateFlow = MutableStateFlow(LoginUiState())
    private val eventFlow = MutableSharedFlow<LoginEvent>()
    private val accessTokenChannelFlow = MutableSharedFlow<Result<String>>() // Para mockear FacebookDataSource

    // Usar TestCoroutineScheduler para controlar el tiempo en runTest
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private lateinit var activityDecorView: View

    @Before
    fun setUp() {
        hiltRule.inject()

        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)


        mockLoginViewModel = mockk(relaxed = true)
        mockGoogleSignInDataSource = mockk(relaxed = true)
        mockFacebookSignInDataSource = mockk(relaxed = true)

        every { mockLoginViewModel.uiState } returns uiStateFlow
        every { mockLoginViewModel.event } returns eventFlow

        coEvery { mockGoogleSignInDataSource.getSignInIntent() } returns Intent()
        every { mockFacebookSignInDataSource.accessTokenChannel } returns accessTokenChannelFlow
        every { mockFacebookSignInDataSource.registerCallback(any()) } just runs
        every { mockFacebookSignInDataSource.logInWithReadPermissions(any(), any()) } just runs

        // Asegúrate de que el fragmento se añade y se obtiene la decorView DE LA MISMA ACTIVIDAD.
        activityRule.scenario.onActivity { activity ->
            activityDecorView = activity.window.decorView

            val fragment = LoginFragment()

            // Asegurarse de que el fragmento se añade correctamente al decorView de HiltTestActivity
            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment, "LoginFragmentTag")
                .commitNow()

            // Inyectar los mocks. Asegúrate de que los nombres de los campos coincidan
            // con las propiedades `@Inject` en tu LoginFragment.
            val vmField = LoginFragment::class.java.getDeclaredField("viewModel")
            vmField.isAccessible = true
            vmField.set(fragment, mockLoginViewModel)

            val googleDsField = LoginFragment::class.java.getDeclaredField("googleSignInDataSource")
            googleDsField.isAccessible = true
            googleDsField.set(fragment, mockGoogleSignInDataSource)

            val facebookDsField =
                LoginFragment::class.java.getDeclaredField("facebookSignInDataSource")
            facebookDsField.isAccessible = true
            facebookDsField.set(fragment, mockFacebookSignInDataSource)
        }
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        clearAllMocks()
    }

    // Este test es fundamental para validar que el entorno de UI está funcionando correctamente
    @Test
    fun a_very_basic_test_to_check_setup() {
        onView(withId(R.id.tvTitle)).check(matches(isDisplayed()))
    }

    // Tests de Visibilidad de Elementos

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

    // Tests de Entrada de Texto

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

    // Tests de Comportamiento de Login por Email

    @Test
    fun when_valid_credentials_are_entered_and_login_clicked_then_navigates_to_home() =
        runTest(testDispatcher) {
            val email = "valid@example.com"
            val password = "validpassword"

            coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)
                testScheduler.advanceTimeBy(100) // Simula un retraso de la operación
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = false, isSuccess = true)
                eventFlow.emit(LoginEvent.NavigateToHome)
                eventFlow.emit(LoginEvent.ShowMessage("Inicio de sesión exitoso"))
            }

            onView(withId(R.id.etEmailLogin)).perform(typeText(email))
            onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(click())

            coVerify(exactly = 1) { mockLoginViewModel.loginWithEmail(email, password) }

            // Verifica el progressBar durante la carga
            onView(withId(R.id.progressBar)).check(matches(isDisplayed()))
            advanceUntilIdle() // Avanza el tiempo para que las coroutines en mocks se completen

            // Verifica el estado final y el mensaje
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
            onView(withText("Inicio de sesión exitoso"))
                .inRoot(withDecorView(not(`is`(activityDecorView))))
                .check(matches(isDisplayed()))
        }

    @Test
    fun when_invalid_email_is_entered_then_shows_error_message() = runTest(testDispatcher) {
        val email = "invalid-email"
        val password = "validpassword"

        coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
            uiStateFlow.value = uiStateFlow.value.copy(isEmailInvalid = true)
            eventFlow.emit(LoginEvent.ShowMessage("Por favor ingresa un email válido"))
        }

        onView(withId(R.id.etEmailLogin)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        coVerify(exactly = 1) { mockLoginViewModel.loginWithEmail(email, password) }

        advanceUntilIdle() // Asegura que los flujos se emitan

        onView(withText("Por favor ingresa un email válido"))
            .inRoot(withDecorView(not(`is`(activityDecorView))))
            .check(matches(isDisplayed()))

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun when_invalid_password_is_entered_then_shows_error_message() = runTest(testDispatcher) {
        val email = "valid@example.com"
        val password = "short"

        coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
            uiStateFlow.value = uiStateFlow.value.copy(isPasswordInvalid = true)
            eventFlow.emit(LoginEvent.ShowMessage("La contraseña debe tener al menos 6 caracteres"))
        }

        onView(withId(R.id.etEmailLogin)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        coVerify(exactly = 1) { mockLoginViewModel.loginWithEmail(email, password) }

        advanceUntilIdle() // Asegura que los flujos se emitan

        onView(withText("La contraseña debe tener al menos 6 caracteres"))
            .inRoot(withDecorView(not(`is`(activityDecorView))))
            .check(matches(isDisplayed()))

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun when_login_fails_with_network_error_then_shows_specific_message() =
        runTest(testDispatcher) {
            val email = "test@example.com"
            val password = "password123"
            val errorMessage = "Error de red. Por favor, revisa tu conexión"

            coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)
                testScheduler.advanceTimeBy(100)
                uiStateFlow.value =
                    uiStateFlow.value.copy(isLoading = false, errorMessage = errorMessage)
                eventFlow.emit(LoginEvent.ShowMessage(errorMessage))
            }

            onView(withId(R.id.etEmailLogin)).perform(typeText(email))
            onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(click())
            advanceUntilIdle()

            onView(withText(errorMessage))
                .inRoot(withDecorView(not(`is`(activityDecorView))))
                .check(matches(isDisplayed()))
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        }

    // Tests de Login Social (Google)

    @Test
    fun when_google_sign_in_button_is_clicked_then_getSignInIntent_is_called() =
        runTest(testDispatcher) {
            val googleSignInIntent = mockk<Intent>()
            coEvery { mockGoogleSignInDataSource.getSignInIntent() } returns googleSignInIntent

            onView(withId(R.id.btnGoogleSignIn)).perform(click())
            advanceUntilIdle()

            coVerify(exactly = 1) { mockGoogleSignInDataSource.getSignInIntent() }
        }

    @Test
    fun when_google_sign_in_result_is_success_for_new_user_then_navigates_to_signup_with_args() =
        runTest(testDispatcher) {
            val intentData = mockk<Intent>()
            val idToken = "some_google_id_token"
            val socialEmail = "new_google@example.com"
            val socialName = "New Google User"

            coEvery { mockGoogleSignInDataSource.handleSignInResult(intentData) } returns Result.success(idToken)

            coEvery { mockLoginViewModel.loginWithGoogle(idToken) } coAnswers {
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)
                testScheduler.advanceTimeBy(100)
                eventFlow.emit(
                    LoginEvent.NavigateToSignupWithArgs(
                        socialUserEmail = socialEmail,
                        socialUserName = socialName,
                        isSocialLoginFlow = true
                    )
                )
                eventFlow.emit(LoginEvent.ShowMessage("Completa tu perfil para continuar"))
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = false, isSuccess = true)
            }

            activityRule.scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("LoginFragmentTag") as LoginFragment
                fragment.handleGoogleSignInResult(intentData)
            }
            advanceUntilIdle()

            coVerify(exactly = 1) { mockGoogleSignInDataSource.handleSignInResult(intentData) }
            coVerify(exactly = 1) { mockLoginViewModel.loginWithGoogle(idToken) }

            onView(withText("Completa tu perfil para continuar"))
                .inRoot(withDecorView(not(`is`(activityDecorView))))
                .check(matches(isDisplayed()))

            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        }

    @Test
    fun when_google_sign_in_result_is_failure_then_shows_error_message() = runTest(testDispatcher) {
        val intentData = mockk<Intent>()
        val errorMessage = "Error en Sign-In: Fallo de Google"

        coEvery { mockGoogleSignInDataSource.handleSignInResult(intentData) } returns Result.failure(
            Exception("Fallo de Google")
        )

        activityRule.scenario.onActivity { activity ->
            val fragment =
                activity.supportFragmentManager.findFragmentByTag("LoginFragmentTag") as LoginFragment
            fragment.handleGoogleSignInResult(intentData)
        }
        advanceUntilIdle()

        coVerify(exactly = 1) { mockGoogleSignInDataSource.handleSignInResult(intentData) }
        coVerify(exactly = 0) { mockLoginViewModel.loginWithGoogle(any()) }

        onView(withText(errorMessage))
            .inRoot(withDecorView(not(`is`(activityDecorView))))
            .check(matches(isDisplayed()))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    // Tests de Login Social (Facebook)

    @Test
    fun when_facebook_login_button_is_clicked_then_logInWithReadPermissions_is_called() =
        runTest(testDispatcher) {
            onView(withId(R.id.btnFacebookLogin)).perform(click())
            advanceUntilIdle()

            coVerify(exactly = 1) {
                mockFacebookSignInDataSource.logInWithReadPermissions(
                    any(),
                    listOf("email", "public_profile")
                )
            }
        }

    @Test
    fun when_facebook_accessTokenChannel_success_then_registers_user_and_navigates_to_home() =
        runTest(testDispatcher) {
            val accessToken = "facebook_access_token"

            coEvery { mockLoginViewModel.loginWithFacebook(accessToken) } coAnswers {
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)
                testScheduler.advanceTimeBy(100)
                eventFlow.emit(LoginEvent.NavigateToHome)
                eventFlow.emit(LoginEvent.ShowMessage("Inicio de sesión con Facebook exitoso"))
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = false, isSuccess = true)
            }

            // Simular la emisión del accessTokenChannel del DataSource
            launch(testDispatcher) { accessTokenChannelFlow.emit(Result.success(accessToken)) }
            advanceUntilIdle()

            coVerify(exactly = 1) { mockLoginViewModel.loginWithFacebook(accessToken) }

            onView(withText("Inicio de sesión con Facebook exitoso"))
                .inRoot(withDecorView(not(`is`(activityDecorView))))
                .check(matches(isDisplayed()))
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        }

    @Test
    fun when_facebook_accessTokenChannel_failure_then_shows_error_message() =
        runTest(testDispatcher) {
            val errorMessage = "Error: Facebook login failed"

            // Simular la emisión del accessTokenChannel del DataSource con un fallo.
            launch(testDispatcher) { accessTokenChannelFlow.emit(Result.failure(Exception("Facebook login failed"))) }
            advanceUntilIdle()

            coVerify(exactly = 0) { mockLoginViewModel.loginWithFacebook(any()) }

            onView(withText(errorMessage))
                .inRoot(withDecorView(not(`is`(activityDecorView))))
                .check(matches(isDisplayed()))
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        }

    // Tests de Navegación

    @Test
    fun when_forgot_password_is_clicked_then_navigates_to_ForgotPasswordFragment() =
        runTest(testDispatcher) {
            coEvery { mockLoginViewModel.onForgotPasswordClicked() } coAnswers {
                eventFlow.emit(LoginEvent.NavigateToForgotPassword)
            }

            onView(withId(R.id.tvForgotPassword)).perform(click())
            advanceUntilIdle()

            coVerify(exactly = 1) { mockLoginViewModel.onForgotPasswordClicked() }
        }

    @Test
    fun when_back_button_is_clicked_then_navigates_back() = runTest(testDispatcher) {
        coEvery { mockLoginViewModel.onBackPressed() } coAnswers {
            eventFlow.emit(LoginEvent.NavigateBack)
        }

        onView(withId(R.id.ivBack)).perform(click())
        advanceUntilIdle()

        coVerify(exactly = 1) { mockLoginViewModel.onBackPressed() }
    }

    @Test
    fun when_sign_up_button_is_clicked_then_navigates_to_SignUpFragment() {
        // Para este test de navegación, necesitas el NavController mockeado o real,
        // o usar una approach diferente como en MainActivityTest donde el NavHostFragment
        // ya está en la actividad. Aquí, si navegas fuera del LoginFragment,
        // Espresso ya no podrá encontrar elementos del LoginFragment.
        // Si tu implementación de navegación es puramente a través del evento del ViewModel,
        // debes mockear esa navegación o verificar el evento emitido.
        // Asumiendo que tvSignUpBtn realiza una acción de navegación interna directa:
        onView(withId(R.id.tvSignUpBtn)).perform(click())
        // Si la navegación ocurre dentro del mismo NavHost, puedes verificar el elemento del nuevo Fragmento.
        // Si implica un cambio de actividad o algo más complejo, el test puede necesitar adaptarse.
        // Este test de navegación a SignUpFragment podría ser más robusto si verificas que el NavController
        // fue llamado con la acción correcta, en lugar de verificar la UI del siguiente fragmento
        // directamente en este test de LoginFragment.
        onView(withId(R.id.etName)).check(matches(isDisplayed()))
        onView(withText(R.string.register_title)).check(matches(isDisplayed()))
    }

    // Tests de Estado de Carga y Mensajes de Error Generales

    @Test
    fun when_loading_then_progress_bar_is_displayed() = runTest(testDispatcher) {
        launch(testDispatcher) {
            uiStateFlow.emit(LoginUiState(isLoading = true))
        }
        advanceUntilIdle()
        onView(withId(R.id.progressBar)).check(matches(isDisplayed()))
    }

    @Test
    fun when_not_loading_then_progress_bar_is_hidden() = runTest(testDispatcher) {
        launch(testDispatcher) {
            uiStateFlow.emit(LoginUiState(isLoading = false))
        }
        advanceUntilIdle()
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun when_error_message_in_UiState_then_shows_Snackbar() = runTest(testDispatcher) {
        val errorMessage = "Algo salió mal"
        launch(testDispatcher) {
            uiStateFlow.emit(LoginUiState(errorMessage = errorMessage))
        }
        advanceUntilIdle()

        onView(withText(errorMessage))
            .inRoot(withDecorView(not(`is`(activityDecorView))))
            .check(matches(isDisplayed()))
    }
}