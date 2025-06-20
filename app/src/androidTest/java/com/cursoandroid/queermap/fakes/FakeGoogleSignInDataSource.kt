package com.cursoandroid.queermap.fakes

import android.content.Context
import android.content.Intent
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource

class FakeGoogleSignInDataSource(
    private val context: Context
) : GoogleSignInDataSource {
    var wasCalled = false

    override fun getSignInIntent(): Intent {
        wasCalled = true
        // Devuelve un intent inofensivo que no lanza nada
        return Intent()
    }

    override suspend fun handleSignInResult(data: Intent?): Result<String> {
        TODO("Not yet implemented")
    }
}
