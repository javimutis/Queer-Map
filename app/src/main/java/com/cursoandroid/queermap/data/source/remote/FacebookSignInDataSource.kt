package com.cursoandroid.queermap.data.source.remote

import android.app.Activity
import android.content.Context
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

//maneja el flujo de login con Facebook (callback, permisos).
class FacebookSignInDataSource(private val context: Context) {

    private val callbackManager = CallbackManager.Factory.create()

    fun getCallbackManager(): CallbackManager = callbackManager

    fun login(activity: Activity) {
        LoginManager.getInstance()
            .logInWithReadPermissions(activity, listOf("email", "public_profile"))
    }

    suspend fun handleFacebookAccessToken(result: Any): String =
        suspendCancellableCoroutine { cont ->
            val accessToken = result.accessToken.token
            cont.resume(accessToken)
        }

    fun registerCallback(
        onSuccess: (LoginResult) -> Unit,
        onCancel: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) = onSuccess(result)
                override fun onCancel() = onCancel()
                override fun onError(error: FacebookException) = onError(error)
            })
    }
}
