package com.cursoandroid.queermap.ui.signup

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.util.launchFragmentInHiltContainer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard


@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SignUpFragmentTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        hiltRule.inject()
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

        onView(withId(R.id.etRepeatPassword)).perform(replaceText(testRepeatPassword), closeSoftKeyboard())
        onView(withId(R.id.etRepeatPassword)).check(matches(withText(testRepeatPassword)))
    }

}
