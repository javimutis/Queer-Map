package com.cursoandroid.queermap.ui.login

import androidx.navigation.NavDirections

sealed class LoginEvent {
    object NavigateToForgotPassword : LoginEvent()
    object NavigateBack : LoginEvent()
    object NavigateToHome : LoginEvent()
    data class NavigateToSignupWithArgs(
        val socialUserEmail: String?,
        val socialUserName: String?,
        val isSocialLoginFlow: Boolean
    ) : LoginEvent()
    data class ShowMessage(val message: String) : LoginEvent()
}