package com.cursoandroid.queermap.ui.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.DatePicker
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.utils.ValidationUtils
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SigninActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var nameEditText: TextInputEditText
    private lateinit var userEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var repeatPasswordEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var birthdayEditText: TextInputEditText
    private lateinit var datePickerButton: ImageView
    private lateinit var popupPassword: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin)

        mAuth = FirebaseAuth.getInstance()
        initializeViews()
        setupDatePicker()

        val registerButton: Button = findViewById(R.id.registerButton)
        registerButton.setOnClickListener {
            registerUser()
        }
        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()
        }


        popupPassword.setOnClickListener {
            showErrorPopup(
                popupPassword,
                "La contraseña debe tener al menos 8 caracteres, incluir una mayúscula, una minúscula, un número y un carácter especial."
            )
        }
    }



    private fun initializeViews() {
        nameEditText = findViewById(R.id.nameEditText)
        userEditText = findViewById(R.id.userEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        repeatPasswordEditText = findViewById(R.id.repeatPasswordEditText)
        emailEditText = findViewById(R.id.emailEditText)
        birthdayEditText = findViewById(R.id.birthdayEditText)
        datePickerButton = findViewById(R.id.calendar_icon)
        popupPassword = findViewById(R.id.popupPassword)
    }

    private fun showErrorPopup(anchorView: View, errorMessage: String) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_layout, null)
        val popupWindow = PopupWindow(
            popupView,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        popupWindow.elevation = 10f
        popupWindow.animationStyle = R.style.PopupAnimation
        popupWindow.isFocusable = true
        popupWindow.update()

        val errorTextView = popupView.findViewById<TextView>(R.id.errorTextView)
        errorTextView.text = errorMessage

        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val xOffset = location[0] - anchorView.width / 2
        val yOffset = location[1] - anchorView.height
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, xOffset, yOffset)
    }
    override fun onBackPressed() {
        val intent = Intent(this, CoverActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun setupDatePicker() {
        datePickerButton.setOnClickListener {
            showDatePickerDialog()
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
            setError(nameEditText, "Nombre inválido")
            return
        }

        if (!ValidationUtils.isValidSignUsername(username)) {
            setError(userEditText, "Nombre de usuario inválido")
            return
        }

        if (!ValidationUtils.isValidSignPassword(password)) {
            setError(passwordEditText, "Contraseña inválida")
            return
        }

        if (!ValidationUtils.isValidSignEmail(email)) {
            setError(emailEditText, "Correo electrónico inválido")
            return
        }

        if (!ValidationUtils.isValidSignBirthday(birthday)) {
            setError(birthdayEditText, "Fecha de nacimiento inválida")
            return
        }

        if (repeatPassword != password) {
            setError(repeatPasswordEditText, "Las contraseñas no coinciden")
            return
        }

        if (!ValidationUtils.isStrongSignPassword(password)) {
            setError(repeatPasswordEditText, "Ingresa una contraseña válida")
            return
        }

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId: String = mAuth.currentUser?.uid ?: ""
                    val currentUser = mAuth.currentUser

                    if (currentUser != null) {
                        val userRef =
                            FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid)
                        val userData = HashMap<String, Any>()
                        userData["name"] = name
                        userData["username"] = username
                        userData["email"] = email
                        userData["birthday"] = birthday

                        userRef.setValue(userData)
                            .addOnSuccessListener {
                                navigateToMapActivity()
                            }
                            .addOnFailureListener { e ->
                                // Error al guardar los datos adicionales
                            }
                    }
                }
            }
    }

    private fun navigateToMapActivity() {
        val intent = Intent(this, MapActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            R.style.DatePickerDialogStyle,
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

        private fun setError(textInputEditText: TextInputEditText, errorMessage: String) {
        textInputEditText.error = errorMessage
        textInputEditText.requestFocus()
    }
}
