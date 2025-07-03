// com.cursoandroid.queermap/HiltTestActivity.kt
package com.cursoandroid.queermap

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.FrameLayout // Import FrameLayout

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HiltTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure this FrameLayout exists and has a known ID
        val frameLayout = FrameLayout(this)
        frameLayout.id = android.R.id.content // Or R.id.fragment_container if you define one
        setContentView(frameLayout)
    }
}