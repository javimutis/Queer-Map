package com.cursoandroid.queermap.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import com.cursoandroid.queermap.R

class ReadTermsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.read_terms_layout)

        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            navigateBackToLogin()
        }
    }

    private fun navigateBackToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
