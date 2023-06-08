package com.cursoandroid.queermap.presenter

import com.cursoandroid.queermap.interfaces.CoverContract

class CoverPresenter(private val view: CoverContract.View) : CoverContract.Presenter {
    override fun start() {
        view.showTitle()
    }

    override fun onLoginButtonClicked() {
        view.navigateToLogin()
    }

    override fun onSigninButtonClicked() {
        view.navigateToSignin()
    }
}
