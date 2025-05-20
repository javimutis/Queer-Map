package com.cursoandroid.queermap.domain.usecase

import com.cursoandroid.queermap.domain.repository.AuthRepository
import javax.inject.Inject

class SendResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        return authRepository.sendPasswordResetEmail(email)
    }
}
