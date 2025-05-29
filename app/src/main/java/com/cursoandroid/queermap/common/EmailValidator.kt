package com.cursoandroid.queermap.common

import java.util.regex.Pattern

object EmailValidator {
    private val EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-]+@[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9]\\.[a-zA-Z]{2,}"
    )

    fun isValidEmail(email: String): Boolean {
        return EMAIL_PATTERN.matcher(email).matches()
    }
}