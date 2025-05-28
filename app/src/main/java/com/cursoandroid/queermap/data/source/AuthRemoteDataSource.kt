package com.cursoandroid.queermap.data.source

import com.cursoandroid.queermap.domain.model.User
import com.google.firebase.firestore.DocumentSnapshot

interface AuthRemoteDataSource {
    suspend fun loginWithEmailAndPassword(email: String, password: String): Result<User>
    suspend fun verifyUserInFirestore(uid: String): Result<DocumentSnapshot>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    suspend fun firebaseAuthWithGoogle(idToken: String): Result<User>
    suspend fun firebaseAuthWithFacebook(accessToken: String): Result<User>
}