package com.cursoandroid.queermap.data.repository

import com.cursoandroid.queermap.data.source.AuthRemoteDataSource
import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.google.firebase.firestore.DocumentSnapshot
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: AuthRemoteDataSource
) : AuthRepository {

    override suspend fun loginWithEmailAndPassword(email: String, password: String): Result<User> {
        return remoteDataSource.loginWithEmailAndPassword(email, password)
    }

    override suspend fun verifyUserInFirestore(uid: String): Result<DocumentSnapshot> {
        return remoteDataSource.verifyUserInFirestore(uid)
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return remoteDataSource.sendPasswordResetEmail(email)
    }

    override suspend fun firebaseAuthWithGoogle(idToken: String): Result<Boolean> {
        return remoteDataSource.firebaseAuthWithGoogle(idToken)
    }

    override suspend fun firebaseAuthWithFacebook(token: String): Result<Boolean> {
        return remoteDataSource.firebaseAuthWithFacebook(token)
    }
}
