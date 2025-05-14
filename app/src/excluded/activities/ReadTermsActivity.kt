package com.cursoandroid.queermap.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.R

class ReadTermsActivity : AppCompatActivity() {
    private lateinit var acceptButton: Button
    private lateinit var cancelButton: Button
    private lateinit var termsAndConditionsTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_terms)

        // Inicializar los componentes de la vista
        acceptButton = findViewById(R.id.acceptButton)
        cancelButton = findViewById(R.id.cancelButton)
        termsAndConditionsTextView = findViewById(R.id.termsAndConditionsTextView)

        // Configurar el onClickListener para el botón de aceptar
        acceptButton.setOnClickListener {
            navigateToMapActivity()
        }

        // Configurar el onClickListener para el botón de cancelar
        cancelButton.setOnClickListener {
            closeView()
        }

        // Configurar el onClickListener para el texto de términos y condiciones
        termsAndConditionsTextView.setOnClickListener {
            showTermsPopup()
        }
    }

    private fun navigateToMapActivity() {
        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)
    }

    private fun closeView() {
        finish()
    }

    private fun showTermsPopup() {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.popup_read_terms_layout, null)
        dialogBuilder.setView(dialogView)

        val closeButton = dialogView.findViewById<Button>(R.id.closePopup)
        val alertDialog = dialogBuilder.create()

        closeButton.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }
}
