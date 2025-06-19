package com.cursoandroid.queermap.ui.login


import android.content.Intent
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
class LoginFragmentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    @JvmField
    val mockLoginViewModel: LoginViewModel = mockk(relaxed = true)

    @BindValue
    @JvmField
    val mockInputValidator: InputValidator = mockk(relaxed = true)

    @BindValue
    @JvmField
    val fragmentFactory: FragmentFactory = object : FragmentFactory() {
        override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
            return when (className) {
                LoginFragment::class.java.name -> LoginFragment()
                SignUpFragment::class.java.name -> SignUpFragment()
                ForgotPasswordFragment::class.java.name -> ForgotPasswordFragment()
                MapFragment::class.java.name -> MapFragment()
                else -> super.instantiate(classLoader, className)
            }
        }
    }

    @JvmField
    val mockGoogleSignInDataSource: GoogleSignInDataSource = mockk(relaxed = true)

    @JvmField
    val mockFacebookSignInDataSource: FacebookSignInDataSource = mockk(relaxed = true)

    private lateinit var uiStateFlow: MutableStateFlow<LoginUiState>
    private lateinit var eventFlow: MutableSharedFlow<LoginEvent>
    private lateinit var accessTokenChannelFlow: MutableSharedFlow<Result<String>>

    // testScheduler y testDispatcher se mantienen para el control de runTest y advanceUntilIdle
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = UnconfinedTestDispatcher(testScheduler)


    private lateinit var activityDecorView: View
    private lateinit var activityScenario: ActivityScenario<HiltTestActivity>
    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        hiltRule.inject()

        uiStateFlow = MutableStateFlow(LoginUiState())
        eventFlow = MutableSharedFlow()
        accessTokenChannelFlow = MutableSharedFlow()

        clearAllMocks()

        every { mockLoginViewModel.uiState } returns uiStateFlow
        every { mockLoginViewModel.event } returns eventFlow

        coEvery {
            mockLoginViewModel.loginWithEmail(
                any(),
                any()
            )
        } coAnswers { /* handled in specific tests */ }
        coEvery { mockLoginViewModel.loginWithGoogle(any()) } coAnswers { /* handled in specific tests */ }
        coEvery { mockLoginViewModel.loginWithFacebook(any()) } coAnswers { /* handled in specific tests */ }
        coEvery { mockLoginViewModel.onForgotPasswordClicked() } coAnswers { /* handled in specific tests */ }
        coEvery { mockLoginViewModel.onBackPressed() } coAnswers { /* handled in specific tests */ }
        coEvery {
            mockLoginViewModel.saveUserCredentials(
                any(),
                any()
            )
        } coAnswers { /* handled in specific tests */ }
        coEvery { mockLoginViewModel.loadUserCredentials() } coAnswers { /* handled in specific tests */ }

        every { mockInputValidator.isValidEmail(any()) } returns true
        every { mockInputValidator.isValidPassword(any()) } returns true
        every { mockInputValidator.isStrongPassword(any()) } returns true
        every { mockInputValidator.isValidUsername(any()) } returns true
        every { mockInputValidator.isValidFullName(any()) } returns true
        every { mockInputValidator.isValidBirthday(any()) } returns true

        coEvery { mockGoogleSignInDataSource.getSignInIntent() } returns Intent()
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

                    // Aplaza esta ejecución para cuando la vista esté lista
                    fragment.view?.post {
                        Navigation.setViewNavController(fragment.requireView(), navController)
                    }
                }
            }


            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment, null)
                .commitNow()
            activityDecorView = activity.window.decorView
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        activityScenario.close()
        uiStateFlow.value = LoginUiState()
        testDispatcher.scheduler.advanceUntilIdle()
        // Restablecer el Main Dispatcher
        // kotlinx.coroutines.Dispatchers.resetMain() // Podría ser necesario si se usó setMain
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
//passed
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
                // ⚠️ No emitas aquí el ShowMessage si vas a testear navegación
            }

            onView(withId(R.id.etEmailLogin)).perform(typeText(email))
            onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(click())

            testScheduler.advanceUntilIdle()

            assertThat(navController.currentDestination?.id).isEqualTo(R.id.mapFragment)

        }
