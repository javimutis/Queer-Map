package com.cursoandroid.queermap.ui.login


import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.activity.result.ActivityResult
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
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isSystemAlertWindow
import androidx.test.espresso.matcher.ViewMatchers.*
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
import com.google.android.material.snackbar.Snackbar
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

    private fun dismissSnackbarViewAction(message: String): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return allOf(isDisplayed(), withText(message));
            }

            override fun getDescription(): String {
                return "dismiss snackbar with message: $message"
            }

            override fun perform(uiController: UiController?, view: View?) {
                var currentParent: ViewParent? = view?.parent
                var snackbarLayout: Snackbar.SnackbarLayout? = null

                while (currentParent != null) {
                    if (currentParent is Snackbar.SnackbarLayout) {
                        snackbarLayout = currentParent
                        break
                    }
                    currentParent = currentParent.parent
                }

                snackbarLayout?.let { layout ->
                    (layout.parent as? ViewGroup)?.removeView(layout)
                }
            }
        }
    }

    private fun launchFragmentInScenarioForUiStateTests() {
        val scenario = ActivityScenario.launch(HiltTestActivity::class.java)

        scenario.onActivity { activity ->
            val fragment = LoginFragment() // Uses default constructor, not the one with mocks
            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment, "LoginFragmentTag")
                .commitNow()
        }
    }

    private fun launchLoginFragmentWithFactoryInScenario(scenario: ActivityScenario<HiltTestActivity>): Array<LoginFragment?> {
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
        return fragmentRef
    }

    private fun getLoginFragmentFromActivityScenario(): LoginFragment {
        var fragment: LoginFragment? = null
        activityScenario.onActivity { activity ->
            fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content) as? LoginFragment
            // Ensure fragment is not null, throw an exception if it's not found
            if (fragment == null) {
                throw IllegalStateException("LoginFragment not found in the activity scenario.")
            }
        }
        return fragment!! // We've checked for null, so it's safe to assert non-null
    }

    private fun setupNavControllerSpyForFragment(): TestNavHostController {
        val navControllerSpy = spyk(mockNavController)
        activityScenario.onActivity { activity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content) as LoginFragment
            activity.runOnUiThread {
                Navigation.setViewNavController(fragment.requireView(), navControllerSpy)
            }
        }
        return navControllerSpy
    }

    /**
     * Helper to launch LoginFragment with specific mocked dependencies via TestLoginFragmentFactory.
     * Use this when you need to test the interaction with mocked data sources or launchers.
     * @param scenario The ActivityScenario to launch the fragment in.
     * @param googleSignInDataSource Custom GoogleSignInDataSource mock. Defaults to mockGoogleSignInDataSource.
     * @param facebookSignInDataSource Custom FacebookSignInDataSource mock. Defaults to mockFacebookSignInDataSource.
     * @param mockGoogleSignInLauncher Custom ActivityResultLauncher mock. Defaults to mockGoogleSignInLauncher.
     * @param callbackManager Custom CallbackManager mock. Defaults to mockCallbackManager.
     * @return The LoginFragment instance launched in the scenario.
     */
    private fun launchLoginFragmentWithCustomDependencies(
        scenario: ActivityScenario<HiltTestActivity>,
        googleSignInDataSource: GoogleSignInDataSource = mockGoogleSignInDataSource,
        facebookSignInDataSource: FacebookSignInDataSource = mockFacebookSignInDataSource,
        mockGoogleSignInLauncher: ActivityResultLauncher<Intent> = this.mockGoogleSignInLauncher,
        callbackManager: CallbackManager = this.mockCallbackManager
    ): LoginFragment {
        var fragment: LoginFragment? = null
        scenario.onActivity { activity ->
            activity.supportFragmentManager.fragmentFactory = TestLoginFragmentFactory(
                googleSignInDataSource = googleSignInDataSource,
                facebookSignInDataSource = facebookSignInDataSource,
                mockGoogleSignInLauncher = mockGoogleSignInLauncher,
                callbackManager = callbackManager
            )

            fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
                activity.classLoader,
                LoginFragment::class.java.name
            ) as LoginFragment

            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment!!, "LoginFragmentTag")
                .commitNow()

            // Also set up NavController for general fragment operations if not already done by setUp
            val navController = TestNavHostController(activity)
            navController.setGraph(R.navigation.nav_graph)
            navController.setCurrentDestination(R.id.loginFragment)
            Navigation.setViewNavController(fragment!!.requireView(), navController)
        }
        return fragment!!
    }

    /**
     * Helper to launch LoginFragment and apply custom Log helpers (for D, E, W).
     * This is useful for testing logging behavior specifically.
     * @param logDHelper Custom logD helper function.
     * @param logEHelper Custom logE helper function.
     * @param logWHelper Custom logW helper function.
     * @return The LoginFragment instance launched in the scenario.
     */
    private fun launchLoginFragmentWithCustomLogHelpers(
        logDHelper: ((String, String) -> Unit)? = null,
        logEHelper: ((String, String, Throwable?) -> Unit)? = null,
        logWHelper: ((String, String) -> Unit)? = null
    ): LoginFragment {
        var fragment: LoginFragment? = null
        activityScenario = ActivityScenario.launch(HiltTestActivity::class.java) // Launch a new scenario for isolated log tests
        activityScenario.onActivity { activity ->
            fragment = LoginFragment(
                googleSignInDataSource = mockGoogleSignInDataSource,
                facebookSignInDataSource = mockFacebookSignInDataSource,
                googleSignInLauncher = mockGoogleSignInLauncher,
                callbackManager = mockCallbackManager
            ).apply {
                logDHelper?.let { testLogHelper = it }
                logEHelper?.let { testLogEHelper = it }
                logWHelper?.let { testLogWHelper = it }
            }
            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment!!, "LoginFragmentTag")
                .commitNow()
            Navigation.setViewNavController(fragment!!.requireView(), mockNavController)
        }
        return fragment!!
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
    /*  Initialization and UI State */

    @Test
    fun when_login_fragment_is_launched_then_all_essential_ui_elements_are_displayed() {
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
    fun when_viewmodel_uiState_loading_is_true_then_progress_bar_is_visible() = runTest {
        // Usa el método auxiliar para lanzar el fragmento y configurar el NavController.
        launchFragmentInScenarioForUiStateTests()

        uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)

        advanceUntilIdle()

        onView(withId(R.id.progressBar))
            .check(matches(isDisplayed()))
    }

    @Test
    fun when_viewmodel_uiState_loading_is_false_then_progress_bar_is_hidden() = runTest {
        // Usa el método auxiliar para lanzar el fragmento y configurar el NavController.
        launchFragmentInScenarioForUiStateTests()

        uiStateFlow.value = uiStateFlow.value.copy(isLoading = false)

        advanceUntilIdle()

        onView(withId(R.id.progressBar))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun when_fragment_is_destroyed_then_binding_is_set_to_null() = runTest {
        clearMocks(
            mockGoogleSignInDataSource, mockFacebookSignInDataSource,
            mockGoogleSignInLauncher, mockCallbackManager
        )

        val scenario = ActivityScenario.launch(HiltTestActivity::class.java)

        // Usa la función auxiliar para el lanzamiento del fragmento dentro de este escenario específico.
        val fragmentRef = launchLoginFragmentWithFactoryInScenario(scenario)

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

    /* Input Fields Interaction and Credential Loading */
    @Test
    fun when_typing_in_email_field_then_text_is_updated() {
        onView(withId(R.id.etEmailLogin)).perform(typeText("test@example.com"), closeSoftKeyboard())
        onView(withId(R.id.etEmailLogin)).check(matches(withText("test@example.com")))
    }

    @Test
    fun when_typing_in_password_field_then_text_is_updated() {
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).check(matches(withText("password123")))
    }

    @Test
    fun when_login_loads_credentials_then_email_and_password_fields_are_updated() = runTest {
        val savedEmail = "saved@example.com"
        val savedPassword = "savedPassword123"

        coEvery { mockLoginViewModel.loadUserCredentials() } coAnswers {
            uiStateFlow.value = uiStateFlow.value.copy(
                email = savedEmail,
                password = savedPassword
            )
        }

        // Ensuring the ViewModel call happens on the main thread for UI interactions if needed
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mockLoginViewModel.loadUserCredentials()
        }
        mainDispatcherRule.testScope.advanceUntilIdle()
        Espresso.onIdle()

        onView(withId(R.id.etEmailLogin)).check(matches(withText(savedEmail)))
        onView(withId(R.id.etPassword)).check(matches(withText(savedPassword)))

        coVerify(atLeast = 1) { mockLoginViewModel.loadUserCredentials() }
    }


    /* Email/Password Login Flow */
    @Test
    fun when_login_button_is_clicked_then_loginWithEmail_is_called_with_correct_data_and_navigates_to_home_on_success() =
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

    @Test
    fun when_login_button_is_clicked_and_email_is_invalid_then_error_message_is_shown() = runTest {
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

    @Test
    fun when_login_button_is_clicked_and_password_is_invalid_then_error_message_is_shown() =
        runTest {
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

    @Test
    fun when_login_fails_due_to_general_error_then_error_message_is_shown() = runTest {
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

    @Test
    fun when_email_password_fields_are_empty_and_login_clicked_then_error_messages_are_shown() =
        runTest {
            onView(withId(R.id.etEmailLogin)).perform(clearText())
            onView(withId(R.id.etPassword)).perform(clearText())

            val combinedEmptyFieldsError = "El email y/o la contraseña no pueden estar vacíos."
            coEvery { mockLoginViewModel.loginWithEmail("", "") } coAnswers {
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = false)
                launch(mainDispatcherRule.testDispatcher) {
                    eventFlow.emit(LoginEvent.ShowMessage(combinedEmptyFieldsError))
                }
            }
            onView(withId(R.id.btnLogin)).perform(click())

            advanceUntilIdle()
            Espresso.onIdle()
            onView(withText(combinedEmptyFieldsError))
                .inRoot(withDecorView(isDisplayed()))
                .check(matches(isDisplayed()))

            onView(withText(combinedEmptyFieldsError))
                .inRoot(withDecorView(isDisplayed()))
                .perform(dismissSnackbarViewAction(combinedEmptyFieldsError))

            delay(500)
            advanceUntilIdle()
            Espresso.onIdle()

            coVerify(exactly = 1) { mockLoginViewModel.loginWithEmail("", "") }
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        }

    /* Google Sign-In Flow */
    @Test
    fun when_google_button_is_clicked_then_launcher_is_invoked_and_navigates_to_home_on_success() =
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

            // Centralizamos la obtención del fragment y la configuración del NavController Spy
            val navControllerSpy = setupNavControllerSpyForFragment()

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

    @Test
    fun when_google_login_is_for_new_user_then_navigates_to_signup_with_args() = runTest {
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

        // Centralizamos la obtención del fragment y la configuración del NavController.
        val fragment = getLoginFragmentFromActivityScenario()
        activityScenario.onActivity { activity ->
            Navigation.setViewNavController(fragment.requireView(), mockNavController)
        }

        fragment.handleGoogleSignInResult(Intent())


        mainDispatcherRule.testScope.advanceUntilIdle()
        Espresso.onIdle()

        waitForNavigationTo(mockNavController, R.id.signupFragment)

        val backStackEntry = mockNavController.getBackStackEntry(R.id.signupFragment)
        val args = backStackEntry.arguments

        assertThat(args?.getString("socialUserEmail")).isEqualTo(socialEmail)
        assertThat(args?.getString("socialUserName")).isEqualTo(socialName)
        assertThat(args?.getBoolean("isSocialLoginFlow")).isEqualTo(true)

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))

        onView(withText(messageForNewUser))
            .inRoot(withDecorView(isDisplayed()))
            .check(matches(isDisplayed()))

        onView(withText(messageForNewUser))
            .inRoot(withDecorView(isDisplayed()))
            .perform(dismissSnackbarViewAction(messageForNewUser))

        delay(500)
        advanceUntilIdle()
        Espresso.onIdle()

        coVerify(exactly = 1) { mockGoogleSignInDataSource.handleSignInResult(any()) }
        coVerify(exactly = 1) { mockLoginViewModel.loginWithGoogle(idToken) }
    }

    @Test
    fun when_google_login_fails_then_error_message_is_shown() = runTest {
        val exceptionMessage = "Error de autenticación de Google"
        val expectedDisplayMessage = "Error en Sign-In: $exceptionMessage"

        coEvery { mockGoogleSignInDataSource.handleSignInResult(any()) } returns Result.Failure(
            Exception(exceptionMessage)
        )

        val fragment = getLoginFragmentFromActivityScenario()
        fragment.handleGoogleSignInResult(Intent())


        advanceUntilIdle()
        delay(500) // Se mantiene por si es necesario para la visualización del Snackbar/Toast
        Espresso.onIdle()

        coVerify(exactly = 1) { mockGoogleSignInDataSource.handleSignInResult(any()) }
        coVerify(exactly = 0) { mockLoginViewModel.loginWithGoogle(any()) }

        onView(withText(expectedDisplayMessage))
            .check(matches(isDisplayed()))

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun when_google_login_results_in_cancelled_status_then_snackbar_is_shown() = runTest {
        val cancelledMessage = "Inicio de sesión cancelado"

        // La captura de resultCallbackSlot no es necesaria si solo verificamos la emisión de evento
        every { mockGoogleSignInLauncher.launch(any()) } answers {
            mainDispatcherRule.testScope.launch {
                eventFlow.emit(LoginEvent.ShowMessage(cancelledMessage))
            }
        }

        onView(withId(R.id.btnGoogleSignIn)).perform(click())

        mainDispatcherRule.testScope.advanceUntilIdle()
        Espresso.onIdle()

        onView(withText(cancelledMessage))
            .inRoot(withDecorView(isDisplayed()))
            .check(matches(isDisplayed()))

        onView(withText(cancelledMessage))
            .inRoot(withDecorView(isDisplayed()))
            .perform(dismissSnackbarViewAction(cancelledMessage))

        delay(500) // Se mantiene por si es necesario para la visualización y descarte del Snackbar
        advanceUntilIdle()
        Espresso.onIdle()

        coVerify(exactly = 0) { mockLoginViewModel.loginWithGoogle(any()) }
        coVerify(exactly = 0) { mockGoogleSignInDataSource.handleSignInResult(any()) }

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    /* Facebook Sign-In Flow */
    @Test
    fun when_facebook_button_is_clicked_then_logInWithReadPermissions_is_called() = runTest {
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

    @Test
    fun when_facebook_login_is_successful_then_loginWithFacebook_is_called_with_token_and_navigates_to_home() =
        runTest {
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

            // Ensuring Facebook callback is captured after clicking the button
            onView(withId(R.id.btnFacebookLogin)).perform(scrollTo(), click())
            Espresso.onIdle()
            assert(facebookCallbackSlot.isCaptured) { "Facebook Callback was not registered." }

            // Simulate Facebook SDK calling onSuccess
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
            assertThat(mockNavController.currentDestination?.id).isEqualTo(R.id.mapFragment)

            onView(withText(successMessage))
                .inRoot(isSystemAlertWindow())
                .check(matches(isDisplayed()))
        }

    @Test
    fun when_facebook_login_is_cancelled_then_snackbar_with_cancel_message_is_shown() = runTest {
        val cancelMessage = "Inicio de sesión con Facebook cancelado."

        // Ensuring Facebook callback is captured after clicking the button
        onView(withId(R.id.btnFacebookLogin)).perform(scrollTo(), click())
        Espresso.onIdle()
        assert(facebookCallbackSlot.isCaptured) { "Facebook Callback was not registered." }

        // Simulate Facebook SDK calling onCancel
        activityScenario.onActivity {
            val callback = facebookCallbackSlot.captured
            callback.onCancel()
        }

        advanceUntilIdle()
        Espresso.onIdle()

        onView(withText(cancelMessage))
            .inRoot(withDecorView(isDisplayed()))
            .check(matches(isDisplayed()))
    }

    @Test
    fun when_facebook_login_has_error_then_snackbar_with_error_message_is_shown() = runTest {
        val errorMessage = "Error simulado de Facebook."
        val expectedSnackbarMessage = "Error: $errorMessage"

        // Ensuring Facebook callback is captured after clicking the button
        onView(withId(R.id.btnFacebookLogin)).perform(scrollTo(), click())
        Espresso.onIdle()
        assert(facebookCallbackSlot.isCaptured) { "Facebook Callback was not registered." }

        val facebookException = FacebookException(errorMessage)

        // Simulate Facebook SDK calling onError
        activityScenario.onActivity {
            val callback = facebookCallbackSlot.captured
            callback.onError(facebookException)
        }

        advanceUntilIdle()
        Espresso.onIdle()

        onView(withText(expectedSnackbarMessage))
            .inRoot(withDecorView(isDisplayed()))
            .check(matches(isDisplayed()))
    }

    @Test
    fun when_facebook_login_sends_null_data_then_error_snackbar_is_shown() = runTest {
        val expectedErrorMessage =
            "El token de acceso de Facebook es nulo. Por favor, inténtelo de nuevo."

        // Mock the behavior when loginWithReadPermissions is called and should emit an error message
        every { mockFacebookSignInDataSource.logInWithReadPermissions(any(), any()) } answers {
            mainDispatcherRule.testScope.launch {
                eventFlow.emit(LoginEvent.ShowMessage(expectedErrorMessage))
            }
        }

        onView(withId(R.id.btnFacebookLogin)).perform(click())

        advanceUntilIdle()
        Espresso.onIdle()

        coVerify(exactly = 0) { mockLoginViewModel.loginWithFacebook(any()) }

        onView(withText(expectedErrorMessage))
            .inRoot(withDecorView(isDisplayed()))
            .check(matches(isDisplayed()))

        onView(withText(expectedErrorMessage))
            .inRoot(withDecorView(isDisplayed()))
            .perform(dismissSnackbarViewAction(expectedErrorMessage))

        delay(500) // Keep if necessary for Snackbar dismissal
        advanceUntilIdle()
        Espresso.onIdle()

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    /* Navigation and Additional User Actions */
    @Test
    fun when_back_icon_is_clicked_then_navigate_back_is_called() = runTest {
        coEvery { mockLoginViewModel.onBackPressed() } coAnswers {
            launch(mainDispatcherRule.testDispatcher) {
                eventFlow.emit(LoginEvent.NavigateBack)
            }
        }

        // Setup the NavController to simulate being on LoginFragment after navigating from MapFragment
        activityScenario.onActivity { activity ->
            activity.runOnUiThread {
                mockNavController.setCurrentDestination(R.id.mapFragment)
                mockNavController.navigate(R.id.loginFragment)
            }
        }
        Espresso.onIdle()
        assertThat(mockNavController.currentDestination?.id).isEqualTo(R.id.loginFragment)

        // Use the shared helper to get a spied NavController
        val navControllerSpy = setupNavControllerSpyForFragment()

        onView(withId(R.id.ivBack)).perform(click())
        advanceUntilIdle()
        Espresso.onIdle()

        coVerify(exactly = 1) { mockLoginViewModel.onBackPressed() }
        coVerify(exactly = 1) { navControllerSpy.popBackStack() }
        assertThat(navControllerSpy.currentDestination?.id).isEqualTo(R.id.mapFragment)
    }

    @Test
    fun when_signup_text_is_clicked_then_navigates_to_signup_fragment() = runTest {
        // Use the shared helper to get a spied NavController
        val navControllerSpy = setupNavControllerSpyForFragment()

        onView(withId(R.id.tvSignUpBtn)).perform(click())

        waitForNavigationTo(navControllerSpy, R.id.signupFragment)

        assertThat(navControllerSpy.currentDestination?.id).isEqualTo(R.id.signupFragment)

        coVerify(exactly = 1) { navControllerSpy.navigate(R.id.action_loginFragment_to_signupFragment) }
    }

    @Test
    fun when_forgot_password_text_is_clicked_then_navigates_to_forgot_password_fragment() =
        runTest {
            coEvery { mockLoginViewModel.onForgotPasswordClicked() } coAnswers {
                launch(mainDispatcherRule.testDispatcher) {
                    eventFlow.emit(LoginEvent.NavigateToForgotPassword)
                }
            }
            // Use the shared helper to get a spied NavController
            val navControllerSpy = setupNavControllerSpyForFragment()

            onView(withId(R.id.tvForgotPassword)).perform(click())

            advanceUntilIdle()
            Espresso.onIdle()

            coVerify(exactly = 1) { mockLoginViewModel.onForgotPasswordClicked() }

            waitForNavigationTo(navControllerSpy, R.id.forgotPasswordFragment)
            assertThat(navControllerSpy.currentDestination?.id).isEqualTo(R.id.forgotPasswordFragment)

            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        }

    /* Message Handling (Snackbars) */
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

    @Test
    fun when_viewModel_emits_errorMessage_then_snackbar_is_shown() = runTest {
        clearMocks(mockLoginViewModel) // Clears previous mock behaviors for this specific test

        val errorMessage = "Hubo un problema al iniciar sesión."

        uiStateFlow.value = uiStateFlow.value.copy(errorMessage = errorMessage)

        advanceUntilIdle()
        onView(withText(errorMessage))
            .inRoot(withDecorView(isDisplayed()))
            .check(matches(isDisplayed()))
    }

    @Test
    fun when_snackbar_is_shown_then_it_can_be_dismissed() = runTest {
        clearMocks(mockLoginViewModel) // Clears previous mock behaviors for this specific test

        val snackbarMessage = "Este es un mensaje de prueba para el Snackbar."

        launch(mainDispatcherRule.testDispatcher) {
            eventFlow.emit(LoginEvent.ShowMessage(snackbarMessage))
        }
        delay(500) // Small delay to allow Snackbar to be shown
        advanceUntilIdle()
        Espresso.onIdle()

        onView(withText(snackbarMessage))
            .inRoot(withDecorView(isDisplayed()))
            .check(matches(isDisplayed()))

        // Reusing the general dismissSnackbarViewAction
        onView(withText(snackbarMessage))
            .inRoot(withDecorView(isDisplayed()))
            .perform(dismissSnackbarViewAction(snackbarMessage)) // Use the existing helper

        delay(500) // Small delay to allow Snackbar to be dismissed
        advanceUntilIdle()
        Espresso.onIdle()

        onView(withText(snackbarMessage))
            .check(doesNotExist())
    }


    /*  Dependency Injection and Helper Testing (Mocking) */
    @Test
    fun when_custom_googleSignInDataSource_is_set_then_it_is_used_instead_of_default() = runTest {
        val testGoogleSignInDataSource = mockk<GoogleSignInDataSource>(relaxed = true)
        val testIntent = Intent("TEST_GOOGLE_SIGN_IN_INTENT")
        every { testGoogleSignInDataSource.getSignInIntent() } returns testIntent

        // Use the new helper for launching fragment with custom factory and scenario
        val testActivityScenario = ActivityScenario.launch(HiltTestActivity::class.java)
        launchLoginFragmentWithCustomDependencies(
            scenario = testActivityScenario,
            googleSignInDataSource = testGoogleSignInDataSource
        )
        Espresso.onIdle()

        onView(withId(R.id.btnGoogleSignIn)).perform(click())

        coVerify(exactly = 1) { testGoogleSignInDataSource.getSignInIntent() }
        coVerify(exactly = 0) { mockGoogleSignInDataSource.getSignInIntent() } // Ensure default is NOT used
        coVerify(exactly = 1) { mockGoogleSignInLauncher.launch(testIntent) }

        testActivityScenario.close()
    }

    @Test
    fun when_custom_facebookSignInDataSource_is_set_then_it_is_used_instead_of_default() = runTest {
        clearMocks(mockFacebookSignInDataSource) // Clear existing mocks to ensure new one is used

        val testFacebookSignInDataSource = mockk<FacebookSignInDataSource>(relaxed = true)
        val testCallbackManager = mockk<CallbackManager>(relaxed = true)
        val slotForTestCallback =
            slot<FacebookCallback<LoginResult>>() // Captured slot for verification
        every {
            testFacebookSignInDataSource.registerCallback(any(), capture(slotForTestCallback))
        } just Runs
        every { testFacebookSignInDataSource.logInWithReadPermissions(any(), any()) } just Runs

        // Use the new helper for launching fragment with custom factory and scenario
        val testActivityScenario = ActivityScenario.launch(HiltTestActivity::class.java)
        launchLoginFragmentWithCustomDependencies(
            scenario = testActivityScenario,
            facebookSignInDataSource = testFacebookSignInDataSource,
            callbackManager = testCallbackManager
        )
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

    @Test
    fun when_custom_callbackManager_is_set_then_it_is_used_instead_of_default() = runTest {
        clearMocks(mockCallbackManager, mockFacebookSignInDataSource) // Clear existing mocks

        val testCallbackManager = mockk<CallbackManager>(relaxed = true)
        val testFacebookSignInDataSource = mockk<FacebookSignInDataSource>(relaxed = true)
        val slotForTestCallback =
            slot<FacebookCallback<LoginResult>>() // Captured slot for verification

        every {
            testFacebookSignInDataSource.registerCallback(
                eq(testCallbackManager),
                capture(slotForTestCallback)
            )
        } just Runs
        every { testFacebookSignInDataSource.logInWithReadPermissions(any(), any()) } just Runs

        // Use the new helper for launching fragment with custom factory and scenario
        val testActivityScenario = ActivityScenario.launch(HiltTestActivity::class.java)
        val fragment = launchLoginFragmentWithCustomDependencies(
            scenario = testActivityScenario,
            facebookSignInDataSource = testFacebookSignInDataSource,
            callbackManager = testCallbackManager
        )
        Espresso.onIdle()

        val requestCode = 64206
        val resultCode = Activity.RESULT_OK
        val data = Intent()

        testActivityScenario.onActivity { activity ->
            // Directly call onActivityResult on the fragment instance
            fragment.onActivityResult(requestCode, resultCode, data)
        }

        coVerify(exactly = 1) {
            testCallbackManager.onActivityResult(
                requestCode,
                resultCode,
                data
            )
        }
        coVerify(exactly = 0) {
            mockCallbackManager.onActivityResult(
                any(),
                any(),
                any()
            )
        } // Ensure default is NOT used
        coVerify(exactly = 1) {
            testFacebookSignInDataSource.registerCallback(
                eq(testCallbackManager),
                any()
            )
        }
        coVerify(exactly = 0) {
            mockFacebookSignInDataSource.registerCallback(
                any(),
                any()
            )
        } // Ensure default is NOT used

        testActivityScenario.close()
    }

    @Test
    fun when_custom_googleSignInLauncher_is_set_then_it_is_used_instead_of_default() = runTest {
        clearMocks(mockGoogleSignInLauncher, mockGoogleSignInDataSource) // Clear existing mocks
        val testGoogleSignInLauncher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)
        val testGoogleSignInDataSource = mockk<GoogleSignInDataSource>(relaxed = true)

        val expectedIntent = Intent("TEST_GOOGLE_SIGN_IN_INTENT")
        every { testGoogleSignInDataSource.getSignInIntent() } returns expectedIntent
        every { testGoogleSignInLauncher.launch(any()) } just Runs

        // Use the new helper for launching fragment with custom factory and scenario
        val testActivityScenario = ActivityScenario.launch(HiltTestActivity::class.java)
        launchLoginFragmentWithCustomDependencies(
            scenario = testActivityScenario,
            googleSignInDataSource = testGoogleSignInDataSource,
            mockGoogleSignInLauncher = testGoogleSignInLauncher
        )
        Espresso.onIdle()

        onView(withId(R.id.btnGoogleSignIn)).perform(click())

        coVerify(exactly = 1) { testGoogleSignInLauncher.launch(expectedIntent) }
        coVerify(exactly = 1) { testGoogleSignInDataSource.getSignInIntent() }
        coVerify(exactly = 0) { mockGoogleSignInLauncher.launch(any()) } // Ensure default is NOT used
        coVerify(exactly = 0) { mockGoogleSignInDataSource.getSignInIntent() } // Ensure default is NOT used

        testActivityScenario.close()
    }

    @Test
    fun when_custom_logD_helper_is_set_then_it_is_used() = runTest {
        val capturedLogTag = slot<String>()
        val capturedLogMessage = slot<String>()

        val mockLogHelper: (String, String) -> Unit = mockk(relaxed = true)
        every { mockLogHelper(capture(capturedLogTag), capture(capturedLogMessage)) } just Runs

        coEvery { mockGoogleSignInDataSource.handleSignInResult(any()) } returns Result.Success("fake_google_id_token")

        // Use the new helper for launching fragment with custom logD helper
        val fragment = launchLoginFragmentWithCustomLogHelpers(
            logDHelper = mockLogHelper
        )
        Espresso.onIdle() // Wait for fragment to be ready

        // Trigger an action that should cause a logD call
        val fakeIntent = mockk<Intent>(relaxed = true)
        activityScenario.onActivity {
            fragment.handleGoogleSignInResult(fakeIntent)
        }

        mainDispatcherRule.testScope.advanceUntilIdle()

        val expectedTag = "LoginFragment"
        val expectedPartialMessage = "Inside handleGoogleSignInResult"

        coVerify {
            mockLogHelper(
                expectedTag,
                match { msg -> msg.startsWith(expectedPartialMessage) }
            )
        }
    }

    @Test
    fun when_custom_logE_helper_is_set_then_it_is_used() = runTest {
        val capturedLogTag = slot<String>()
        val capturedLogMessage = slot<String>()

        val mockLogEHelper: (String, String, Throwable?) -> Unit = mockk(relaxed = true)
        every {
            mockLogEHelper(
                capture(capturedLogTag),
                capture(capturedLogMessage),
                any()
            )
        } just Runs

        val expectedException = Exception("Simulated Google Sign-In error for test")
        coEvery { mockGoogleSignInDataSource.handleSignInResult(any()) } returns Result.Failure(
            expectedException
        )

        // Use the new helper for launching fragment with custom logE helper
        val fragment = launchLoginFragmentWithCustomLogHelpers(
            logEHelper = mockLogEHelper
        )
        Espresso.onIdle() // Wait for fragment to be ready

        // Trigger an action that should cause a logE call
        val fakeIntent = mockk<Intent>(relaxed = true)
        activityScenario.onActivity {
            fragment.handleGoogleSignInResult(fakeIntent)
        }

        mainDispatcherRule.testScope.advanceUntilIdle()

        val expectedTag = "LoginFragment"
        val expectedPartialMessage = "Google Sign-In failed"
        val expectedErrorMessageFromException = expectedException.message

        coVerify(atLeast = 1) {
            mockLogEHelper(
                expectedTag,
                match { msg ->
                    msg.contains(expectedPartialMessage) && msg.contains(
                        expectedErrorMessageFromException!!
                    )
                },
                null // Since we expect the exception to be passed, adjust if the helper signature expects Throwable
            )
        }

        assertThat(capturedLogTag.captured).isEqualTo(expectedTag)
        assertThat(capturedLogMessage.captured).contains(expectedPartialMessage)
        assertThat(capturedLogMessage.captured).contains(expectedErrorMessageFromException!!)

        val expectedSnackbarMessage = "Error en Sign-In: ${expectedException.message}"
        onView(withText(expectedSnackbarMessage))
            .inRoot(withDecorView(isDisplayed()))
            .check(matches(isDisplayed()))

        onView(withText(expectedSnackbarMessage))
            .inRoot(withDecorView(isDisplayed()))
            .perform(dismissSnackbarViewAction(expectedSnackbarMessage))

        delay(500)
        advanceUntilIdle()
        Espresso.onIdle()

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun when_custom_logW_helper_is_set_then_it_is_used() = runTest {
        val capturedLogTag = slot<String>()
        val capturedLogMessage = slot<String>()

        val mockLogWHelper: (String, String) -> Unit = mockk(relaxed = true)
        every { mockLogWHelper(capture(capturedLogTag), capture(capturedLogMessage)) } just Runs
        coEvery { mockGoogleSignInDataSource.handleSignInResult(any()) } returns Result.Success("fake_valid_google_token")

        // Use the new helper for launching fragment with custom logW helper
        val fragment = launchLoginFragmentWithCustomLogHelpers(
            logWHelper = mockLogWHelper
        )
        Espresso.onIdle() // Wait for fragment to be ready

        mainDispatcherRule.testScope.launch {
            eventFlow.emit(LoginEvent.ShowMessage("Advertencia: algunas características podrían no funcionar."))
        }
        advanceUntilIdle() // Ensure the event emission and UI update are processed

        val expectedTag = "LoginFragment"
        val expectedMessage = "Advertencia: algunas características podrían no funcionar."

        coVerify(atLeast = 1) {
            mockLogWHelper(expectedTag, expectedMessage)
        }
        assertThat(capturedLogTag.captured).isEqualTo(expectedTag)
        assertThat(capturedLogMessage.captured).isEqualTo(expectedMessage)

        onView(withText(expectedMessage))
            .inRoot(withDecorView(isDisplayed()))
            .check(matches(isDisplayed()))

        onView(withText(expectedMessage))
            .inRoot(withDecorView(isDisplayed()))
            .perform(dismissSnackbarViewAction(expectedMessage))

        delay(500)
        advanceUntilIdle()
        Espresso.onIdle()

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }
}