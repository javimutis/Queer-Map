package com.cursoandroid.queermap.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.R
import com.squareup.picasso.Picasso

class CoverActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cover)

        // Set background image using Picasso library
        val coverImage: ImageView = findViewById(R.id.cover)
        Picasso.get().load(R.drawable.cover_background).into(coverImage)

        // Hide the title text view initially
        val titleTextView: TextView = findViewById(R.id.titleTextView)
        titleTextView.visibility = View.INVISIBLE

        // Delayed animation to reveal the title text view
        titleTextView.postDelayed({
            val revealAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
            titleTextView.visibility = View.VISIBLE
            titleTextView.startAnimation(revealAnimation)
        }, 1300)

        // Set click listeners for login and sign-in buttons
        val loginButton: Button = findViewById(R.id.cover_login_button)
        val signinButton: Button = findViewById(R.id.cover_signin_button)

        loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        signinButton.setOnClickListener {
            val intent = Intent(this, SigninActivity::class.java)
            startActivity(intent)
        }
    }
}