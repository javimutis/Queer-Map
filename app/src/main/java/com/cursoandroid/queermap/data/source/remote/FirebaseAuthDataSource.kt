package com.cursoandroid.queermap.data.source.remote

import com.cursoandroid.queermap.data.source.AuthRemoteDataSource
import com.cursoandroid.queermap.domain.model.User
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRemoteDataSource {

    override suspend fun loginWithEmailAndPassword(email: String, password: String): Result<User> =
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                // Se podría cargar el User completo desde Firestore si existe
                val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
                if (userDoc.exists()) {
                    val userFromFirestore = userDoc.toObject(User::class.java)
                    Result.success(userFromFirestore ?: User(firebaseUser.uid, firebaseUser.displayName, null, firebaseUser.email, null))
                } else {
                    // Si el usuario no está en Firestore, devolver con datos de Firebase Auth
                    Result.success(User(firebaseUser.uid, firebaseUser.displayName, null, firebaseUser.email, null))
                }
            } else {
                Result.failure(Exception("Usuario no encontrado."))
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

    // Asegurar que el User devuelto tenga los campos necesarios
    override suspend fun firebaseAuthWithGoogle(idToken: String): Result<User> = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        val firebaseUser = authResult.user
        if (firebaseUser != null) {
            // Mejorar la creación del objeto User para reflejar el estado real
            Result.success(User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName,
                username = null, // Username aún no disponible para social login inicial
                email = firebaseUser.email,
                birthday = null // Birthday aún no disponible
            ))
        } else {
            Result.failure(Exception("Autenticación con Google fallida: Usuario nulo."))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Asegurar que el User devuelto tenga los campos necesarios
    override suspend fun firebaseAuthWithFacebook(accessToken: String): Result<User> = try {
        val credential = FacebookAuthProvider.getCredential(accessToken)
        val authResult = auth.signInWithCredential(credential).await()
        val firebaseUser = authResult.user
        if (firebaseUser != null) {
            // Mejorar la creación del objeto User para reflejar el estado real
            Result.success(User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName,
                username = null, // Username aún no disponible para social login inicial
                email = firebaseUser.email,
                birthday = null // Birthday aún no disponible
            ))
        } else {
            Result.failure(Exception("Autenticación con Facebook fallida: Usuario nulo."))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}