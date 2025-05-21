package com.cursoandroid.queermap.domain.usecase

import com.cursoandroid.queermap.domain.repository.AuthRepository
import javax.inject.Inject

class LoginWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): Result<Boolean> {
        return authRepository.firebaseAuthWithGoogle(idToken)
    }
}
