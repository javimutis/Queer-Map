package com.cursoandroid.queermap.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.R

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DURATION = 3500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler().postDelayed({
            navigateToNextActivity()
        }, SPLASH_DURATION)
    }

    private fun navigateToNextActivity() {
        val intent = Intent(this, CoverActivity::class.java)
        startActivity(intent)

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

        finish()
    }
}
