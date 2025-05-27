package com.cursoandroid.queermap.ui.signup

data class SignUpUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isEmailInvalid: Boolean = false,
    val isPasswordInvalid: Boolean = false,
    val doPasswordsMismatch: Boolean = false,
    val errorMessage: String? = null,
    val email: String? = null,
    val password: String? = null,
    val confirmPassword: String? = null,
    val name: String? = null
)
