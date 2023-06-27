package com.cursoandroid.queermap.activities

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        placeContainer = findViewById(R.id.placeContainer)
        verifyButton = findViewById(R.id.verifyButton)
        placeService = PlaceService()

        verifyButton.setOnClickListener {
            val selectedPlaces = getSelectedPlaces()
            if (selectedPlaces.isNotEmpty()) {
                verifyPlaces(selectedPlaces)
            } else {
                Toast.makeText(this, "Selecciona al menos un lugar", Toast.LENGTH_SHORT).show()
            }
        }

        loadPendingPlaces()
    }

    private fun loadPendingPlaces() {
        placeService.getPendingPlaces { places ->
            pendingPlaces = places
            displayPendingPlaces(places)
        }
    }

    private fun displayPendingPlaces(pendingPlaces: List<Place>) {
        for (place in pendingPlaces) {
            val checkBox = CheckBox(this)
            checkBox.text = place.name
            checkBox.id = View.generateViewId() // Agregar un id único
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    val selectedPlace = pendingPlaces.find { it.name == checkBox.text }
                    selectedPlace?.let {
                        selectedPlaces.add(it)
                    }
                } else {
                    val deselectedPlace = pendingPlaces.find { it.name == checkBox.text }
                    deselectedPlace?.let {
                        selectedPlaces.remove(it)
                    }
                }
            }
            placeContainer.addView(checkBox)
        }
    }

    private fun getSelectedPlaces(): List<Place> {
        val selectedPlaces = mutableListOf<Place>()
        for (i in 0 until placeContainer.childCount) {
            val view = placeContainer.getChildAt(i)
            if (view is CheckBox && view.isChecked) {
                val placeName = view.text.toString()
                // Obtener el Place correspondiente al lugar seleccionado
                val place: Place? = pendingPlaces.find { it.name == placeName }
                place?.let { selectedPlaces.add(it) }
            }
        }
        return selectedPlaces
    }

    private fun verifyPlaces(places: List<Place>) {
        for (place in places) {
            placeService.verifyPlace(place) { success ->
                if (success) {
                    // El lugar se verificó correctamente
                    Toast.makeText(this, "Lugar verificado: ${place.name}", Toast.LENGTH_SHORT)
                        .show()
                    // Actualizar el estado del lugar en la lista de lugares pendientes
                    place.verified = true
                    // Eliminar la vista del lugar verificado del layout
                    val checkBox = placeContainer.findViewWithTag<CheckBox>(place.name)
                    placeContainer.removeView(checkBox)
                } else {
                    // Hubo un error al verificar el lugar
                    Toast.makeText(
                        this,
                        "Error al verificar el lugar: ${place.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}

