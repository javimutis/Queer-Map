package com.cursoandroid.queermap.domain.usecase

import com.cursoandroid.queermap.domain.repository.AuthRepository
import javax.inject.Inject

class LoginWithFacebookUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(token: String): Result<Boolean> {
        return repository.firebaseAuthWithFacebook(token)
    }
}
