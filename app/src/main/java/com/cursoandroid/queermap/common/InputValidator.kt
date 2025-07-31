package com.cursoandroid.queermap.common

import android.util.Patterns
import javax.inject.Inject

class InputValidator @Inject constructor() {
    companion object {
               private const val PASSWORD_MIN_LENGTH = 6
    }

    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= PASSWORD_MIN_LENGTH
    }

    fun isStrongPassword(password: String): Boolean {
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=])(?=\\S+\$).{8,}$"
        return password.matches(passwordRegex.toRegex())
    }

    fun isValidUsername(username: String): Boolean {
        return username.isNotBlank()
    }

    fun isValidFullName(fullName: String): Boolean {
        return fullName.isNotBlank()
    }

    fun isValidBirthday(birthday: String): Boolean {
        return birthday.isNotBlank()
    }
}