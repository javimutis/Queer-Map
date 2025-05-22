package com.cursoandroid.queermap.ui.forgotpassword

object ForgotPasswordValidator {
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
