package com.cursoandroid.queermap.data.source.remote

import com.cursoandroid.queermap.data.source.AuthRemoteDataSource
import com.cursoandroid.queermap.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRemoteDataSource {

    override suspend fun loginWithEmailAndPassword(email: String, password: String): Result<User> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user
        if (firebaseUser != null) {
            Result.success(User(firebaseUser.uid, firebaseUser.email))
        } else {
            Result.failure(Exception("Usuario no encontrado"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun verifyUserInFirestore(uid: String): Result<DocumentSnapshot> = try {
        val doc = firestore.collection("users").document(uid).get().await()
        Result.success(doc)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun firebaseAuthWithGoogle(idToken: String): Result<Boolean> = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun firebaseAuthWithFacebook(accessToken: String): Result<Boolean> = try {
        val credential = FacebookAuthProvider.getCredential(accessToken)
        auth.signInWithCredential(credential).await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
