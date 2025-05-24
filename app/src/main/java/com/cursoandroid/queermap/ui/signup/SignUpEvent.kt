package com.cursoandroid.queermap.ui.signup

sealed class SignUpEvent {
    object NavigateToHome : SignUpEvent()
    object NavigateBack : SignUpEvent()
    data class ShowMessage(val message: String) : SignUpEvent()
}
