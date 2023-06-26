package com.cursoandroid.queermap.activities

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.utils.ValidationUtils
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.squareup.picasso.Picasso

// Actividad principal
class LoginActivity : AppCompatActivity(), FacebookCallback<LoginResult> {

    // Variables miembro
    private lateinit var auth: FirebaseAuth
    private lateinit var forgotPasswordDialog: Dialog
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var rememberCheckBox: CheckBox
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    private lateinit var eyeIcon: ImageView
    private val RC_GOOGLE_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initializeFirebaseAuthentication()
        initializeViews()
        initializeGoogleSignIn()
        initializeFacebookLogin()
        initializeForgotPasswordDialog()
        loadSavedCredentials()
        checkUserLoggedIn()
    }

    // Inicializa la autenticación de Firebase
    private fun initializeFirebaseAuthentication() {
        auth = FirebaseAuth.getInstance()
    }

    // Inicializa las vistas y asigna los listeners a los botones
    private fun initializeViews() {
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        rememberCheckBox = findViewById(R.id.rememberCheckBox)
        eyeIcon = findViewById(R.id.eyeIcon)

        eyeIcon.setOnClickListener {
            togglePasswordVisibility()
        }

        val loginButton: Button = findViewById(R.id.login_button)
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            onLoginButtonClick(email, password)
        }

        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, CoverActivity::class.java)
            startActivity(intent)
            finish()
        }
        val loginSigninButton: TextView = findViewById(R.id.loginSignin)
        loginSigninButton.setOnClickListener {
            goToSignInActivity()
        }

        val forgotPasswordTextView: TextView = findViewById(R.id.forgotPasswordTextView)
        forgotPasswordTextView.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    // Inicializa el inicio de sesión de Google
    private fun initializeGoogleSignIn() {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        val googleSignInButton: ImageButton = findViewById(R.id.googleSignInButton)
        Picasso.get().load(R.drawable.google_icon).into(googleSignInButton)
        googleSignInButton.setOnClickListener {
            onGoogleSignInButtonClicked()
        }
    }

    // Inicializa el inicio de sesión de Facebook
    private fun initializeFacebookLogin() {
        callbackManager = CallbackManager.Factory.create()

        val facebookLoginButton: ImageButton = findViewById(R.id.facebookLoginButton)
        Picasso.get().load(R.drawable.facebook_icon).into(facebookLoginButton)
        facebookLoginButton.setOnClickListener {
            onFacebookLoginClicked()
        }
    }

    // Inicializa el diálogo de restablecimiento de contraseña
    private fun initializeForgotPasswordDialog() {
        forgotPasswordDialog = Dialog(this)
        forgotPasswordDialog.setContentView(R.layout.forgot_password)

        val resetButton: Button = forgotPasswordDialog.findViewById(R.id.resetPasswordButton)
        val cancelButton: Button = forgotPasswordDialog.findViewById(R.id.cancelButton)
        val emailEditText: EditText = forgotPasswordDialog.findViewById(R.id.emailEditText)

        resetButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                onForgotPasswordClick(email)
                forgotPasswordDialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            forgotPasswordDialog.dismiss()
        }
    }

    // Acción cuando se hace clic en el botón de restablecimiento de contraseña
    private fun onForgotPasswordClick(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showPasswordResetEmailSent(email)
                } else {
                    showPasswordResetEmailError()
                }
            }
    }

    // Alterna la visibilidad de la contraseña
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

    // Carga las credenciales guardadas
    private fun loadSavedCredentials() {
        val sharedPref = getSharedPreferences("login", Context.MODE_PRIVATE)
        val email = sharedPref.getString("email", "")
        val password = sharedPref.getString("password", "")

        emailEditText.setText(email)
        passwordEditText.setText(password)
        rememberCheckBox.isChecked = true
    }

    // Verifica si el usuario ha iniciado sesión
    private fun checkUserLoggedIn() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            showSignInSuccess()
        }
    }

    // Acción cuando se hace clic en el botón de inicio de sesión
    private fun onLoginButtonClick(email: String, password: String) {
        if (isValidEmail(email) && isValidPassword(password)) {
            showSigningInMessage()
            signInWithEmailAndPassword(email, password)
        } else {
            showInvalidCredentialsError()
        }
    }

    // Inicia sesión con correo electrónico y contraseña
    private fun signInWithEmailAndPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showSignInSuccess()
                } else {
                    showSignInError()
                }
            }

        val rememberMe = rememberCheckBox.isChecked

        if (rememberMe) {
            saveCredentials(email, password)
        }
    }

    // Guarda las credenciales en SharedPreferences
    private fun saveCredentials(email: String, password: String) {
        val sharedPref = getSharedPreferences("login", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("email", email)
        editor.putString("password", password)
        editor.apply()
    }

    // Muestra el diálogo de restablecimiento de contraseña
    private fun showForgotPasswordDialog() {
        forgotPasswordDialog.show()
    }

    override fun onSuccess(loginResult: LoginResult) {
        onFacebookLoginSuccess()
    }

    override fun onCancel() {
        onFacebookLoginCancel()
    }

    override fun onError(error: FacebookException) {
        onFacebookLoginError()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        callbackManager.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            handleGoogleSignInResult(data)
        }
    }

    // Acción cuando se hace clic en el botón de inicio de sesión de Google
    private fun onGoogleSignInButtonClicked() {
        val signInIntent = googleSignInClient.signInIntent
        showGoogleSignInIntent(signInIntent)
    }

    // Muestra el intento de inicio de sesión de Google
    private fun showGoogleSignInIntent(signInIntent: Intent) {
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
    }

    // Maneja el resultado del inicio de sesión de Google
    private fun handleGoogleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task?.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                firebaseAuthWithGoogle(idToken)
            }
        } catch (e: ApiException) {
            showGoogleSignInError()
        }
    }

    // Acción cuando se hace clic en el inicio de sesión de Facebook
    private fun onFacebookLoginClicked() {
        LoginManager.getInstance().logInWithReadPermissions(
            this as Activity, listOf("public_profile", "email")
        )
    }

    // Acción cuando el inicio de sesión de Facebook es exitoso
    private fun onFacebookLoginSuccess() {
        showReadTermsScreen()
    }

    // Acción cuando se cancela el inicio de sesión de Facebook
    private fun onFacebookLoginCancel() {
        Toast.makeText(this as Context, "Facebook login canceled.", Toast.LENGTH_SHORT).show()
    }

    // Acción cuando ocurre un error en el inicio de sesión de Facebook
    private fun onFacebookLoginError() {
        showLoginError()
    }

    // Autentica con Google en Firebase
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showReadTermsScreen()
                } else {
                    showGoogleSignInErrorMessage()
                }
            }
    }

    // Envía un correo electrónico de restablecimiento de contraseña
    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showPasswordResetEmailSent(email)
                } else {
                    showPasswordResetEmailError()
                }
            }
    }

    // Valida si el correo electrónico es válido
    private fun isValidEmail(email: String): Boolean {
        return ValidationUtils.isValidEmail(email)
    }

    // Valida si la contraseña es válida
    private fun isValidPassword(password: String): Boolean {
        return ValidationUtils.isValidPassword(password)
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

    // Muestra un mensaje de éxito para el restablecimiento de contraseña
    private fun showPasswordResetEmailSent(email: String) {
        Toast.makeText(
            this,
            "Password reset email sent to $email",
            Toast.LENGTH_SHORT
        ).show()
    }

    // Muestra un mensaje de error para el restablecimiento de contraseña
    private fun showPasswordResetEmailError() {
        Toast.makeText(this, "Failed to send password reset email", Toast.LENGTH_SHORT).show()
    }

    // Muestra un mensaje de error de inicio de sesión de Google
    private fun showGoogleSignInError() {
        Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show()
    }

    // Muestra un mensaje de error de inicio de sesión de Google
    private fun showGoogleSignInErrorMessage() {
        Toast.makeText(this, "Google sign in error", Toast.LENGTH_SHORT).show()
    }

    // Muestra un mensaje de error de inicio de sesión
    private fun showLoginError() {
        Toast.makeText(this, "Login error", Toast.LENGTH_SHORT).show()
    }

    // Abre la actividad de términos y condiciones
    private fun showReadTermsScreen() {
        val intent = Intent(this, ReadTermsActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Abre la actividad de registro
    private fun goToSignInActivity() {
        val intent = Intent(this, SigninActivity::class.java)
        startActivity(intent)
    }
}
