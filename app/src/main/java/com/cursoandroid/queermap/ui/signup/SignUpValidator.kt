package com.cursoandroid.queermap.ui.signup

import android.util.Patterns

object SignUpValidator {


    private const val PASSWORD_MIN_LENGTH = 8

    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= PASSWORD_MIN_LENGTH
    }

    fun isStrongPassword(password: String): Boolean {
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=])(?=\\S+\$).{8,}$"
        return password.matches(passwordRegex.toRegex())
    }

    fun isValidUser(user: String): Boolean {
        return user.isNotBlank()
    }

    fun isValidUsername(username: String): Boolean {
        return username.isNotBlank()
    }

    fun isValidBirthday(birthday: String): Boolean {
        return birthday.isNotBlank()
    }
}