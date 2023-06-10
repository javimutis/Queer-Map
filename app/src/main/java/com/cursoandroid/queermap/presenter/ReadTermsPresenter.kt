package com.cursoandroid.queermap.presenter

import com.cursoandroid.queermap.interfaces.ReadTermsContract

class ReadTermsPresenter(private val view: ReadTermsContract.View) : ReadTermsContract.Presenter {

    override fun onAcceptButtonClicked() {
        view.navigateToMapActivity()
    }

    override fun onCancelButtonClicked() {
        view.closeView()
    }

    override fun onTermsAndConditionsClicked() {
        view.showTermsPopup()
    }
}
