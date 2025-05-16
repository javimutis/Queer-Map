package com.cursoandroid.queermap.ui.test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SingleFragmentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_fragment)
    }
}
