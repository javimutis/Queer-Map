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

// ¡¡¡CAMBIO CRÍTICO: Importa tu clase Result personalizada!!!
import com.cursoandroid.queermap.util.Result
import com.cursoandroid.queermap.util.success
import com.cursoandroid.queermap.util.failure // Asegúrate de que esta importación esté presente

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

            account?.idToken?.let {
                success(it)
            } ?: failure(Exception("Google ID token es nulo."))
        } catch (e: ApiException) {
            // Esta línea ahora es válida porque 'failure' acepta el mensaje
            failure(e, "Error de Google Sign-In: ${e.statusCode} - ${e.message}")
        } catch (e: Exception) {
            failure(e)
        }
    }
}
