// Queermap(androidTest)/ui/main/MainActivityTest.kt
package com.cursoandroid.queermap.ui.main

import android.app.Activity
import android.content.Intent
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.util.EspressoIdlingResource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class MainActivityTest {

    // Regla de Hilt para inyección de dependencias en tests
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    // Regla para lanzar la actividad bajo prueba
    @get:Rule(order = 1)
    var mainActivityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        hiltRule.inject()
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        // Inicializa Intents para verificar interacciones con otras actividades
        Intents.init()
    }

    @After
    fun tearDown() {
        // Desregistra el IdlingResource para evitar fugas de memoria o interferencias con otros tests
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        // Libera Intents
        Intents.release()
    }

    // Test 1: Verificar que el NavHostFragment se muestra correctamente al inicio
    @Test
    fun whenMainActivityIsLaunched_navHostFragmentIsDisplayed() {
        // Espresso esperará automáticamente a que el IdlingResource se ponga inactivo
        onView(withId(R.id.nav_host_fragment))
            .check(matches(isDisplayed()))
    }

    // Test 2: Verificar que el CoverFragment es el destino inicial y sus botones son visibles
    @Test
    fun whenMainActivityIsLaunched_coverFragmentIsDisplayedWithButtons() {
        // Asegúrate de que el NavHostFragment esté visible
        onView(withId(R.id.nav_host_fragment))
            .check(matches(isDisplayed()))

        // Verifica que los botones del CoverFragment estén visibles.
        // Esto confirma que la navegación al CoverFragment fue exitosa.
        onView(withId(R.id.btnCoverLogin)) // Suponiendo que este es el ID del botón de Login en fragment_cover.xml
            .check(matches(isDisplayed()))
        onView(withId(R.id.btnCoverSignIn)) // Suponiendo que este es el ID del botón de Sign In en fragment_cover.xml
            .check(matches(isDisplayed()))
    }

    // Test 3: Verificar que el título del CoverFragment se muestra después del delay
    @Test
    fun whenCoverFragmentIsDisplayed_titleAppearsAfterDelay() {
        // Espresso ya esperará por el IdlingResource del CoverViewModel,
        // así que el título debería estar visible cuando se ejecuta la aserción.
        onView(withId(R.id.tvTitle)) // Suponiendo que tvTitle es el ID del TextView del título en fragment_cover.xml
            .check(matches(isDisplayed()))
        // Opcional: También puedes verificar el texto si lo deseas.
        // .check(matches(withText(R.string.tu_titulo_de_cover)))
    }

    // Test 4: Simular el click en el botón de Login y verificar la navegación
    @Test
    fun clickLoginButton_navigatesToLoginFragment() {
        // Asegúrate de que el CoverFragment esté listo
        onView(withId(R.id.btnCoverLogin)).check(matches(isDisplayed()))

        // Realiza el click en el botón de Login
        onView(withId(R.id.btnCoverLogin)).perform(click())

        // Verifica que el LoginFragment está ahora visible buscando un elemento único en él
        // (por ejemplo, un campo de email o un botón de login específico de ese fragmento).
        onView(withId(R.id.etEmail)) // Suponiendo que etEmail es el ID del campo de email en fragment_login.xml
            .check(matches(isDisplayed()))
        onView(withId(R.id.btnLogin)) // Suponiendo que btnLogin es el ID del botón de Login en fragment_login.xml
            .check(matches(isDisplayed()))
    }

    // Test 5: Simular el click en el botón de Sign Up y verificar la navegación
    @Test
    fun clickSignUpButton_navigatesToSignUpFragment() {
        // Asegúrate de que el CoverFragment esté listo
        onView(withId(R.id.btnCoverSignIn)).check(matches(isDisplayed()))

        // Realiza el click en el botón de Sign Up
        onView(withId(R.id.btnCoverSignIn)).perform(click())

        // Verifica que el SignUpFragment está ahora visible buscando un elemento único en él
        // (por ejemplo, un campo de confirmación de contraseña o un botón de registro).
        onView(withId(R.id.etRepeatPassword)) // Suponiendo que etConfirmPassword es el ID en fragment_signup.xml
            .check(matches(isDisplayed()))
        onView(withId(R.id.btnRegister)) // Suponiendo que btnSignUp es el ID del botón de registro en fragment_signup.xml
            .check(matches(isDisplayed()))
    }

    // Test 6: Verificar el comportamiento de onSupportNavigateUp() (cuando se presiona el botón Atrás)
    // Este test puede ser más complejo dependiendo de tu implementación de la barra de acción/toolbar.
    // Para fragmentos, `findNavController().navigateUp()` es el encargado.
    // Aquí simularemos el botón de atrás del sistema si no hay un botón de "volver" visible.
    @Test
    fun pressingBackButton_navigatesBackFromLoginToCover() {
        // Navega al LoginFragment primero
        onView(withId(R.id.btnCoverLogin)).perform(click())
        onView(withId(R.id.etEmail)).check(matches(isDisplayed())) // Confirma que estamos en Login

        // Presiona el botón de atrás del sistema
        Espresso.pressBack()

        // Verifica que hemos vuelto al CoverFragment buscando un elemento de CoverFragment
        onView(withId(R.id.btnCoverLogin)).check(matches(isDisplayed()))
        // Y asegurándonos de que un elemento del LoginFragment ya NO esté visible
        try {
            onView(withId(R.id.etEmail)).check(matches(isDisplayed()))
            fail("LoginFragment is still displayed after pressing back.")
        } catch (e: NoMatchingViewException) {
            // Expected: etEmail should not be displayed
        }
    }

    // Test 7: Verificar el manejo de onActivityResult para Facebook CallbackManager
    // Esto es un test más avanzado y requiere mocking o una configuración específica de Intents.
    // Para simplificar, verificaremos que no hay un crash si se recibe un onActivityResult.
    // Nota: Mockear Facebook SDK es complejo. Este es un test de "sanity check".
    @Test
    fun onActivityResult_doesNotCrashWithFacebookCallback() {
        // Puedes simular que MainActivity está en primer plano y luego enviar un resultado.
        // Esto requiere una simulación de intent un poco más detallada.
        // Aquí, solo verificamos que la actividad se lanza y permanece estable.
        // La prueba más robusta para esto sería una integración test de la autenticación de Facebook.
        // Para fines de MainActivityTest, simplemente asegurar que la actividad no se bloquea
        // al pasar por onActivityResult.

        // Dado que facebookCallbackManager es una dependencia inyectada por Hilt,
        // en un test unitario (o un test de integración más enfocado) podríamos mockearla.
        // En este test de interfaz de usuario de extremo a extremo, es difícil verificar su comportamiento interno.
        // Sin embargo, podemos verificar que la Activity sigue viva después de un supuesto onActivityResult.

        // Simular un onActivityResult simple (esto no ejecutará el callback de Facebook real,
        // solo verifica que el método no causa un crash en la Activity)
        mainActivityRule.scenario.onActivity { activity ->
            activity.onActivityResult(123, Activity.RESULT_OK, Intent())
        }

        // Si la actividad no se bloquea, el test continuará y las aserciones anteriores deberían seguir siendo válidas.
        onView(withId(R.id.nav_host_fragment)).check(matches(isDisplayed()))
    }
}