package com.cursoandroid.queermap.ui.login


import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isSystemAlertWindow
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cursoandroid.queermap.HiltTestActivity
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.common.InputValidator
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import com.cursoandroid.queermap.util.EspressoIdlingResource
import com.cursoandroid.queermap.util.MainDispatcherRule
import com.cursoandroid.queermap.util.Result
import com.cursoandroid.queermap.util.waitForNavigationTo
import com.cursoandroid.queermap.util.withDecorView
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
class LoginFragmentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val mainDispatcherRule = MainDispatcherRule()

    @BindValue
    @JvmField
    val mockLoginViewModel: LoginViewModel = mockk(relaxed = true)

    @BindValue
    @JvmField
    val mockInputValidator: InputValidator = mockk(relaxed = true)

    private lateinit var mockGoogleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var mockGoogleSignInDataSource: GoogleSignInDataSource
    private lateinit var mockFacebookSignInDataSource: FacebookSignInDataSource
    private lateinit var mockCallbackManager: CallbackManager

    private lateinit var activityScenario: ActivityScenario<HiltTestActivity>
    private lateinit var mockNavController: TestNavHostController

    private lateinit var uiStateFlow: MutableStateFlow<LoginUiState>
    private lateinit var eventFlow: MutableSharedFlow<LoginEvent>

    private val FAKE_GOOGLE_ID_TOKEN = "fake_google_id_token"
    private val FAKE_GOOGLE_EMAIL = "test_google@example.com"
    private val FAKE_GOOGLE_NAME = "Test Google User"

    private lateinit var facebookCallbackSlot: CapturingSlot<FacebookCallback<LoginResult>>

    class TestLoginFragmentFactory(
        private val googleSignInDataSource: GoogleSignInDataSource,
        private val facebookSignInDataSource: FacebookSignInDataSource,
        private val mockGoogleSignInLauncher: ActivityResultLauncher<Intent>,
        private val callbackManager: CallbackManager
    ) : FragmentFactory() {
        override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
            return when (className) {
                LoginFragment::class.java.name -> LoginFragment(
                    googleSignInDataSource = googleSignInDataSource,
                    facebookSignInDataSource = facebookSignInDataSource,
                    googleSignInLauncher = mockGoogleSignInLauncher,
                    callbackManager = callbackManager
                )

                else -> super.instantiate(classLoader, className)
            }
        }
    }

    @Before
    fun setUp() {
        hiltRule.inject()
        clearAllMocks()

        uiStateFlow = MutableStateFlow(LoginUiState())
        eventFlow = MutableSharedFlow()

        every { mockLoginViewModel.uiState } returns uiStateFlow
        every { mockLoginViewModel.event } returns eventFlow
        every { mockLoginViewModel.loadUserCredentials() } just Runs
        every { mockLoginViewModel.loginWithEmail(any(), any()) } just Runs
        every { mockLoginViewModel.loginWithGoogle(any()) } just Runs
        every { mockLoginViewModel.onForgotPasswordClicked() } just Runs
        every { mockLoginViewModel.onBackPressed() } just Runs

        mockGoogleSignInDataSource = mockk(relaxed = true)
        every { mockGoogleSignInDataSource.getSignInIntent() } answers {
            Intent("com.google.android.gms.auth.GOOGLE_SIGN_IN")
        }

        mockGoogleSignInLauncher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)

        mockFacebookSignInDataSource = mockk(relaxed = true)
        facebookCallbackSlot = slot()
        every {
            mockFacebookSignInDataSource.registerCallback(
                any(),
                capture(facebookCallbackSlot)
            )
        } just Runs
        every { mockFacebookSignInDataSource.logInWithReadPermissions(any(), any()) } just Runs

        mockCallbackManager = mockk(relaxed = true)
        every { mockCallbackManager.onActivityResult(any(), any(), any()) } returns true


        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)

        activityScenario = ActivityScenario.launch(HiltTestActivity::class.java)

        activityScenario.moveToState(Lifecycle.State.RESUMED)

        activityScenario.onActivity { activity ->
            mockNavController = TestNavHostController(activity)
            mockNavController.setGraph(R.navigation.nav_graph)
            mockNavController.setCurrentDestination(R.id.loginFragment)


            activity.supportFragmentManager.fragmentFactory = TestLoginFragmentFactory(
                mockGoogleSignInDataSource,
                mockFacebookSignInDataSource,
                mockGoogleSignInLauncher,
                mockCallbackManager
            )

            val fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
                activity.classLoader,
                LoginFragment::class.java.name
            ) as LoginFragment

            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commitNow()

            val latch = CountDownLatch(1)
            fragment.viewLifecycleOwnerLiveData.observe(fragment) { viewLifecycleOwner ->
                if (viewLifecycleOwner != null && fragment.view != null &&
                    viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                ) {
                    Navigation.setViewNavController(fragment.requireView(), mockNavController)
                    latch.countDown()
                }
            }
            latch.await(2, TimeUnit.SECONDS)
        }
        Espresso.onIdle()
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        if (this::activityScenario.isInitialized) {
            activityScenario.close()
        }
        clearAllMocks()
    }


    // Passed
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

    //passed
    @Test
    fun when_viewmodel_uiState_loading_is_true_then_progress_bar_is_visible() = runTest {
        val scenario = ActivityScenario.launch(HiltTestActivity::class.java)

        scenario.onActivity { activity ->
            val fragment = LoginFragment()

            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment, "TAG")
                .commitNow()
        }

        uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)

        advanceUntilIdle()

        onView(withId(R.id.progressBar))
            .check(matches(isDisplayed()))
    }

    //passed
    @Test
    fun when_viewmodel_uiState_loading_is_false_then_progress_bar_is_hidden() = runTest {
        val scenario = ActivityScenario.launch(HiltTestActivity::class.java)

        scenario.onActivity { activity ->
            val fragment = LoginFragment()

            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment, "LOGIN_FRAGMENT")
                .commitNow()
        }

        uiStateFlow.value = uiStateFlow.value.copy(isLoading = false)

        advanceUntilIdle()

        onView(withId(R.id.progressBar))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    /* User Interaction and UI Update Tests */
    // Passed
    @Test
    fun when_typing_in_email_field_text_is_updated() {
        onView(withId(R.id.etEmailLogin)).perform(typeText("test@example.com"), closeSoftKeyboard())
        onView(withId(R.id.etEmailLogin)).check(matches(withText("test@example.com")))
    }

    // Passed
    @Test
    fun when_typing_in_password_field_text_is_updated() {
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).check(matches(withText("password123")))
    }

    // Passed
    @Test
    fun when_login_loads_credentials_email_and_password_fields_are_updated() = runTest {
        val savedEmail = "saved@example.com"
        val savedPassword = "savedPassword123"

        coEvery { mockLoginViewModel.loadUserCredentials() } coAnswers {
            uiStateFlow.value = uiStateFlow.value.copy(
                email = savedEmail,
                password = savedPassword
            )
        }

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mockLoginViewModel.loadUserCredentials()
        }
        mainDispatcherRule.testScope.advanceUntilIdle()
        Espresso.onIdle()

        onView(withId(R.id.etEmailLogin)).check(matches(withText(savedEmail)))
        onView(withId(R.id.etPassword)).check(matches(withText(savedPassword)))

        coVerify(atLeast = 1) { mockLoginViewModel.loadUserCredentials() }
    }

    /* Login Button (Email/Password) Interaction Tests */
    // Passed
    @Test
    fun when_login_button_is_clicked_loginWithEmail_is_called_with_correct_data_and_navigates_to_home_on_success() =
        runTest {
            val email = "valid@example.com"
            val password = "validpassword"

            coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)
                delay(200)
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = false, isSuccess = true)
                mainDispatcherRule.testScope.launch {
                    eventFlow.emit(LoginEvent.NavigateToHome)
                }
            }

            onView(withId(R.id.etEmailLogin)).perform(typeText(email), closeSoftKeyboard())
            onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(click())

            mainDispatcherRule.testScope.advanceUntilIdle()
            Espresso.onIdle()

            coVerify { mockLoginViewModel.loginWithEmail(email, password) }

            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
            assertThat(mockNavController.currentDestination?.id).isEqualTo(R.id.mapFragment)
        }

    // Passed
    @Test
    fun when_login_button_is_clicked_and_email_is_invalid_error_message_is_shown() = runTest {
        val email = "invalid-email"
        val password = "validpassword"
        val errorMessage = "Por favor ingresa un email válido"

        coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
            uiStateFlow.value = uiStateFlow.value.copy(isEmailInvalid = true)
            mainDispatcherRule.testScope.launch {
                eventFlow.emit(LoginEvent.ShowMessage(errorMessage))
            }
        }

        onView(withId(R.id.etEmailLogin)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        mainDispatcherRule.testScope.advanceUntilIdle()
        Espresso.onIdle() // Wait for the Snackbar to appear

        coVerify { mockLoginViewModel.loginWithEmail(email, password) }

        onView(withText(errorMessage))
            .inRoot(isSystemAlertWindow())
            .check(matches(isDisplayed()))

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    // Passed
    @Test
    fun when_login_button_is_clicked_and_password_is_invalid_error_message_is_shown() = runTest {
        val email = "valid@example.com"
        val password = "short"
        val errorMessage = "La contraseña debe tener al menos 6 caracteres"

        coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
            uiStateFlow.value = uiStateFlow.value.copy(isPasswordInvalid = true)
            mainDispatcherRule.testScope.launch {
                eventFlow.emit(LoginEvent.ShowMessage(errorMessage))
            }
        }

        onView(withId(R.id.etEmailLogin)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        mainDispatcherRule.testScope.advanceUntilIdle()
        Espresso.onIdle()

        coVerify(exactly = 1) { mockLoginViewModel.loginWithEmail(email, password) }

        onView(withText(errorMessage))
            .inRoot(isSystemAlertWindow())
            .check(matches(isDisplayed()))

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    // Passed
    @Test
    fun when_login_fails_due_to_general_error_error_message_is_shown() = runTest {
        val email = "test@example.com"
        val password = "password123"
        val errorMessage = "Error inesperado. Intenta de nuevo más tarde"

        coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
            uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)
            delay(200)
            uiStateFlow.value = uiStateFlow.value.copy(
                isLoading = false,
                errorMessage = errorMessage
            )
            mainDispatcherRule.testScope.launch {
                eventFlow.emit(LoginEvent.ShowMessage(errorMessage))
            }
        }

        onView(withId(R.id.etEmailLogin)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        mainDispatcherRule.testScope.advanceUntilIdle()
        Espresso.onIdle()

        coVerify(exactly = 1) { mockLoginViewModel.loginWithEmail(email, password) }

        onView(withText(errorMessage))
            .inRoot(isSystemAlertWindow())
            .check(matches(isDisplayed()))

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    /* Social Login (Google) Interaction Tests */
    //passed
    @Test
    fun when_google_button_is_clicked_launcher_is_invoked_and_navigates_to_home_on_success() =
        runTest {
            val idToken = FAKE_GOOGLE_ID_TOKEN
            val successMessage = "Inicio de sesión social exitoso"

            coEvery { mockGoogleSignInDataSource.handleSignInResult(any()) } returns Result.Success(
                idToken
            )

            coEvery { mockLoginViewModel.loginWithGoogle(idToken) } coAnswers {
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)
                delay(200)
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = false, isSuccess = true)
                mainDispatcherRule.testScope.launch {
                    eventFlow.emit(LoginEvent.NavigateToHome)
                    eventFlow.emit(LoginEvent.ShowMessage(successMessage))
                }
            }

            val navControllerSpy = spyk(mockNavController)
            activityScenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentById(android.R.id.content) as LoginFragment
                activity.runOnUiThread {
                    Navigation.setViewNavController(fragment.requireView(), navControllerSpy)
                }
            }

            onView(withId(R.id.btnGoogleSignIn)).perform(click())
            mainDispatcherRule.testScope.advanceUntilIdle()
            Espresso.onIdle()

            waitForNavigationTo(
                navControllerSpy,
                R.id.mapFragment
            )

            coVerify(exactly = 1) { mockGoogleSignInDataSource.handleSignInResult(any()) }
            coVerify(exactly = 1) { mockLoginViewModel.loginWithGoogle(FAKE_GOOGLE_ID_TOKEN) }
            coVerify(exactly = 1) { navControllerSpy.navigate(R.id.action_loginFragment_to_mapFragment) }

            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))

            onView(withText(successMessage))
                .inRoot(isSystemAlertWindow())
                .check(matches(isDisplayed()))
        }

    //passed
    @Test
    fun when_google_login_is_for_new_user_navigates_to_signup_with_args() = runTest {
        val idToken = "some_new_google_id_token"
        val socialEmail = FAKE_GOOGLE_EMAIL
        val socialName = FAKE_GOOGLE_NAME
        val messageForNewUser = "Completa tu perfil para continuar"

        coEvery { mockGoogleSignInDataSource.handleSignInResult(any()) } returns Result.Success(
            idToken
        )

        coEvery { mockLoginViewModel.loginWithGoogle(idToken) } coAnswers {
            uiStateFlow.value = uiStateFlow.value.copy(isLoading = false, isSuccess = true)
            mainDispatcherRule.testScope.launch {
                eventFlow.emit(
                    LoginEvent.NavigateToSignupWithArgs(
                        socialUserEmail = socialEmail,
                        socialUserName = socialName,
                        isSocialLoginFlow = true
                    )
                )
                eventFlow.emit(LoginEvent.ShowMessage(messageForNewUser))
            }
        }

        activityScenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager
                .findFragmentById(android.R.id.content) as LoginFragment

            Navigation.setViewNavController(fragment.requireView(), mockNavController)

            fragment.viewLifecycleOwnerLiveData.observeForever { lifecycleOwner ->
                lifecycleOwner?.let {
                    if (it.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                        (it.lifecycle as? LifecycleRegistry)?.currentState = Lifecycle.State.STARTED
                    }
                }
            }

            fragment.handleGoogleSignInResult(Intent())
        }


        mainDispatcherRule.testScope.advanceUntilIdle()

        coVerify(exactly = 1) { mockGoogleSignInDataSource.handleSignInResult(any()) }
        coVerify(exactly = 1) { mockLoginViewModel.loginWithGoogle(idToken) }

        waitForNavigationTo(mockNavController, R.id.signupFragment)

        val backStackEntry = mockNavController.getBackStackEntry(R.id.signupFragment)
        val args = backStackEntry.arguments

        assertThat(args?.getString("socialUserEmail")).isEqualTo(socialEmail)
        assertThat(args?.getString("socialUserName")).isEqualTo(socialName)
        assertThat(args?.getBoolean("isSocialLoginFlow")).isEqualTo(true)

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    //passed
    @Test
    fun when_google_login_fails_error_message_is_shown() = runTest {
        val exceptionMessage = "Error de autenticación de Google"
        val expectedDisplayMessage = "Error en Sign-In: $exceptionMessage"

        coEvery { mockGoogleSignInDataSource.handleSignInResult(any()) } returns Result.Failure(
            Exception(exceptionMessage)
        )

        activityScenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager
                .findFragmentById(android.R.id.content) as LoginFragment
            fragment.handleGoogleSignInResult(Intent())
        }

        advanceUntilIdle()
        delay(500)
        Espresso.onIdle()

        coVerify(exactly = 1) { mockGoogleSignInDataSource.handleSignInResult(any()) }
        coVerify(exactly = 0) { mockLoginViewModel.loginWithGoogle(any()) }

        onView(withText(expectedDisplayMessage))
            .check(matches(isDisplayed()))

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    /* Pruebas de Interacción de Login Social (Facebook) */
    //passed
    @Test
    fun when_facebook_button_is_clicked_logInWithReadPermissions_is_called() = runTest {
        onView(withId(R.id.btnFacebookLogin))
            .perform(scrollTo(), click())

        advanceUntilIdle()
        Espresso.onIdle()

        coVerify(exactly = 1) {
            mockFacebookSignInDataSource.logInWithReadPermissions(
                any(),
                listOf("email", "public_profile")
            )
        }
    }

    //passed
    @Test
    fun when_facebook_login_successful_then_loginWithFacebook_is_called_with_token() = runTest {
        val accessTokenValue = "fake_facebook_token"
        val successMessage = "Inicio de sesión social exitoso"

        val accessToken: AccessToken = mockk()
        every { accessToken.token } returns accessTokenValue

        val loginResult: LoginResult = mockk()
        every { loginResult.accessToken } returns accessToken

        coEvery { mockLoginViewModel.loginWithFacebook(accessTokenValue) } coAnswers {
            uiStateFlow.value = uiStateFlow.value.copy(isLoading = false, isSuccess = true)
            launch(mainDispatcherRule.testDispatcher) {
                eventFlow.emit(LoginEvent.NavigateToHome)
                eventFlow.emit(LoginEvent.ShowMessage(successMessage))
            }
        }

        Espresso.onIdle()

        assert(facebookCallbackSlot.isCaptured) { "El Callback de Facebook no fue registrado." }

        onView(withId(R.id.btnFacebookLogin)).perform(scrollTo(), click())
        activityScenario.onActivity {
            val callback = facebookCallbackSlot.captured
            callback.onSuccess(loginResult)
        }

        advanceUntilIdle()

        Espresso.onIdle()
        coVerify { mockLoginViewModel.loginWithFacebook(accessTokenValue) }

        waitForNavigationTo(
            mockNavController,
            R.id.mapFragment,
            timeoutMs = 2000L
        )
        assert(mockNavController.currentDestination?.id == R.id.mapFragment)
    }

    //passed
    @Test
    fun when_facebook_login_cancelled_then_snackbar_with_cancel_message_is_shown() = runTest {
        val cancelMessage = "Inicio de sesión con Facebook cancelado."

        Espresso.onIdle()
        assert(facebookCallbackSlot.isCaptured) { "El Callback de Facebook no fue registrado." }
        onView(withId(R.id.btnFacebookLogin)).perform(scrollTo(), click())

        activityScenario.onActivity {
            val callback = facebookCallbackSlot.captured
            callback.onCancel()
        }

        advanceUntilIdle()

        Espresso.onIdle()

        Espresso.onView(withText(cancelMessage))
            .inRoot(withDecorView(isDisplayed()))
            .check(matches(isDisplayed()))
    }

    //passed
    @Test
    fun when_facebook_login_error_then_snackbar_with_error_message_is_shown() = runTest {
        val errorMessage = "Error simulado de Facebook."
        val expectedSnackbarMessage = "Error: $errorMessage"
        Espresso.onIdle()

        assert(facebookCallbackSlot.isCaptured) { "El Callback de Facebook no fue registrado." }

        onView(withId(R.id.btnFacebookLogin)).perform(scrollTo(), click())

        val facebookException = FacebookException(errorMessage)

        activityScenario.onActivity {
            val callback = facebookCallbackSlot.captured
            callback.onError(facebookException)
        }

        advanceUntilIdle()
        Espresso.onIdle()

        Espresso.onView(withText(expectedSnackbarMessage))
            .inRoot(withDecorView(isDisplayed()))
            .check(matches(isDisplayed()))
    }

    /* Eventos de navegación (ViewModel) */
    //passed
    @Test
    fun when_back_icon_is_clicked_then_navigate_back_is_called() = runTest {
        coEvery { mockLoginViewModel.onBackPressed() } coAnswers {
            launch(mainDispatcherRule.testDispatcher) {
                eventFlow.emit(LoginEvent.NavigateBack)
            }
        }

        activityScenario.onActivity { activity ->
            activity.runOnUiThread {
                mockNavController.setCurrentDestination(R.id.mapFragment)
                mockNavController.navigate(R.id.loginFragment)
            }
        }


        Espresso.onIdle()
        assertThat(mockNavController.currentDestination?.id).isEqualTo(R.id.loginFragment)

        val navControllerSpy = spyk(mockNavController)
        activityScenario.onActivity { activity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content) as LoginFragment
            activity.runOnUiThread {
                Navigation.setViewNavController(fragment.requireView(), navControllerSpy)
            }
        }
        Espresso.onIdle()
        onView(withId(R.id.ivBack)).perform(click())
        advanceUntilIdle()
        Espresso.onIdle()

        coVerify(exactly = 1) { mockLoginViewModel.onBackPressed() }
        coVerify(exactly = 1) { navControllerSpy.popBackStack() }
        assertThat(navControllerSpy.currentDestination?.id).isEqualTo(R.id.mapFragment)
    }

    //passed
    @Test
    fun when_signup_text_is_clicked_then_navigates_to_signup_fragment() = runTest {
        val navControllerSpy = spyk(mockNavController)
        activityScenario.onActivity { activity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content) as LoginFragment
            activity.runOnUiThread {
                Navigation.setViewNavController(fragment.requireView(), navControllerSpy)
            }
        }
        Espresso.onIdle()
        onView(withId(R.id.tvSignUpBtn)).perform(click())

        waitForNavigationTo(navControllerSpy, R.id.signupFragment)

        assertThat(navControllerSpy.currentDestination?.id).isEqualTo(R.id.signupFragment)

        coVerify(exactly = 1) { navControllerSpy.navigate(R.id.action_loginFragment_to_signupFragment) }
    }

    //passed
    @Test
    fun when_event_showMessage_is_emitted_then_snackbar_with_message_is_shown() = runTest {
        val expectedMessage = "Snackbar de prueba!"

        mainDispatcherRule.testScope.launch {
            eventFlow.emit(LoginEvent.ShowMessage(expectedMessage))
        }

        advanceUntilIdle()

        Espresso.onIdle()

        onView(withText(expectedMessage))
            .inRoot(withDecorView(isDisplayed()))
            .check(matches(isDisplayed()))
    }

    /*  Test internos y mockeables */
    //passed
    @Test
    fun when_testGoogleSignInDataSource_is_set_then_it_is_used_instead_of_real_one() = runTest {

        val testGoogleSignInDataSource = mockk<GoogleSignInDataSource>(relaxed = true)
        val testIntent = Intent("TEST_GOOGLE_SIGN_IN_INTENT")
        every { testGoogleSignInDataSource.getSignInIntent() } returns testIntent

        val testActivityScenario = ActivityScenario.launch(HiltTestActivity::class.java)
        testActivityScenario.moveToState(Lifecycle.State.RESUMED)

        testActivityScenario.onActivity { activity ->
            activity.supportFragmentManager.fragmentFactory = TestLoginFragmentFactory(
                googleSignInDataSource = testGoogleSignInDataSource,
                facebookSignInDataSource = mockFacebookSignInDataSource,
                mockGoogleSignInLauncher = mockGoogleSignInLauncher,
                callbackManager = mockCallbackManager
            )

            val fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
                activity.classLoader,
                LoginFragment::class.java.name
            ) as LoginFragment

            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commitNow()

            val navController = TestNavHostController(activity)
            navController.setGraph(R.navigation.nav_graph)
            navController.setCurrentDestination(R.id.loginFragment)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }
        Espresso.onIdle()

        onView(withId(R.id.btnGoogleSignIn)).perform(click())

        coVerify(exactly = 1) { testGoogleSignInDataSource.getSignInIntent() }

        coVerify(exactly = 0) { mockGoogleSignInDataSource.getSignInIntent() }

        coVerify(exactly = 1) { mockGoogleSignInLauncher.launch(testIntent) }

        testActivityScenario.close()
    }

    //passed
    @Test
    fun when_testFacebookSignInDataSource_is_set_then_it_is_used_instead_of_real_one() = runTest {
        clearMocks(mockFacebookSignInDataSource)

        val testFacebookSignInDataSource = mockk<FacebookSignInDataSource>(relaxed = true)
        val testCallbackManager = mockk<CallbackManager>(relaxed = true)
        val slotForTestCallback = slot<FacebookCallback<LoginResult>>()
        every {
            testFacebookSignInDataSource.registerCallback(
                any(),
                capture(slotForTestCallback)
            )
        } just Runs
        every { testFacebookSignInDataSource.logInWithReadPermissions(any(), any()) } just Runs
        val testActivityScenario = ActivityScenario.launch(HiltTestActivity::class.java)
        testActivityScenario.moveToState(Lifecycle.State.RESUMED)

        testActivityScenario.onActivity { activity ->
            activity.supportFragmentManager.fragmentFactory = TestLoginFragmentFactory(
                googleSignInDataSource = mockGoogleSignInDataSource,
                facebookSignInDataSource = testFacebookSignInDataSource,
                mockGoogleSignInLauncher = mockGoogleSignInLauncher,
                callbackManager = testCallbackManager
            )

            val fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
                activity.classLoader,
                LoginFragment::class.java.name
            ) as LoginFragment

            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commitNow()

            val navController = TestNavHostController(activity)
            navController.setGraph(R.navigation.nav_graph)
            navController.setCurrentDestination(R.id.loginFragment)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }
        Espresso.onIdle()

        onView(withId(R.id.btnFacebookLogin)).perform(click())

        coVerify(exactly = 1) {
            testFacebookSignInDataSource.logInWithReadPermissions(
                any(),
                listOf("email", "public_profile")
            )
        }

        coVerify(exactly = 1) {
            testFacebookSignInDataSource.registerCallback(
                eq(testCallbackManager),
                any()
            )
        }

        coVerify(exactly = 0) {
            mockFacebookSignInDataSource.logInWithReadPermissions(
                any(),
                any()
            )
        }
        coVerify(exactly = 0) { mockFacebookSignInDataSource.registerCallback(any(), any()) }

        testActivityScenario.close()
    }

    //passed
    @Test
    fun when_testCallbackManager_is_set_then_it_is_used_instead_of_real_one() = runTest {

        clearMocks(mockCallbackManager, mockFacebookSignInDataSource)

        val testCallbackManager = mockk<CallbackManager>(relaxed = true)
        val testFacebookSignInDataSource = mockk<FacebookSignInDataSource>(relaxed = true)
        val slotForTestCallback = slot<FacebookCallback<LoginResult>>()

        every {
            testFacebookSignInDataSource.registerCallback(
                eq(testCallbackManager),
                capture(slotForTestCallback)
            )
        } just Runs
        every { testFacebookSignInDataSource.logInWithReadPermissions(any(), any()) } just Runs

        val testActivityScenario = ActivityScenario.launch(HiltTestActivity::class.java)
        testActivityScenario.moveToState(Lifecycle.State.RESUMED)

        testActivityScenario.onActivity { activity ->
            activity.supportFragmentManager.fragmentFactory = TestLoginFragmentFactory(
                googleSignInDataSource = mockGoogleSignInDataSource,
                facebookSignInDataSource = testFacebookSignInDataSource,
                mockGoogleSignInLauncher = mockGoogleSignInLauncher,
                callbackManager = testCallbackManager
            )

            val fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
                activity.classLoader,
                LoginFragment::class.java.name
            ) as LoginFragment

            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commitNow()

            val navController = TestNavHostController(activity)
            navController.setGraph(R.navigation.nav_graph)
            navController.setCurrentDestination(R.id.loginFragment)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }
        Espresso.onIdle()


        val requestCode = 64206
        val resultCode = Activity.RESULT_OK
        val data = Intent()


        testActivityScenario.onActivity { activity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content) as LoginFragment
            fragment.onActivityResult(requestCode, resultCode, data)
        }

        coVerify(exactly = 1) {
            testCallbackManager.onActivityResult(
                requestCode,
                resultCode,
                data
            )
        }

        coVerify(exactly = 0) { mockCallbackManager.onActivityResult(any(), any(), any()) }

        coVerify(exactly = 1) {
            testFacebookSignInDataSource.registerCallback(
                eq(testCallbackManager),
                any()
            )
        }

        coVerify(exactly = 0) { mockFacebookSignInDataSource.registerCallback(any(), any()) }


        testActivityScenario.close()
    }

    //passed
    @Test
    fun when_testGoogleSignInLauncher_is_set_then_it_is_used() = runTest {
        clearMocks(mockGoogleSignInLauncher, mockGoogleSignInDataSource)
        val testGoogleSignInLauncher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)
        val testGoogleSignInDataSource = mockk<GoogleSignInDataSource>(relaxed = true)

        val expectedIntent = Intent("TEST_GOOGLE_SIGN_IN_INTENT")
        every { testGoogleSignInDataSource.getSignInIntent() } returns expectedIntent
        every { testGoogleSignInLauncher.launch(any()) } just Runs
        val testActivityScenario = ActivityScenario.launch(HiltTestActivity::class.java)
        testActivityScenario.moveToState(Lifecycle.State.RESUMED)

        testActivityScenario.onActivity { activity ->
            activity.supportFragmentManager.fragmentFactory = TestLoginFragmentFactory(
                googleSignInDataSource = testGoogleSignInDataSource,
                facebookSignInDataSource = mockFacebookSignInDataSource,
                mockGoogleSignInLauncher = testGoogleSignInLauncher,
                callbackManager = mockCallbackManager
            )

            val fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
                activity.classLoader,
                LoginFragment::class.java.name
            ) as LoginFragment

            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commitNow()

            val navController = TestNavHostController(activity)
            navController.setGraph(R.navigation.nav_graph)
            navController.setCurrentDestination(R.id.loginFragment)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }
        Espresso.onIdle()


        onView(withId(R.id.btnGoogleSignIn)).perform(click())

        coVerify(exactly = 1) { testGoogleSignInLauncher.launch(expectedIntent) }

        coVerify(exactly = 1) { testGoogleSignInDataSource.getSignInIntent() }

        coVerify(exactly = 0) { mockGoogleSignInLauncher.launch(any()) }
        coVerify(exactly = 0) { mockGoogleSignInDataSource.getSignInIntent() }

        testActivityScenario.close()
    }

    /*  Cliclo de vida */
    //passed
    @Test
    fun when_fragment_is_destroyed_then_binding_is_set_to_null() = runTest {

        clearMocks(
            mockGoogleSignInDataSource, mockFacebookSignInDataSource,
            mockGoogleSignInLauncher, mockCallbackManager
        )

        val scenario = ActivityScenario.launch(HiltTestActivity::class.java)

        val fragmentRef = arrayOf<LoginFragment?>(null)

        scenario.onActivity { activity ->
            activity.supportFragmentManager.fragmentFactory = TestLoginFragmentFactory(
                googleSignInDataSource = mockGoogleSignInDataSource,
                facebookSignInDataSource = mockFacebookSignInDataSource,
                mockGoogleSignInLauncher = mockGoogleSignInLauncher,
                callbackManager = mockCallbackManager
            )

            val loginFragment = activity.supportFragmentManager.fragmentFactory.instantiate(
                activity.classLoader,
                LoginFragment::class.java.name
            ) as LoginFragment

            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, loginFragment, "LoginFragmentTag")
                .commitNow()
            fragmentRef[0] = loginFragment
        }
        Espresso.onIdle()
        val fragment = fragmentRef[0]

        Truth.assertThat(fragment?.binding).isNotNull()

        scenario.onActivity { activity ->
            val fragmentToRemove =
                activity.supportFragmentManager.findFragmentByTag("LoginFragmentTag")
            if (fragmentToRemove != null) {
                activity.supportFragmentManager.beginTransaction()
                    .remove(fragmentToRemove)
                    .commitNow()
            }
        }
        Espresso.onIdle()

        Truth.assertThat(fragment?.binding).isNull()

        scenario.close()
    }


}
