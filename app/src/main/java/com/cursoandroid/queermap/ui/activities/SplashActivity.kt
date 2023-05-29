package com.cursoandroid.queermap.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.MainActivity
import com.cursoandroid.queermap.R
class SplashActivity : AppCompatActivity() {

    private val SPLASH_DURATION = 3500 // Duration of the splash animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Start the splash animation
        Handler().postDelayed({

            // Open the next activity
            val intent = Intent(this, CoverActivity::class.java)
            startActivity(intent)

            // Apply transition animation
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

            // Finish the current activity
            finish()
        }, SPLASH_DURATION.toLong())
    }
}
