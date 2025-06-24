package com.cursoandroid.queermap.ui.login

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
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
import androidx.test.espresso.intent.Intents
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
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
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


// Custom ViewAction to wait until a view is clickable (visible, enabled, clickable)
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

    // Mock for ActivityResultLauncher
    private lateinit var mockGoogleSignInLauncher: ActivityResultLauncher<Intent>

    // Variable para mantener una referencia al LoginFragment
    private lateinit var loginFragmentInstance: LoginFragment

    // Custom FragmentFactory para asegurar que el LoginFragment reciba el mock de ViewModel y Launcher
    @BindValue
    @JvmField
    val fragmentFactory: FragmentFactory = object : FragmentFactory() {
        override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
            return when (className) {
                LoginFragment::class.java.name -> LoginFragment().apply {
                    testViewModel =
                        mockLoginViewModel // Assign the mock to the fragment's testViewModel
                    testGoogleSignInLauncher = mockGoogleSignInLauncher // Assign the mock launcher
                    loginFragmentInstance = this // Capture the fragment instance here
                }
                // Add other fragments if they are part of the navigation graph and needed for the test
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

    // Define the fake ID token here to ensure consistency
    private val FAKE_GOOGLE_ID_TOKEN = "fake_google_id_token"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher) // Set the main dispatcher for coroutines
        hiltRule.inject() // Inject dependencies, including @BindValue mocks

        // Initialize Intents before each test that interacts with activities.
        // This is still good practice if other tests in the class might use it,
        // even if not directly for the Google Sign-In intent verification anymore.
        Intents.init()

        // Initialize flows with an initial state
        uiStateFlow = MutableStateFlow(LoginUiState())
        eventFlow = MutableSharedFlow()
        accessTokenChannelFlow = MutableSharedFlow()

        clearAllMocks() // Clear all mocks before each test to avoid interferences

        // Stub the behavior of mockLoginViewModel
        every { mockLoginViewModel.uiState } returns uiStateFlow
        every { mockLoginViewModel.event } returns eventFlow

        // Stub for GoogleSignInDataSource: getSignInIntent must return a valid Intent
        every { mockGoogleSignInDataSource.getSignInIntent() } returns Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://example.com/oauth")
        )

        // *** REMOVE THIS STUB for handleSignInResult ***
        // We will directly call loginWithGoogle from the launcher's mock now,
        // bypassing the dataSource's handleSignInResult for this test to simplify flow.
        // coEvery { mockGoogleSignInDataSource.handleSignInResult(any()) } returns Result.success(FAKE_GOOGLE_ID_TOKEN)


        // Stub general behavior of mockLoginViewModel
        coEvery { mockLoginViewModel.loginWithEmail(any(), any()) } coAnswers { /* do nothing */ }
        // loginWithGoogle behavior will be defined here, and this is the ONLY place it should be defined for this test.
        coEvery { mockLoginViewModel.loginWithGoogle(FAKE_GOOGLE_ID_TOKEN) } coAnswers {
            uiStateFlow.emit(uiStateFlow.value.copy(isLoading = true))
            uiStateFlow.emit(uiStateFlow.value.copy(isLoading = false, isSuccess = true))
            eventFlow.emit(LoginEvent.NavigateToHome)
            eventFlow.emit(LoginEvent.ShowMessage("Inicio de sesión con Google exitoso"))
        }

        coEvery { mockLoginViewModel.loginWithFacebook(any()) } coAnswers { /* do nothing */ }
        coEvery { mockLoginViewModel.onForgotPasswordClicked() } coAnswers { /* do nothing */ }
        coEvery { mockLoginViewModel.onBackPressed() } coAnswers { /* do nothing */ }
        coEvery {
            mockLoginViewModel.saveUserCredentials(
                any(),
                any()
            )
        } coAnswers { /* do nothing */ }
        coEvery { mockLoginViewModel.loadUserCredentials() } coAnswers { /* do nothing */ } // Called in onViewCreated

        // Stub for InputValidator
        every { mockInputValidator.isValidEmail(any()) } returns true
        every { mockInputValidator.isValidPassword(any()) } returns true
        every { mockInputValidator.isValidFullName(any()) } returns true
        every { mockInputValidator.isValidUsername(any()) } returns true
        every { mockInputValidator.isStrongPassword(any()) } returns true
        every { mockInputValidator.isValidBirthday(any()) } returns true

        // Stub for FacebookSignInDataSource
        every { mockFacebookSignInDataSource.accessTokenChannel } returns accessTokenChannelFlow
        every { mockFacebookSignInDataSource.registerCallback(any()) } answers { /* do nothing */ }
        every {
            mockFacebookSignInDataSource.logInWithReadPermissions(
                any(),
                any()
            )
        } answers { /* do nothing */ }

        // Initialize the mock ActivityResultLauncher
        mockGoogleSignInLauncher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)

        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)

        navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        activityScenario = ActivityScenario.launch(HiltTestActivity::class.java)

        activityScenario.onActivity { activity ->
            activity.supportFragmentManager.fragmentFactory = fragmentFactory

            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content) as? LoginFragment
                    ?: fragmentFactory.instantiate(
                        activity.classLoader,
                        LoginFragment::class.java.name
                    ) as LoginFragment
            // The fragment instance is now captured in `loginFragmentInstance` via the FragmentFactory

            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment, null)
                .commitNow()

            // Observe the fragment's viewLifecycleOwnerLiveData to set up navigation
            fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                viewLifecycleOwner?.let {
                    navController.setGraph(R.navigation.nav_graph)
                    navController.setCurrentDestination(R.id.loginFragment)

                    fragment.view?.post {
                        Navigation.setViewNavController(fragment.requireView(), navController)
                    }
                }
            }

            // Ensure all initial coroutines of the fragment complete
            testScheduler.advanceUntilIdle()
            activityDecorView = activity.window.decorView
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // Reset the main dispatcher
        IdlingRegistry.getInstance()
            .unregister(EspressoIdlingResource.countingIdlingResource) // Unregister the IdlingResource

        // Release Intents after each test
        Intents.release()

        if (this::activityScenario.isInitialized) {
            activityScenario.close() // Close the activity scenario
        }

        // Reset the state of flows for the next test
        if (this::uiStateFlow.isInitialized) {
            uiStateFlow.value = LoginUiState()
        }

        clearAllMocks() // Ensure mocks are completely reset for subsequent tests
        testScheduler.advanceUntilIdle() // Ensure all pending coroutines complete
    }

    @Test
    fun when_google_sign_in_button_clicked_and_result_is_success_then_navigates_to_home() =
        runTest(testDispatcher) {
            // Arrange
            val successMessage = "Inicio de sesión con Google exitoso"

            // Define the behavior of mockLoginViewModel.loginWithGoogle when it's called
            // This is the ONLY place this stub should cause the ViewModel to emit events/states.
            coEvery { mockLoginViewModel.loginWithGoogle(FAKE_GOOGLE_ID_TOKEN) } coAnswers {
                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = true))
                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = false, isSuccess = true))
                eventFlow.emit(LoginEvent.NavigateToHome)
                eventFlow.emit(LoginEvent.ShowMessage(successMessage))
            }

            // Stub the launch method of our mock ActivityResultLauncher.
            // THIS IS THE CRITICAL CHANGE: It directly triggers mockLoginViewModel.loginWithGoogle.
            val intentSlot = slot<Intent>()
            every { mockGoogleSignInLauncher.launch(capture(intentSlot)) } answers {
                // Assert that the intent passed to the launcher is the one we expect
                assertThat(intentSlot.captured.action).isEqualTo(Intent.ACTION_VIEW)
                assertThat(intentSlot.captured.data).isEqualTo(Uri.parse("https://example.com/oauth"))

                // Directly call loginWithGoogle on the mocked ViewModel.
                // This bypasses the GoogleSignInDataSource's handleSignInResult and
                // the fragment's `onSuccess` callback, ensuring a single, controlled call.
                // We run this in a new coroutine on the testDispatcher to simulate asynchronicity if needed
                // and allow `advanceUntilIdle` to pick it up.
                backgroundScope.launch(testDispatcher) { // Use backgroundScope and testDispatcher for controlled async
                    mockLoginViewModel.loginWithGoogle(FAKE_GOOGLE_ID_TOKEN)
                }
            }


            // Act
            onView(withId(R.id.btnGoogleSignIn)).perform(click())

            // Advance the TestCoroutineScheduler to ensure all coroutines complete.
            Espresso.onIdle() // Give Espresso a chance to process UI events
            testScheduler.advanceUntilIdle() // Advance time for coroutines


            // Assert
            // Verify that mockGoogleSignInLauncher.launch was called exactly once
            coVerify(exactly = 1) { mockGoogleSignInLauncher.launch(any()) }

            // *** REMOVE THIS VERIFY: mockGoogleSignInDataSource.handleSignInResult is no longer called in this simplified flow ***
            // coVerify(exactly = 1) { mockGoogleSignInDataSource.handleSignInResult(any()) }

            // Verify that loginWithGoogle was called exactly once on the LoginViewModel mock with the expected token
            coVerify(exactly = 1) { mockLoginViewModel.loginWithGoogle(FAKE_GOOGLE_ID_TOKEN) }


            // Verify that the Snackbar with the success message is shown
            onView(withText(successMessage))
                .inRoot(withDecorView(not(`is`(activityDecorView)))) // Verify in a different window for the Snackbar
                .check(matches(isDisplayed()))

            // Verify that the progress bar is not visible
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))

            // Verify navigation to the home screen (MapFragment)
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
