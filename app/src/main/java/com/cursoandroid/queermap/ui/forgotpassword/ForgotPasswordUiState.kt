package com.cursoandroid.queermap.ui.forgotpassword

data class ForgotPasswordUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
    // 'message' se movió a ForgotPasswordEvent
)