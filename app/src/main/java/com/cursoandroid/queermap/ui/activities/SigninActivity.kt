package com.cursoandroid.queermap.ui.activities

import android.app.DatePickerDialog
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.DatePicker
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.utils.ValidationUtils
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Import statements
// ...

class SigninActivity : AppCompatActivity() {

    // Declare member variables
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin)

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance()

        // Initialize views
        initializeViews()

        // Setup date picker
        setupDatePicker()

        // Initialize the Facebook callback manager
        callbackManager = CallbackManager.Factory.create()

        // Set click listeners
        eyeIcon.setOnClickListener {
            togglePasswordVisibility()
        }
        repeatEyeIcon.setOnClickListener {
            toggleRepeatPasswordVisibility()
        }

        val registerButton: Button = findViewById(R.id.registerButton)
        registerButton.setOnClickListener {
            registerUser()
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

        popupPassword.setOnClickListener {
            showErrorPopup(
                popupPassword,
                "The password must have at least 8 characters, including an uppercase letter, a lowercase letter, a number, and a special character."
            )
        }

        popupBirthday.setOnClickListener {
            showErrorPopup(
                popupBirthday,
                "We ask for your birthday to adjust events to your age range."
            )
        }
    }

    // Initialize views by finding them in the layout
    private fun initializeViews() {
        nameEditText = findViewById(R.id.nameEditText)
        userEditText = findViewById(R.id.userEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        repeatPasswordEditText = findViewById(R.id.repeatPasswordEditText)
        emailEditText = findViewById(R.id.emailEditText)
        birthdayEditText = findViewById(R.id.birthdayEditText)
        datePickerButton = findViewById(R.id.calendar_icon)
        popupPassword = findViewById(R.id.popupPassword)
        eyeIcon = findViewById(R.id.eyeIcon)
        repeatEyeIcon = findViewById(R.id.repeatEyeIcon)
        popupBirthday = findViewById(R.id.popupBirthday)
    }

    // Show an error popup window
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

    // Setup the date picker dialog
    private fun setupDatePicker() {
        datePickerButton.setOnClickListener {
            showDatePickerDialog()
        }
    }

    // Toggle password visibility based on the eye icon state
    private fun togglePasswordVisibility() {
        val inputType = passwordEditText.inputType

        if (inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT) {
            passwordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_CLASS_TEXT
            eyeIcon.setImageResource(R.drawable.open_eye)
        } else {
            passwordEditText.inputType =
                InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
            eyeIcon.setImageResource(R.drawable.closed_eye)
        }

        // Set the cursor position to the end of the password text
        passwordEditText.setSelection(passwordEditText.text?.length ?: 0)
    }

    // Toggle repeat password visibility based on the eye icon state
    private fun toggleRepeatPasswordVisibility() {
        val inputType = repeatPasswordEditText.inputType

        if (inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT) {
            repeatPasswordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_CLASS_TEXT
            repeatEyeIcon.setImageResource(R.drawable.open_eye)
        } else {
            repeatPasswordEditText.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
            repeatEyeIcon.setImageResource(R.drawable.closed_eye)
        }

        // Set the cursor position to the end of the repeat password text
        repeatPasswordEditText.setSelection(repeatPasswordEditText.text?.length ?: 0)
    }

    // Register a new user
    private fun registerUser() {
        val name: String = nameEditText.text.toString()
        val username: String = userEditText.text.toString()
        val password: String = passwordEditText.text.toString()
        val repeatPassword: String = repeatPasswordEditText.text.toString()
        val email: String = emailEditText.text.toString()
        val birthday: String = birthdayEditText.text.toString()

        toggleRepeatPasswordVisibility()

        if (!ValidationUtils.isValidSignName(name)) {
            showErrorPopup(nameEditText, "Invalid name")
            return
        }

        if (!ValidationUtils.isValidSignUsername(username)) {
            showErrorPopup(userEditText, "Invalid username")
            return
        }

        if (!ValidationUtils.isValidSignPassword(password)) {
            showErrorPopup(passwordEditText, "Invalid password")
            return
        }

        if (repeatPassword != password) {
            showErrorPopup(repeatPasswordEditText, "Passwords do not match")
            return
        }

        if (!ValidationUtils.isValidSignEmail(email)) {
            showErrorPopup(emailEditText, "Invalid email")
            return
        }

        if (!ValidationUtils.isValidSignBirthday(birthday)) {
            showErrorPopup(birthdayEditText, "Invalid birthday")
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
                                // Error saving additional data
                            }
                    }
                }
            }
    }

    // Navigate to the map activity
    private fun navigateToMapActivity() {
        val intent = Intent(this, MapActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun finish() {
        TODO("Not yet implemented")
    }

    // Show the date picker dialog
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
                val date = dateFormat.format(selectedDate.time)
                birthdayEditText.setText(date)
            },
            year,
            month,
            day
        )

        datePickerDialog.show()
    }

    // Set error message for a TextInputEditText and focus it
    private fun setError(input: TextInputEditText, message: String) {
        input.error = message
        input.requestFocus()
    }

    // Sign in with Google
    private fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
    }

    // Sign in with Facebook
    private fun signInWithFacebook() {
        LoginManager.getInstance().registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
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

    // Handle Facebook access token
    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Successful login
                    navigateToMapActivity()
                } else {
                    // Error in login
                }
            }
    }

    companion object {
        private const val RC_GOOGLE_SIGN_IN = 1001
    }
}
