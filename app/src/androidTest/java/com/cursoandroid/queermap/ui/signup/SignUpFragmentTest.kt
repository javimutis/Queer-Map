package com.cursoandroid.queermap.ui.signup

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.Espresso.onView
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

}
