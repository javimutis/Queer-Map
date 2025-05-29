package com.cursoandroid.queermap.data.repository

import com.cursoandroid.queermap.data.source.AuthRemoteDataSource
import com.cursoandroid.queermap.data.source.local.SharedPreferencesDataSource
import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: AuthRemoteDataSource,
    private val sharedPreferencesDataSource: SharedPreferencesDataSource,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun loginWithEmailAndPassword(email: String, password: String): Result<User> {
        return remoteDataSource.loginWithEmailAndPassword(email, password)
    }

    override suspend fun verifyUserInFirestore(uid: String): Result<Boolean> {
        return try {
            val snapshot = remoteDataSource.verifyUserInFirestore(uid).getOrThrow()
            Result.success(snapshot.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendResetPasswordEmail(email: String): Result<Unit> {
        return remoteDataSource.sendPasswordResetEmail(email)
    }

    override suspend fun firebaseAuthWithGoogle(idToken: String): Result<User> {
        return remoteDataSource.firebaseAuthWithGoogle(idToken)
    }

    override suspend fun firebaseAuthWithFacebook(token: String): Result<User> {
        return remoteDataSource.firebaseAuthWithFacebook(token)
    }

    override fun saveCredentials(email: String, password: String) {
        sharedPreferencesDataSource.saveCredentials(email, password)
    }

    override fun loadSavedCredentials(): Pair<String?, String?> {
        return sharedPreferencesDataSource.loadSavedCredentials()
    }

    override suspend fun registerUser(user: User, password: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(user.email!!, password).await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception("Usuario nulo después del registro."))
            val userId = firebaseUser.uid
            val userMap = mapOf(
                "id" to userId,
                "name" to user.name,
                "username" to user.username,
                "email" to user.email,
                "birthday" to user.birthday
            )
            firestore.collection("users").document(userId).set(userMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserProfile(uid: String, user: User): Result<Unit> {
        return try {
            val userMap = mutableMapOf<String, Any>(
                "id" to uid,
                "name" to (user.name ?: ""),
                "username" to (user.username ?: ""),
                "birthday" to (user.birthday ?: "")
            )

            user.email?.let {
                userMap["email"] = it
            }

            firestore.collection("users").document(uid).set(userMap, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}