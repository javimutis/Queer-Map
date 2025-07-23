package com.cursoandroid.queermap.domain.usecase.auth

import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.util.Result // <-- IMPORTANT: Use your custom Result
import javax.inject.Inject

class LoginWithFacebookUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(token: String): Result<User> {
        return repository.firebaseAuthWithFacebook(token)
    }
}