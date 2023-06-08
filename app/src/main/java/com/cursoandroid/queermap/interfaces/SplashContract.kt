package com.cursoandroid.queermap.interfaces

interface SplashContract {
    interface View {
        fun navigateToNextActivity()
    }

    interface Presenter {
        fun start()
    }
}
