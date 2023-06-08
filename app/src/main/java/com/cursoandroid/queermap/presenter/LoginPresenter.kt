package com.cursoandroid.queermap.presenter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.cursoandroid.queermap.interfaces.LoginContract
import com.cursoandroid.queermap.utils.ValidationUtils
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginPresenter(
    private val view: LoginContract.View,
    private val auth: FirebaseAuth
) : LoginContract.Presenter {

    companion object {
        const val RC_GOOGLE_SIGN_IN = 123
    }

    override fun onLoginButtonClick(email: String, password: String) {
        if (isValidEmail(email) && isValidPassword(password)) {
            view.showSigningInMessage()
            signInWithEmailAndPassword(email, password)
        } else {
            view.showInvalidCredentialsError()
        }
    }

    override fun onForgotPasswordClick(email: String) {
        sendPasswordResetEmail(email)
    }

    override fun onGoToSignInActivityClick() {
        view.goToSignInActivity()
    }

    override fun onGoogleSignInButtonClicked() {
        val signInIntent = view.getGoogleSignInIntent()
        view.showGoogleSignInIntent(signInIntent)
    }

    override fun handleGoogleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task?.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                firebaseAuthWithGoogle(idToken)
            }
        } catch (e: ApiException) {
            view.showGoogleSignInError()
        }
    }

    override fun onFacebookLoginClicked() {
        LoginManager.getInstance().logInWithReadPermissions(
            view as Activity, listOf("public_profile", "email")
        )
    }

    override fun onFacebookLoginSuccess() {
        view.showTermsScreen()
    }

    override fun onFacebookLoginCancel() {
        Toast.makeText(
            view as Context, "Facebook login canceled.",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onFacebookLoginError() {
        view.showLoginError()
    }

    fun checkUserLoggedIn() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            view.showSignInSuccess()
        } else {
            // No need to load saved credentials in this case
        }
    }

    private fun signInWithEmailAndPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    view.showSignInSuccess()
                } else {
                    view.showSignInError()
                }
            }
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    view.showPasswordResetEmailSent(email)
                } else {
                    view.showPasswordResetEmailError()
                }
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    view.showTermsActivity()
                } else {
                    view.showGoogleSignInErrorMessage()
                }
            }
    }

    private fun isValidEmail(email: String): Boolean {
        return ValidationUtils.isValidEmail(email)
    }

    private fun isValidPassword(password: String): Boolean {
        return ValidationUtils.isValidPassword(password)
    }
}
