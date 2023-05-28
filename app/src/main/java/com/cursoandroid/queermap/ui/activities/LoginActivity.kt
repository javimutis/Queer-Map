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

        auth = FirebaseAuth.getInstance()

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        callbackManager = CallbackManager.Factory.create()
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        rememberCheckBox = findViewById(R.id.rememberCheckBox)

        val loginButton: Button = findViewById(R.id.login_button)
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (ValidationUtils.isValidEmail(email) && ValidationUtils.isValidPassword(password)) {
                signInWithEmailAndPassword(email, password)
            } else {
                Toast.makeText(
                    this,
                    "Por favor, ingresa un correo electrónico válido y una contraseña segura.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val googleSignInButton: ImageButton = findViewById(R.id.googleSignInButton)
        Picasso.get().load(R.drawable.google_icon).into(googleSignInButton)
        googleSignInButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
        }

        val facebookLoginButton: ImageButton = findViewById(R.id.facebookLoginButton)
        Picasso.get().load(R.drawable.facebook_icon).into(facebookLoginButton)
        facebookLoginButton.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("public_profile", "email"))
        }

        val loginImage: ImageView = findViewById(R.id.loginImage)
        Picasso.get().load(R.drawable.login_cover).into(loginImage)

        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, CoverActivity::class.java)
            startActivity(intent)
            finish()
        }

        forgotPasswordDialog = Dialog(this)
        forgotPasswordDialog.setContentView(R.layout.forgot_password)

        val forgotPasswordTextView: TextView = findViewById(R.id.forgotPasswordTextView)
        forgotPasswordTextView.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun signInWithEmailAndPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val intent = Intent(this, MapActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this, "Error al iniciar sesión. Verifica tus credenciales.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        val rememberMe = rememberCheckBox.isChecked

        if (rememberMe) {
            saveCredentials(email, password)
        }

        Toast.makeText(this, "Iniciando sesión...", Toast.LENGTH_SHORT).show()
    }

    private fun saveCredentials(email: String, password: String) {
        val sharedPref = getSharedPreferences("login", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("email", email)
        editor.putString("password", password)
        editor.apply()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleGoogleSignInResult(task)
        } else {
            callbackManager.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>?) {
        try {
            val account = task?.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                firebaseAuthWithGoogle(idToken)
            }
        } catch (e: ApiException) {
            Toast.makeText(
                this, "Error al iniciar sesión con Google. Verifica tus credenciales.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val intent = Intent(this, MapActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Error al iniciar sesión con Google. Por favor, inténtalo de nuevo.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    override fun onSuccess(loginResult: LoginResult) {
        val accessToken = loginResult.accessToken
        val intent = Intent(this@LoginActivity, MapActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onCancel() {
        Toast.makeText(
            this@LoginActivity, "Inicio de sesión con Facebook cancelado.",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onError(error: FacebookException) {
        Toast.makeText(
            this@LoginActivity, "Error al iniciar sesión con Facebook. Verifica tus credenciales.",
            Toast.LENGTH_SHORT
        ).show()
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
}
