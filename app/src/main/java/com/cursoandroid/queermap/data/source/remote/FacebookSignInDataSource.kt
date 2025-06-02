package com.cursoandroid.queermap.data.source.remote

import android.app.Activity
import android.content.Intent
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow // Import this
import kotlinx.coroutines.channels.ReceiveChannel
import javax.inject.Inject
import javax.inject.Singleton

interface FacebookSignInDataSource {
    fun registerCallback(callbackManager: CallbackManager)
    fun logInWithReadPermissions(fragment: androidx.fragment.app.Fragment, permissions: List<String>)
    fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    )
    val accessTokenChannel: Flow<Result<String>>
    suspend fun handleFacebookAccessToken(loginResult: LoginResult): String
}

@Singleton
class FacebookSignInDataSourceImpl @Inject constructor() : FacebookSignInDataSource {

    private val _accessTokenChannel = Channel<Result<String>>()
    override val accessTokenChannel: Flow<Result<String>> = _accessTokenChannel.receiveAsFlow() // Convert to Flow here

    override fun registerCallback(callbackManager: CallbackManager) {
        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    loginResult.accessToken?.let {
                        _accessTokenChannel.trySend(Result.success(it.token))
                    }
                }

                override fun onCancel() {
                    _accessTokenChannel.trySend(Result.failure(Exception("Inicio de sesión de Facebook cancelado.")))
                }

                override fun onError(error: FacebookException) {
                    _accessTokenChannel.trySend(Result.failure(Exception("Error de inicio de sesión de Facebook: ${error.message}")))
                }
            })
    }

    override fun logInWithReadPermissions(fragment: androidx.fragment.app.Fragment, permissions: List<String>) {
        LoginManager.getInstance().logInWithReadPermissions(fragment, permissions)
    }

    override suspend fun handleFacebookAccessToken(loginResult: LoginResult): String {
        return loginResult.accessToken.token
    }

    override fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        }
}