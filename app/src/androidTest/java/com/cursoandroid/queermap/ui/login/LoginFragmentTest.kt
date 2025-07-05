package com.cursoandroid.queermap.ui.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
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
import com.cursoandroid.queermap.util.isToastMessageDisplayed
import com.cursoandroid.queermap.util.waitFor
import com.facebook.FacebookCallback
import com.facebook.login.LoginResult
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.resume

suspend fun waitForNavigationTo(
    navController: NavController,
    destinationId: Int,
    timeoutMs: Long = 3000L
) {
    withTimeout(timeoutMs) {
        suspendCancellableCoroutine<Unit> { cont ->
            val listener = object : NavController.OnDestinationChangedListener {
                override fun onDestinationChanged(
                    controller: NavController,
                    destination: NavDestination,
                    arguments: Bundle?
                ) {
                    if (destination.id == destinationId) {
                        navController.removeOnDestinationChangedListener(this)
                        if (cont.isActive) cont.resume(Unit)
                    }
                }
            }

            navController.addOnDestinationChangedListener(listener)

            cont.invokeOnCancellation {
                navController.removeOnDestinationChangedListener(listener)
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

        // *** CAMBIO CLAVE 1: Asegurar que el fragmento esté listo antes de llamar handleGoogleSignInResult ***
        // Usar un `runOnUiThread` y `onView(isRoot()).perform(waitFor(millis))`
        // o `waitUntilVisibleAndEnabledAndCompletelyDisplayed()` puede ser necesario para sincronización
        every { mockGoogleSignInLauncher.launch(any()) } answers {
            activityScenario.onActivity { activity ->
                // Asegúrate de que el fragmento esté completamente en pantalla y con el foco
                val fragment =
                    activity.supportFragmentManager.findFragmentById(android.R.id.content) as? LoginFragment
                if (fragment != null && fragment.isAdded && fragment.view != null) {
                    InstrumentationRegistry.getInstrumentation().runOnMainSync {
                        runBlocking {
                            // Añadir una pequeña espera para asegurar que la UI se asiente si es necesario
                            // Aunque para handleGoogleSignInResult, el fragmento ya debería estar listo.
                            // Si sigues viendo problemas, podrías añadir un waitFor aquí, pero no es lo ideal.
                            fragment.handleGoogleSignInResult(mockGoogleSignInResultIntent)
                        }
                    }
                } else {
                    Log.w(
                        "TEST",
                        "Fragment no está listo o ya no está activo. No se puede lanzar handleGoogleSignInResult"
                    )
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

        activityScenario = ActivityScenario.launch(HiltTestActivity::class.java)

        activityScenario.onActivity { activity ->
            mockNavController = TestNavHostController(ApplicationProvider.getApplicationContext())
            mockNavController.setGraph(R.navigation.nav_graph)
            // Navegamos y aseguramos que el fragmento se cargue correctamente.
            // Asegúrate de que tu `HiltTestActivity` tenga un `FragmentContainerView` o similar con `android.R.id.content`
            // o el ID de tu FragmentContainerView.
            val fragment = LoginFragment()
            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commitNow()

            // Esperar que el Fragmento esté en estado `STARTED` y su vista esté disponible
            // Esto es crucial para que el NavController se asocie correctamente
            // y para que Espresso pueda interactuar con las vistas del fragmento.
            val fragmentViewReadyLatch = CountDownLatch(1)
            fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                if (viewLifecycleOwner != null && fragment.view != null && fragment.lifecycle.currentState.isAtLeast(
                        Lifecycle.State.STARTED
                    )
                ) {
                    Navigation.setViewNavController(fragment.requireView(), mockNavController)
                    fragmentViewReadyLatch.countDown()
                }
            }
            // Esperar un poco más para asegurar que el NavController esté completamente configurado
            // Esto es más robusto que solo setViewNavController.
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            Espresso.onIdle()

            fragment.testGoogleSignInLauncher = mockGoogleSignInLauncher
            fragment.testGoogleSignInDataSource = mockGoogleSignInDataSource
            fragment.testFacebookSignInDataSource = mockFacebookSignInDataSource
            fragment.testCallbackManager = mockk(relaxed = true)

            activityDecorView = activity.window.decorView
        }
        Espresso.onIdle() // Asegurarse de que todas las operaciones de UI pendientes han terminado
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

    //passed
    @Test
    fun when_typing_in_password_field_text_is_updated() {
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).check(matches(withText("password123")))
    }

    //passed
    @Test
    fun when_login_loads_credentials_email_and_password_fields_are_updated() {
        val savedEmail = "saved@example.com"
        val savedPassword = "savedPassword123"

        coEvery { mockLoginViewModel.loadUserCredentials() } coAnswers {
            uiStateFlow.value = uiStateFlow.value.copy(
                email = savedEmail,
                password = savedPassword
            )
        }

        activityScenario.onActivity { activity ->
            val fragment = LoginFragment()

            mockNavController.setGraph(R.navigation.nav_graph)
            mockNavController.setCurrentDestination(R.id.loginFragment)

            fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                if (fragment.view != null) {
                    Navigation.setViewNavController(fragment.requireView(), mockNavController)
                }
            }

            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commitNow()

            fragment.testGoogleSignInLauncher = mockGoogleSignInLauncher
            fragment.testGoogleSignInDataSource = mockGoogleSignInDataSource
            fragment.testFacebookSignInDataSource = mockFacebookSignInDataSource
            fragment.testCallbackManager = mockk(relaxed = true)
        }

        // ⚠️ Espera breve para que Espresso capture la UI ya actualizada
        onView(isRoot()).perform(waitFor(500))

        // Afirmaciones
        onView(withId(R.id.etEmailLogin)).check(matches(withText(savedEmail)))
        onView(withId(R.id.etPassword)).check(matches(withText(savedPassword)))

        coVerify(atLeast = 1) { mockLoginViewModel.loadUserCredentials() }
    }

    /* Pruebas de Interacción del Botón de Login (Email/Password) */
    //passed
    @Test
    fun when_login_button_is_clicked_loginWithEmail_is_called_with_correct_data_and_navigates_to_home_on_success() {
        val email = "valid@example.com"
        val password = "validpassword"

        coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
            uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)
            uiStateFlow.value = uiStateFlow.value.copy(isLoading = false, isSuccess = true)
            eventFlow.emit(LoginEvent.NavigateToHome)
        }

        onView(withId(R.id.etEmailLogin)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        // Esperar que cambie la UI
        onView(isRoot()).perform(waitFor(500))

        coVerify { mockLoginViewModel.loginWithEmail(email, password) }

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))

        // Afirmamos que el NavController cambió de destino
        assertThat(mockNavController.currentDestination?.id).isEqualTo(R.id.mapFragment)
    }

    //passed
    @Test
    fun when_login_button_is_clicked_and_email_is_invalid_error_message_is_shown() {
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

        onView(isRoot()).perform(waitFor(1500)) // dejar tiempo para mostrar Toast

        coVerify { mockLoginViewModel.loginWithEmail(email, password) }

        assertTrue("Toast con mensaje no encontrado", isToastMessageDisplayed(errorMessage))

        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    //passed
    @Test
    fun when_login_button_is_clicked_and_password_is_invalid_error_message_is_shown() {
        val email = "valid@example.com"
        val password = "short"
        val errorMessage = "La contraseña debe tener al menos 6 caracteres"

        // Simula el evento que modifica el UIState y emite el mensaje
        coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
            uiStateFlow.value = uiStateFlow.value.copy(isPasswordInvalid = true)
            mainDispatcherRule.testScope.launch {
                eventFlow.emit(LoginEvent.ShowMessage(errorMessage))
            }
        }

        // Simula ingreso de credenciales inválidas
        onView(withId(R.id.etEmailLogin)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        // Espera que el Toast aparezca
        onView(isRoot()).perform(waitFor(1500))

        // Verifica llamada al ViewModel
        coVerify(exactly = 1) { mockLoginViewModel.loginWithEmail(email, password) }

        // Verifica que el Toast se muestre
        assertTrue("Toast con mensaje no encontrado", isToastMessageDisplayed(errorMessage))

        // Verifica que el ProgressBar no esté visible
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    //passed
    @Test
    fun when_login_fails_due_to_general_error_error_message_is_shown() {
        val email = "test@example.com"
        val password = "password123"
        val errorMessage = "Error inesperado. Intenta de nuevo más tarde"

        coEvery { mockLoginViewModel.loginWithEmail(email, password) } coAnswers {
            uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)

            // Simular que termina el loading y ocurre un error
            uiStateFlow.value = uiStateFlow.value.copy(
                isLoading = false,
                errorMessage = errorMessage
            )

            // Emitir evento del mensaje de error
            mainDispatcherRule.testScope.launch {
                eventFlow.emit(LoginEvent.ShowMessage(errorMessage))
            }
        }

        // Simular ingreso de credenciales válidas
        onView(withId(R.id.etEmailLogin)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())

        // Esperar aparición del Toast
        onView(isRoot()).perform(waitFor(1500))

        // Verificación del método del ViewModel
        coVerify(exactly = 1) { mockLoginViewModel.loginWithEmail(email, password) }

        // Verificar que el mensaje Toast se muestre
        assertTrue("Toast con mensaje no encontrado", isToastMessageDisplayed(errorMessage))

        // Verificar que el ProgressBar ya no esté visible
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    /* Pruebas de Interacción de Login Social (Google) */
    //Passed
    @Test
    fun when_google_button_is_clicked_launcher_is_invoked_and_navigates_to_home_on_success() =
        runTest {
            val idToken = FAKE_GOOGLE_ID_TOKEN
            val successMessage = "Inicio de sesión social exitoso"

            // Simular que el DataSource devuelve éxito con el idToken falso
            coEvery {
                mockGoogleSignInDataSource.handleSignInResult(mockGoogleSignInResultIntent)
            } returns Result.Success(idToken)

            // Simular comportamiento del ViewModel
            coEvery { mockLoginViewModel.loginWithGoogle(idToken) } coAnswers {
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)

                // Simular fin del loading y éxito
                uiStateFlow.value = uiStateFlow.value.copy(isLoading = false, isSuccess = true)

                mainDispatcherRule.testScope.launch {
                    delay(100) // para dar tiempo al fragmento de estar en STARTED
                    eventFlow.emit(LoginEvent.NavigateToHome)
                    eventFlow.emit(LoginEvent.ShowMessage(successMessage))
                }


                // Crear un spy para el NavController para detectar la navegación
                val navControllerSpy = spyk(mockNavController)

                // Reemplazar NavController del fragmento con el spy
                activityScenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentById(android.R.id.content) as LoginFragment
                    Navigation.setViewNavController(fragment.requireView(), navControllerSpy)
                }

                // Ejecutar click en botón Google Sign In
                onView(withId(R.id.btnGoogleSignIn)).perform(click())

                // Esperar que la navegación ocurra usando la función mejorada (basada en listener)
                waitForNavigationTo(navControllerSpy, R.id.mapFragment)

                // Verificar que el launcher de Google SignIn fue invocado
                coVerify(exactly = 1) { mockGoogleSignInLauncher.launch(any()) }

                // Verificar que el datasource manejó el resultado
                coVerify(exactly = 1) {
                    mockGoogleSignInDataSource.handleSignInResult(
                        mockGoogleSignInResultIntent
                    )
                }

                // Verificar que el ViewModel recibió el token y navegó
                coVerify(exactly = 1) { mockLoginViewModel.loginWithGoogle(FAKE_GOOGLE_ID_TOKEN) }

                // Verificar que el NavController realizó la navegación esperada
                coVerify(exactly = 1) { navControllerSpy.navigate(R.id.action_loginFragment_to_mapFragment) }

                // Verificar que el progressBar no está visible
                onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))

                // Verificar que el Toast con mensaje exitoso aparece
                assertTrue(
                    "Toast con mensaje no encontrado",
                    isToastMessageDisplayed(successMessage)
                )
            }
        }

    @Test
    fun when_google_login_is_for_new_user_navigates_to_signup_with_args() = runTest {
        val idToken = "some_new_google_id_token"
        val socialEmail = "new.user@example.com"
        val socialName = "New Google User"
        val messageForNewUser = "Completa tu perfil para continuar"

        // Mock resultado exitoso del datasource
        coEvery {
            mockGoogleSignInDataSource.handleSignInResult(mockGoogleSignInResultIntent)
        } returns Result.Success(idToken)

        // Mock comportamiento ViewModel con evento de navegación y mensaje
        coEvery { mockLoginViewModel.loginWithGoogle(idToken) } coAnswers {
            uiStateFlow.value = uiStateFlow.value.copy(isLoading = false, isSuccess = true)
            launch {
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

        // NO necesitas esta sección aquí, ya está en setUp y se ejecuta antes de cada test.
        // Forzar que el NavController mock esté asignado al fragmento y visible ANTES del click
        // activityScenario.onActivity { activity ->
        //     val fragment =
        //         activity.supportFragmentManager.findFragmentById(android.R.id.content) as LoginFragment
        //     Navigation.setViewNavController(fragment.requireView(), mockNavController)
        // }

        // Limpieza y sincronización previa
        Espresso.onIdle()

        // Ejecutar click que dispara la lógica
        onView(withId(R.id.btnGoogleSignIn)).perform(click())

        // Esperar que la navegación al signup ocurra con timeout razonable
        waitForNavigationTo(mockNavController, R.id.signupFragment, timeoutMs = 5000L)

        // Verificar que el launcher se invocó para lanzar intent de login Google
        coVerify { mockGoogleSignInLauncher.launch(any()) }

        // Verificar que datasource procesó resultado
        coVerify { mockGoogleSignInDataSource.handleSignInResult(mockGoogleSignInResultIntent) }

        // Verificar que ViewModel manejó el token
        coVerify { mockLoginViewModel.loginWithGoogle(idToken) }

        // Verificar que los argumentos en navegación son correctos
        val backStackEntry = mockNavController.getBackStackEntry(R.id.signupFragment)
        val args = backStackEntry.arguments

        assertThat(args?.getString("socialUserEmail")).isEqualTo(socialEmail)
        assertThat(args?.getString("socialUserName")).isEqualTo(socialName)
        assertThat(args?.getBoolean("isSocialLoginFlow")).isEqualTo(true)

        // Confirmar que ProgressBar no está visible
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))

        // *** CAMBIO CLAVE 2: Verificación de Toast usando isSystemAlertWindow() ***
        // Los Toasts se muestran en un tipo de ventana diferente (TYPE_TOAST o TYPE_SYSTEM_ALERT)
        // que no es el mismo que el DecorView de tu actividad.
        // Por eso, la condición `not(activityDecorView)` es correcta, pero la forma de acceder a esa Root
        // debe ser más general, usando `isSystemAlertWindow()` o `isDialog()`
        onView(withText(messageForNewUser))
            .inRoot(isSystemAlertWindow()) // O RootMatchers.isDialog() si el Toast se emite como diálogo
            .check(matches(isDisplayed()))

        // Alternativa si el toast no es detectado con isSystemAlertWindow (puede variar con el SDK o personalizaciones):
        // onView(withText(messageForNewUser))
        //    .inRoot(RootMatchers.withDecorView(not(activityDecorView))) // Mantener esta si es tu DecorView el que no tiene foco
        //    .check(matches(isDisplayed()));

        // Otra alternativa más genérica para Toasts:
        // onView(withText(messageForNewUser))
        //    .inRoot(RootMatchers.isFocusable()) // Los Toasts suelen ser focusable temporalmente
        //    .check(matches(isDisplayed()));
    }
}


//    @Test
//    fun when_google_login_fails_error_message_is_shown() = runTest {
//        val exceptionMessage = "Error de autenticación de Google"
//        val expectedDisplayMessage =
//            "Error en Sign-In: $exceptionMessage"
//
//        coEvery { mockGoogleSignInDataSource.handleSignInResult(mockGoogleSignInResultIntent) } returns Result.Failure(
//            Exception(exceptionMessage)
//        )
//
//        coEvery { eventFlow.emit(LoginEvent.ShowMessage(expectedDisplayMessage)) } just Runs
//
//        onView(withId(R.id.btnGoogleSignIn)).perform(click())
//
//        advanceUntilIdle()
//
//        coVerify(exactly = 1) { mockGoogleSignInLauncher.launch(any()) }
//        coVerify(exactly = 1) {
//            mockGoogleSignInDataSource.handleSignInResult(
//                mockGoogleSignInResultIntent
//            )
//        }
//        coVerify(exactly = 0) { mockLoginViewModel.loginWithGoogle(any()) }
//
//        onView(withText(expectedDisplayMessage))
//            .inRoot(withDecorView(not(activityDecorView)))
//            .check(matches(isDisplayed()))
//        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
//    }
//
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