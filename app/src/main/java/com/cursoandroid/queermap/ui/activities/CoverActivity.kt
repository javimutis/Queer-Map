package com.cursoandroid.queermap.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.interfaces.CoverContract
import com.cursoandroid.queermap.presenter.CoverPresenter

class CoverActivity : AppCompatActivity(), CoverContract.View {

    private lateinit var presenter: CoverContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cover)

        presenter = CoverPresenter(this)
        presenter.start()

        val loginButton: Button = findViewById(R.id.cover_login_button)
        val signinButton: Button = findViewById(R.id.cover_signin_button)

        loginButton.setOnClickListener {
            presenter.onLoginButtonClicked()
        }

        signinButton.setOnClickListener {
            presenter.onSigninButtonClicked()
        }
    }

    override fun showTitle() {
        val titleTextView: TextView = findViewById(R.id.titleTextView)
        titleTextView.visibility = View.INVISIBLE

        titleTextView.postDelayed({
            val revealAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
            titleTextView.visibility = View.VISIBLE
            titleTextView.startAnimation(revealAnimation)
        }, 1300)
    }

    override fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    override fun navigateToSignin() {
        val intent = Intent(this, SigninActivity::class.java)
        startActivity(intent)
    }
}
