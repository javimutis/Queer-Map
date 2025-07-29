package com.cursoandroid.queermap.data.source.remote

import com.cursoandroid.queermap.data.source.AuthRemoteDataSource
import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.util.Result // <-- ¡IMPORTANTE! Asegúrate de importar tu clase Result personalizada aquí
import com.cursoandroid.queermap.util.success // <-- ¡AÑADIDO! Importa tu función helper 'success'
import com.cursoandroid.queermap.util.failure // <-- ¡AÑADIDO! Importa tu función helper 'failure'
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
                    // Usa tu función helper 'success'
                    success(userFromFirestore ?: User(firebaseUser.uid, firebaseUser.displayName, null, firebaseUser.email, null))
                } else {
                    // Si el usuario no está en Firestore, devolver con datos de Firebase Auth
                    // Usa tu función helper 'success'
                    success(User(firebaseUser.uid, firebaseUser.displayName, null, firebaseUser.email, null))
                }
            } else {
                // FIX: Explicitly pass the message to the failure helper
                failure(Exception("Usuario no encontrado."), "Usuario no encontrado.")
            }
        } catch (e: Exception) {
            // FIX: Explicitly pass the exception's message to the failure helper
            failure(e, e.message)
        }

    override suspend fun verifyUserInFirestore(uid: String): Result<DocumentSnapshot> = try {
        val doc = firestore.collection("users").document(uid).get().await()
        // Usa tu función helper 'success'
        success(doc)
    } catch (e: Exception) {
        // FIX: Explicitly pass the exception's message to the failure helper
        failure(e, e.message)
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        // Usa tu función helper 'success'
        success(Unit)
    } catch (e: Exception) {
        // FIX: Explicitly pass the exception's message to the failure helper
        failure(e, e.message)
    }

    // Asegurar que el User devuelto tenga los campos necesarios
    override suspend fun firebaseAuthWithGoogle(idToken: String): Result<User> = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        val firebaseUser = authResult.user
        if (firebaseUser != null) {
            // Mejorar la creación del objeto User para reflejar el estado real
            // Usa tu función helper 'success'
            success(User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName,
                username = null, // Username aún no disponible para social login inicial
                email = firebaseUser.email,
                birthday = null // Birthday aún no disponible
            ))
        } else {
            // FIX: Explicitly pass the message to the failure helper
            failure(Exception("Autenticación con Google fallida: Usuario nulo."), "Autenticación con Google fallida: Usuario nulo.")
        }
    } catch (e: Exception) {
        // FIX: Explicitly pass the exception's message to the failure helper
        failure(e, e.message)
    }

    // Asegurar que el User devuelto tenga los campos necesarios
    override suspend fun firebaseAuthWithFacebook(accessToken: String): Result<User> = try {
        val credential = FacebookAuthProvider.getCredential(accessToken)
        val authResult = auth.signInWithCredential(credential).await()
        val firebaseUser = authResult.user
        if (firebaseUser != null) {
            // Mejorar la creación del objeto User para reflejar el estado real
            // Usa tu función helper 'success'
            success(User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName,
                username = null, // Username aún no disponible para social login inicial
                email = firebaseUser.email,
                birthday = null // Birthday aún no disponible
            ))
        } else {
            // FIX: Explicitly pass the message to the failure helper
            failure(Exception("Autenticación con Facebook fallida: Usuario nulo."), "Autenticación con Facebook fallida: Usuario nulo.")
        }
    } catch (e: Exception) {
        // FIX: Explicitly pass the exception's message to the failure helper
        failure(e, e.message)
    }
}
