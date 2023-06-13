package com.cursoandroid.queermap.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.MapsActivity
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.interfaces.ReadTermsContract
import com.cursoandroid.queermap.presenter.ReadTermsPresenter

class ReadTermsActivity : AppCompatActivity(), ReadTermsContract.View {

    private lateinit var acceptButton: Button
    private lateinit var cancelButton: Button
    private lateinit var termsAndConditionsTextView: TextView

    private lateinit var presenter: ReadTermsContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_terms)

        // Inicializar los componentes de la vista
        acceptButton = findViewById(R.id.acceptButton)
        cancelButton = findViewById(R.id.cancelButton)
        termsAndConditionsTextView = findViewById(R.id.termsAndConditionsTextView)

        // Crear el presenter
        presenter = ReadTermsPresenter(this)

        // Configurar el onClickListener para el botón de aceptar
        acceptButton.setOnClickListener {
            presenter.onAcceptButtonClicked()
        }

        // Configurar el onClickListener para el botón de cancelar
        cancelButton.setOnClickListener {
            presenter.onCancelButtonClicked()
        }

        // Configurar el onClickListener para el texto de términos y condiciones
        termsAndConditionsTextView.setOnClickListener {
            presenter.onTermsAndConditionsClicked()
        }
    }

    override fun navigateToMapActivity() {
        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)
    }

    override fun closeView() {
        finish()
    }

    override fun showTermsPopup() {
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
