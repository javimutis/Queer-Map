package com.cursoandroid.queermap

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HiltSmokeTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Test
    fun testInjection() {
        hiltRule.inject()
        org.junit.Assert.assertTrue(true)
    }
}