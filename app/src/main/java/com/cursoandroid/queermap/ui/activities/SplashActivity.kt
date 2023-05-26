package com.cursoandroid.queermap.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.MainActivity
import com.cursoandroid.queermap.R

/*Aquí implementarás la lógica del Splash, como mostrar el diseño de actividad correspondiente durante unos segundos y luego dirigirte a la actividad de Login.*/
/*Implementa la lógica del Splash.
En el método onCreate, utiliza un Handler para retrasar la transición a la actividad de inicio de sesión después de un breve período de tiempo. Puedes utilizar un Intent para iniciar la LoginActivity.
Asegúrate de agregar la actividad SplashActivity al archivo AndroidManifest.xml.*/
class SplashActivity : AppCompatActivity() {

    private val SPLASH_DURATION = 3000 // Duración de la animación del splash en milisegundos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Iniciar la animación del splash
        Handler().postDelayed({

            // Abrir la siguiente actividad
            val intent = Intent(this, CoverActivity::class.java)
            startActivity(intent)

            // Aplicar la animación de transición
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

            // Finalizar la actividad actual
            finish()
        }, SPLASH_DURATION.toLong())
    }
}