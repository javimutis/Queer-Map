package com.cursoandroid.queermap

import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HiltTestActivity : AppCompatActivity() {
    // No es necesario añadir nada aquí. Hilt y tu FragmentFactory @BindValue se encargan del resto.
}