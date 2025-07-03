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
    // MODIFICACIÓN: Añade FacebookCallback como segundo argumento
    fun registerCallback(callbackManager: CallbackManager, facebookCallback: FacebookCallback<LoginResult>)
    fun logInWithReadPermissions(fragment: androidx.fragment.app.Fragment, permissions: List<String>)
    val accessTokenChannel: Flow<Result<String>>
}

@Singleton
class FacebookSignInDataSourceImpl @Inject constructor() : FacebookSignInDataSource {

    private val _accessTokenChannel = Channel<Result<String>>(Channel.BUFFERED)
    override val accessTokenChannel: Flow<Result<String>> = _accessTokenChannel.receiveAsFlow()

    // MODIFICACIÓN: Implementa el nuevo método registerCallback con ambos argumentos
    override fun registerCallback(callbackManager: CallbackManager, facebookCallback: FacebookCallback<LoginResult>) {
        LoginManager.getInstance().registerCallback(callbackManager, facebookCallback)
    }

    override fun logInWithReadPermissions(fragment: androidx.fragment.app.Fragment, permissions: List<String>) {
        LoginManager.getInstance().logInWithReadPermissions(fragment, permissions)
    }
}