//passed
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

            testScheduler.advanceUntilIdle()



            coVerify(exactly = 1) { mockGoogleSignInDataSource.getSignInIntent() }

        }


    @Test

    fun when_google_sign_in_result_is_success_for_existing_user_then_navigates_to_home() =

        runTest(testDispatcher) {

            val intentData = mockk<Intent>()

            val idToken = "some_google_id_token"



            coEvery { mockGoogleSignInDataSource.handleSignInResult(intentData) } returns Result.success(

                idToken

            )

            coEvery { mockLoginViewModel.loginWithGoogle(idToken) } coAnswers {

                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = true))

                testScheduler.advanceTimeBy(100)

                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = false, isSuccess = true))

                eventFlow.emit(LoginEvent.NavigateToHome)

                eventFlow.emit(LoginEvent.ShowMessage("Inicio de sesión con Google exitoso"))

            }



            activityScenario.onActivity { activity ->

                val fragment =

                    activity.supportFragmentManager.findFragmentById(android.R.id.content) as LoginFragment

                fragment.handleGoogleSignInResult(intentData)

            }

            testScheduler.advanceUntilIdle()



            coVerify(exactly = 1) { mockGoogleSignInDataSource.handleSignInResult(intentData) }

            coVerify(exactly = 1) { mockLoginViewModel.loginWithGoogle(idToken) }



            onView(withText("Inicio de sesión con Google exitoso"))

                .inRoot(withDecorView(not(`is`(activityDecorView))))

                .check(matches(isDisplayed()))

            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))

            assertThat(navController.currentDestination?.id).isEqualTo(R.id.mapFragment)

        }


    @Test

    fun when_google_sign_in_result_is_success_for_new_user_then_navigates_to_signup_with_args() =

        runTest(testDispatcher) {

            val intentData = mockk<Intent>()

            val idToken = "some_google_id_token"

            val socialEmail = "new_google@example.com"

            val socialName = "New Google User"



            coEvery { mockGoogleSignInDataSource.handleSignInResult(intentData) } returns Result.success(

                idToken

            )

            coEvery { mockLoginViewModel.loginWithGoogle(idToken) } coAnswers {

                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = true))

                testScheduler.advanceTimeBy(100)

                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = false, isSuccess = true))

                eventFlow.emit(

                    LoginEvent.NavigateToSignupWithArgs(

                        socialUserEmail = socialEmail,

                        socialUserName = socialName,

                        isSocialLoginFlow = true

                    )

                )

                eventFlow.emit(LoginEvent.ShowMessage("Completa tu perfil para continuar"))

            }



            activityScenario.onActivity { activity ->

                val fragment =

                    activity.supportFragmentManager.findFragmentById(android.R.id.content) as LoginFragment

                fragment.handleGoogleSignInResult(intentData)

            }

            testScheduler.advanceUntilIdle()



            coVerify(exactly = 1) { mockGoogleSignInDataSource.handleSignInResult(intentData) }

            coVerify(exactly = 1) { mockLoginViewModel.loginWithGoogle(idToken) }



            onView(withText("Completa tu perfil para continuar"))

                .inRoot(withDecorView(not(`is`(activityDecorView))))

                .check(matches(isDisplayed()))

            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))



            assertThat(navController.currentDestination?.id).isEqualTo(R.id.signupFragment)

            val args = navController.backStack.last().arguments

            assertThat(args?.getString("socialUserEmail")).isEqualTo(socialEmail)

            assertThat(args?.getString("socialUserName")).isEqualTo(socialName)

            assertThat(args?.getBoolean("isSocialLoginFlow")).isTrue()

        }


    @Test

    fun when_google_sign_in_result_is_failure_then_shows_error_message() = runTest(testDispatcher) {

        val intentData = mockk<Intent>()

        val exceptionMessage = "Fallo de Google"

        val expectedSnackbarMessage = "Error en Sign-In: $exceptionMessage"



        coEvery { mockGoogleSignInDataSource.handleSignInResult(intentData) } returns Result.failure(

            Exception(exceptionMessage)

        )



        activityScenario.onActivity { activity ->

            val fragment =

                activity.supportFragmentManager.findFragmentById(android.R.id.content) as LoginFragment

            fragment.handleGoogleSignInResult(intentData)

        }

        testScheduler.advanceUntilIdle()



        coVerify(exactly = 1) { mockGoogleSignInDataSource.handleSignInResult(intentData) }

        coVerify(exactly = 0) { mockLoginViewModel.loginWithGoogle(any()) }



        onView(withText(expectedSnackbarMessage))

            .inRoot(withDecorView(not(`is`(activityDecorView))))

            .check(matches(isDisplayed()))

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))

    }


    @Test

    fun when_facebook_login_button_is_clicked_then_logInWithReadPermissions_is_called() =

        runTest(testDispatcher) {

            onView(withId(R.id.btnFacebookLogin)).perform(click())

            testScheduler.advanceUntilIdle()



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

                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = true))

                testScheduler.advanceTimeBy(100)

                uiStateFlow.emit(uiStateFlow.value.copy(isLoading = false, isSuccess = true))

                eventFlow.emit(LoginEvent.NavigateToHome)

                eventFlow.emit(LoginEvent.ShowMessage("Inicio de sesión con Facebook exitoso"))

            }


