package com.cursoandroid.queermap

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        // Aseguramos que HiltTestApplication se usa para el test
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}