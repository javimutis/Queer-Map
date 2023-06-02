package com.cursoandroid.queermap.ui.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.R

class ReadTermsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView (R.layout.read_terms_layout)
    }

    fun closePopup(view: View) {
        finish()
    }
}