// Emitir el resultado al canal, esto activará la observación en el Fragment

            launch(testDispatcher) { // Asegúrate de que esta emisión también esté en el testDispatcher

                accessTokenChannelFlow.emit(Result.success(accessToken))

            }

            testScheduler.advanceUntilIdle() // Asegúrate de que el Fragment haya procesado la emisión


            coVerify(exactly = 1) { mockLoginViewModel.loginWithFacebook(accessToken) }



            onView(withText("Inicio de sesión con Facebook exitoso"))

                .inRoot(withDecorView(not(`is`(activityDecorView))))

                .check(matches(isDisplayed()))

            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))

            assertThat(navController.currentDestination?.id).isEqualTo(R.id.mapFragment)

        }


    @Test

    fun when_facebook_accessTokenChannel_failure_then_shows_error_message() =

        runTest(testDispatcher) {

            val exceptionMessage = "Facebook login failed"

            val expectedSnackbarMessage = "Error: $exceptionMessage"



            launch(testDispatcher) {

                accessTokenChannelFlow.emit(Result.failure(Exception(exceptionMessage)))

            }

            testScheduler.advanceUntilIdle()



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

                eventFlow.emit(LoginEvent.NavigateToForgotPassword)

            }



            onView(withId(R.id.tvForgotPassword)).perform(click())

            testScheduler.advanceUntilIdle()



            coVerify(exactly = 1) { mockLoginViewModel.onForgotPasswordClicked() }

            assertThat(navController.currentDestination?.id).isEqualTo(R.id.forgotPasswordFragment)

        }


    @Test

    fun when_back_button_is_clicked_then_navigates_back() = runTest(testDispatcher) {

        navController.navigate(R.id.mapFragment)

        testScheduler.advanceUntilIdle()

        assertThat(navController.currentDestination?.id).isEqualTo(R.id.mapFragment)



        coEvery { mockLoginViewModel.onBackPressed() } coAnswers {

            eventFlow.emit(LoginEvent.NavigateBack)

        }



        onView(withId(R.id.ivBack)).perform(click())

        testScheduler.advanceUntilIdle()



        coVerify(exactly = 1) { mockLoginViewModel.onBackPressed() }

        assertThat(navController.currentDestination?.id).isEqualTo(R.id.loginFragment)

    }

}