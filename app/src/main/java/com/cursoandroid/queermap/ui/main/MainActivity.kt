// Queermap/ui/main/MainActivity.kt
package com.cursoandroid.queermap.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.NavController
import androidx.navigation.navOptions
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.databinding.ActivityMainBinding
import com.facebook.CallbackManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var facebookCallbackManager: CallbackManager

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        // Instala la Splash Screen antes de llamar a super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Configura la Splash Screen para la animación de salida
        setupSplashScreen(splashScreen)

        // Asegúrate de que el NavHostFragment esté completamente dibujado y tenga foco
        // antes de permitir que Espresso interactúe.
        // Esto es crucial para el RootViewWithoutFocusException.
        // También podemos usar un onPreDrawListener en el NavHostFragment para asegurar
        // que el fragmento inicial (CoverFragment) esté completamente listo.
        navHostFragment.view?.viewTreeObserver?.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    // Solo si ya estamos navegando y el navHostFragment ha dibujado su contenido
                    // podemos remover el listener.
                    // Aquí, el objetivo es simplemente esperar a que el primer frame del NavHostFragment
                    // y su fragmento inicial (CoverFragment) estén dibujados.
                    navHostFragment.view?.viewTreeObserver?.removeOnPreDrawListener(this)
                    return true // Continuar con el dibujo
                }
            }
        )
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun setupSplashScreen(splashScreen: SplashScreen) {
        // En este punto, la Splash Screen está activa.
        // Queremos que se mantenga hasta que el contenido de la actividad esté "listo" para mostrarse.
        // Para Espresso, "listo" significa que la actividad principal (con el NavHostFragment)
        // ha terminado de inicializarse y ha dibujado su primer frame.

        // Mantenemos la Splash Screen en pantalla hasta que se cumplan las condiciones para ocultarla.
        // En este caso, simplemente esperamos a que la animación de salida del splash termine.
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            // Inicia la animación de salida del splash screen.
            splashScreenView.view.animate()
                .alpha(0f) // Desvanece el splash screen
                .setDuration(500L) // Duración de la animación
                .withEndAction {
                    // Cuando la animación de salida termina, se puede quitar el splash screen.
                    splashScreenView.remove()
                    // Si hubiera alguna carga asíncrona real en MainActivity que dependiera de un IdlingResource,
                    // aquí sería un buen lugar para decrementar un contador, pero la navegación
                    // ya está cubierta por la propia animación de salida y la espera de Espresso
                    // a que el RootView tenga foco.
                }.start()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}