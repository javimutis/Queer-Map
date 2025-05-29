package com.cursoandroid.queermap.ui.forgotpassword

import com.cursoandroid.queermap.common.EmailValidator // Importa el nuevo validador

object ForgotPasswordValidator {
    fun isValidEmail(email: String): Boolean {
        return EmailValidator.isValidEmail(email)
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
}