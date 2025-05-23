package com.cursoandroid.queermap.ui.login

sealed class LoginEvent {
    object NavigateToForgotPassword : LoginEvent()
    object NavigateBack : LoginEvent()
    object NavigateToHome : LoginEvent()
    data class ShowMessage(val message: String) : LoginEvent()
}