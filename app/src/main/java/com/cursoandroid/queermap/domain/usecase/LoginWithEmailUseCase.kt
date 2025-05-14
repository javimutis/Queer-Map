package com.cursoandroid.queermap.domain.usecase

import com.cursoandroid.queermap.domain.repository.AuthRepository

class LoginWithEmailUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String) =
        authRepository.loginWithEmailAndPassword(email, password)
}
