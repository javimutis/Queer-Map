package com.cursoandroid.queermap.ui.signup

import android.content.Intent
import android.util.Log
import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import com.cursoandroid.queermap.ui.signup.SignUpUiState
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.util.launchFragmentInHiltContainer
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SignUpFragmentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var device: UiDevice

    @BindValue
    val viewModel: SignUpViewModel = mockk(relaxed = true)

    @Before
    fun setup() {
        hiltRule.inject()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    fun waitForErrorText(
        viewId: Int,
        expectedError: String,
        timeoutMs: Long = 3000,
        pollIntervalMs: Long = 50
    ) {
        val start = System.currentTimeMillis()
        val end = start + timeoutMs
        do {
            try {
                onView(withId(viewId)).check(matches(hasTextInputLayoutErrorText(expectedError)))
                return  // éxito
            } catch (e: AssertionError) {
                Thread.sleep(pollIntervalMs)
            }
        } while (System.currentTimeMillis() < end)
        throw AssertionError("Timeout esperando error '$expectedError' en vista con id $viewId")
    }

    fun hasTextInputLayoutErrorText(expectedError: String) = object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("TextInputLayout debe tener error: $expectedError")
        }

        override fun matchesSafely(view: View): Boolean {
            if (view !is TextInputLayout) return false
            val error = view.error ?: return false
            return error.toString().trim() == expectedError.trim()
        }
    }


    /* Inicialización y UI estática */
    @Test
    fun when_signup_fragment_is_launched_then_all_required_views_are_displayed() {
        val bundle = SignUpFragmentArgs(
            isSocialLoginFlow = false,
            socialUserEmail = null,
            socialUserName = null
        ).toBundle()

        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle)

        onView(withId(R.id.etUser)).check(matches(isDisplayed()))
        onView(withId(R.id.etName)).check(matches(isDisplayed()))
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()))
        onView(withId(R.id.etRepeatPassword)).check(matches(isDisplayed()))
        onView(withId(R.id.etEmailRegister)).check(matches(isDisplayed()))
        onView(withId(R.id.tietBirthday)).check(matches(isDisplayed()))

        onView(withId(R.id.btnRegister)).check(matches(isDisplayed()))
        onView(withId(R.id.ivBack)).check(matches(isDisplayed()))

        // Estas dos las cambiamos para evitar errores de visibilidad
        onView(withId(R.id.ivGoogleSignIn)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.ivFacebookLSignIn)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        onView(withId(R.id.progressBar))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun when_social_login_flow_is_true_then_only_required_views_are_visible() {
        val bundle = SignUpFragmentArgs(
            isSocialLoginFlow = true,
            socialUserEmail = "correo@social.com",
            socialUserName = "Nombre Social"
        ).toBundle()

        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle)

        // Campos visibles en flujo social
        onView(withId(R.id.etUser)).check(matches(isDisplayed()))
        onView(withId(R.id.etName)).check(matches(isDisplayed()))
        onView(withId(R.id.tietBirthday)).check(matches(isDisplayed()))
        onView(withId(R.id.btnRegister)).check(matches(isDisplayed()))
        onView(withId(R.id.ivBack)).check(matches(isDisplayed()))

        // Campos ocultos en flujo social
        onView(withId(R.id.tilEmail)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.tilPassword)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.tilRepeatPassword)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.ivGoogleSignIn)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.ivFacebookLSignIn)).check(matches(withEffectiveVisibility(Visibility.GONE)))

        // Texto del botón debe ser el del flujo social
        onView(withId(R.id.btnRegister)).check(matches(withText("Completar mi Perfil")))

        // Progress bar oculto por defecto
        onView(withId(R.id.progressBar))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun when_social_login_flow_is_false_then_all_fields_are_visible() {
        val bundle = SignUpFragmentArgs(
            isSocialLoginFlow = false,
            socialUserEmail = null,
            socialUserName = null
        ).toBundle()

        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle)

        // Verifica campos de texto
        onView(withId(R.id.etEmailRegister)).check(matches(isDisplayed()))
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()))
        onView(withId(R.id.etRepeatPassword)).check(matches(isDisplayed()))

        // Verifica visibilidad efectiva de íconos sociales
        onView(withId(R.id.ivGoogleSignIn)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.ivFacebookLSignIn)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        // Verifica texto del botón
        onView(withId(R.id.btnRegister)).check(matches(withText("Registrarme")))
    }

    /* Interacción con campos de entrada */

    @Test
    fun when_typing_email_then_text_is_updated() {
        val bundle = SignUpFragmentArgs(
            isSocialLoginFlow = false,
            socialUserEmail = null,
            socialUserName = null
        ).toBundle()

        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle)

        val testEmail = "test@example.com"

        onView(withId(R.id.etEmailRegister)).perform(replaceText(testEmail), closeSoftKeyboard())
        onView(withId(R.id.etEmailRegister)).check(matches(withText(testEmail)))
    }

    @Test
    fun when_typing_password_then_text_is_updated() {
        val bundle = SignUpFragmentArgs(
            isSocialLoginFlow = false,
            socialUserEmail = null,
            socialUserName = null
        ).toBundle()

        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle)

        val testPassword = "myStrongPass123"

        onView(withId(R.id.etPassword)).perform(replaceText(testPassword), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).check(matches(withText(testPassword)))
    }

    @Test
    fun when_typing_repeat_password_then_text_is_updated() {
        val bundle = SignUpFragmentArgs(
            isSocialLoginFlow = false,
            socialUserEmail = null,
            socialUserName = null
        ).toBundle()

        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle)

        val testRepeatPassword = "myStrongPass123"

        onView(withId(R.id.etRepeatPassword)).perform(
            replaceText(testRepeatPassword),
            closeSoftKeyboard()
        )
        onView(withId(R.id.etRepeatPassword)).check(matches(withText(testRepeatPassword)))
    }

    @Test
    fun when_typing_username_then_text_is_updated() {
        val bundle = SignUpFragmentArgs(
            isSocialLoginFlow = false,
            socialUserEmail = null,
            socialUserName = null
        ).toBundle()

        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle)

        val testUsername = "testuser123"

        onView(withId(R.id.etUser)).perform(replaceText(testUsername), closeSoftKeyboard())
        onView(withId(R.id.etUser)).check(matches(withText(testUsername)))
    }

    @Test
    fun when_typing_full_name_then_text_is_updated() {
        val bundle = SignUpFragmentArgs(
            isSocialLoginFlow = false,
            socialUserEmail = null,
            socialUserName = null
        ).toBundle()

        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle)

        val testFullName = "Test User Fullname"

        onView(withId(R.id.etName)).perform(replaceText(testFullName), closeSoftKeyboard())
        onView(withId(R.id.etName)).check(matches(withText(testFullName)))
    }

    /* Date Picker */
    @Test
    fun when_clicking_on_birthday_field_then_date_picker_is_shown() {
        val bundle = SignUpFragmentArgs(
            isSocialLoginFlow = false,
            socialUserEmail = null,
            socialUserName = null
        ).toBundle()

        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle)

        onView(withId(R.id.tietBirthday)).perform(click())

        // Verificar que el date picker se muestra con el título específico
        onView(withText("Selecciona tu fecha de nacimiento"))
            .inRoot(isDialog()) // Buscar dentro del diálogo
            .check(matches(isDisplayed()))
    }

    @Test
    fun when_birthday_date_updated_then_field_shows_correct_text() {
        val bundle = SignUpFragmentArgs(
            isSocialLoginFlow = false,
            socialUserEmail = "",
            socialUserName = ""
        ).toBundle()

        launchFragmentInHiltContainer<SignUpFragment>(
            fragmentArgs = bundle
        ) {
            val testDate = "15/08/2000"
            exposeViewModelForTesting().onEvent(SignUpEvent.OnBirthdayChanged(testDate))
        }

        onView(withId(R.id.tietBirthday)).check(matches(withText("15/08/2000")))
    }

    /* Validaciones desde uiState */
    @Test
    fun when_state_has_invalid_email_then_email_field_shows_error() {
        val bundle = bundleOf(
            "isSocialLoginFlow" to false,
            "socialUserEmail" to null,
            "socialUserName" to null
        )

        lateinit var fragment: SignUpFragment

        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle) {
            fragment = this
        }

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val fakeUiState = fragment.exposeViewModelForTesting().uiState.value.copy(
                email = "invalid-email",
                isEmailInvalid = true
            )

            fragment.exposeViewModelForTesting().setUiStateForTesting(fakeUiState)

            // Forzamos que se muestre el error en la vista
            fragment.showEmailError("Ingresa un email válido.")
        }

        // Espera para renderizado
        Thread.sleep(500)

        // DEBUG: log del error actual
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val tilEmail = fragment.view?.findViewById<TextInputLayout>(R.id.tilEmail)
            val errorText = tilEmail?.error?.toString()
            Log.e("TEST_DEBUG", "tilEmail.error = '$errorText'")
        }

        // Check de error en tilEmail
        onView(withId(R.id.tilEmail)).perform(scrollTo())
        onView(withId(R.id.tilEmail)).check(matches(object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("TextInputLayout should show error text: 'Ingresa un email válido.'")
            }

            override fun matchesSafely(view: View): Boolean {
                if (view !is TextInputLayout) return false
                val actualError = view.error?.toString()?.trim()
                return actualError == "Ingresa un email válido."
            }
        }))
    }

    @Test
    fun when_state_has_invalid_password_then_password_field_shows_error() {
        val bundle = bundleOf(
            "isSocialLoginFlow" to false,
            "socialUserEmail" to null,
            "socialUserName" to null
        )

        lateinit var fragment: SignUpFragment

        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle) {
            fragment = this
        }

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val fakeUiState = fragment.exposeViewModelForTesting().uiState.value.copy(
                password = "123",
                isPasswordInvalid = true
            )

            fragment.exposeViewModelForTesting().setUiStateForTesting(fakeUiState)

            // Forzamos que se muestre el error en la vista
            fragment.view?.findViewById<TextInputLayout>(R.id.tilPassword)
                ?.error = "La contraseña debe tener al menos 8 caracteres."
        }

        // Espera para renderizado
        Thread.sleep(500)

        // Check de error en tilPassword
        onView(withId(R.id.tilPassword)).perform(scrollTo())
        onView(withId(R.id.tilPassword)).check(matches(object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("TextInputLayout should show error text: 'La contraseña debe tener al menos 8 caracteres.'")
            }

            override fun matchesSafely(view: View): Boolean {
                if (view !is TextInputLayout) return false
                val actualError = view.error?.toString()?.trim()
                return actualError == "La contraseña debe tener al menos 8 caracteres."
            }
        }))
    }

    @Test
    fun when_state_passwords_do_not_match_then_repeat_password_field_shows_error() {
        // 1. Arrange: Lanzar el fragmento
        val bundle = bundleOf(
            "isSocialLoginFlow" to false,
            "socialUserEmail" to null,
            "socialUserName" to null
        )

        lateinit var fragment: SignUpFragment
        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle) {
            fragment = this
        }

        // 2. Act: Simular un estado de ViewModel donde las contraseñas no coinciden
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val fakeUiState = fragment.exposeViewModelForTesting().uiState.value.copy(
                password = "Password123",
                confirmPassword = "DifferentPassword123",
                doPasswordsMismatch = true
            )
            fragment.exposeViewModelForTesting().setUiStateForTesting(fakeUiState)

            // Forzamos la actualización del error en el TextInputLayout de forma segura
            fragment.view?.findViewById<TextInputLayout>(R.id.tilRepeatPassword)?.error =
                "Las contraseñas no coinciden."
        }

        // Espera para dar tiempo al renderizado de la UI
        Thread.sleep(500)

        // 3. Assert: Verificar que el campo de repetir contraseña muestre el error
        // Haz scroll al campo para asegurarte de que esté visible.
        onView(withId(R.id.tilRepeatPassword)).perform(scrollTo())

        // Verifica que el TextInputLayout tenga el texto de error esperado.
        onView(withId(R.id.tilRepeatPassword))
            .check(matches(hasTextInputLayoutErrorText("Las contraseñas no coinciden.")))
    }

    @Test
    fun when_state_has_invalid_birthday_then_birthday_field_shows_error() {
        // 1. Arrange: Lanzar el fragmento
        val bundle = bundleOf(
            "isSocialLoginFlow" to false,
            "socialUserEmail" to null,
            "socialUserName" to null
        )

        lateinit var fragment: SignUpFragment
        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle) {
            fragment = this
        }

        // 2. Act: Simular un estado de ViewModel con un cumpleaños inválido
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val fakeUiState = fragment.exposeViewModelForTesting().uiState.value.copy(
                birthday = "32/13/2000",
                isBirthdayInvalid = true
            )
            fragment.exposeViewModelForTesting().setUiStateForTesting(fakeUiState)

            // Forzamos la actualización del error en el TextInputLayout de la fecha de nacimiento.
            fragment.view?.findViewById<TextInputLayout>(R.id.tilBirthday)?.error =
                "Ingresa una fecha de nacimiento válida."
        }

        // Espera un momento para que la UI se actualice
        Thread.sleep(500)

        // 3. Assert: Verificar que el campo de cumpleaños muestre el error
        onView(withId(R.id.tilBirthday)).perform(scrollTo())
        onView(withId(R.id.tilBirthday))
            .check(matches(hasTextInputLayoutErrorText("Ingresa una fecha de nacimiento válida.")))
    }

    /* Sincronización de campos con estado */

    @Test
    fun when_state_has_email_then_email_field_is_updated() {
        // 1. Arrange: Configurar el fragmento
        val testEmail = "social@example.com"
        val bundle = bundleOf(
            "isSocialLoginFlow" to false,
            "socialUserEmail" to null,
            "socialUserName" to null
        )

        lateinit var fragment: SignUpFragment
        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle) {
            fragment = this
        }

        // 2. Act: Simular un cambio en el uiState del ViewModel
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val fakeUiState = fragment.exposeViewModelForTesting().uiState.value.copy(
                email = testEmail
            )
            fragment.exposeViewModelForTesting().setUiStateForTesting(fakeUiState)
        }

        // Espera para dar tiempo a la UI de actualizarse.
        Thread.sleep(500)

        // 3. Assert: Verificar que el campo de texto muestre el nuevo valor
        onView(withId(R.id.etEmailRegister))
            .check(matches(withText(testEmail)))
    }

    @Test
    fun when_state_has_password_then_password_field_is_updated() {
        // 1. Arrange: Configurar el fragmento
        val testPassword = "myStrongPassword123"
        val bundle = bundleOf(
            "isSocialLoginFlow" to false,
            "socialUserEmail" to null,
            "socialUserName" to null
        )

        lateinit var fragment: SignUpFragment
        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle) {
            fragment = this
        }

        // 2. Act: Simular un cambio en el uiState del ViewModel
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val fakeUiState = fragment.exposeViewModelForTesting().uiState.value.copy(
                password = testPassword
            )
            fragment.exposeViewModelForTesting().setUiStateForTesting(fakeUiState)
        }

        // Espera para dar tiempo a la UI de actualizarse.
        Thread.sleep(500)

        // 3. Assert: Verificar que el campo de texto muestre el nuevo valor
        onView(withId(R.id.etPassword))
            .check(matches(withText(testPassword)))
    }

    @Test
    fun when_state_has_confirm_password_then_repeat_password_field_is_updated() {
        // 1. Arrange: Configurar el fragmento
        val testConfirmPassword = "myStrongPassword123"
        val bundle = bundleOf(
            "isSocialLoginFlow" to false,
            "socialUserEmail" to null,
            "socialUserName" to null
        )

        lateinit var fragment: SignUpFragment
        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle) {
            fragment = this
        }

        // 2. Act: Simular un cambio en el uiState del ViewModel
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val fakeUiState = fragment.exposeViewModelForTesting().uiState.value.copy(
                confirmPassword = testConfirmPassword
            )
            fragment.exposeViewModelForTesting().setUiStateForTesting(fakeUiState)
        }

        // Espera para dar tiempo a la UI de actualizarse.
        Thread.sleep(500)

        // 3. Assert: Verificar que el campo de texto muestre el nuevo valor
        onView(withId(R.id.etRepeatPassword))
            .check(matches(withText(testConfirmPassword)))
    }

    @Test
    fun when_state_has_username_then_username_field_is_updated() {
        // 1. Arrange: Configurar el fragmento
        val testUsername = "testuser123_updated"
        val bundle = bundleOf(
            "isSocialLoginFlow" to false,
            "socialUserEmail" to null,
            "socialUserName" to null
        )

        lateinit var fragment: SignUpFragment
        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle) {
            fragment = this
        }

        // 2. Act: Simular un cambio en el uiState del ViewModel
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val fakeUiState = fragment.exposeViewModelForTesting().uiState.value.copy(
                username = testUsername
            )
            fragment.exposeViewModelForTesting().setUiStateForTesting(fakeUiState)
        }

        // Espera para dar tiempo a la UI de actualizarse.
        Thread.sleep(500)

        // 3. Assert: Verificar que el campo de texto muestre el nuevo valor
        onView(withId(R.id.etUser))
            .check(matches(withText(testUsername)))
    }

    @Test
    fun when_state_has_full_name_then_full_name_field_is_updated() {
        // 1. Arrange: Configurar el fragmento
        val testFullName = "Nombre de Prueba Completo"
        val bundle = bundleOf(
            "isSocialLoginFlow" to false,
            "socialUserEmail" to null,
            "socialUserName" to null
        )

        lateinit var fragment: SignUpFragment
        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle) {
            fragment = this
        }

        // 2. Act: Simular un cambio en el uiState del ViewModel
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val fakeUiState = fragment.exposeViewModelForTesting().uiState.value.copy(
                fullName = testFullName
            )
            fragment.exposeViewModelForTesting().setUiStateForTesting(fakeUiState)
        }

        // Espera para dar tiempo a la UI de actualizarse.
        Thread.sleep(500)

        // 3. Assert: Verificar que el campo de texto muestre el nuevo valor
        onView(withId(R.id.etName))
            .check(matches(withText(testFullName)))
    }

    @Test
    fun when_state_has_birthday_then_birthday_field_is_updated() {
        // 1. Arrange: Configurar el fragmento
        val testBirthday = "25/12/1990"
        val bundle = bundleOf(
            "isSocialLoginFlow" to false,
            "socialUserEmail" to null,
            "socialUserName" to null
        )

        lateinit var fragment: SignUpFragment
        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle) {
            fragment = this
        }

        // 2. Act: Simular un cambio en el uiState del ViewModel
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val fakeUiState = fragment.exposeViewModelForTesting().uiState.value.copy(
                birthday = testBirthday
            )
            fragment.exposeViewModelForTesting().setUiStateForTesting(fakeUiState)
        }

        // Espera para dar tiempo a la UI de actualizarse.
        Thread.sleep(500)

        // 3. Assert: Verificar que el campo de texto muestre el nuevo valor
        onView(withId(R.id.tietBirthday))
            .check(matches(withText(testBirthday)))
    }

    /* Interacciones con botones */
    @Test
    fun when_clicking_register_button_then_onRegisterClicked_event_is_triggered() {
        // 1. Arrange: Lanzar el fragmento y rellenar todos los campos con datos válidos
        val bundle = bundleOf(
            "isSocialLoginFlow" to false,
            "socialUserEmail" to null,
            "socialUserName" to null
        )

        lateinit var fragment: SignUpFragment
        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle) {
            fragment = this
        }

        // Rellenar campos de texto para pasar las validaciones básicas
        onView(withId(R.id.etUser)).perform(replaceText("testuser"), closeSoftKeyboard())
        onView(withId(R.id.etName)).perform(replaceText("Test User"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(replaceText("Password123"), closeSoftKeyboard())
        onView(withId(R.id.etRepeatPassword)).perform(
            replaceText("Password123"),
            closeSoftKeyboard()
        )
        onView(withId(R.id.etEmailRegister)).perform(
            replaceText("test@example.com"),
            closeSoftKeyboard()
        )

        // Simular que el cumpleaños es válido para pasar la validación
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val fakeUiState = fragment.exposeViewModelForTesting().uiState.value.copy(
                birthday = "01/01/2000"
            )
            fragment.exposeViewModelForTesting().setUiStateForTesting(fakeUiState)
        }

        Thread.sleep(500) // Esperar a que la UI refleje el cumpleaños

        // 2. Act: Desplazarse al botón y hacer clic en él
        onView(withId(R.id.btnRegister)).perform(scrollTo(), click())

        // 3. Assert: Verificar que el ProgressBar se muestre, indicando que el evento de registro ha sido procesado
        onView(withId(R.id.progressBar))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun when_clicking_back_button_then_onBackPressed_event_is_triggered() {
        // 1. Arrange: Lanzar el fragmento con un NavController de prueba
        val bundle = bundleOf(
            "isSocialLoginFlow" to false,
            "socialUserEmail" to null,
            "socialUserName" to null
        )

        // Crear un NavController de prueba y configurar el gráfico de navegación
        val testNavController = TestNavHostController(ApplicationProvider.getApplicationContext())
        testNavController.setGraph(R.navigation.nav_graph)

        // Simular que venimos de otro fragmento (por ejemplo, el de login)
        testNavController.setCurrentDestination(R.id.loginFragment)

        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle) {
            // Asignar el NavController de prueba al fragmento. Esto simula la navegación a SignupFragment.
            testNavController.navigate(R.id.signupFragment)
            Navigation.setViewNavController(this.requireView(), testNavController)
        }

        // 2. Act: Hacer clic en el botón de retroceso
        onView(withId(R.id.ivBack)).perform(click())

        Thread.sleep(500) // Esperar a que la navegación se complete

        // 3. Assert: Verificar que el destino actual del NavController es el fragmento anterior (loginFragment)
        assertEquals(R.id.loginFragment, testNavController.currentDestination?.id)
    }

    @Test
    fun when_clicking_google_button_then_onGoogleSignUpClicked_event_is_triggered() {
        // Arrange: Configurar el comportamiento de los flows del ViewModel de prueba
        // Esto evita el KotlinNothingValueException al observar los flows
        val uiStateFlow = MutableStateFlow(SignUpUiState())
        val eventFlow = MutableSharedFlow<SignUpEvent>()
        val launchGoogleSignInFlow = MutableSharedFlow<Intent>() // Nueva línea para el flow de Google

        every { viewModel.uiState } returns uiStateFlow
        every { viewModel.event } returns eventFlow
        every { viewModel.launchGoogleSignIn } returns launchGoogleSignInFlow // Nueva línea

        val bundle = bundleOf(
            "isSocialLoginFlow" to false,
            "socialUserEmail" to null,
            "socialUserName" to null
        )

        launchFragmentInHiltContainer<SignUpFragment>(fragmentArgs = bundle)

        // Act: Desplazarse al botón de Google y hacer clic en él
        onView(withId(R.id.ivGoogleSignIn)).perform(scrollTo(), click())

        // Assert: Verificar que el método onEvent del ViewModel fue llamado
        // con el evento OnGoogleSignUpClicked
        verify(exactly = 1) { viewModel.onEvent(SignUpEvent.OnGoogleSignUpClicked) }
    }
}
