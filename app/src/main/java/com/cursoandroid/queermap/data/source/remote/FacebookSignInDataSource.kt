package com.cursoandroid.queermap.data.source.remote

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

// IMPORTANTE: Asegúrate de importar tu clase Result personalizada aquí
import com.cursoandroid.queermap.util.Result
import com.cursoandroid.queermap.util.success
import com.cursoandroid.queermap.util.failure

interface FacebookSignInDataSource {
    fun registerCallback(callbackManager: CallbackManager, facebookCallback: FacebookCallback<LoginResult>)
    fun logInWithReadPermissions(fragment: androidx.fragment.app.Fragment, permissions: List<String>)
    // Asegúrate de que este Flow use tu Result personalizado
    val accessTokenChannel: Flow<Result<String>>
}

@Singleton
class FacebookSignInDataSourceImpl @Inject constructor() : FacebookSignInDataSource {

    // Asegúrate de que este Channel use tu Result personalizado
    private val _accessTokenChannel = Channel<Result<String>>(Channel.BUFFERED)
    override val accessTokenChannel: Flow<Result<String>> = _accessTokenChannel.receiveAsFlow()

    override fun registerCallback(callbackManager: CallbackManager, facebookCallback: FacebookCallback<LoginResult>) {
        LoginManager.getInstance().registerCallback(callbackManager, facebookCallback)
    }

    override fun logInWithReadPermissions(fragment: androidx.fragment.app.Fragment, permissions: List<String>) {
        LoginManager.getInstance().logInWithReadPermissions(fragment, permissions)
    }
}
