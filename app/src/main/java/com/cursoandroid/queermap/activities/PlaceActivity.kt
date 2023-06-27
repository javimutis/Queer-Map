package com.cursoandroid.queermap.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.models.Place
import com.cursoandroid.queermap.services.PlaceService

class PlaceActivity : AppCompatActivity() {
    private lateinit var latitudeEditText: EditText
    private lateinit var longitudeEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place)

        // Obtener referencias a los elementos de la interfaz de usuario
        val addNameEditText = findViewById<EditText>(R.id.addNameEditText)
        val descriptionEditText = findViewById<EditText>(R.id.descriptionEditText)
        val phoneEditText = findViewById<EditText>(R.id.phoneEditText)
        val websiteEditText = findViewById<EditText>(R.id.websiteEditText)
        val spinnerCategory: Spinner = findViewById(R.id.spinnerCategory)
        val addPlaceButton: Button = findViewById(R.id.addPlaceButton)
        val backButton: ImageView = findViewById(R.id.backButton)
        val placeService = PlaceService()

        // Obtener la latitud y longitud desde el intent
        val latitude = intent.getDoubleExtra("latitud", 0.0)
        val longitude = intent.getDoubleExtra("longitud", 0.0)

        // Asignar los valores de latitud y longitud a los campos correspondientes
        latitudeEditText = findViewById(R.id.latitudeEditText)
        longitudeEditText = findViewById(R.id.longitudeEditText)
        latitudeEditText.setText(latitude.toString())
        longitudeEditText.setText(longitude.toString())

        // Obtener las categorías desde los recursos
        val categories = resources.getStringArray(R.array.spinnerCategory)
        // Configurar el adaptador del Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        // Agregar un lugar cuando se presiona el botón "Agregar"
        addPlaceButton.setOnClickListener {
            val place = Place(
                null,
                addNameEditText.text.toString(),
                descriptionEditText.text.toString(),
                phoneEditText.text.toString().takeIf { it.isNotBlank() },
                websiteEditText.text.toString().takeIf { it.isNotEmpty() },
                spinnerCategory.selectedItem.toString(),
                latitude,
                longitude,
                false
            )

            placeService.addPlace(place)

            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Volver a la actividad anterior cuando se hace clic en el botón "Atrás"
        backButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
