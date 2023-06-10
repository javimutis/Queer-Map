package com.cursoandroid.queermap.presenter

import com.cursoandroid.queermap.interfaces.SigninContract

class SigninPresenter(private val view: SigninContract.View) : SigninContract.Presenter {
    override fun onCreate() {
        // Realiza cualquier inicialización necesaria en la vista
    }

    override fun onRegisterButtonClick() {
        view.validateAndShowTermsPopup()
    }

    override fun onBackButtonClick() {
        view.onBackPressed()
    }

    override fun onGoogleSignInButtonClick() {
        view.signInWithGoogle()
    }

    override fun onFacebookSignInButtonClick() {
        view.signInWithFacebook()
    }
}

