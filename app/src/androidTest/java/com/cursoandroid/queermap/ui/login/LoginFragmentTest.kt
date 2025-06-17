package com.cursoandroid.queermap.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.test.core.app.ActivityScenario // Ahora lanzamos ActivityScenario directamente
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cursoandroid.queermap.HiltTestActivity
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import com.cursoandroid.queermap.util.EspressoIdlingResource
import dagger.hilt.android.testing.BindValue
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
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import javax.inject.Inject

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
class LoginFragmentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @BindValue @JvmField // Mantén tu mock de ViewModel
    val mockLoginViewModel: LoginViewModel = mockk(relaxed = true)

    // Inyecta tu FragmentFactory personalizada (si tienes una, de lo contrario, usa la predeterminada)
    // Aquí asumimos que tienes una FragmentFactory configurada por Hilt o una simple para mocks.
    // Si no tienes una FragmentFactory proporcionada por Hilt, esta parte puede ser un poco diferente.
    @BindValue @JvmField
    val fragmentFactory: FragmentFactory = object : FragmentFactory() {
        override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
            return when (className) {
                LoginFragment::class.java.name -> LoginFragment()
                // Si tu fragmento navega a otros, también debes proveerlos aquí
                // Ejemplo: SignUpFragment::class.java.name -> SignUpFragment()
                else -> super.instantiate(classLoader, className)
            }
        }
    }

    @Inject
    lateinit var mockGoogleSignInDataSource: GoogleSignInDataSource
    @Inject
    lateinit var mockFacebookSignInDataSource: FacebookSignInDataSource

    private lateinit var uiStateFlow: MutableStateFlow<LoginUiState>
    private lateinit var eventFlow: MutableSharedFlow<LoginEvent>
    private lateinit var accessTokenChannelFlow: MutableSharedFlow<Result<String>>

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private lateinit var activityDecorView: View
    private lateinit var activityScenario: ActivityScenario<HiltTestActivity> // Declarada para poder cerrarla

    @Before
    fun setUp() {
        hiltRule.inject()

        uiStateFlow = MutableStateFlow(LoginUiState())
        eventFlow = MutableSharedFlow()
        accessTokenChannelFlow = MutableSharedFlow()

        clearAllMocks()

        every { mockLoginViewModel.uiState } returns uiStateFlow
        every { mockLoginViewModel.event } returns eventFlow
        every { mockLoginViewModel.loadUserCredentials() } just runs

        coEvery { mockGoogleSignInDataSource.getSignInIntent() } returns Intent()
        every { mockFacebookSignInDataSource.accessTokenChannel } returns accessTokenChannelFlow
        every { mockFacebookSignInDataSource.registerCallback(any()) } just runs
        every { mockFacebookSignInDataSource.logInWithReadPermissions(any(), any()) } just runs

        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)

        // *** NUEVA ESTRATEGIA: LANZAR HiltTestActivity y añadir el fragmento manualmente ***
        // 1. Lanza la ActivityScenario para HiltTestActivity
        activityScenario = ActivityScenario.launch(HiltTestActivity::class.java)

        // 2. Adjunta el LoginFragment a la actividad de prueba
        activityScenario.onActivity { activity ->
            // Establece la FragmentFactory para esta actividad
            activity.supportFragmentManager.fragmentFactory = fragmentFactory

            val fragment = fragmentFactory.instantiate(activity.classLoader, LoginFragment::class.java.name)
            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment, null) // Reemplaza el contenido de la actividad
                .commitNow() // CommitNow para que sea síncrono en los tests
            activityDecorView = activity.window.decorView
        }
        // Nota: No se necesita FragmentScenario.launch() en setUp con esta estrategia.
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        activityScenario.close() // Siempre cierra el ActivityScenario después de cada prueba
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
//ERROR_ REVISAR MAÑANA
    @Test
    fun when_valid_credentials_are_entered_and_login_clicked_then_navigates_to_home() =
        runTest(testDispatcher) {
            val email = "valid@example.com"
            val password = "validpassword"

            coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)
                launch(testDispatcher) {
                    testScheduler.advanceTimeBy(100)
                    uiStateFlow.value = uiStateFlow.value.copy(isLoading = false, isSuccess = true)
                    eventFlow.emit(LoginEvent.NavigateToHome)
                    eventFlow.emit(LoginEvent.ShowMessage("Inicio de sesión exitoso"))
                }
            }

            onView(withId(R.id.etEmailLogin)).perform(typeText(email))
            onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(click())

            coVerify(exactly = 1) { mockLoginViewModel.loginWithEmail(email, password) }

            advanceUntilIdle()

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
            launch(testDispatcher) {
                uiStateFlow.value = uiStateFlow.value.copy(isEmailInvalid = true)
                eventFlow.emit(LoginEvent.ShowMessage("Por favor ingresa un email válido"))
            }
        }

        onView(withId(R.id.etEmailLogin)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        coVerify(exactly = 1) { mockLoginViewModel.loginWithEmail(email, password) }

        advanceUntilIdle()

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
            launch(testDispatcher) {
                uiStateFlow.value = uiStateFlow.value.copy(isPasswordInvalid = true)
                eventFlow.emit(LoginEvent.ShowMessage("La contraseña debe tener al menos 6 caracteres"))
            }
        }

        onView(withId(R.id.etEmailLogin)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        coVerify(exactly = 1) { mockLoginViewModel.loginWithEmail(email, password) }

        advanceUntilIdle()

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
                launch(testDispatcher) {
                    uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)
                    testScheduler.advanceTimeBy(100)
                    uiStateFlow.value =
                        uiStateFlow.value.copy(isLoading = false, errorMessage = errorMessage)
                    eventFlow.emit(LoginEvent.ShowMessage(errorMessage))
                }
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
                launch(testDispatcher) {
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
            }

            // *** RELANZANDO FRAGMENT PARA ESTA PRUEBA ESPECÍFICA DE FORMA MANUAL ***
            // Cierra el escenario actual si existe para asegurar un estado limpio
            activityScenario.close()

            val testActivityScenario = ActivityScenario.launch(HiltTestActivity::class.java)
            testActivityScenario.onActivity { activity ->
                activity.supportFragmentManager.fragmentFactory = fragmentFactory
                val fragment = fragmentFactory.instantiate(activity.classLoader, LoginFragment::class.java.name)
                activity.supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, fragment, null)
                    .commitNow()

                (fragment as LoginFragment).handleGoogleSignInResult(intentData)
            }
            advanceUntilIdle()
            testActivityScenario.close() // Cierra el escenario de prueba al finalizar

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

        // *** RELANZANDO FRAGMENT PARA ESTA PRUEBA ESPECÍFICA DE FORMA MANUAL ***
        // Cierra el escenario actual si existe para asegurar un estado limpio
        activityScenario.close()

        val testActivityScenario = ActivityScenario.launch(HiltTestActivity::class.java)
        testActivityScenario.onActivity { activity ->
            activity.supportFragmentManager.fragmentFactory = fragmentFactory
            val fragment = fragmentFactory.instantiate(activity.classLoader, LoginFragment::class.java.name)
            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment, null)
                .commitNow()

            (fragment as LoginFragment).handleGoogleSignInResult(intentData)
        }
        advanceUntilIdle()
        testActivityScenario.close() // Cierra el escenario de prueba al finalizar

        coVerify(exactly = 1) { mockGoogleSignInDataSource.handleSignInResult(intentData) }
        coVerify(exactly = 0) { mockLoginViewModel.loginWithGoogle(any()) }

        onView(withText(errorMessage))
            .inRoot(withDecorView(not(`is`(activityDecorView))))
            .check(matches(isDisplayed()))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

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
                launch(testDispatcher) {
                    uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)
                    testScheduler.advanceTimeBy(100)
                    eventFlow.emit(LoginEvent.NavigateToHome)
                    eventFlow.emit(LoginEvent.ShowMessage("Inicio de sesión con Facebook exitoso"))
                    uiStateFlow.value = uiStateFlow.value.copy(isLoading = false, isSuccess = true)
                }
            }

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
            val exceptionMessage = "Facebook login failed"
            val expectedSnackbarMessage = "Error: $exceptionMessage"

            launch(testDispatcher) {
                accessTokenChannelFlow.emit(Result.failure(Exception(exceptionMessage)))
            }
            advanceUntilIdle()

            coVerify(exactly = 0) { mockLoginViewModel.loginWithFacebook(any()) }

            onView(withText(expectedSnackbarMessage))
                .inRoot(withDecorView(not(`is`(activityDecorView))))
                .check(matches(isDisplayed()))
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        }

    @Test
    fun when_forgot_password_is_clicked_then_navigates_to_ForgotPasswordFragment() =
        runTest(testDispatcher) {
            coEvery { mockLoginViewModel.onForgotPasswordClicked() } coAnswers {
                launch(testDispatcher) { eventFlow.emit(LoginEvent.NavigateToForgotPassword) }
            }

            onView(withId(R.id.tvForgotPassword)).perform(click())
            advanceUntilIdle()

            coVerify(exactly = 1) { mockLoginViewModel.onForgotPasswordClicked() }
        }

    @Test
    fun when_back_button_is_clicked_then_navigates_back() = runTest(testDispatcher) {
        coEvery { mockLoginViewModel.onBackPressed() } coAnswers {
            launch(testDispatcher) { eventFlow.emit(LoginEvent.NavigateBack) }
        }

        onView(withId(R.id.ivBack)).perform(click())
        advanceUntilIdle()

        coVerify(exactly = 1) { mockLoginViewModel.onBackPressed() }
    }

    @Test
    fun when_sign_up_button_is_clicked_then_navigates_to_SignUpFragment() {
        onView(withId(R.id.tvSignUpBtn)).perform(click())
        // Assuming SignUpFragment has etName and a specific title for verification
        // Si tienes un SignUpFragment real, asegúrate de que tu FragmentFactory lo instancie
        onView(withId(R.id.etName)).check(matches(isDisplayed()))
        onView(withText(R.string.register_title)).check(matches(isDisplayed()))
    }

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