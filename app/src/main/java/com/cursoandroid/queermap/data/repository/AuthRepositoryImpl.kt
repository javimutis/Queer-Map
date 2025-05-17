package com.cursoandroid.queermap.data.repository

import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val remote: AuthRepository
) : AuthRepository {
    override suspend fun loginWithEmailAndPassword(email: String, password: String): Result<User> =
        remote.loginWithEmailAndPassword(email, password)

    override suspend fun verifyUserInFirestore(uid: String): Result<DocumentSnapshot> =
        remote.verifyUserInFirestore(uid)

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

