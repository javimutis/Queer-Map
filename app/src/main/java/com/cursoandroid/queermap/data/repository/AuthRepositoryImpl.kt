package com.cursoandroid.queermap.data.repository

import com.cursoandroid.queermap.data.source.AuthRemoteDataSource
import com.cursoandroid.queermap.data.source.local.SharedPreferencesDataSource
import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException // Importar
import com.google.firebase.auth.FirebaseAuthWeakPasswordException // Importar
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException // Importar
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

    // Mejorado manejo de errores específicos de Firebase
    override suspend fun registerUser(user: User, password: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(user.email!!, password).await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception("Usuario nulo después del registro."))
            val userId = firebaseUser.uid

            val userMap = mapOf(
                "id" to userId,
                "name" to (user.name ?: ""), // Asegurar que no sea null
                "username" to (user.username ?: ""), // Asegurar que no sea null
                "email" to (user.email ?: ""), // Asegurar que no sea null
                "birthday" to (user.birthday ?: "") // Asegurar que no sea null
            )
            firestore.collection("users").document(userId).set(userMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Capturar excepciones específicas para mensajes claros
            when (e) {
                is FirebaseAuthUserCollisionException -> Result.failure(Exception("Este email ya está registrado."))
                is FirebaseAuthWeakPasswordException -> Result.failure(Exception("La contraseña es demasiado débil."))
                is FirebaseAuthInvalidCredentialsException -> Result.failure(Exception("Email o contraseña inválidos."))
                else -> Result.failure(e)
            }
        }
    }

    override suspend fun updateUserProfile(uid: String, user: User): Result<Unit> {
        return try {
            val userMap = mutableMapOf<String, Any>(
                "name" to (user.name ?: ""),
                "username" to (user.username ?: ""),
                "birthday" to (user.birthday ?: "")
            )
            // No incluir el id en el merge si ya es el ID del documento
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