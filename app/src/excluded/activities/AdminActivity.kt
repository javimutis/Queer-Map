package com.cursoandroid.queermap.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.models.Place
import com.cursoandroid.queermap.services.PlaceService

class AdminActivity : AppCompatActivity() {

    private lateinit var placeContainer: LinearLayout
    private lateinit var verifyButton: Button
    private lateinit var placeService: PlaceService
    private lateinit var pendingPlaces: List<Place>
    private val selectedPlaces: MutableList<Place> = mutableListOf()
    private val verifiedPlaces: MutableList<Place> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // Obtener referencias a los elementos de la interfaz de usuario
        placeContainer = findViewById(R.id.placeContainer)
        verifyButton = findViewById(R.id.verifyButton)

        // Crear una instancia de PlaceService
        placeService = PlaceService()

        // Configurar el evento de clic del botón de verificación
        verifyButton.setOnClickListener {
            // Obtener los lugares seleccionados
            val selectedPlaces = getSelectedPlaces()
            if (selectedPlaces.isNotEmpty()) {
                // Verificar los lugares seleccionados
                verifyPlaces(selectedPlaces)
                showMapsActivity()
            } else {
                // Mostrar un mensaje de error si no se seleccionó ningún lugar
                Toast.makeText(this, "Selecciona al menos un lugar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Cargar los lugares pendientes
        loadPendingPlaces()
    }

    // Cargar los lugares pendientes desde el servicio
    private fun loadPendingPlaces() {
        placeService.getPendingPlaces { places ->
            pendingPlaces = places
            verifiedPlaces.clear() // Limpiar la lista de lugares verificados
            val unverifiedPlaces = places.filter { !it.verified }
            displayPendingPlaces(unverifiedPlaces)
        }
    }

    // Mostrar los lugares pendientes en la interfaz de usuario
    private fun displayPendingPlaces(pendingPlaces: List<Place>) {
        placeContainer.removeAllViews() // Limpiar el contenedor de lugares antes de mostrar los nuevos lugares

        for (place in pendingPlaces) {
            // Verificar si el lugar está verificado
            if (place.verified) {
                // No mostrar los lugares verificados en la interfaz de usuario
                continue
            }

            // Crear un CheckBox para cada lugar pendiente
            val checkBox = CheckBox(this)
            checkBox.text = place.name
            checkBox.id = View.generateViewId() // Agregar un id único

            // Mostrar información del lugar junto al CheckBox
            val placeInfo = TextView(this)
            placeInfo.text =
                "Categoría: ${place.category}\nDescripción: ${place.description}\nTelefono: ${place.phone}\nSitio web: ${place.website}"
            placeContainer.addView(placeInfo)

            // Configurar el evento de cambio de estado del CheckBox
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Agregar el lugar a la lista de lugares seleccionados
                    val selectedPlace = pendingPlaces.find { it.name == checkBox.text }
                    selectedPlace?.let {
                        selectedPlaces.add(it)
                    }
                } else {
                    // Eliminar el lugar de la lista de lugares seleccionados
                    val deselectedPlace = pendingPlaces.find { it.name == checkBox.text }
                    deselectedPlace?.let {
                        selectedPlaces.remove(it)
                    }
                }
            }
            // Agregar el CheckBox al contenedor de lugares
            placeContainer.addView(checkBox)

            // Verificar si el lugar está verificado
            if (place.verified) {
                // No mostrar los lugares verificados en la interfaz de usuario
                placeContainer.removeView(checkBox)
                continue
            }

        }
    }

    // Obtener los lugares seleccionados
    private fun getSelectedPlaces(): List<Place> {
        val selectedPlaces = mutableListOf<Place>()
        for (i in 0 until placeContainer.childCount) {
            val view = placeContainer.getChildAt(i)
            if (view is CheckBox && view.isChecked) {
                // Obtener el nombre del lugar seleccionado
                val placeName = view.text.toString()
                // Obtener el Place correspondiente al lugar seleccionado
                val place: Place? = pendingPlaces.find { it.name == placeName }
                place?.let { selectedPlaces.add(it) }
            }
        }
        return selectedPlaces
    }

    // Verificar los lugares seleccionados
    private fun verifyPlaces(places: List<Place>) {
        for (place in places) {
            verifyPlace(place)
        }
    }

    private fun verifyPlace(place: Place) {
        if (!place.verified) {
            // Llamar al servicio para verificar el lugar
            placeService.verifyPlace(place) { success ->
                if (success) {
                    Toast.makeText(this, "Lugar verificado: ${place.name}", Toast.LENGTH_SHORT)
                        .show()
                    place.verified = true
                    verifiedPlaces.add(place) // Agregar el lugar a la lista de lugares verificados
                } else {
                    Toast.makeText(
                        this,
                        "Error al verificar el lugar: ${place.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showMapsActivity() {
        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)
    }
}
