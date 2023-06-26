package com.cursoandroid.queermap.activities

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.DatePicker
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.utils.ValidationUtils
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.squareup.picasso.Picasso
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
    private lateinit var callbackManager: CallbackManager
    private lateinit var eyeIcon: ImageView
    private lateinit var repeatEyeIcon: ImageView
    private lateinit var popupBirthday: ImageView
    private val RC_GOOGLE_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin)

        mAuth = FirebaseAuth.getInstance()
        initializeViews()
        setupDatePicker()
        callbackManager = CallbackManager.Factory.create()

        eyeIcon.setOnClickListener {
            togglePasswordVisibility()
        }
        repeatEyeIcon.setOnClickListener {
            toggleRepeatPasswordVisibility()
        }

        val registerButton: Button = findViewById(R.id.registerButton)
        registerButton.setOnClickListener {
            validateAndShowTermsPopup()
        }


        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()
        }


        val googleSignInButton: ImageButton = findViewById(R.id.googleSignInButton)
        Picasso.get().load(R.drawable.google_icon).into(googleSignInButton)
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }


        val facebookSignInButton: ImageButton = findViewById(R.id.facebookLSignInButton)
        Picasso.get().load(R.drawable.facebook_icon).into(facebookSignInButton)
        facebookSignInButton.setOnClickListener {
            signInWithFacebook()
        }


        popupPassword = findViewById(R.id.popupPassword)
        popupPassword.setOnClickListener {
            showErrorPopup(
                popupPassword,
                "La contraseña debe tener al menos 8 caracteres, incluyendo una letra mayúscula, una letra minúscula, un número y un carácter especial."
            )
        }

        popupBirthday = findViewById(R.id.popupBirthday)
        popupBirthday.setOnClickListener {
            showErrorPopup(
                popupBirthday,
                "Pedimos tu fecha de nacimiento para ajustar los eventos a tu rango de edad."
            )
        }

        initializeViews()
    }

    private fun initializeViews() {
        nameEditText = findViewById(R.id.nameEditText)
        userEditText = findViewById(R.id.userEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        repeatPasswordEditText = findViewById(R.id.repeatPasswordEditText)
        emailEditText = findViewById(R.id.emailEditText)
        birthdayEditText = findViewById(R.id.birthdayEditText)
        datePickerButton = findViewById(R.id.calendar_icon)
        eyeIcon = findViewById(R.id.eyeIcon)
        repeatEyeIcon = findViewById(R.id.repeatEyeIcon)

        setupDatePicker()
        setupPasswordVisibilityToggle()
        setupRepeatPasswordVisibilityToggle()
    }

    fun showErrorPopup(input: ImageView, errorMessage: String) {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
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

        val anchorView = input
        val location = IntArray(2)
        anchorView.getLocationInWindow(location)
        val xOffset = location[0] - anchorView.width / 2
        val yOffset = location[1] - anchorView.height
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, xOffset, yOffset)
    }


    private fun setupDatePicker() {
        datePickerButton.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun setupPasswordVisibilityToggle() {
        eyeIcon.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun setupRepeatPasswordVisibilityToggle() {
        repeatEyeIcon.setOnClickListener {
            toggleRepeatPasswordVisibility()
        }
    }

    private fun togglePasswordVisibility() {
        val inputType = passwordEditText.inputType

        if (inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT) {
            passwordEditText.inputType =
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_CLASS_TEXT
            eyeIcon.setImageResource(R.drawable.open_eye)
        } else {
            passwordEditText.inputType =
                InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
            eyeIcon.setImageResource(R.drawable.closed_eye)
        }

        passwordEditText.setSelection(passwordEditText.text?.length ?: 0)
    }

    private fun toggleRepeatPasswordVisibility() {
        val inputType = repeatPasswordEditText.inputType

        if (inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT) {
            repeatPasswordEditText.inputType =
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_CLASS_TEXT
            repeatEyeIcon.setImageResource(R.drawable.open_eye)
        } else {
            repeatPasswordEditText.inputType =
                InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
            repeatEyeIcon.setImageResource(R.drawable.closed_eye)
        }

        repeatPasswordEditText.setSelection(repeatPasswordEditText.text?.length ?: 0)
    }

    fun validateAndShowTermsPopup() {
        val name: String = nameEditText.text.toString().trim()
        val username: String = userEditText.text.toString().trim()
        val password: String = passwordEditText.text.toString()
        val repeatPassword: String = repeatPasswordEditText.text.toString()
        val email: String = emailEditText.text.toString().trim()
        val birthday: String = birthdayEditText.text.toString().trim()

        // Validar campos y mostrar mensajes de error según corresponda
        if (username.isEmpty()) {
            setError(userEditText, "Ingrese un nombre de usuario")
            return
        }
        if (name.isEmpty()) {
            setError(nameEditText, "Ingrese un nombre")
            return
        }
        if (password.isEmpty()) {
            setError(passwordEditText, "Ingrese una contraseña")
            return
        }

        if (repeatPassword.isEmpty()) {
            setError(repeatPasswordEditText, "Repita la contraseña")
            return
        }

        if (email.isEmpty()) {
            setError(emailEditText, "Ingrese un correo electrónico")
            return
        }

        if (birthday.isEmpty()) {
            setError(birthdayEditText, "Ingrese una fecha de nacimiento")
            return
        }

        if (!ValidationUtils.isValidSignUsername(username)) {
            setError(userEditText, "Nombre de usuario inválido")
            return
        }

        if (!ValidationUtils.isValidSignName(name)) {
            setError(nameEditText, "Nombre inválido")
            return
        }

        if (!ValidationUtils.isValidSignPassword(password)) {
            setError(passwordEditText, "Contraseña inválida")
            return
        }

        if (repeatPassword != password) {
            setError(repeatPasswordEditText, "Las contraseñas no coinciden")
            return
        }

        if (!ValidationUtils.isValidSignEmail(email)) {
            setError(emailEditText, "Email inválido")
            return
        }

        if (!ValidationUtils.isValidSignBirthday(birthday)) {
            setError(birthdayEditText, "Fecha de nacimiento inválida")
            return
        }


        showTermsPopup()
    }


    private fun setError(input: TextInputEditText, message: String) {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_error_layout, null)
        val errorTextView = popupView.findViewById<TextView>(R.id.errorTextView)

        errorTextView.text = message

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.animationStyle = R.style.PopupAnimation
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

    }

    override fun finish() {
        super.finish()
        // Implement finish logic if needed
    }


    override fun onBackPressed() {
        val intent = Intent(this, CoverActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun showDatePickerDialog() {
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
                val date = dateFormat.format(selectedDate.time)
                birthdayEditText.setText(date)
            },
            year,
            month,
            day
        )

        datePickerDialog.show()
    }

    fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        mAuth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                // Successful login
                showTermsPopup()
            } else {
                // Error in login
            }
        }
    }

    fun navigateToMapActivity() {
        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun showTermsPopup() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.activity_read_terms, null)
        val popupWindow = PopupWindow(
            popupView,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            true
        )
        popupWindow.animationStyle = R.style.PopupAnimation
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

        // Asignar listeners y realizar acciones según corresponda en el popup de términos
        val termsTextView = popupView.findViewById<TextView>(R.id.termsAndConditionsTextView)
        val acceptButton = popupView.findViewById<Button>(R.id.acceptButton)
        val cancelButton = popupView.findViewById<Button>(R.id.cancelButton)

        termsTextView.setOnClickListener {
            showReadTermsPopup()
        }


        acceptButton.setOnClickListener {
            navigateToMapActivity()
        }

        cancelButton.setOnClickListener {
            popupWindow.dismiss()
        }
    }

    fun showReadTermsPopup() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_read_terms_layout, null)
        val popupWindow = PopupWindow(
            popupView,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            true
        )
        popupWindow.animationStyle = R.style.PopupAnimation
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

        // Asignar listeners y realizar acciones según corresponda en el popup de lectura de términos
        val closeButton = popupView.findViewById<Button>(R.id.closePopup)
        closeButton.setOnClickListener {
            popupWindow.dismiss()
        }
    }

    fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
    }


    fun signInWithFacebook() {
        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    handleFacebookAccessToken(loginResult.accessToken)
                }

                override fun onCancel() {
                    // Canceled by user
                }

                override fun onError(error: FacebookException) {
                    // Error in Facebook login
                }
            })

        LoginManager.getInstance().logInWithReadPermissions(this, listOf("email"))
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                // Error in Google login
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        mAuth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                // Successful login
                showTermsPopup()
            } else {
                // Error in login
            }
        }
    }
}