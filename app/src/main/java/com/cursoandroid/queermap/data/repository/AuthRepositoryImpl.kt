package com.cursoandroid.queermap.data.repository

import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val remote: AuthRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
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

    override suspend fun firebaseAuthWithGoogle(idToken: String): Result<Boolean> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun firebaseAuthWithFacebook(token: String): Result<Boolean> {
        return try {
            val credential = FacebookAuthProvider.getCredential(token.toString())
            val authResult = auth.signInWithCredential(credential).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}

