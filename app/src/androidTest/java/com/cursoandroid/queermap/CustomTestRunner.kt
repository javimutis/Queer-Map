package com.cursoandroid.queermap // THIS PACKAGE MUST BE THE applicationID

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class CustomTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        // This line is correct and crucial for Hilt to use its test application.
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}