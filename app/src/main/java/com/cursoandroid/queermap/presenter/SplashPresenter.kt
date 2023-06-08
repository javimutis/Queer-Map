package com.cursoandroid.queermap.presenter

import android.os.Handler
import com.cursoandroid.queermap.interfaces.SplashContract

class SplashPresenter(private val view: SplashContract.View) : SplashContract.Presenter {
    private val SPLASH_DURATION = 3500

    override fun start() {
        Handler().postDelayed({
            view.navigateToNextActivity()
        }, SPLASH_DURATION.toLong())
    }
}
