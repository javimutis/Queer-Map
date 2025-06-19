package com.cursoandroid.queermap.common

import android.util.Patterns
import javax.inject.Inject

class InputValidator @Inject constructor() {
    companion object {
        // AJUSTADO A 6 para coincidir con el mensaje de error del LoginViewModel
        // y con la validación mínima esperada en el contexto de login.
        private const val PASSWORD_MIN_LENGTH = 6
    }

    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= PASSWORD_MIN_LENGTH
    }

    fun isStrongPassword(password: String): Boolean {
        // Nota: Esta regex actual espera 8 caracteres.
        // Si PASSWORD_MIN_LENGTH es 6, hay una inconsistencia si usas esta para validación mínima.
        // Decide si necesitas una contraseña "fuerte" con más requisitos y más longitud.
        // Para el login, usualmente solo isValidPassword es suficiente.
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