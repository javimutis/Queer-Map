package com.cursoandroid.queermap.domain.usecase

import com.cursoandroid.queermap.domain.repository.AuthRepository
import javax.inject.Inject

class LoginWithEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String) =
        authRepository.loginWithEmailAndPassword(email, password)
}
