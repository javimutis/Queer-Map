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
import com.cursoandroid.queermap.util.Result // <--- ESTA LÍNEA ES FUNDAMENTAL
// Si por alguna razón Android Studio no la encuentra, asegúrate de que el paquete sea correcto
// (por ejemplo, si tu archivo Result.kt está en 'com.cursoandroid.queermap.common.Result',
// entonces la importación debería ser `import com.cursoandroid.queermap.common.Result`)


interface GoogleSignInDataSource {
    // CAMBIO: Asegura que el tipo de retorno use tu Result personalizado
    fun getSignInIntent(): Intent
    suspend fun handleSignInResult(data: Intent?): Result<String> // <--- CAMBIO AQUÍ
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

    override suspend fun handleSignInResult(data: Intent?): Result<String> { // <--- CAMBIO AQUÍ
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)

            // ¡¡¡CAMBIO CRÍTICO: Usa el constructor de tu clase Success/Failure!!!
            account?.idToken?.let {
                Result.Success(it) // <--- Usa el constructor de tu Result.Success
            } ?: Result.Failure(Exception("Google ID token es nulo.")) // <--- Usa el constructor de tu Result.Failure
        } catch (e: ApiException) {
            Result.Failure(e, "Error de Google Sign-In: ${e.statusCode} - ${e.message}") // <--- Usa el constructor de tu Result.Failure
        } catch (e: Exception) {
            Result.Failure(e) // <--- Usa el constructor de tu Result.Failure
        }
    }
}
