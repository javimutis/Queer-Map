package com.cursoandroid.queermap.data.source.remote

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.cursoandroid.queermap.R

class GoogleSignInDataSource(private val context: Context) {

    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id)) // debe estar en tu `google-services.json`
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(context, gso)
    }
}
