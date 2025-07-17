package com.cursoandroid.queermap

import android.app.Application
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import dagger.hilt.android.HiltAndroidApp // <-- ¡Esta línea es crucial!

@HiltAndroidApp
class QueerMapApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeFacebookSdk()
    }

    private fun initializeFacebookSdk() {
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)
    }
}