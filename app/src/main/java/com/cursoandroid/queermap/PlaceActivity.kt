package com.cursoandroid.queermap

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.models.Place
import com.cursoandroid.queermap.services.PlaceService

class PlaceActivity : AppCompatActivity() {
    private lateinit var latitudeEditText: EditText
    private lateinit var longitudeEditText: EditText
//    private lateinit var selectedIcon: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place)

        val addNameEditText = findViewById<EditText>(R.id.addNameEditText)
        val descriptionEditText = findViewById<EditText>(R.id.descriptionEditText)
        val spinnerCategory: Spinner = findViewById(R.id.spinnerCategory)
        val addPlaceButton: Button = findViewById(R.id.addPlaceButton)
        val backButton: ImageView = findViewById(R.id.backButton)
        val placeService = PlaceService()

        // Obtener la latitud y longitud desde el intent
        val latitude = intent.getDoubleExtra("latitud", 0.0)
        val longitude = intent.getDoubleExtra("longitud", 0.0)

        // Obtener referencias a los elementos de la interfaz de usuario
        latitudeEditText = findViewById(R.id.latitudeEditText)
        longitudeEditText = findViewById(R.id.longitudeEditText)

        // Asignar los valores de latitud y longitud a los campos correspondientes
        latitudeEditText.setText(latitude.toString())
        longitudeEditText.setText(longitude.toString())

        // Obtener las categorías desde los recursos
        val categories = resources.getStringArray(R.array.spinnerCategory)

        // Configurar el adaptador del Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerCategory.adapter = adapter


        addPlaceButton.setOnClickListener {
            val place = Place(
                null,
                addNameEditText.text.toString(),
                descriptionEditText.text.toString(),
                spinnerCategory.selectedItem.toString(),
                latitude,
                longitude
            )
            Log.d(TAG, "BBDD enviada")

            placeService.createPlace(place)
        }
        // Despues actualizar a botón atrás
        backButton.setOnClickListener {
            placeService.getPlaces { places ->
                val placesList = places
                Log.d(TAG, "La lista de riesgos ${places}")
            }

        }
    }
}

//                // Volver a la actividad MapActivity
//            val intent = Intent(this, MapsActivity::class.java)
//
//            startActivity(intent)









