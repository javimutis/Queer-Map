package com.cursoandroid.queermap.ui.main

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.util.EspressoIdlingResource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class MainActivityTest {

    // Regla que configura Hilt antes de correr los tests
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    // Regla que inicia MainActivity antes de cada test
    @get:Rule(order = 1)
    var mainActivityRule = ActivityScenarioRule(MainActivity::class.java)

    // Este método se ejecuta antes de cada test
    @Before
    fun setUp() {
        // Inyecta las dependencias con Hilt
        hiltRule.inject()
        // Registra el IdlingResource antes de que comience la actividad.
        // Es CRUCIAL para que Espresso espere las operaciones asíncronas
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    // Este método se ejecuta después de cada test
    @After
    fun tearDown() {
        // Asegúrate de desregistrar el IdlingResource después de cada prueba.
        // Esto evita que las pruebas se afecten entre sí.
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    // TEST 1: Verificar que el NavHostFragment se muestra correctamente
    @Test
    fun when_main_activity_is_launched_then_nav_host_fragment_is_displayed() {
        // Espresso esperará por el IdlingResource (del CoverViewModel)
        // hasta que el delay finalice y el contador GLOBAL llegue a 0.
        onView(withId(R.id.nav_host_fragment)).check(matches(isDisplayed()))
    }

    // TEST 2: Verificar que el CoverFragment es el primer fragmento visible y sus elementos clave están en pantalla.
    @Test
    fun when_main_activity_is_launched_then_cover_fragment_elements_are_displayed() {
        // Esperamos a que el IdlingResource indique que el CoverFragment está listo (delay del ViewModel)
        // Verificamos que el NavHostFragment está en pantalla
        onView(withId(R.id.nav_host_fragment)).check(matches(isDisplayed()))

        // Verificamos elementos específicos del CoverFragment
        onView(withId(R.id.btnCoverLogin)).check(matches(isDisplayed()))
        onView(withId(R.id.btnCoverSignIn)).check(matches(isDisplayed()))
        onView(withId(R.id.tvTitle)).check(matches(isDisplayed()))
    }

    // TEST 3: Verificar la navegación del CoverFragment al LoginFragment al hacer clic en el botón de inicio de sesión
    @Test
    fun when_login_button_is_clicked_then_navigate_to_login_fragment() {
        // Primero, esperamos a que el CoverFragment esté listo (gracias al IdlingResource)
        // y verificamos que el botón de Login es visible.
        onView(withId(R.id.btnCoverLogin)).check(matches(isDisplayed()))

        // Realizamos el clic en el botón de inicio de sesión
        onView(withId(R.id.btnCoverLogin)).perform(click())

        // Ahora, verificamos que el LoginFragment se muestra
        onView(withId(R.id.etEmailLogin)).check(matches(isDisplayed()))

        onView(withText(R.string.login_title)).check(matches(isDisplayed()))
    }

    // TEST 4: Verificar la navegación del CoverFragment al SignUpFragment al hacer clic en el botón de registro
    @Test
    fun when_signup_button_is_clicked_then_navigate_to_signup_fragment() {
        // Primero, esperamos a que el CoverFragment esté listo y verificamos el botón de registro.
        onView(withId(R.id.btnCoverSignIn)).check(matches(isDisplayed()))

        // Realizamos el clic en el botón de registro
        onView(withId(R.id.btnCoverSignIn)).perform(click())

        // Verificamos que el SignUpFragment se muestra
        onView(withId(R.id.etName)).check(matches(isDisplayed()))
        onView(withText(R.string.register_title)).check(matches(isDisplayed()))
    }

}