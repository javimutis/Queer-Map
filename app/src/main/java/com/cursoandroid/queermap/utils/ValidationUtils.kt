package com.cursoandroid.queermap.utils

/* Una clase de utilidad donde puedes implementar funciones para validar los campos de entrada, como el formato del correo electrónico y la fortaleza de la contraseña. */
object ValidationUtils {
    private const val PASSWORD_MIN_LENGTH = 6

    fun isValidEmail(email: String): Boolean {
        val emailRegex = "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
        return email.matches(emailRegex.toRegex())
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= PASSWORD_MIN_LENGTH && isStrongPassword(password)
    }

    fun isStrongPassword(password: String): Boolean {
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"
        return password.matches(passwordRegex.toRegex())
    }
}
