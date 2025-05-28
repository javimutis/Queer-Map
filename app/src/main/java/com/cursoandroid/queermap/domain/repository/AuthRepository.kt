package com.cursoandroid.queermap.domain.repository

import com.cursoandroid.queermap.domain.model.User

interface AuthRepository {
    suspend fun loginWithEmailAndPassword(email: String, password: String): Result<User>
    suspend fun verifyUserInFirestore(uid: String): Result<Boolean>
    suspend fun sendResetPasswordEmail(email: String): Result<Unit>

    suspend fun firebaseAuthWithGoogle(idToken: String): Result<User>
    suspend fun firebaseAuthWithFacebook(token: String): Result<User>

    fun saveCredentials(email: String, password: String)
    fun loadSavedCredentials(): Pair<String?, String?>
    suspend fun registerUser(user: User, password: String): Result<Unit>
}