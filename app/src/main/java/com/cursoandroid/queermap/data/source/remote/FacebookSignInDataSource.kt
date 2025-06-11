package com.cursoandroid.queermap.data.source.remote

import android.content.Intent
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

interface FacebookSignInDataSource {
    fun registerCallback(callbackManager: CallbackManager)
    fun logInWithReadPermissions(fragment: androidx.fragment.app.Fragment, permissions: List<String>)
    // handleActivityResult ya no es necesario aquí, lo maneja el CallbackManager
    val accessTokenChannel: Flow<Result<String>>
    // handleFacebookAccessToken no debería ser suspend, y su lógica se mueve al callback
}

@Singleton
class FacebookSignInDataSourceImpl @Inject constructor() : FacebookSignInDataSource {

    private val _accessTokenChannel = Channel<Result<String>>(Channel.BUFFERED) // Usar un buffer si se espera que las emisiones no siempre se recolecten inmediatamente
    override val accessTokenChannel: Flow<Result<String>> = _accessTokenChannel.receiveAsFlow()

    override fun registerCallback(callbackManager: CallbackManager) {
        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    loginResult.accessToken?.let {
                        _accessTokenChannel.trySend(Result.success(it.token))
                    } ?: _accessTokenChannel.trySend(Result.failure(Exception("Token de acceso de Facebook nulo."))) // Manejo de null token
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
}