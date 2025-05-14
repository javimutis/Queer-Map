package com.cursoandroid.queermap.activities

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.cursoandroid.queermap.utils.ValidationUtils.isValidEmail
import com.cursoandroid.queermap.utils.ValidationUtils.isValidPassword
import com.google.firebase.auth.FirebaseUser

class LoginActivityExample_ {

    private fun onLoginButtonClick(email: String, password: String) {
        if (isValidEmail(email) && isValidPassword(password)) {
            showSigningInMessage()
            signInWithEmailAndPassword(email, password)
        } else {
            showInvalidCredentialsError()
        }
    }

    private fun signInWithEmailAndPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showSignInSuccess()
                } else {
                    showSignInError()
                }
            }

        verifyUserInFirestore()

        val rememberMe = rememberCheckBox.isChecked

        if (rememberMe) {
            saveCredentials(email, password)
        }
    }
    // Muestra un mensaje de inicio de sesión en curso
    private fun showSigningInMessage() {
        Toast.makeText(this, "Signing in...", Toast.LENGTH_SHORT).show()
    }

    // Muestra un mensaje de error de credenciales inválidas
    private fun showInvalidCredentialsError() {
        Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
    }

    // Muestra un mensaje de inicio de sesión exitoso
    private fun showSignInSuccess() {
        Toast.makeText(this, "Sign in successful", Toast.LENGTH_SHORT).show()
        showReadTermsScreen()
    }
    // Muestra un mensaje de error de inicio de sesión
    private fun showSignInError() {
        Toast.makeText(this, "Sign in error", Toast.LENGTH_SHORT).show()
    }
    private fun verifyUserInFirestore() {
        val user: FirebaseUser? = auth.currentUser

        user?.let {
            firestore.collection("users")
                .document(it.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Verificación exitosa
                        // Realiza la acción deseada
                    } else {
                        // Documento no encontrado en Firestore
                        // Realiza la acción deseada
                    }
                }
                .addOnFailureListener { e ->
                    // Error al acceder a Firestore
                    // Realiza la acción deseada
                }
        }
    }
    private fun saveCredentials(email: String, password: String) {
        val sharedPref = getSharedPreferences("login", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("email", email)
        editor.putString("password", password)
        editor.apply()
    }
    // Abre la actividad de términos y condiciones
    private fun showReadTermsScreen() {
        val intent = Intent(this, ReadTermsActivity::class.java)
        startActivity(intent)
        finish()
    }

}