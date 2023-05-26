package com.cursoandroid.queermap.ui.activities

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
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

        val coverImage: ImageView = findViewById(R.id.cover)
        Picasso.get().load(R.drawable.cover_background).into(coverImage)

        val titleTextView: TextView = findViewById(R.id.titleTextView)
        titleTextView.visibility = View.INVISIBLE
        titleTextView.postDelayed({
            val revealAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
            titleTextView.visibility = View.VISIBLE
            titleTextView.startAnimation(revealAnimation)
        }, 1000)

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