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
            navigateToNextActivity() // Open the next activity after the splash duration
        }, SPLASH_DURATION.toLong())
    }

    // Open the next activity and apply transition animation
    private fun navigateToNextActivity() {
        val intent = Intent(this, CoverActivity::class.java)
        startActivity(intent)

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out) // Apply fade-in and fade-out animation

        finish() // Finish the current activity to prevent going back to the splash screen
    }
}
