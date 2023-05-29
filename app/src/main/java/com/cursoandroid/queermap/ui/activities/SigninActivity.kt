package com.cursoandroid.queermap.ui.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.utils.ValidationUtils
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class SigninActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var nameEditText: TextInputEditText
    private lateinit var userEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var repeatPasswordEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var birthdayEditText: TextInputEditText
    private lateinit var datePickerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin)

        mAuth = FirebaseAuth.getInstance()
        nameEditText = findViewById(R.id.nameEditText)
        userEditText = findViewById(R.id.userEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        repeatPasswordEditText = findViewById(R.id.repeatPasswordEditText)
        emailEditText = findViewById(R.id.emailEditText)
        birthdayEditText = findViewById(R.id.birthdayEditText)
        datePickerButton = findViewById(R.id.calendar_icon)

        datePickerButton.setOnClickListener {
            showDatePickerDialog()
        }

        val registerButton: Button = findViewById(R.id.registerButton)
        registerButton.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val name: String = nameEditText.text.toString()
        val username: String = userEditText.text.toString()
        val password: String = passwordEditText.text.toString()
        val repeatPassword: String = repeatPasswordEditText.text.toString()
        val email: String = emailEditText.text.toString()
        val birthday: String = birthdayEditText.text.toString()

        if (!ValidationUtils.isValidSignName(name)) {
            nameEditText.error = "Nombre inválido"
            return
        }

        if (!ValidationUtils.isValidSignUsername(username)) {
            userEditText.error = "Nombre de usuario inválido"
            return
        }

        if (!ValidationUtils.isValidSignPassword(password)) {
            passwordEditText.error = "Contraseña inválida"
            return
        }

        if (!ValidationUtils.isValidSignEmail(email)) {
            emailEditText.error = "Correo electrónico inválido"
            return
        }

        if (!ValidationUtils.isValidSignBirthday(birthday)) {
            birthdayEditText.error = "Fecha de nacimiento inválida"
            return
        }

        if (password != repeatPassword) {
            repeatPasswordEditText.error = "Las contraseñas no coinciden"
            return
        }

        if (!ValidationUtils.isStrongSignPassword(password)) {
            repeatPasswordEditText.error = "Ingresa una contraseña válida"
            return
        }

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId: String = mAuth.currentUser?.uid ?: ""
                    val currentUser = mAuth.currentUser

                    if (currentUser != null) {
                        val userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid)
                        val userData = HashMap<String, Any>()
                        userData["name"] = name
                        userData["username"] = username
                        userData["email"] = email
                        userData["birthday"] = birthday

                        userRef.setValue(userData)
                            .addOnSuccessListener {
                                // Registro y guardado de datos exitoso
                                // Realizar las acciones necesarias (por ejemplo, mostrar un mensaje de éxito)
                            }
                            .addOnFailureListener { e ->
                                // Error al guardar los datos adicionales
                                // Mostrar un mensaje de error o realizar las acciones correspondientes
                            }
                    }
                }
            }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener { _: DatePicker, selectedYear: Int, monthOfYear: Int, dayOfMonth: Int ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, monthOfYear, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val selectedDateString = dateFormat.format(selectedDate.time)
                birthdayEditText.setText(selectedDateString)
            },
            year,
            month,
            day
        )
        datePickerDialog.datePicker.calendarViewShown = false
        datePickerDialog.show()
    }
}
