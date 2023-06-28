package com.cursoandroid.queermap.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
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
            val name = addNameEditText.text.toString()
            val description = descriptionEditText.text.toString()
            val phone = phoneEditText.text.toString()
            val website = websiteEditText.text.toString()
            val category = spinnerCategory.selectedItem.toString()

            // Verificar el formato del teléfono
            val validPhone = if (phone.isNotEmpty() && !isValidPhone(phone)) {
                Toast.makeText(this, "Formato de teléfono incorrecto", Toast.LENGTH_SHORT).show()
                false
            } else {
                true
            }

            // Verificar el formato del sitio web
            val validWebsite = if (website.isNotEmpty() && !isValidWebsite(website)) {
                Toast.makeText(this, "Formato de sitio web incorrecto", Toast.LENGTH_SHORT).show()
                false
            } else {
                true
            }

            // Si los campos de teléfono y sitio web son válidos, agregar el lugar
            if (validPhone && validWebsite) {
                val place = Place(
                    null,
                    name,
                    description,
                    phone,
                    website,
                    category,
                    latitude,
                    longitude,
                    false
                )

                placeService.addPlace(place)

                val intent = Intent(this, MapsActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        // Volver a la actividad anterior cuando se hace clic en el botón "Atrás"
        backButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Función para verificar el formato del teléfono en Chile
    private fun isValidPhone(phone: String): Boolean {
        // Eliminar espacios en blanco y guiones del número de teléfono
        val cleanPhone = phone.replace("\\s|\\-".toRegex(), "")

        // Verificar que el número de teléfono tenga 9 dígitos
        if (cleanPhone.length != 9) {
            return false
        }

        // Verificar que el número de teléfono comience con un dígito válido
        val validStartDigits = listOf("9", "2", "3", "4", "5", "6", "7")
        val startDigit = cleanPhone.substring(0, 1)
        if (!validStartDigits.contains(startDigit)) {
            return false
        }

        // Verificar que todos los caracteres restantes sean dígitos numéricos
        val numericPattern = "\\d+".toRegex()
        return cleanPhone.substring(1).matches(numericPattern)
    }

    // Función para verificar el formato del sitio web
    private fun isValidWebsite(website: String): Boolean {
        val urlPattern = "^(https?://)?(www\\.)?.+[a-zA-Z]{2,3}(/\\S*)?$"
        val regex = Regex(urlPattern)
        return regex.matches(website)
    }
}

