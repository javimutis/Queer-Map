package com.cursoandroid.queermap // Asegúrate que este paquete sea correcto para tus tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue // Añade esto para un assert simple

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HiltSmokeTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Test
    fun testInjection() {
        hiltRule.inject()
        // Si la inyección ocurre sin errores, el test debería pasar.
        assertTrue(true) // Asegura que el test sea verde
    }
}