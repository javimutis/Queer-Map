package com.cursoandroid.queermap.ui.signup

sealed class SignUpEvent {
    data class OnUserChanged(val user: String) : SignUpEvent()
    data class OnEmailChanged(val email: String) : SignUpEvent()
    data class OnPasswordChanged(val password: String) : SignUpEvent()
    data class OnConfirmPasswordChanged(val confirmPassword: String) : SignUpEvent()
    object OnRegisterClicked : SignUpEvent()
    object NavigateToHome : SignUpEvent()
    object NavigateBack : SignUpEvent()
    data class ShowMessage(val message: String) : SignUpEvent()
}
