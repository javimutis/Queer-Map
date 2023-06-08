package com.cursoandroid.queermap.interfaces

interface CoverContract {
    interface View {
        fun showTitle()
        fun navigateToLogin()
        fun navigateToSignin()
    }

    interface Presenter {
        fun start()
        fun onLoginButtonClicked()
        fun onSigninButtonClicked()
    }
}
