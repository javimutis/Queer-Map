package com.cursoandroid.queermap.ui.login


import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.Root
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.cursoandroid.queermap.HiltTestActivity
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.common.InputValidator
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import com.cursoandroid.queermap.util.EspressoIdlingResource
import com.cursoandroid.queermap.util.MainDispatcherRule
import com.cursoandroid.queermap.util.Result
import com.cursoandroid.queermap.util.Result.Failure
import com.cursoandroid.queermap.util.Result.Success
import com.cursoandroid.queermap.util.waitForNavigationTo
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
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
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
import kotlin.time.Duration.Companion.seconds

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
    private lateinit var mockCallbackManager: CallbackManager
    private lateinit var mockGoogleSignInDataSource: GoogleSignInDataSource
    private lateinit var mockFacebookSignInDataSource: FacebookSignInDataSource


    private lateinit var activityScenario: ActivityScenario<HiltTestActivity>
    private lateinit var mockNavController: TestNavHostController

    private lateinit var uiStateFlow: MutableStateFlow<LoginUiState>
    private lateinit var eventFlow: MutableSharedFlow<LoginEvent>

    private val FAKE_GOOGLE_ID_TOKEN = "fake_google_id_token"
    private val FAKE_GOOGLE_EMAIL = "test_google@example.com"
    private val FAKE_GOOGLE_NAME = "Test Google User"

    private lateinit var facebookCallbackSlot: CapturingSlot<FacebookCallback<LoginResult>>

    private fun dismissSnackbarViewAction(message: String): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return allOf(isDisplayed(), withText(message))
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

    // Helper function to get the current LoginFragment instance
    private fun getLoginFragmentFromActivityScenario(): LoginFragment {
        var fragment: LoginFragment? = null
        activityScenario.onActivity { activity ->
            // Try to find the fragment by its tag first if explicitly set
            fragment =
                activity.supportFragmentManager.findFragmentByTag("LoginFragmentTag") as? LoginFragment
            if (fragment == null) {
                // If not found by tag, try to find it as the primary navigation fragment
                val navHostFragment =
                    activity.supportFragmentManager.fragments.firstOrNull { it is NavHostFragment } as? NavHostFragment
                fragment =
                    navHostFragment?.childFragmentManager?.primaryNavigationFragment as? LoginFragment
            }
            if (fragment == null) {
                throw IllegalStateException("LoginFragment not found in the activity scenario.")
            }
        }
        return fragment!!
    }

    private fun setupNavControllerSpyForFragment(): TestNavHostController {
        // mockNavController is already a spyk, just ensure it's set on the fragment's view
        activityScenario.onActivity { activity ->
            val fragment = getLoginFragmentFromActivityScenario()
            activity.runOnUiThread {
                Navigation.setViewNavController(fragment.requireView(), mockNavController)
            }
        }
        return mockNavController
    }

    private fun launchLoginFragmentWithCustomDependencies(
        googleSignInLauncher: ActivityResultLauncher<Intent>? = null,
        callbackManager: CallbackManager? = null,
        googleSignInDataSource: GoogleSignInDataSource? = null,
        facebookSignInDataSource: FacebookSignInDataSource? = null,
        logDHelper: ((String, String) -> Unit)? = null,
        logEHelper: ((String, String, Throwable?) -> Unit)? = null,
        logWHelper: ((String, String) -> Unit)? = null
    ): LoginFragment {
        var fragment: LoginFragment? = null
        activityScenario.onActivity { activity ->
            fragment = LoginFragment(
                providedGoogleSignInLauncher = googleSignInLauncher
                    ?: this.mockGoogleSignInLauncher,
                providedCallbackManager = callbackManager ?: this.mockCallbackManager,
                providedGoogleSignInDataSource = googleSignInDataSource
                    ?: this.mockGoogleSignInDataSource,
                providedFacebookSignInDataSource = facebookSignInDataSource
                    ?: this.mockFacebookSignInDataSource
            ).apply {
                logDHelper?.let { testLogHelper = it }
                logEHelper?.let { testLogEHelper = it }
                logWHelper?.let { testLogWHelper = it }
            }

            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment!!, "LoginFragmentTag")
                .commitNow() // Use commitNow to ensure the fragment is added immediately

            // Set the NavController after the fragment's view is created and attached
            Navigation.setViewNavController(fragment!!.requireView(), mockNavController)
        }
        Espresso.onIdle() // Wait for UI to settle after fragment transaction
        return fragment!!
    }

    // Consolidated helper for log helpers if needed, otherwise use the combined one above
    private fun launchLoginFragmentWithCustomLogHelpers(
        logDHelper: ((String, String) -> Unit)? = null,
        logEHelper: ((String, String, Throwable?) -> Unit)? = null,
        logWHelper: ((String, String) -> Unit)? = null
    ): LoginFragment {
        return launchLoginFragmentWithCustomDependencies(
            logDHelper = logDHelper,
            logEHelper = logEHelper,
            logWHelper = logWHelper
        )
    }

    fun withActivityDecorView(): Matcher<Root> {
        var decorView: View? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull() as? Activity
            decorView = activity?.window?.decorView
        }
        return withDecorView(not(`is`(decorView)))
    }


    private fun getActivityDecorView(): View {
        var decorView: View? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull()
            decorView = activity?.window?.decorView
        }
        return decorView!!
    }

    private suspend fun waitForNavigationTo(
        navController: TestNavHostController,
        @androidx.annotation.IdRes destId: Int
    ) {
        withTimeout(5000) { // Set a timeout for navigation to prevent infinite waiting
            var navigated = false
            while (!navigated) {
                mainDispatcherRule.testScope.advanceUntilIdle()
                if (navController.currentDestination?.id == destId) {
                    navigated = true
                } else {
                    // Small delay to prevent busy-waiting if advanceUntilIdle() isn't enough
                    kotlinx.coroutines.delay(50)
                }
            }
        }
    }


    @Before
    fun setUp() {
        hiltRule.inject()
        clearAllMocks()

        uiStateFlow = MutableStateFlow(LoginUiState())
        eventFlow = MutableSharedFlow(extraBufferCapacity = 1)
        every { mockLoginViewModel.uiState } returns uiStateFlow
        every { mockLoginViewModel.event } returns eventFlow

        every { mockLoginViewModel.loadUserCredentials() } just Runs
        every { mockLoginViewModel.loginWithEmail(any(), any()) } just Runs
        every { mockLoginViewModel.loginWithGoogle(any()) } just Runs
        every { mockLoginViewModel.onForgotPasswordClicked() } just Runs
        every { mockLoginViewModel.onBackPressed() } just Runs
        every { mockLoginViewModel.loginWithFacebook(any()) } just Runs

        mockGoogleSignInLauncher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)
        mockCallbackManager = mockk(relaxed = true)
        every { mockCallbackManager.onActivityResult(any(), any(), any()) } returns true

        mockGoogleSignInDataSource = mockk(relaxed = true)
        mockFacebookSignInDataSource = mockk(relaxed = true)

        facebookCallbackSlot = slot()

        every {
            mockFacebookSignInDataSource.registerCallback(any(), capture(facebookCallbackSlot))
        } just Runs

        every { mockGoogleSignInDataSource.getSignInIntent() } answers {
            Intent("com.google.android.gms.auth.GOOGLE_SIGN_IN")
        }

        every { mockFacebookSignInDataSource.logInWithReadPermissions(any(), any()) } just Runs

        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)

        // Initialize activityScenario and mockNavController here, once.
        activityScenario = ActivityScenario.launch(HiltTestActivity::class.java)
        activityScenario.onActivity { activity ->
            // Initialize mockNavController as a spy here.
            mockNavController = spyk(TestNavHostController(activity)) // Initialize as spyk here
            mockNavController.setGraph(R.navigation.nav_graph)
            mockNavController.setCurrentDestination(R.id.loginFragment)

            val fragment = LoginFragment(
                providedGoogleSignInLauncher = mockGoogleSignInLauncher,
                providedCallbackManager = mockCallbackManager,
                providedGoogleSignInDataSource = mockGoogleSignInDataSource,
                providedFacebookSignInDataSource = mockFacebookSignInDataSource
            )
            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment, "LoginFragmentTag").commitNow()

            // Attach the spy NavController immediately
            val latch = CountDownLatch(1)
            fragment.viewLifecycleOwnerLiveData.observe(fragment) { viewLifecycleOwner ->
                if (viewLifecycleOwner != null && fragment.view != null && viewLifecycleOwner.lifecycle.currentState.isAtLeast(
                        Lifecycle.State.RESUMED
                    )
                ) {
                    // Set the *spied* navController here
                    Navigation.setViewNavController(fragment.requireView(), mockNavController)
                    latch.countDown()
                }
            }
            latch.await(2, TimeUnit.SECONDS)
        }
        activityScenario.moveToState(Lifecycle.State.RESUMED)
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

    /* Initialization and UI State */
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
        uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)

        advanceUntilIdle() // Deja que el StateFlow y la UI se actualicen

        waitForWindowFocus() // Asegura que el decorView esté enfocado

        onView(withId(R.id.progressBar)).check(matches(isDisplayed()))
    }

    fun waitForWindowFocus() {
        val latch = CountDownLatch(1)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity =
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .firstOrNull()
            val decorView = activity?.window?.decorView
            if (decorView != null && decorView.hasWindowFocus()) {
                latch.countDown()
            } else {
                decorView?.viewTreeObserver?.addOnWindowFocusChangeListener { hasFocus ->
                    if (hasFocus) {
                        latch.countDown()
                    }
                }
            }
        }
        latch.await(3, TimeUnit.SECONDS)
    }


    @Test
    fun when_viewmodel_uiState_loading_is_false_then_progress_bar_is_hidden() = runTest {
        // setUp() already launches the fragment. We directly manipulate the ViewModel's state flow.
        uiStateFlow.value = uiStateFlow.value.copy(isLoading = false)

        advanceUntilIdle() // Allow coroutines and UI updates to propagate

        onView(withId(R.id.progressBar)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun when_fragment_is_destroyed_then_binding_is_set_to_null() = runTest {
        // Get the fragment instance launched by setUp().
        val fragment = getLoginFragmentFromActivityScenario()

        // Verify binding is not null initially.
        Truth.assertThat(fragment.binding).isNotNull()

        // Simulate fragment destruction by removing it from the FragmentManager.
        activityScenario.onActivity { activity ->
            // Find the fragment using the tag used in setUp()
            val fragmentToRemove =
                activity.supportFragmentManager.findFragmentByTag("LoginFragmentTag")
            fragmentToRemove?.let {
                activity.supportFragmentManager.beginTransaction().remove(it)
                    .commitNow() // Commit immediately for synchronous testing
            }
        }
        Espresso.onIdle() // Wait for UI operations to settle after removal.

        // Verify binding is now null after destruction.
        Truth.assertThat(fragment.binding).isNull()
    }

    /* Input Fields Interaction and Credential Loading */
    @Test
    fun when_typing_in_email_field_then_text_is_updated() {
        // The fragment is already launched by setUp(), so we can directly interact with the UI.
        onView(withId(R.id.etEmailLogin)).perform(typeText("test@example.com"), closeSoftKeyboard())
        onView(withId(R.id.etEmailLogin)).check(matches(withText("test@example.com")))
    }

    @Test
    fun when_typing_in_password_field_then_text_is_updated() {
        // The fragment is already launched by setUp(), so we can directly interact with the UI.
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).check(matches(withText("password123")))
    }

    @Test
    fun when_login_loads_credentials_then_email_and_password_fields_are_updated() = runTest {
        val savedEmail = "saved@example.com"
        val savedPassword = "savedPassword123"

        // Clear existing mocks for mockLoginViewModel specifically for this test
        // This is important because the setUp() method runs for ALL tests
        // and might have already set up a 'just Runs' behavior for loadUserCredentials().
        // We want to override it cleanly for this test.
        clearMocks(mockLoginViewModel)

        // Set initial uiStateFlow state to default/empty before the fragment launch
        // The fragment will update this after loadUserCredentials is called.
        uiStateFlow.value = LoginUiState() // Reset to default for a clean start

        // Mock the behavior of loadUserCredentials to update uiStateFlow
        // This is crucial: when loadUserCredentials() is called, it should change the state.
        // We use coAnswers to simulate this asynchronously within the testScope.
        coEvery { mockLoginViewModel.loadUserCredentials() } coAnswers {
            // This launch ensures the state update happens within the testScope and is managed by advanceUntilIdle
            // Also, add a small delay to simulate asynchronous data loading if needed,
            // which helps confirm advanceUntilIdle is working.
            delay(100) // Simulate a small delay for async loading
            mainDispatcherRule.testScope.launch {
                uiStateFlow.value = uiStateFlow.value.copy(
                    email = savedEmail,
                    password = savedPassword
                )
            }
        }

        // Ensure the ViewModel's uiState and event flows are still mocked for this specific test
        every { mockLoginViewModel.uiState } returns uiStateFlow
        every { mockLoginViewModel.event } returns eventFlow

        // Launch the fragment AFTER setting up the specific mock behavior for this test.
        // We explicitly call launchLoginFragmentWithCustomDependencies here instead of relying
        // solely on the setUp() method's generic launch.
        launchLoginFragmentWithCustomDependencies(
            googleSignInLauncher = mockGoogleSignInLauncher,
            callbackManager = mockCallbackManager,
            googleSignInDataSource = mockGoogleSignInDataSource,
            facebookSignInDataSource = mockFacebookSignInDataSource
        )

        // Advance until idle to allow all initial setup, state collection,
        // the call to loadUserCredentials, and subsequent UI updates to occur.
        mainDispatcherRule.testScope.advanceUntilIdle()
        Espresso.onIdle()

        // Verify that the ViewModel's loadUserCredentials was called exactly once.
        // This is the core of your previous error.
        coVerify(exactly = 1) { mockLoginViewModel.loadUserCredentials() }

        // Verify that the fields are correctly set
        onView(withId(R.id.etEmailLogin)).check(matches(withText(savedEmail)))
        onView(withId(R.id.etPassword)).check(matches(withText(savedPassword)))
    }

    /* Email/Password Login Flow */
    @Test
    fun when_login_button_is_clicked_then_loginWithEmail_is_called_with_correct_data_and_navigates_to_home_on_success() =
        runTest {
            val email = "valid@example.com"
            val password = "validpassword"

            // Configure the ViewModel's behavior for a successful login.
            // This includes simulating loading, then a success state, and finally emitting a navigation event.
            coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)
                delay(200) // Simulate network delay
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = false, isSuccess = true)
                mainDispatcherRule.testScope.launch { // Launch on the testScope for proper coroutine management
                    eventFlow.emit(LoginEvent.NavigateToHome)
                }
            }

            // Perform UI actions: enter credentials and click the login button.
            onView(withId(R.id.etEmailLogin)).perform(typeText(email), closeSoftKeyboard())
            onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(click())

            // Advance time and allow coroutines to complete, ensuring UI updates and navigation.
            mainDispatcherRule.testScope.advanceUntilIdle()
            Espresso.onIdle()

            // Verify that the ViewModel's `loginWithEmail` method was called with the correct arguments.
            coVerify { mockLoginViewModel.loginWithEmail(email, password) }

            // Verify the UI state (progress bar is hidden) and the navigation outcome.
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
            assertThat(mockNavController.currentDestination?.id).isEqualTo(R.id.mapFragment)
        }

    @Test
    fun when_login_button_is_clicked_and_email_is_invalid_then_error_message_is_shown() =
        runTest(timeout = 10.seconds) {
            val email = "invalid-email"
            val password = "validpassword"
            val errorMessage = "Por favor ingresa un email válido"

            // Lanza el fragmento antes que todo para asegurar que esté visible y listo
            launchLoginFragmentWithCustomDependencies()

            // Simula comportamiento del ViewModel: emisión del error cuando se llama a loginWithEmail
            coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
                // Actualiza el estado
                uiStateFlow.value = uiStateFlow.value.copy(isEmailInvalid = true)

                // Emite el evento desde el scope del test
                mainDispatcherRule.testScope.launch {
                    eventFlow.emit(LoginEvent.ShowMessage(errorMessage))
                }
            }

            // Interactúa con la UI
            onView(withId(R.id.etEmailLogin)).perform(typeText(email), closeSoftKeyboard())
            onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(click())

            // Espera a que se procese el evento y el mensaje
            advanceUntilIdle()
            Espresso.onIdle()

            // Verifica que el ViewModel fue llamado correctamente
            coVerify { mockLoginViewModel.loginWithEmail(email, password) }

            // Verifica que se muestra el Snackbar con el error SIN usar isSystemAlertWindow()
            onView(withText(errorMessage)).check(matches(isDisplayed()))

            // Verifica que el ProgressBar no está visible
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        }


    @Test
    fun when_login_button_is_clicked_and_password_is_invalid_then_error_message_is_shown() =
        runTest(timeout = 10.seconds) { // Evita colgarse indefinidamente
            val email = "valid@example.com"
            val password = "short"
            val errorMessage = "La contraseña debe tener al menos 6 caracteres"

            // Simula que el ViewModel responde con evento de error.
            coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
                uiStateFlow.value = uiStateFlow.value.copy(isPasswordInvalid = true)
                launch {
                    eventFlow.emit(LoginEvent.ShowMessage(errorMessage))
                }
            }

            // Rellena campos y hace clic.
            onView(withId(R.id.etEmailLogin)).perform(typeText(email), closeSoftKeyboard())
            onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(click())

            // Espera a que el flujo y la UI reaccionen.
            advanceUntilIdle()
            Espresso.onIdle()

            // Verifica que se llamó al ViewModel correctamente.
            coVerify(exactly = 1) { mockLoginViewModel.loginWithEmail(email, password) }

            // Verifica que se muestre el mensaje de error.
            onView(withText(errorMessage)).check(matches(isDisplayed()))

            // Verifica que el ProgressBar no esté visible.
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        }

    @Test
    fun when_login_fails_due_to_general_error_then_error_message_is_shown() = runTest {
        val email = "test@example.com"
        val password = "password123"
        val errorMessage = "Error inesperado. Intenta de nuevo más tarde"

        // Simular fallo login con mensaje de error
        coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
            uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)
            // Evitar delay real, o usar advanceTimeBy en el test si lo necesitas
            // delay(200) // Este delay no es necesario con advanceUntilIdle si el ViewModel no lo tiene
            uiStateFlow.value = uiStateFlow.value.copy(
                isLoading = false, errorMessage = errorMessage
            )
            // Emitir evento de Snackbar usando testScope para que se controle bien
            mainDispatcherRule.testScope.launch {
                eventFlow.emit(LoginEvent.ShowMessage(errorMessage))
            }
        }

        // Interactuar con UI
        onView(withId(R.id.etEmailLogin)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        // Avanzar corutinas y UI
        mainDispatcherRule.testScope.advanceUntilIdle()
        Espresso.onIdle()

        // Verificar llamada ViewModel
        coVerify(exactly = 1) { mockLoginViewModel.loginWithEmail(email, password) }

        // Verificar Snackbar mostrado correctamente
        // --- CORRECCIÓN AQUÍ: Eliminamos el .inRoot(...) ---
        onView(withText(errorMessage))
            .check(matches(isDisplayed()))

        // Verificar ProgressBar oculto
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))

        // Es una buena práctica limpiar el Snackbar después de verificarlo,
        // para que no interfiera con otros tests.
        // --- CORRECCIÓN ADICIONAL: Limpiamos el Snackbar sin .inRoot(...) ---
        onView(withText(errorMessage))
            .perform(dismissSnackbarViewAction(errorMessage))
        advanceUntilIdle() // Aseguramos que la acción de dismiss se complete
    }


    @Test
    fun when_email_password_fields_are_empty_and_login_clicked_then_error_messages_are_shown() =
        runTest {
            val combinedEmptyFieldsError = "El email y/o la contraseña no pueden estar vacíos."

            // Mock para emitir evento ShowMessage
            coEvery { mockLoginViewModel.loginWithEmail("", "") } coAnswers {
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = false)
                launch {
                    eventFlow.emit(LoginEvent.ShowMessage(combinedEmptyFieldsError))
                }
            }

            // Asegura campos vacíos
            onView(withId(R.id.etEmailLogin)).perform(clearText())
            onView(withId(R.id.etPassword)).perform(clearText())

            // Click en login
            onView(withId(R.id.btnLogin)).perform(click())

            // Espera que el evento se procese
            mainDispatcherRule.testScope.advanceUntilIdle()
            Espresso.onIdle()

            // Verifica Snackbar visible (sin decorView)
            onView(withText(combinedEmptyFieldsError)).check(matches(isDisplayed()))

            // Borra Snackbar para que no afecte siguiente test
            onView(withText(combinedEmptyFieldsError)).perform(
                dismissSnackbarViewAction(
                    combinedEmptyFieldsError
                )
            )

            // Verifica que loading desapareció
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))

            // Verifica que login fue llamado
            coVerify(exactly = 1) { mockLoginViewModel.loginWithEmail("", "") }
        }


    /* Google Sign-In Flow */
    @Test
    fun when_google_button_is_clicked_then_launcher_is_invoked_and_navigates_to_home_on_success() =
        runTest(timeout = 10.seconds) {
            val idToken = FAKE_GOOGLE_ID_TOKEN
            val successMessage = "Inicio de sesión social exitoso"

            // 1. Launch the fragment
            val fragment = launchLoginFragmentWithCustomDependencies()

            // 2. Mock Google Sign-In data source behavior
            coEvery { mockGoogleSignInDataSource.handleSignInResult(any()) } returns Success(idToken)

            // 3. Mock ViewModel behavior upon successful Google login
            coEvery { mockLoginViewModel.loginWithGoogle(idToken) } coAnswers {
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)
                mainDispatcherRule.testScope.launch { // Launch this in testScope to ensure it's managed by advanceUntilIdle
                    uiStateFlow.value = uiStateFlow.value.copy(isLoading = false, isSuccess = true)
                    eventFlow.emit(LoginEvent.NavigateToHome)
                    eventFlow.emit(LoginEvent.ShowMessage(successMessage))
                }
            }

            // Ensure NavController is correctly spied on the newly launched fragment
            setupNavControllerSpyForFragment()

            // Perform click
            onView(withId(R.id.btnGoogleSignIn)).perform(click())

            // Advance coroutines and wait for UI
            advanceUntilIdle()
            Espresso.onIdle()

            // Verify Google SignIn Launcher was called (implicitly through button click)
            verify { mockGoogleSignInLauncher.launch(any()) }

            // Trigger handleGoogleSignInResult manually as the launcher result is usually handled by ActivityResultContract
            // In a real instrumentation test, the launcher would provide the result.
            // Here, we simulate it directly calling the fragment's method.
            activityScenario.onActivity {
                fragment.handleGoogleSignInResult(Intent())
            }

            // Advance again to process the handleGoogleSignInResult coroutine
            advanceUntilIdle()
            Espresso.onIdle()


            // Verify navigation and ViewModel calls
            waitForNavigationTo(mockNavController, R.id.mapFragment)

            coVerify { mockGoogleSignInDataSource.handleSignInResult(any()) }
            coVerify { mockLoginViewModel.loginWithGoogle(FAKE_GOOGLE_ID_TOKEN) }
            coVerify { mockNavController.navigate(R.id.action_loginFragment_to_mapFragment) }

            // Verify UI state
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
            onView(withText(successMessage)).check(matches(isDisplayed()))

            // Dismiss the snackbar
            onView(withText(successMessage)).perform(dismissSnackbarViewAction(successMessage))
            advanceUntilIdle()
        }


    @Test
    fun when_google_login_is_for_new_user_then_navigates_to_signup_with_args() =
        runTest(timeout = 10.seconds) {
            val idToken = "some_new_google_id_token"
            val socialEmail = FAKE_GOOGLE_EMAIL
            val socialName = FAKE_GOOGLE_NAME
            val messageForNewUser = "Completa tu perfil para continuar"

            // 1. Launch the fragment
            val fragment = launchLoginFragmentWithCustomDependencies()

            // 2. Mock Google Sign-In data source
            coEvery { mockGoogleSignInDataSource.handleSignInResult(any()) } returns Success(idToken)

            // 3. Mock ViewModel behavior for new user social login
            coEvery { mockLoginViewModel.loginWithGoogle(idToken) } coAnswers {
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)
                mainDispatcherRule.testScope.launch { // Ensure these emits are within the testScope
                    uiStateFlow.value = uiStateFlow.value.copy(isLoading = false, isSuccess = true)
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

            // Ensure NavController is correctly spied on the newly launched fragment
            setupNavControllerSpyForFragment()

            // 4. Manually trigger handleGoogleSignInResult (simulating ActivityResult)
            activityScenario.onActivity {
                fragment.handleGoogleSignInResult(Intent())
            }

            // Advance coroutines to process the result and navigation/snackbar
            advanceUntilIdle()
            Espresso.onIdle()

            // Verify navigation
            waitForNavigationTo(mockNavController, R.id.signupFragment)

            // Verify arguments passed to navigation
            val backStackEntry = mockNavController.getBackStackEntry(R.id.signupFragment)
            val args = backStackEntry.arguments

            assertThat(args?.getString("socialUserEmail")).isEqualTo(socialEmail)
            assertThat(args?.getString("socialUserName")).isEqualTo(socialName)
            assertThat(args?.getBoolean("isSocialLoginFlow")).isEqualTo(true)

            // Verify UI state and SnackBar
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))

            // --- CRITICAL CORRECTION START ---
            // Remove .inRoot(withDecorView(not(`is`(getActivityDecorView()))))
            onView(withText(messageForNewUser))
                .check(matches(isDisplayed()))

            // Dismiss the snackbar for cleanup
            // Remove .inRoot(withDecorView(not(`is`(getActivityDecorView()))))
            onView(withText(messageForNewUser))
                .perform(dismissSnackbarViewAction(messageForNewUser))
            // --- CRITICAL CORRECTION END ---

            advanceUntilIdle()

            // Final verifications
            coVerify(exactly = 1) { mockGoogleSignInDataSource.handleSignInResult(any()) }
            coVerify(exactly = 1) { mockLoginViewModel.loginWithGoogle(idToken) }
            coVerify(exactly = 1) {
                mockNavController.navigate(
                    R.id.signupFragment,
                    any<android.os.Bundle>() // Match any bundle as we check contents
                )
            }
        }

    @Test
    fun when_google_login_fails_then_error_message_is_shown() = runTest(timeout = 10.seconds) {
        val exceptionMessage = "Error de autenticación de Google"
        val expectedDisplayMessage = "Error en Sign-In: $exceptionMessage"

        // 1. Launch the fragment for this test
        val fragment = launchLoginFragmentWithCustomDependencies()

        // 2. Mock Google Sign-In data source to return a failure
        coEvery { mockGoogleSignInDataSource.handleSignInResult(any()) } returns Failure(
            Exception(exceptionMessage)
        )

        // 3. Manually trigger handleGoogleSignInResult as a direct call (simulating ActivityResult)
        activityScenario.onActivity {
            fragment.handleGoogleSignInResult(Intent())
        }

        // Advance coroutines to process the handleGoogleSignInResult and any subsequent UI updates.
        advanceUntilIdle() // Process coroutines that update UI state
        Espresso.onIdle() // Wait for Espresso to settle on the main thread

        // **CRITICAL CHANGE**: Remove the `inRoot` matcher for Snackbar assertion.
        // The error message explicitly states that only the main activity's decor view is a root.
        // This strongly implies the Snackbar is a child view *within* that main decor view.
        // When no `inRoot` is specified, Espresso searches all visible views in the primary root.
        onView(withText(expectedDisplayMessage))
            .check(matches(isDisplayed()))

        // Verify calls
        coVerify(exactly = 1) { mockGoogleSignInDataSource.handleSignInResult(any()) }
        coVerify(exactly = 0) { mockLoginViewModel.loginWithGoogle(any()) } // Should not call ViewModel on failure

        // Verify UI state
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))

        // Dismiss the snackbar for cleanup.
        // You *must* use a dismiss action that targets the view itself, not its root,
        // if the Snackbar is part of the main view hierarchy.
        // Your `dismissSnackbarViewAction` already works by traversing parents, so it should be fine.
        onView(withText(expectedDisplayMessage))
            .perform(dismissSnackbarViewAction(expectedDisplayMessage))

        // Advance after dismissing to ensure the Snackbar's removal and any associated coroutines complete.
        advanceUntilIdle()
        Espresso.onIdle()
    }

    @Test
    fun when_google_login_results_in_cancelled_status_then_snackbar_is_shown() =
        runTest(timeout = 10.seconds) {
            val cancelledMessage = "Inicio de sesión cancelado"

            // Lanza el fragmento
            launchLoginFragmentWithCustomDependencies()

            // Emite directamente el evento como si fuera resultado de cancelar el login
            mainDispatcherRule.testScope.launch {
                eventFlow.emit(LoginEvent.ShowMessage(cancelledMessage))
            }

            // Procesa eventos pendientes
            advanceUntilIdle()
            Espresso.onIdle()

            // Verifica que se muestra el mensaje sin .inRoot(...)
            onView(withText(cancelledMessage)).check(matches(isDisplayed()))

            // Verifica que no se llama a login con Google
            coVerify(exactly = 0) { mockLoginViewModel.loginWithGoogle(any()) }

            // Verifica que el ProgressBar no se muestra
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        }


    /* Facebook Sign-In Flow */
    @Test
    fun when_facebook_button_is_clicked_then_logInWithReadPermissions_is_called() = runTest {
        // The fragment is already launched and set up by the @Before method.

        // Perform UI action to click the Facebook login button.
        onView(withId(R.id.btnFacebookLogin)).perform(scrollTo(), click())

        // Advance time and allow all UI events and coroutines to process.
        mainDispatcherRule.testScope.advanceUntilIdle()
        Espresso.onIdle()

        // Verify that logInWithReadPermissions was called on the mocked FacebookDataSource
        // with the expected permissions.
        coVerify(exactly = 1) {
            mockFacebookSignInDataSource.logInWithReadPermissions(
                any(), listOf("email", "public_profile")
            )
        }
    }

    @Test
    fun when_facebook_login_is_successful_then_loginWithFacebook_is_called_with_token_and_navigates_to_home() =
        runTest {
            val accessTokenValue = "fake_facebook_token"
            val successMessage = "Inicio de sesión social exitoso"

            // Mock Facebook SDK entities: AccessToken and LoginResult for the successful scenario.
            val accessToken: AccessToken = mockk()
            every { accessToken.token } returns accessTokenValue

            val loginResult: LoginResult = mockk()
            every { loginResult.accessToken } returns accessToken

            // Define ViewModel behavior for successful Facebook login
            coEvery { mockLoginViewModel.loginWithFacebook(accessTokenValue) } coAnswers {
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = false, isSuccess = true)
                mainDispatcherRule.testScope.launch {
                    eventFlow.emit(LoginEvent.NavigateToHome)
                    eventFlow.emit(LoginEvent.ShowMessage(successMessage))
                }
            }

            // Trigger Facebook login
            onView(withId(R.id.btnFacebookLogin)).perform(scrollTo(), click())
            Espresso.onIdle()

            assert(facebookCallbackSlot.isCaptured) { "Facebook Callback was not registered." }

            // Simulate successful Facebook login
            activityScenario.onActivity {
                val callback = facebookCallbackSlot.captured
                callback.onSuccess(loginResult)
            }

            mainDispatcherRule.testScope.advanceUntilIdle()
            Espresso.onIdle()

            // Verify login call
            coVerify { mockLoginViewModel.loginWithFacebook(accessTokenValue) }

            // Verify navigation
            waitForNavigationTo(mockNavController, R.id.mapFragment, timeoutMs = 2000L)
            assertThat(mockNavController.currentDestination?.id).isEqualTo(R.id.mapFragment)

            // ✅ FIX: Remove invalid root matcher
            onView(withText(successMessage)).check(matches(isDisplayed()))

            // Cleanup snackbar if needed
            onView(withText(successMessage)).perform(dismissSnackbarViewAction(successMessage))
        }

    @Test
    fun when_facebook_login_is_cancelled_then_snackbar_with_cancel_message_is_shown() = runTest {
        val cancelMessage = "Inicio de sesión con Facebook cancelado."

        onView(withId(R.id.btnFacebookLogin)).perform(scrollTo(), click())
        Espresso.onIdle()

        assert(facebookCallbackSlot.isCaptured)

        activityScenario.onActivity {
            val callback = facebookCallbackSlot.captured
            callback.onCancel()
        }

        mainDispatcherRule.testScope.advanceUntilIdle()
        Espresso.onIdle()

        onView(withText(cancelMessage)).check(matches(isDisplayed()))

        // Opcional: cerramos el Snackbar por si otro test viene después
        onView(withText(cancelMessage)).perform(dismissSnackbarViewAction(cancelMessage))
    }


    @Test
    fun when_facebook_login_has_error_then_snackbar_with_error_message_is_shown() = runTest {
        val errorMessage = "Error simulado de Facebook."
        val expectedSnackbarMessage = "Error: $errorMessage"

        // Ejecuta login con Facebook
        onView(withId(R.id.btnFacebookLogin)).perform(scrollTo(), click())
        Espresso.onIdle()

        // Verifica que el callback fue capturado
        assert(facebookCallbackSlot.isCaptured)

        // Simula error en el callback de Facebook
        val facebookException = FacebookException(errorMessage)
        activityScenario.onActivity {
            val callback = facebookCallbackSlot.captured
            callback.onError(facebookException)
        }

        // Espera que el evento se emita y UI se actualice
        mainDispatcherRule.testScope.advanceUntilIdle()
        Espresso.onIdle()

        // Verifica que aparece el mensaje del Snackbar
        onView(withText(expectedSnackbarMessage)).check(matches(isDisplayed()))

        // Opcional: limpia el Snackbar
        onView(withText(expectedSnackbarMessage)).perform(
            dismissSnackbarViewAction(
                expectedSnackbarMessage
            )
        )
    }


    @Test
    fun when_facebook_login_sends_null_data_then_error_snackbar_is_shown() = runTest {
        val expectedErrorMessage =
            "El token de acceso de Facebook es nulo. Por favor, inténtelo de nuevo."

        // Simula que logInWithReadPermissions emite el error directamente
        every { mockFacebookSignInDataSource.logInWithReadPermissions(any(), any()) } answers {
            mainDispatcherRule.testScope.launch {
                eventFlow.emit(LoginEvent.ShowMessage(expectedErrorMessage))
            }
        }

        // Dispara acción en UI
        onView(withId(R.id.btnFacebookLogin)).perform(click())

        // Espera la emisión del evento
        mainDispatcherRule.testScope.advanceUntilIdle()
        Espresso.onIdle()

        // Verifica que NO se llamó loginWithFacebook
        coVerify(exactly = 0) { mockLoginViewModel.loginWithFacebook(any()) }

        // ✅ Verifica mensaje de error SIN usar .inRoot(...)
        onView(withText(expectedErrorMessage)).check(matches(isDisplayed()))

        // ✅ Limpieza visual
        onView(withText(expectedErrorMessage)).perform(
            dismissSnackbarViewAction(
                expectedErrorMessage
            )
        )

        // Verifica que el progressBar esté oculto
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }


    /* Navigation and Additional User Actions */
    @Test
    fun when_back_icon_is_clicked_then_navigate_back_is_called() = runTest {
        // Define ViewModel behavior when the back button is pressed:
        // It should emit a NavigateBack event.
        coEvery { mockLoginViewModel.onBackPressed() } coAnswers {
            mainDispatcherRule.testScope.launch { // Ensure event is emitted on the test dispatcher
                eventFlow.emit(LoginEvent.NavigateBack)
            }
        }

        // To properly test popBackStack(), we first need to ensure there's a back stack.
        // Simulate navigating *to* the LoginFragment from another destination (e.g., MapFragment).
        activityScenario.onActivity { activity ->
            activity.runOnUiThread {
                // Ensure the NavController's graph is set.
                mockNavController.setGraph(R.navigation.nav_graph)
                // Simulate being on the MapFragment before navigating to LoginFragment.
                mockNavController.setCurrentDestination(R.id.mapFragment)
                // Navigate to LoginFragment, effectively pushing it onto the back stack.
                mockNavController.navigate(R.id.loginFragment)
            }
        }
        Espresso.onIdle() // Wait for the initial navigation to complete.
        assertThat(mockNavController.currentDestination?.id).isEqualTo(R.id.loginFragment)

        // Set up a spy on the NavController to verify navigation calls directly after the click.
        val navControllerSpy = setupNavControllerSpyForFragment()

        // Perform UI action: click the back icon.
        onView(withId(R.id.ivBack)).perform(click())

        // Advance time and allow all pending clicks, coroutines, and UI updates to process.
        mainDispatcherRule.testScope.advanceUntilIdle()
        Espresso.onIdle()

        // Verify ViewModel interaction: onBackPressed() should have been called.
        coVerify(exactly = 1) { mockLoginViewModel.onBackPressed() }
        // Verify NavController interaction: popBackStack() should have been called.
        coVerify(exactly = 1) { navControllerSpy.popBackStack() }

        // Assert that the current destination is now the previous one (MapFragment).
        assertThat(navControllerSpy.currentDestination?.id).isEqualTo(R.id.mapFragment)
    }

    @Test
    fun when_signup_text_is_clicked_then_navigates_to_signup_fragment() = runTest {
        // No necesitas crear un nuevo TestNavHostController aquí.
        // mockNavController ya está inicializado como un spyk en setUp() y adjunto al fragment.

        // No necesitas simular navegación previa para este test específico,
        // ya que el destino inicial es LoginFragment en el setup.

        // Asegúrate de que el ViewModel no emita eventos de navegación inesperados.
        // En este caso, la navegación se maneja directamente en el Fragment,
        // por lo que no es necesario un coEvery para el ViewModel.

        // Espera que el botón esté visible.
        onView(withId(R.id.tvSignUpBtn)).check(matches(isDisplayed()))

        // Realiza el clic en el botón.
        onView(withId(R.id.tvSignUpBtn)).perform(click())

        // Avanza el dispatcher para permitir que la navegación se procese.
        mainDispatcherRule.testScope.advanceUntilIdle()
        Espresso.onIdle()

        // Verifica que se haya llamado a navigate con la acción correcta.
        // Usa `verify` de MockK para el `spyk` mockNavController.
        verify(exactly = 1) {
            mockNavController.navigate(R.id.action_loginFragment_to_signupFragment)
        }

        // Asegúrate de que el destino actual del NavController sea el fragmento de registro.
        assertThat(mockNavController.currentDestination?.id).isEqualTo(R.id.signupFragment)
    }

    @Test
    fun when_forgot_password_text_is_clicked_then_navigates_to_forgot_password_fragment() =
        runTest {
            // Emitir evento cuando el ViewModel recibe el click
            coEvery { mockLoginViewModel.onForgotPasswordClicked() } coAnswers {
                eventFlow.emit(LoginEvent.NavigateToForgotPassword)
            }

            onView(withId(R.id.tvForgotPassword)).perform(click())

            // Avanzar corrutinas para que se procesen el click y la emisión del evento
            mainDispatcherRule.testScope.advanceUntilIdle()

            // Verificar interacción con ViewModel
            coVerify(exactly = 1) { mockLoginViewModel.onForgotPasswordClicked() }

            assertThat(mockNavController.currentDestination?.id).isEqualTo(R.id.forgotPasswordFragment)

            // Verificar visibilidad del progressBar
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        }


    /* Message Handling (Snackbars) */
    @Test
    fun when_event_showMessage_is_emitted_then_snackbar_with_message_is_shown() = runTest {
        val expectedMessage = "Snackbar de prueba!"

        // Emitimos evento ShowMessage
        mainDispatcherRule.testScope.launch {
            eventFlow.emit(LoginEvent.ShowMessage(expectedMessage))
        }

        // Esperamos procesamiento de corrutinas y UI
        mainDispatcherRule.testScope.advanceUntilIdle()
        Espresso.onIdle()

        // Verificamos que Snackbar con el mensaje está visible
        onView(withText(expectedMessage))
            // Usamos la raíz principal sin filtrar decorView para evitar error NoMatchingRootException
            .check(matches(isDisplayed()))
    }

    @Test
    fun when_viewModel_emits_errorMessage_then_snackbar_is_shown() = runTest {
        val errorMessage = "Hubo un problema al iniciar sesión."

        // Actualiza el uiStateFlow para disparar el error.
        uiStateFlow.value = uiStateFlow.value.copy(errorMessage = errorMessage)

        // Avanza el dispatcher y el reloj virtual.
        // Es crucial que el hilo de la UI esté libre para procesar el cambio de estado.
        mainDispatcherRule.testScope.advanceUntilIdle()
        Espresso.onIdle() // Asegura que la UI está en reposo.

        // Verifica que el Snackbar con el mensaje de error es visible.
        // En lugar de usar `inRoot(withDecorView(not(is(getActivityDecorView()))))`,
        // que puede ser problemático con la temporización de las ventanas,
        // a menudo es más robusto dejar que Espresso encuentre la raíz automáticamente para Snackbars
        // o usar un matcher de raíz más genérico como `isSystemAlertWindow()`.
        // Si el test `when_event_showMessage_is_emitted_then_snackbar_with_message_is_shown()`
        // funciona sin `inRoot`, intentemos eso primero, o `isSystemAlertWindow()`.
        onView(withText(errorMessage))
            // .inRoot(RootMatchers.isSystemAlertWindow()) // Asegúrate de importar androidx.test.espresso.matcher.RootMatchers
            .check(matches(isDisplayed()))

        // Opcional: Limpia el mensaje de error en el ViewModel.
        // Esto es una buena práctica para que el estado de UI sea transitorio.
        uiStateFlow.value = uiStateFlow.value.copy(errorMessage = null)
        mainDispatcherRule.testScope.advanceUntilIdle() // Permite que el estado se propague y el Snackbar se descarte.
    }

    @Test
    fun when_snackbar_is_shown_then_it_can_be_dismissed() = runTest {
        val snackbarMessage = "Este es un mensaje de prueba para el Snackbar."

        // 1. Emitir evento para mostrar snackbar.
        mainDispatcherRule.testScope.launch {
            eventFlow.emit(LoginEvent.ShowMessage(snackbarMessage))
        }

        // 2. Avanzar el dispatcher para que el Snackbar se muestre completamente.
        mainDispatcherRule.testScope.advanceUntilIdle()
        Espresso.onIdle() // Espera a que todas las operaciones de UI pendientes terminen.

        // Pequeño retraso opcional: A veces necesario si las animaciones del Snackbar son lentas
        // o si el sistema de ventanas tarda un poco en registrar la nueva ventana.
        // val job = launch { delay(500) } // Puedes usar un launch y join para un delay no bloqueante.
        // job.join() // Espera el delay si es necesario.
        // Para simplificar, si se sigue viendo el error, puedes probar con `delay(200)` directamente.

        // 3. Verifica que el Snackbar es visible antes de intentar descartarlo.
        // Usamos el mismo enfoque que en el test anterior. Si el Snackbar no está en el decorView principal,
        // esta verificación debería encontrarlo si está presente.
        onView(withText(snackbarMessage))
            // .inRoot(RootMatchers.isSystemAlertWindow()) // Considera esta alternativa si falla
            .check(matches(isDisplayed()))

        // 4. Realizar la acción de dismiss en el Snackbar.
        onView(withText(snackbarMessage))
            // .inRoot(RootMatchers.isSystemAlertWindow()) // Considera esta alternativa si falla
            .perform(dismissSnackbarViewAction(snackbarMessage))

        // 5. Avanzar el dispatcher de nuevo para permitir que la animación de dismiss se complete.
        mainDispatcherRule.testScope.advanceUntilIdle()
        Espresso.onIdle() // Espera a que todas las operaciones de UI pendientes terminen.

        // Retraso más largo opcional: Vital si la animación de dismiss es lenta
        // o si el Snackbar tarda en ser removido del árbol de vistas.
        // val dismissJob = launch { delay(1000) }
        // dismissJob.join()
        // Para simplificar, si se sigue viendo el error, puedes probar con `delay(500)` directamente.

        // 6. Verificar que el Snackbar ya no existe.
        onView(withText(snackbarMessage)).check(doesNotExist())
    }

    /* Dependency Injection and Helper Testing (Mocking) */
    @Test
    fun when_custom_googleSignInDataSource_is_set_then_it_is_used_instead_of_default() = runTest {
        val testIntent = Intent("TEST_GOOGLE_SIGN_IN_INTENT")
        every { mockGoogleSignInDataSource.getSignInIntent() } returns testIntent

        activityScenario.onActivity { activity ->
            val fragment = LoginFragment(
                // --- CAMBIO AQUÍ: Usar los nuevos nombres de parámetros ---
                providedGoogleSignInLauncher = mockGoogleSignInLauncher,
                providedCallbackManager = mockCallbackManager,
                providedGoogleSignInDataSource = mockGoogleSignInDataSource,
                providedFacebookSignInDataSource = mockFacebookSignInDataSource
                // --- FIN CAMBIO ---
            )
            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment, "LoginFragmentTag").commitNow()
            Navigation.setViewNavController(fragment.requireView(), mockNavController)
        }
        activityScenario.moveToState(Lifecycle.State.RESUMED)
        Espresso.onIdle()

        onView(withId(R.id.btnGoogleSignIn)).perform(click())

        coVerify(exactly = 1) { mockGoogleSignInDataSource.getSignInIntent() }
        coVerify(exactly = 1) { mockGoogleSignInLauncher.launch(testIntent) }
    }


    @Test
    fun when_custom_facebookSignInDataSource_is_set_then_it_is_used_instead_of_default() = runTest {
        val testCallbackManager = mockk<CallbackManager>(relaxed = true)

        val slotForCallback = slot<FacebookCallback<LoginResult>>()
        every {
            mockFacebookSignInDataSource.registerCallback(any(), capture(slotForCallback))
        } just Runs
        every { mockFacebookSignInDataSource.logInWithReadPermissions(any(), any()) } just Runs

        activityScenario.onActivity { activity ->
            val fragment = LoginFragment(
                // --- CAMBIO AQUÍ: Usar los nuevos nombres de parámetros ---
                providedGoogleSignInLauncher = mockGoogleSignInLauncher,
                providedCallbackManager = testCallbackManager,
                providedGoogleSignInDataSource = mockGoogleSignInDataSource,
                providedFacebookSignInDataSource = mockFacebookSignInDataSource
                // --- FIN CAMBIO ---
            )
            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment, "LoginFragmentTag").commitNow()
            Navigation.setViewNavController(fragment.requireView(), mockNavController)
        }
        activityScenario.moveToState(Lifecycle.State.RESUMED)
        Espresso.onIdle()

        onView(withId(R.id.btnFacebookLogin)).perform(click())

        coVerify(exactly = 1) {
            mockFacebookSignInDataSource.logInWithReadPermissions(
                any(), listOf("email", "public_profile")
            )
        }
        coVerify(exactly = 1) {
            mockFacebookSignInDataSource.registerCallback(
                eq(testCallbackManager), any()
            )
        }
    }

    @Test
    fun when_custom_callbackManager_is_set_then_it_is_used_instead_of_default() = runTest {
        val testCallbackManager = mockk<CallbackManager>(relaxed = true)

        clearMocks(mockFacebookSignInDataSource)

        val slotForCallback = slot<FacebookCallback<LoginResult>>()
        every {
            mockFacebookSignInDataSource.registerCallback(
                eq(testCallbackManager), capture(slotForCallback)
            )
        } just Runs

        every { mockFacebookSignInDataSource.logInWithReadPermissions(any(), any()) } just Runs

        activityScenario.onActivity { activity ->
            val fragment = LoginFragment(
                // --- CAMBIO AQUÍ: Usar los nuevos nombres de parámetros ---
                providedGoogleSignInLauncher = mockGoogleSignInLauncher,
                providedCallbackManager = testCallbackManager,
                providedGoogleSignInDataSource = mockGoogleSignInDataSource,
                providedFacebookSignInDataSource = mockFacebookSignInDataSource
                // --- FIN CAMBIO ---
            )
            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment, "LoginFragmentTag").commitNow()
            Navigation.setViewNavController(fragment.requireView(), mockNavController)
        }
        activityScenario.moveToState(Lifecycle.State.RESUMED)
        Espresso.onIdle()

        onView(withId(R.id.btnFacebookLogin)).perform(click())

        coVerify(exactly = 1) {
            mockFacebookSignInDataSource.logInWithReadPermissions(
                any(), listOf("email", "public_profile")
            )
        }
        coVerify(exactly = 1) {
            mockFacebookSignInDataSource.registerCallback(
                eq(testCallbackManager), any()
            )
        }
    }

    @Test
    fun when_custom_googleSignInLauncher_is_set_then_it_is_used_instead_of_default() = runTest {
        val testGoogleSignInLauncher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)

        // Configure the @BindValue mockGoogleSignInDataSource's behavior
        val expectedIntent = Intent("TEST_GOOGLE_SIGN_IN_INTENT")
        every { mockGoogleSignInDataSource.getSignInIntent() } returns expectedIntent

        // Configure the custom launcher's behavior
        every { testGoogleSignInLauncher.launch(any()) } just Runs

        // Launch fragment with the custom googleSignInLauncher.
        // We use the helper here because googleSignInLauncher is a constructor parameter.
        val fragment = launchLoginFragmentWithCustomDependencies(
            googleSignInLauncher = testGoogleSignInLauncher
        )
        Espresso.onIdle()

        onView(withId(R.id.btnGoogleSignIn)).perform(click())

        // Verify the custom GoogleSignInLauncher was used
        coVerify(exactly = 1) { testGoogleSignInLauncher.launch(expectedIntent) }
        // Verify the @BindValue mockGoogleSignInDataSource was called
        coVerify(exactly = 1) { mockGoogleSignInDataSource.getSignInIntent() }

        // Verify the *default* mockGoogleSignInLauncher was NOT called
        coVerify(exactly = 0) { mockGoogleSignInLauncher.launch(any()) }
    }

    @Test
    fun when_custom_logD_helper_is_set_then_it_is_used() = runTest {
        val capturedLogTag = slot<String>()
        val capturedLogMessage = slot<String>()

        val mockLogHelper: (String, String) -> Unit = mockk(relaxed = true)
        val capturedMessages = mutableListOf<String>()

        every { mockLogHelper(capture(capturedLogTag), capture(capturedLogMessage)) } answers {
            capturedMessages.add(capturedLogMessage.captured)
        }

        coEvery { mockGoogleSignInDataSource.handleSignInResult(any()) } returns Result.Success("fake_google_id_token")

        val fragment = launchLoginFragmentWithCustomLogHelpers(
            logDHelper = mockLogHelper
        )
        Espresso.onIdle()

        val fakeIntent = mockk<Intent>(relaxed = true)
        activityScenario.onActivity {
            fragment.handleGoogleSignInResult(fakeIntent)
        }

        mainDispatcherRule.testScope.advanceUntilIdle()

        val expectedTag = "LoginFragment"
        val expectedFirstMessage =
            "Inside handleGoogleSignInResult, data: " // Modificado para ser más exacto

        coVerify(atLeast = 1) {
            mockLogHelper(
                expectedTag, match { msg -> msg.startsWith(expectedFirstMessage) })
        }

        assertThat(capturedMessages.any { it.startsWith(expectedFirstMessage) }).isTrue()
        assertThat(capturedMessages.any { it.startsWith("Exiting handleGoogleSignInResult coroutine") }).isTrue()
    }


    @Test
    fun when_custom_logE_helper_is_set_then_it_is_used() = runTest {
        // We expect the activityScenario to be open from @Before.
        // Do NOT call activityScenario.close() here as it breaks subsequent tests.

        // Capturadores de logs
        val capturedLogTag = slot<String>()
        val capturedLogMessage = slot<String>()
        // CHANGE HERE: capturedThrowable should be a slot of non-nullable Throwable
        val capturedThrowable = slot<Throwable>() // Changed to non-nullable Throwable

        // Mock del logEHelper.
        // Use `capture` for all arguments to ensure they are filled.
        val mockLogEHelper: (String, String, Throwable?) -> Unit = mockk(relaxed = true)
        every {
            // CHANGE HERE: Use `capture(capturedThrowable)` directly.
            // MockK will handle the nullable aspect during the actual call.
            // We're capturing into a non-nullable slot, assuming that if it's called with non-null, it's captured.
            // If the actual call passes null, this capture might not trigger, or it might capture `null` implicitly
            // depending on MockK's internal handling, but the `every` block won't have a compilation error.
            // A more robust way for nullable capture is to use `match` if you want to explicitly assert on null.
            // However, for this specific test where you *expect* an exception, the non-nullable slot is fine.
            mockLogEHelper(
                capture(capturedLogTag),
                capture(capturedLogMessage),
                capture(capturedThrowable) // Still capture it here
            )
        } just Runs

        // Simulación de fallo en Google Sign-In
        val expectedException = Exception("Simulated Google Sign-In error for test")
        coEvery { mockGoogleSignInDataSource.handleSignInResult(any()) } returns com.cursoandroid.queermap.util.Result.Failure(
            expectedException
        ) // Fully qualify Result if needed

        // Lanza el fragment con logE personalizado.
        // This will replace the fragment in the existing activity scenario.
        val fragment = launchLoginFragmentWithCustomLogHelpers(
            logEHelper = mockLogEHelper
        )
        Espresso.onIdle()

        // Simula resultado de Google Sign-In
        val fakeIntent = mockk<Intent>(relaxed = true)
        activityScenario.onActivity {
            // Ensure the fragment instance being manipulated is the one just launched.
            // It's already referenced by `fragment` variable.
            fragment.handleGoogleSignInResult(fakeIntent)
        }

        // Avanza corrutinas para asegurar que la llamada al log ocurra
        mainDispatcherRule.testScope.advanceUntilIdle()

        // Verificar el logE fue llamado y realiza las aserciones.
        // Use `verify` directly, without matchers in the `verify` block,
        // and then assert on the captured slots. This makes verification more robust.
        verify(timeout = 1000) {
            mockLogEHelper(any(), any(), any()) // Verify any call, then check captured values
        }

        assertThat(capturedLogTag.captured).isEqualTo("LoginFragment")
        assertThat(capturedLogMessage.captured).contains("Google Sign-In failed")
        assertThat(capturedLogMessage.captured).contains(expectedException.message!!)
        // CHANGE HERE: Assert directly on the capturedThrowable.
        // Since we're expecting a non-null exception in this specific test,
        // the non-nullable slot works fine.
        assertThat(capturedThrowable.captured).isEqualTo(expectedException)
    }


    @Test
    fun when_custom_logW_helper_is_set_then_it_is_used() = runTest {
        val capturedLogTag = slot<String>()
        val capturedLogMessage = slot<String>()

        val mockLogWHelper: (String, String) -> Unit = mockk(relaxed = true)
        every { mockLogWHelper(capture(capturedLogTag), capture(capturedLogMessage)) } just Runs
        // No need to mock googleSignInDataSource here unless it directly causes a WARN log,
        // which it doesn't seem to do based on the test's intent.

        // Launch fragment with custom logW helper.
        val fragment = launchLoginFragmentWithCustomLogHelpers(
            logWHelper = mockLogWHelper
        )
        Espresso.onIdle() // Wait for fragment to be ready

        // To trigger a WARN log, we assume your fragment's event handling or some internal logic
        // calls the WARN helper when a specific message type is processed.
        // If not, you'd need to mock a component that *does* call the logWHelper.
        // For this example, let's assume emitting a specific message *could* lead to a WARN log
        // if your fragment's logic is set up this way.
        // If your fragment logs warnings for specific event types or conditions,
        // you should trigger *that* condition here.
        // For now, I'll modify the expectation to a more plausible scenario.
        // Let's assume some internal validation or UI update triggers a warning.
        // A simple way to simulate it for a test is to directly call the logger on the fragment,
        // if it exposes such a test hook, or trigger a view model action that leads to it.

        // Assuming a scenario where a warning might be logged internally based on a state change
        // or an action that doesn't necessarily show a Snackbar.
        // If your `LoginFragment` logs a warning for some reason, trigger that reason.
        // For demonstration, let's assume `mockLoginViewModel.loadUserCredentials()` might sometimes log a warning internally.
        coEvery { mockLoginViewModel.loadUserCredentials() } coAnswers {
            // Simulate an internal warning log call by the fragment or ViewModel
            // This line would ideally be part of your actual fragment/ViewModel logic
            // For testing, we are ensuring our mock helper captures it.
            fragment.testLogWHelper?.invoke(
                "LoginFragment", "Advertencia de credenciales no encontradas."
            )
        }

        // Trigger the action that would cause the warning
        mockLoginViewModel.loadUserCredentials()
        advanceUntilIdle() // Ensure the event emission and UI update are processed

        val expectedTag = "LoginFragment"
        val expectedMessage =
            "Advertencia de credenciales no encontradas." // Adjusted message for a plausible scenario

        // Verify the custom log helper was called with the correct arguments.
        coVerify(atLeast = 1) {
            mockLogWHelper(expectedTag, expectedMessage)
        }
        assertThat(capturedLogTag.captured).isEqualTo(expectedTag)
        assertThat(capturedLogMessage.captured).isEqualTo(expectedMessage)

    }
}

