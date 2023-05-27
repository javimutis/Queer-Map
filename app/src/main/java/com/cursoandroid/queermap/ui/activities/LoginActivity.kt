package com.cursoandroid.queermap.ui.activities

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.MainActivity
import com.cursoandroid.queermap.R
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso


class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var forgotPasswordDialog: Dialog
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var rememberCheckBox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        rememberCheckBox = findViewById(R.id.rememberCheckBox)
        val loginButton: Button = findViewById(R.id.login_button)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Autenticación exitosa
                        val user = auth.currentUser
                        // Realiza las acciones necesarias después del inicio de sesión exitoso
                        val intent = Intent(this, MapActivity::class.java)
                        startActivity(intent)
                        finish() // Finaliza la actividad actual
                    } else {
                        // Autenticación fallida
                        Toast.makeText(
                            this, "Error al iniciar sesión. Verifica tus credenciales.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            val rememberMe = rememberCheckBox.isChecked

            // Guardar las credenciales solo si "Recuérdame" está marcado
            if (rememberMe) {
                val sharedPref = getSharedPreferences("login", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putString("email", email)
                editor.putString("password", password)
                editor.apply()
            }

            // Mostrar un Toast al presionar el botón de ingresar
            Toast.makeText(this, "Iniciando sesión...", Toast.LENGTH_SHORT).show()
        }

        // Cargar imagen de inicio de sesión usando Picasso
        val loginImage: ImageView = findViewById(R.id.loginImage)
        Picasso.get().load(R.drawable.login_cover).into(loginImage)

        // Flecha para volver atrás
        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, CoverActivity::class.java)
            startActivity(intent)
            finish() // Finaliza la actividad
        }

        forgotPasswordDialog = Dialog(this)
        forgotPasswordDialog.setContentView(R.layout.forgot_password)

        val forgotPasswordTextView: TextView = findViewById(R.id.forgotPasswordTextView)
        forgotPasswordTextView.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun showForgotPasswordDialog() {
        val resetButton: Button = forgotPasswordDialog.findViewById(R.id.resetPasswordButton)
        val cancelButton: Button = forgotPasswordDialog.findViewById(R.id.cancelButton)
        val emailEditText: EditText = forgotPasswordDialog.findViewById(R.id.emailEditText)

        resetButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                sendPasswordResetEmail(email)
                forgotPasswordDialog.dismiss()
            } else {
                Toast.makeText(this, "Ingrese un correo electrónico válido", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        cancelButton.setOnClickListener {
            forgotPasswordDialog.dismiss()
        }
        forgotPasswordDialog.show()
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Se ha enviado un correo de restablecimiento de contraseña a $email",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Error al enviar el correo de restablecimiento de contraseña",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun loadSavedCredentials() {
        val sharedPref = getSharedPreferences("login", Context.MODE_PRIVATE)
        val email = sharedPref.getString("email", "")
        val password = sharedPref.getString("password", "")

        emailEditText.setText(email)
        passwordEditText.setText(password)
        rememberCheckBox.isChecked = true
    }

    private fun checkUserLoggedIn() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            loadSavedCredentials()
        }
    }

    override fun onStart() {
        super.onStart()
        checkUserLoggedIn()
    }
}
