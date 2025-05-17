package com.cursoandroid.queermap.domain.usecase

class SendResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        return authRepository.sendPasswordResetEmail(email)
    }
}
