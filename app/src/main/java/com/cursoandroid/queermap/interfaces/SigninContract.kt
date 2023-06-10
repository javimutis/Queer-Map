package com.cursoandroid.queermap.interfaces

import android.widget.ImageView
import com.facebook.AccessToken

interface SigninContract {
    interface View {
        fun validateAndShowTermsPopup()
        fun showErrorPopup(input: ImageView, errorMessage: String)
        fun onBackPressed()
        fun showDatePickerDialog()
        fun handleFacebookAccessToken(token: AccessToken)
        fun navigateToMapActivity()
        fun showTermsPopup()
        fun showReadTermsPopup()
        fun signInWithGoogle()
        fun signInWithFacebook()
    }

    interface Presenter {
        fun onCreate()
        fun onRegisterButtonClick()
        fun onBackButtonClick()
        fun onGoogleSignInButtonClick()
        fun onFacebookSignInButtonClick()
    }
}
