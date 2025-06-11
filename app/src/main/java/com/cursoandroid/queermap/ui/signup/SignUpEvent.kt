package com.cursoandroid.queermap.ui.signup

import android.content.Intent

sealed class SignUpEvent {
    data class OnUsernameChanged(val username: String) : SignUpEvent() // Renombrado de 'user' a 'username'
    data class OnEmailChanged(val email: String) : SignUpEvent()
    data class OnPasswordChanged(val password: String) : SignUpEvent()
    data class OnConfirmPasswordChanged(val confirmPassword: String) : SignUpEvent()
    data class OnFullNameChanged(val fullName: String) : SignUpEvent()
    data class OnBirthdayChanged(val birthday: String) : SignUpEvent()
    object OnRegisterClicked : SignUpEvent()

    object OnGoogleSignUpClicked : SignUpEvent()
    object OnFacebookSignUpClicked : SignUpEvent()
    data class OnGoogleSignInResult(val data: Intent?) : SignUpEvent()
    data class OnFacebookActivityResult(val requestCode: Int, val resultCode: Int, val data: Intent?) : SignUpEvent()

    object NavigateToHome : SignUpEvent()
    object NavigateBack : SignUpEvent()
    data class ShowMessage(val message: String) : SignUpEvent()
}