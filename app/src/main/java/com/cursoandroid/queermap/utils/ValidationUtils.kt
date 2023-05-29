package com.cursoandroid.queermap.utils

import android.util.Patterns

object ValidationUtils {
    private const val PASSWORD_MIN_LENGTH = 6

    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= PASSWORD_MIN_LENGTH
    }

    fun isValidSignName(name: String): Boolean {
        return name.isNotEmpty()
    }

    fun isValidSignUsername(username: String): Boolean {
        return username.isNotEmpty()
    }

    fun isValidSignEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidSignPassword(password: String): Boolean {
        return password.length >= 8
    }

    fun isStrongSignPassword(password: String): Boolean {
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"
        return password.matches(passwordRegex.toRegex())
    }

    fun isValidSignBirthday(birthday: String): Boolean {
        return birthday.isNotEmpty()
    }
}
