package com.cursoandroid.queermap.ui.activities

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.interfaces.LoginContract
import com.cursoandroid.queermap.presenter.LoginPresenter
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

class LoginActivity : AppCompatActivity(), LoginContract.View, FacebookCallback<LoginResult> {

    private lateinit var presenter: LoginContract.Presenter
    private lateinit var auth: FirebaseAuth
    private lateinit var forgotPasswordDialog: Dialog
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var rememberCheckBox: CheckBox
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    private lateinit var eyeIcon: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Initialize presenter
        presenter = LoginPresenter(this, auth)
        // Get references to UI elements
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        eyeIcon = findViewById(R.id.eyeIcon)
        rememberCheckBox = findViewById(R.id.rememberCheckBox)

        // Set click listener for the eye icon to toggle password visibility
        eyeIcon.setOnClickListener {
            togglePasswordVisibility()
        }

        // Set click listener for the login button
        val loginButton: Button = findViewById(R.id.login_button)
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            presenter.onLoginButtonClick(email, password)
        }

        // Set click listener for the back button
        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, CoverActivity::class.java)
            startActivity(intent)
            finish()
        }
        // Configure Google Sign-In options
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // Initialize Facebook callback manager and Google sign-in client
        callbackManager = CallbackManager.Factory.create()
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        // Set click listener for Google sign-in button
        val googleSignInButton: ImageButton = findViewById(R.id.googleSignInButton)
        Picasso.get().load(R.drawable.google_icon).into(googleSignInButton)
        googleSignInButton.setOnClickListener {
            presenter.onGoogleSignInButtonClicked()
        }

        // Set click listener for Facebook login button
        val facebookLoginButton: ImageButton = findViewById(R.id.facebookLoginButton)
        Picasso.get().load(R.drawable.facebook_icon).into(facebookLoginButton)
        facebookLoginButton.setOnClickListener {
            presenter.onFacebookLoginClicked()
        }
        // Set click listener for the forgot password text view
        forgotPasswordDialog = Dialog(this)
        forgotPasswordDialog.setContentView(R.layout.forgot_password)

        // Set click listener for the login/sign in button
        val loginSigninButton: TextView = findViewById(R.id.loginSignin)
        loginSigninButton.setOnClickListener {
            goToSignInActivity()
        }
        val forgotPasswordTextView: TextView = findViewById(R.id.forgotPasswordTextView)
        forgotPasswordTextView.setOnClickListener {
            showForgotPasswordDialog()
        }

        // Check if the user is already logged in
        (presenter as LoginPresenter).checkUserLoggedIn()


    }
    // Toggle password visibility
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

        // Set the cursor position to the end of the password text
        passwordEditText.setSelection(passwordEditText.text?.length ?: 0)
    }

    // Sign in with email and password
    private fun signInWithEmailAndPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val intent = Intent(this, ReadTermsActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this, "Failed to sign in. Please check your credentials.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        // Save user credentials if "Remember Me" checkbox is checked
        val rememberMe = rememberCheckBox.isChecked

        if (rememberMe) {
            saveCredentials(email, password)
        }

        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

    }

    // Show the forgot password dialog
    private fun showForgotPasswordDialog() {
        val resetButton: Button = forgotPasswordDialog.findViewById(R.id.resetPasswordButton)
        val cancelButton: Button = forgotPasswordDialog.findViewById(R.id.cancelButton)
        val emailEditText: EditText = forgotPasswordDialog.findViewById(R.id.emailEditText)

        resetButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                presenter.onForgotPasswordClick(email)
                forgotPasswordDialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        cancelButton.setOnClickListener {
            forgotPasswordDialog.dismiss()
        }

        forgotPasswordDialog.show()
    }
    // Save user credentials to SharedPreferences
    private fun saveCredentials(email: String, password: String) {
        val sharedPref = getSharedPreferences("login", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("email", email)
        editor.putString("password", password)
        editor.apply()
    }

    // Load saved user credentials from SharedPreferences
    private fun loadSavedCredentials() {
        val sharedPref = getSharedPreferences("login", Context.MODE_PRIVATE)
        val email = sharedPref.getString("email", "")
        val password = sharedPref.getString("password", "")

        emailEditText.setText(email)
        passwordEditText.setText(password)
        rememberCheckBox.isChecked = true
    }

    // Implement the functions from LoginContract.View interface
    override fun showInvalidCredentialsError() {
        Toast.makeText(
            this, "Please enter a valid email and password.",
            Toast.LENGTH_SHORT
        ).show()
    }
    override fun showSigningInMessage() {
        Toast.makeText(this, "Signing in...", Toast.LENGTH_SHORT).show()
    }
    override fun showSignInSuccess() {
        val intent = Intent(this, ReadTermsActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun showSignInError() {
        Toast.makeText(
            this, "Failed to sign in. Please check your credentials.",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun showPasswordResetEmailSent(email: String) {
        Toast.makeText(
            this, "Password reset email sent to $email",
            Toast.LENGTH_SHORT
        ).show()
    }
    override fun showPasswordResetEmailError() {
        Toast.makeText(
            this, "Failed to send password reset email.",
            Toast.LENGTH_SHORT
        ).show()

}

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Pass the activity result to the Facebook callback manager
        callbackManager.onActivityResult(requestCode, resultCode, data)

        // Handle Google Sign-In result
        if (requestCode == LoginPresenter.RC_GOOGLE_SIGN_IN) {
            presenter.handleGoogleSignInResult(data)
        }
    }

    override fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    override fun showGoogleSignInIntent(signInIntent: Intent) {
        startActivityForResult(signInIntent, LoginPresenter.RC_GOOGLE_SIGN_IN)
    }

    override fun showGoogleSignInError() {
        Toast.makeText(
            this, "Error signing in with Google. Please check your credentials.",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun showTermsActivity() {
        val intent = Intent(this, ReadTermsActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun showGoogleSignInErrorMessage() {
        Toast.makeText(
            this,
            "Error signing in with Google. Please try again.",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun showTermsScreen() {
        val intent = Intent(this@LoginActivity, ReadTermsActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun showLoginError() {
        Toast.makeText(
            this@LoginActivity, "Error signing in with Facebook. Please check your credentials.",
            Toast.LENGTH_SHORT
        ).show()
    }

    // Facebook login success callback
    override fun onSuccess(loginResult: LoginResult) {
        presenter.onFacebookLoginSuccess()
    }

    // Facebook login cancellation callback
    override fun onCancel() {
        presenter.onFacebookLoginCancel()
    }

    // Facebook login error callback
    override fun onError(error: FacebookException) {
        presenter.onFacebookLoginError()
    }
    override fun goToSignInActivity() {
        val intent = Intent(this, SigninActivity::class.java)
        startActivity(intent)
    }
}
