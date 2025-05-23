package com.cursoandroid.queermap.ui.login

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val isEmailInvalid: Boolean = false,
    val isPasswordInvalid: Boolean = false,
    val email: String? = null,
    val password: String? = null
)