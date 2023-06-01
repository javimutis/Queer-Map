package com.cursoandroid.queermap.utils

import android.util.Patterns

/**
 * This object contains utility methods for validation.
 */
object ValidationUtils {
    private const val PASSWORD_MIN_LENGTH = 6

    /**
     * Checks if the given email is valid.
     *
     * @param email The email to be validated.
     * @return True if the email is valid, false otherwise.
     */
    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Checks if the given password is valid.
     *
     * @param password The password to be validated.
     * @return True if the password is valid, false otherwise.
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= PASSWORD_MIN_LENGTH
    }

    /**
     * Checks if the given name is valid for sign-up.
     *
     * @param name The name to be validated.
     * @return True if the name is valid, false otherwise.
     */
    fun isValidSignName(name: String): Boolean {
        return name.isNotEmpty()
    }

    /**
     * Checks if the given username is valid for sign-up.
     *
     * @param username The username to be validated.
     * @return True if the username is valid, false otherwise.
     */
    fun isValidSignUsername(username: String): Boolean {
        return username.isNotEmpty()
    }

    /**
     * Checks if the given email is valid for sign-up.
     *
     * @param email The email to be validated.
     * @return True if the email is valid, false otherwise.
     */
    fun isValidSignEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Checks if the given password is valid for sign-up.
     *
     * @param password The password to be validated.
     * @return True if the password is valid, false otherwise.
     */
    fun isValidSignPassword(password: String): Boolean {
        return password.length >= 8
    }

    /**
     * Checks if the given password is strong for sign-up.
     * A strong password must contain at least one digit, one lowercase letter,
     * one uppercase letter, one special character, and have a minimum length of 8 characters.
     *
     * @param password The password to be validated.
     * @return True if the password is strong, false otherwise.
     */
    fun isStrongSignPassword(password: String): Boolean {
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"
        return password.length >= PASSWORD_MIN_LENGTH && password.matches(passwordRegex.toRegex())
    }

    /**
     * Checks if the given birthday is valid for sign-up.
     *
     * @param birthday The birthday to be validated.
     * @return True if the birthday is valid, false otherwise.
     */
    fun isValidSignBirthday(birthday: String): Boolean {
        return birthday.isNotEmpty()
    }
}
