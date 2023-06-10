package com.cursoandroid.queermap.interfaces

interface ReadTermsContract {

    interface View {
        fun navigateToMapActivity()
        fun closeView()
        fun showTermsPopup()
    }

    interface Presenter {
        fun onAcceptButtonClicked()
        fun onCancelButtonClicked()
        fun onTermsAndConditionsClicked()
    }
}
