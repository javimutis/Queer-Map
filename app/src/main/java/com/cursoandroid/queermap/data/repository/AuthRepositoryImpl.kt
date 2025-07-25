package com.cursoandroid.queermap.data.repository

import com.cursoandroid.queermap.data.source.AuthRemoteDataSource
import com.cursoandroid.queermap.data.source.local.SharedPreferencesDataSource
import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.util.Result
import com.cursoandroid.queermap.util.failure // Importa tus helpers
import com.cursoandroid.queermap.util.success // Importa tus helpers
import com.cursoandroid.queermap.util.getOrThrow // Asegúrate de importar tu extensión
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
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
        // Asume que remoteDataSource.loginWithEmailAndPassword ya devuelve tu custom Result<User>
        return remoteDataSource.loginWithEmailAndPassword(email, password)
    }

    override suspend fun verifyUserInFirestore(uid: String): Result<Boolean> {
        return try {
            // Usa getOrThrow de tu custom Result para obtener el snapshot
            val snapshot = remoteDataSource.verifyUserInFirestore(uid).getOrThrow()
            success(snapshot.exists()) // Usa tu custom success helper
        } catch (e: Exception) {
            failure(e) // Usa tu custom failure helper
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
                ?: return failure(Exception("Usuario nulo después del registro."))
            val userId = firebaseUser.uid

            val userMap = mapOf(
                "id" to userId,
                "name" to (user.name ?: ""),
                "username" to (user.username ?: ""),
                "email" to (user.email ?: ""),
                "birthday" to (user.birthday ?: "")
            )
            firestore.collection("users").document(userId).set(userMap).await()
            success(Unit)
        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthUserCollisionException -> failure(Exception("Este email ya está registrado."))
                is FirebaseAuthWeakPasswordException -> failure(Exception("La contraseña es demasiado débil."))
                is FirebaseAuthInvalidCredentialsException -> failure(Exception("Email o contraseña inválidos."))
                else -> failure(e)
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
            user.email?.let {
                userMap["email"] = it
            }

            firestore.collection("users").document(uid).set(userMap, SetOptions.merge()).await()
            success(Unit)
        } catch (e: Exception) {
            failure(e)
        }
    }
}