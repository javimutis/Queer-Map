package com.cursoandroid.queermap.data.repository

import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val remoteDataSource: com.cursoandroid.queermap.data.source.remote.FirebaseAuthDataSource
) : AuthRepository {

    override suspend fun loginWithEmailAndPassword(email: String, password: String): Result<User> =
        remoteDataSource.loginWithEmailAndPassword(email, password)

    override suspend fun verifyUserInFirestore(uid: String): Result<DocumentSnapshot> =
        remoteDataSource.verifyUserInFirestore(uid)

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
        remoteDataSource.sendPasswordResetEmail(email)

    override suspend fun firebaseAuthWithGoogle(idToken: String): Result<Boolean> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun firebaseAuthWithFacebook(token: String): Result<Boolean> {
        return try {
            val credential = FacebookAuthProvider.getCredential(token)
            firebaseAuth.signInWithCredential(credential).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
