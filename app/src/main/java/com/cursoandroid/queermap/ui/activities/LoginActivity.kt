package com.cursoandroid.queermap.ui.activities

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.utils.ValidationUtils
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.squareup.picasso.Picasso

class LoginActivity : AppCompatActivity(), FacebookCallback<LoginResult> {

    private lateinit var auth: FirebaseAuth
    private lateinit var forgotPasswordDialog: Dialog
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var rememberCheckBox: CheckBox
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    private val RC_GOOGLE_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Configure Google Sign-In options
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // Initialize Facebook callback manager and Google sign-in client
        callbackManager = CallbackManager.Factory.create()
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        // Get references to UI elements
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        rememberCheckBox = findViewById(R.id.rememberCheckBox)

        // Set click listener for the login button
        val loginButton: Button = findViewById(R.id.login_button)
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Validate email and password format
            if (ValidationUtils.isValidEmail(email) && ValidationUtils.isValidPassword(password)) {
                signInWithEmailAndPassword(email, password)
            } else {
                Toast.makeText(
                    this,
                    "Por favor, ingresa un correo electrónico válido y una contraseña.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Set click listener for Google sign-in button
        val googleSignInButton: ImageButton = findViewById(R.id.googleSignInButton)
        Picasso.get().load(R.drawable.google_icon).into(googleSignInButton)
        googleSignInButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
        }

        // Set click listener for Facebook login button
        val facebookLoginButton: ImageButton = findViewById(R.id.facebookLoginButton)
        Picasso.get().load(R.drawable.facebook_icon).into(facebookLoginButton)
        facebookLoginButton.setOnClickListener {
            LoginManager.getInstance()
                .logInWithReadPermissions(this, listOf("public_profile", "email"))
        }

        // Load and display the login cover image
        val loginImage: ImageView = findViewById(R.id.loginImage)
        Picasso.get().load(R.drawable.login_cover).into(loginImage)

        // Set click listener for the back button
        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()
        }

        // Set click listener for the forgot password text view
        forgotPasswordDialog = Dialog(this)
        forgotPasswordDialog.setContentView(R.layout.forgot_password)

        val forgotPasswordTextView: TextView = findViewById(R.id.forgotPasswordTextView)
        forgotPasswordTextView.setOnClickListener {
            showForgotPasswordDialog()
        }

    }

    // Sign in with email and password
    private fun signInWithEmailAndPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, start MapActivity
                    val intent = Intent(this, MapActivity::class.java)
                    startActivity(intent)
                    finish() // Finish LoginActivity to prevent going back to it after successful login
                } else {
                    // Sign in failed, display an error message
                    Toast.makeText(
                        this,
                        "Inicio de sesión fallido. Verifica tus credenciales e intenta nuevamente.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    // Handle Google sign-in result
    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            Toast.makeText(
                this,
                "Error al iniciar sesión con Google. Por favor, inténtalo nuevamente.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Authenticate with Firebase using the Google account
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, start MapActivity
                    val intent = Intent(this, MapActivity::class.java)
                    startActivity(intent)
                    finish() // Finish LoginActivity to prevent going back to it after successful login
                } else {
                    // Sign in failed, display an error message
                    Toast.makeText(
                        this,
                        "Inicio de sesión con Google fallido. Por favor, inténtalo nuevamente.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    // Handle Facebook login result
    override fun onSuccess(result: LoginResult?) {
        result?.let {
            val credential = FacebookAuthProvider.getCredential(it.accessToken.token)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, start MapActivity
                        val intent = Intent(this, MapActivity::class.java)
                        startActivity(intent)
                        finish() // Finish LoginActivity to prevent going back to it after successful login
                    } else {
                        // Sign in failed, display an error message
                        Toast.makeText(
                            this,
                            "Inicio de sesión con Facebook fallido. Por favor, inténtalo nuevamente.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    override fun onCancel() {
        // Facebook login canceled by the user
    }

    override fun onError(error: FacebookException?) {
        Toast.makeText(
            this,
            "Error al iniciar sesión con Facebook. Por favor, inténtalo nuevamente.",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleGoogleSignInResult(task)
        }
    }

    // Show the forgot password dialog
    private fun showForgotPasswordDialog() {
        val closeButton: ImageView = forgotPasswordDialog.findViewById(R.id.forgotPasswordLayout)
        closeButton.setOnClickListener {
            forgotPasswordDialog.dismiss()
        }

        forgotPasswordDialog.show()
    }
}