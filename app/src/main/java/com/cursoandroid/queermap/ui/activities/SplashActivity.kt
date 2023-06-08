package com.cursoandroid.queermap.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.MainActivity
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.interfaces.SplashContract
import com.cursoandroid.queermap.presenter.SplashPresenter

class SplashActivity : AppCompatActivity(), SplashContract.View {

    private lateinit var presenter: SplashContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        presenter = SplashPresenter(this)
        presenter.start()
    }

    override fun navigateToNextActivity() {
        val intent = Intent(this, CoverActivity::class.java)
        startActivity(intent)

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

        finish()
    }
}
