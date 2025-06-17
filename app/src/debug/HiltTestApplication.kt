package com.cursoandroid.queermap // ESTE PAQUETE DEBE SER EL applicationID

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Solo si necesitas una aplicación de prueba personalizada más allá de la predeterminada de Hilt.
// HiltTestApplication es generada automáticamente por Hilt si no la defines tú.
// Si la definiste, su paquete debe coincidir con el applicationId.
@HiltAndroidApp
class HiltTestApplication : Application() {
    // ...
}