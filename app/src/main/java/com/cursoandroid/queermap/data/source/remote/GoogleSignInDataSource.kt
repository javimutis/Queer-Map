package com.cursoandroid.queermap.data.source.remote

import android.content.Context
import android.content.Intent
import com.cursoandroid.queermap.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface GoogleSignInDataSource {
    fun getSignInIntent(): Intent
    suspend fun handleSignInResult(data: Intent?): Result<String>
}

@Singleton
class GoogleSignInDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GoogleSignInDataSource {

    private val signInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    override fun getSignInIntent(): Intent {
        return signInClient.signInIntent
    }

    override suspend fun handleSignInResult(data: Intent?): Result<String> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { Result.success(it) }
                ?: Result.failure(Exception("Google ID token es nulo."))
        } catch (e: ApiException) {
            Result.failure(Exception("Error de Google Sign-In: ${e.statusCode} - ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}