package com.cursoandroid.queermap.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.RootMatchers.isSystemAlertWindow
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
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
import com.facebook.FacebookCallback
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
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
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

suspend fun waitForNavigationTo(
    navController: NavController,
    destinationId: Int,
    timeoutMs: Long = 5000L
) {
    withContext(Dispatchers.Default.limitedParallelism(1)) {
        withTimeout(timeoutMs) {
            suspendCancellableCoroutine<Unit> { continuation ->
                val listener = object : NavController.OnDestinationChangedListener {
                    override fun onDestinationChanged(
                        controller: NavController,
                        destination: NavDestination,
                        arguments: Bundle?
                    ) {
                        if (destination.id == destinationId) {
                            navController.removeOnDestinationChangedListener(this)
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                    }
                }

                if (navController.currentDestination?.id == destinationId) {
                    continuation.resume(Unit)
                } else {
                    navController.addOnDestinationChangedListener(listener)
                    continuation.invokeOnCancellation {
                        navController.removeOnDestinationChangedListener(listener)
                    }
                }
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

    private lateinit var activityScenario: ActivityScenario<HiltTestActivity>
    private lateinit var mockNavController: TestNavHostController

    private lateinit var uiStateFlow: MutableStateFlow<LoginUiState>
    private lateinit var eventFlow: MutableSharedFlow<LoginEvent>

    private val FAKE_GOOGLE_ID_TOKEN = "fake_google_id_token"
    private val FAKE_GOOGLE_EMAIL = "test_google@example.com"
    private val FAKE_GOOGLE_NAME = "Test Google User"

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

        every { mockGoogleSignInDataSource.getSignInIntent() } answers {
            Intent("com.google.android.gms.auth.GOOGLE_SIGN_IN")
        }

        mockGoogleSignInLauncher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)
        every { mockGoogleSignInLauncher.launch(any()) } answers {
            activityScenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentById(android.R.id.content) as? LoginFragment
                if (fragment != null && fragment.isAdded && fragment.view != null) {
                    InstrumentationRegistry.getInstrumentation().runOnMainSync {
                        val account = mockk<GoogleSignInAccount>()
                        every { account.idToken } returns FAKE_GOOGLE_ID_TOKEN
                        every { account.email } returns FAKE_GOOGLE_EMAIL
                        every { account.displayName } returns FAKE_GOOGLE_NAME

                        val simulatedResultIntent =
                            Intent().putExtra("googleSignInAccount", account)

                        // Call the fragment's handler directly
                        fragment.handleGoogleSignInResult(simulatedResultIntent)
                    }
                } else {
                    Log.w("TEST", "Fragment not ready to handle Google Sign-In result.")
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

        // Register IdlingResource
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)

        activityScenario = ActivityScenario.launch(HiltTestActivity::class.java)

        val fragmentReadyLatch = CountDownLatch(1)

        activityScenario.onActivity { activity ->
            mockNavController =
                TestNavHostController(InstrumentationRegistry.getInstrumentation().targetContext)
            mockNavController.setGraph(R.navigation.nav_graph)

            val fragment = LoginFragment()
            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commitNow()

            fragment.viewLifecycleOwnerLiveData.observe(fragment) { viewLifecycleOwner ->
                if (viewLifecycleOwner != null && fragment.view != null && fragment.lifecycle.currentState.isAtLeast(
                        Lifecycle.State.STARTED
                    )
                ) {
                    activity.runOnUiThread {
                        Navigation.setViewNavController(fragment.requireView(), mockNavController)
                        fragment.testGoogleSignInLauncher = mockGoogleSignInLauncher
                        fragment.testGoogleSignInDataSource = mockGoogleSignInDataSource
                        fragment.testFacebookSignInDataSource = mockFacebookSignInDataSource
                        fragment.testCallbackManager = mockk(relaxed = true)
                        fragmentReadyLatch.countDown()
                    }
                }
            }
        }
        fragmentReadyLatch.await(5, TimeUnit.SECONDS)
        Espresso.onIdle() // Ensure UI is idle after setup
    }

    @After
    fun tearDown() {
        // Unregister IdlingResource
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        Intents.release()
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
            // No need for runBlocking here, as it's within runTest
            mockLoginViewModel.loadUserCredentials()
        }
        // Advance virtual time for coroutines
        mainDispatcherRule.testScope.advanceUntilIdle()
        // Wait for Espresso to be idle after UI updates from the coroutines
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
                // Simulate UI loading and then success, emitting navigation event
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)
                delay(200) // Simulate some work
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = false, isSuccess = true)
                mainDispatcherRule.testScope.launch {
                    eventFlow.emit(LoginEvent.NavigateToHome)
                }
            }

            onView(withId(R.id.etEmailLogin)).perform(typeText(email), closeSoftKeyboard())
            onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(click())

            // Advance virtual time and wait for Espresso to be idle
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

            // Advance virtual time for all coroutines triggered by the click and mock launcher callback
            mainDispatcherRule.testScope.advanceUntilIdle()
            // Ensure Espresso is idle, waiting for UI updates and IdlingResources
            Espresso.onIdle()

            // At this point, navigation should have occurred and the message displayed
            waitForNavigationTo(
                navControllerSpy,
                R.id.mapFragment
            ) // This is a final check for navigation

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

        // 🔧 Dale tiempo a la coroutine para emitir el mensaje
        advanceUntilIdle()
        delay(500) // Dale tiempo a Snackbar
        Espresso.onIdle()

        // Verificaciones
        coVerify(exactly = 1) { mockGoogleSignInDataSource.handleSignInResult(any()) }
        coVerify(exactly = 0) { mockLoginViewModel.loginWithGoogle(any()) }

        onView(withText(expectedDisplayMessage))
            .check(matches(isDisplayed()))

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

}
//    /* Pruebas de Interacción de Login Social (Facebook) */
//
//    @Test
//    fun when_facebook_button_is_clicked_logInWithReadPermissions_is_called() = runTest {
//        onView(withId(R.id.btnFacebookLogin))
//            .perform(waitUntilVisibleAndEnabledAndCompletelyDisplayed(), click())
//
//        advanceUntilIdle()
//
//        coVerify(exactly = 1) {
//            mockFacebookSignInDataSource.logInWithReadPermissions(
//                any(),
//                listOf("email", "public_profile")
//            )
//        }
//    }
//
//    @Test
//    fun when_facebook_access_token_is_received_loginWithFacebook_is_called_and_navigates_to_home() =
//        runTest {
//            val accessTokenString = "facebook_access_token_simulated"
//            val successMessage = "Inicio de sesión con Facebook exitoso"
//
//            every { mockFacebookSignInDataSource.logInWithReadPermissions(any(), any()) } just Runs
//
//            coEvery { mockLoginViewModel.loginWithFacebook(accessTokenString) } coAnswers {
//                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = true))
//                this@runTest.advanceUntilIdle()
//                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = false, isSuccess = true))
//                eventFlow.emit(LoginEvent.NavigateToHome)
//                eventFlow.emit(LoginEvent.ShowMessage(successMessage))
//            }
//
//            every { mockNavController.navigate(R.id.action_loginFragment_to_mapFragment) } just Runs
//
//            onView(withId(R.id.btnFacebookLogin)).perform(click())
//
//            coVerify(exactly = 1) {
//                mockFacebookSignInDataSource.logInWithReadPermissions(any(), any())
//            }
//
//            val mockAccessToken = mockk<AccessToken>(relaxed = true) {
//                every { token } returns accessTokenString
//            }
//            val mockAuthenticationToken = mockk<AuthenticationToken>(relaxed = true) {
//                every { token } returns "mock_auth_token"
//            }
//            val mockLoginResult = LoginResult(
//                accessToken = mockAccessToken,
//                authenticationToken = mockAuthenticationToken,
//                recentlyGrantedPermissions = setOf("email", "public_profile"),
//                recentlyDeniedPermissions = emptySet()
//            )
//
//            // Invoca directamente el callback capturado para simular el éxito de Facebook
//            facebookCallbackSlot.captured.onSuccess(mockLoginResult)
//
//            advanceUntilIdle()
//
//            coVerify(exactly = 1) { mockLoginViewModel.loginWithFacebook(accessTokenString) }
//            onView(withText(successMessage))
//                .inRoot(withDecorView(not(activityDecorView)))
//                .check(matches(isDisplayed()))
//            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
//            assertThat(mockNavController.currentDestination?.id).isEqualTo(R.id.loginFragment)
//            coVerify(exactly = 1) { mockNavController.navigate(R.id.action_loginFragment_to_mapFragment) }
//        }
//
//    @Test
//    fun when_facebook_login_is_cancelled_error_message_is_shown() = runTest {
//        val errorMessage = "Inicio de sesión con Facebook cancelado."
//
//        every { mockFacebookSignInDataSource.logInWithReadPermissions(any(), any()) } just Runs
//
//        // Configura que el ViewModel emita el mensaje de error cuando se le indique (a través del callback)
//        // No hay un coEvery para un 'LoginEvent.ShowMessage' específico en el ViewModel
//        // porque la lógica del error se manejará directamente en el callback capturado.
//
//        onView(withId(R.id.btnFacebookLogin)).perform(click())
//        advanceUntilIdle() // Asegura que el clic y el `logInWithReadPermissions` se procesen
//
//        // Simula la cancelación del login de Facebook invocando el método onCancel del callback capturado
//        facebookCallbackSlot.captured.onCancel()
//        advanceUntilIdle() // Permite que el mensaje de error se emita y se muestre en la UI
//
//        // Verificaciones
//        // Verifica que el método logInWithReadPermissions fue llamado
//        coVerify(exactly = 1) {
//            mockFacebookSignInDataSource.logInWithReadPermissions(
//                any(),
//                any()
//            )
//        }
//
//        // Verifica que no se llamó a loginWithFacebook (porque fue cancelado)
//        coVerify(exactly = 0) { mockLoginViewModel.loginWithFacebook(any()) }
//
//        // Verifica que el mensaje de error se mostró
//        onView(withText(errorMessage))
//            .inRoot(withDecorView(not(activityDecorView)))
//            .check(matches(isDisplayed()))
//
//        // Asegura que la barra de progreso no esté visible
//        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
//    }
//
//    @Test
//    fun when_facebook_login_fails_due_to_error_error_message_is_shown() = runTest {
//        val exceptionMessage = "Error de conexión de red"
//        val errorMessage = "Error: $exceptionMessage" // Como el fragmento lo construye
//
//        every { mockFacebookSignInDataSource.logInWithReadPermissions(any(), any()) } just Runs
//
//        onView(withId(R.id.btnFacebookLogin)).perform(click())
//        advanceUntilIdle()
//
//        // Simula un error en el login de Facebook
//        val facebookException = FacebookException(exceptionMessage)
//        facebookCallbackSlot.captured.onError(facebookException)
//        advanceUntilIdle()
//
//        // Verificaciones
//        coVerify(exactly = 1) {
//            mockFacebookSignInDataSource.logInWithReadPermissions(
//                any(),
//                any()
//            )
//        }
//        coVerify(exactly = 0) { mockLoginViewModel.loginWithFacebook(any()) } // No se llama a loginWithFacebook en caso de error
//
//        onView(withText(errorMessage))
//            .inRoot(withDecorView(not(activityDecorView)))
//            .check(matches(isDisplayed()))
//
//        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
//    }
//
//
//    /* Pruebas de Navegación */
//
//    @Test
//    fun when_forgot_password_is_clicked_navigates_to_forgot_password_fragment() = runTest {
//        every { mockLoginViewModel.onForgotPasswordClicked() } coAnswers {
//            eventFlow.emit(LoginEvent.NavigateToForgotPassword)
//        }
//        every { mockNavController.navigate(R.id.action_loginFragment_to_forgotPasswordFragment) } just Runs
//
//        onView(withId(R.id.tvForgotPassword)).perform(click())
//        advanceUntilIdle()
//
//        coVerify(exactly = 1) { mockLoginViewModel.onForgotPasswordClicked() }
//        assertThat(mockNavController.currentDestination?.id).isEqualTo(R.id.loginFragment)
//        coVerify(exactly = 1) { mockNavController.navigate(R.id.action_loginFragment_to_forgotPasswordFragment) }
//    }
//
//    @Test
//    fun when_back_button_is_clicked_navigates_back() = runTest {
//        every { mockLoginViewModel.onBackPressed() } coAnswers {
//            eventFlow.emit(LoginEvent.NavigateBack)
//        }
//        every { mockNavController.popBackStack() } returns true
//
//        onView(withId(R.id.ivBack)).perform(click())
//        advanceUntilIdle()
//
//        coVerify(exactly = 1) { mockLoginViewModel.onBackPressed() }
//        coVerify(exactly = 1) { mockNavController.popBackStack() }
//    }
//
//    @Test
//    fun when_sign_up_button_is_clicked_navigates_to_signup_fragment() {
//        every { mockNavController.navigate(R.id.action_loginFragment_to_signupFragment) } just Runs
//
//        onView(withId(R.id.tvSignUpBtn)).perform(click())
//
//        assertThat(mockNavController.currentDestination?.id).isEqualTo(R.id.loginFragment)
//        verify(exactly = 1) { mockNavController.navigate(R.id.action_loginFragment_to_signupFragment) }
//    }
//}