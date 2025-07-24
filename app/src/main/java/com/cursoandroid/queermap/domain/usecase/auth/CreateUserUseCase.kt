// In CreateUserUseCase.kt
package com.cursoandroid.queermap.domain.usecase.auth

import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.util.Result // Make sure this is YOUR Result class
import javax.inject.Inject

class CreateUserUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    // The operator fun invoke should also be a suspend function
    suspend operator fun invoke(user: User, password: String): Result<Unit> {
        return repository.registerUser(user, password)
    }
}