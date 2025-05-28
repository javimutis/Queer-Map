package com.cursoandroid.queermap.domain.usecase.auth

import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository

class CreateUserUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(user: User, password: String): Result<Unit> {
        return authRepository.registerUser(user, password)
    }
}