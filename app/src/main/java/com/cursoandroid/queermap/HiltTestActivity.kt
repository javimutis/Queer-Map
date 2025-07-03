// com.cursoandroid.queermap/HiltTestActivity.kt
package com.cursoandroid.queermap

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle // Add import
import android.widget.FrameLayout // Add import if you explicitly add a container

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HiltTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val frameLayout = FrameLayout(this)
        frameLayout.id = android.R.id.content // Usar un ID estándar o uno custom
        setContentView(frameLayout)
    }
}