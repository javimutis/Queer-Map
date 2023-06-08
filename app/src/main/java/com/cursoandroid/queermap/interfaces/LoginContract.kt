package com.cursoandroid.queermap.interfaces

import android.content.Intent

interface LoginContract {
    interface View {
        fun getGoogleSignInIntent(): Intent
        fun showGoogleSignInIntent(signInIntent: Intent)
        fun showGoogleSignInError()
        fun showTermsActivity()
        fun showGoogleSignInErrorMessage()
        fun showTermsScreen()
        fun showLoginError()
        fun showInvalidCredentialsError()
        fun showSigningInMessage()
        fun showSignInSuccess()
        fun showSignInError()
        fun showPasswordResetEmailSent(email: String)
        fun showPasswordResetEmailError()
        fun goToSignInActivity()
    }

    interface Presenter {
        fun onGoogleSignInButtonClicked()
        fun handleGoogleSignInResult(data: Intent?)
        fun onFacebookLoginClicked()
        fun onFacebookLoginSuccess()
        fun onFacebookLoginCancel()
        fun onFacebookLoginError()
        fun onLoginButtonClick(email: String, password: String)
        fun onForgotPasswordClick(email: String)
        fun onGoToSignInActivityClick()
    }
}
