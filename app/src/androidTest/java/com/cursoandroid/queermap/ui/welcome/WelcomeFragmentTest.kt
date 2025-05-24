package com.cursoandroid.queermap.ui.welcome

// Importaciones necesarias para hacer los testing con Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.withId

// Reglas y soporte para test con Activities
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4

// Importamos la activity que vamos a testear
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.ui.cover.CoverFragment
import com.cursoandroid.queermap.ui.main.MainActivity

// Importamos lo necesario para integrar Hilt con nuestros tests
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest

// Importaciones para JUnit
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class WelcomeFragmentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        hiltRule.inject()
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun when_welcome_state_is_true_then_navigate_to_cover() {
        // Asume que la vista con ID action_welcome_to_cover será llamada cuando el estado cambie
        onView(withId(R.id.action_welcome_to_cover)).perform(click())

        // Verifica que la navegación a `CoverFragment` se haya realizado
        intended(hasComponent(CoverFragment::class.java.name))
    }
}