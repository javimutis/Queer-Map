package com.cursoandroid.queermap.domain.usecase.auth

import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.util.Result // <-- IMPORTANT: Use your custom Result
import com.cursoandroid.queermap.util.success // <-- Import your custom success helper
import com.cursoandroid.queermap.util.failure // <-- Import your custom failure helper
import com.cursoandroid.queermap.util.getOrThrow // <-- Import your custom getOrThrow extension
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class RegisterWithFacebookUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) {
    suspend operator fun invoke(accessToken: String): Result<User> {
        return try {
            val firebaseUserResult = authRepository.firebaseAuthWithFacebook(accessToken)

            // Use getOrThrow from your custom Result and then process it
            firebaseUserResult.getOrThrow().let { user ->
                val userId = user.id
                if (userId != null) {
                    val userDocRef = firestore.collection("users").document(userId)
                    val docSnapshot = userDocRef.get().await()

                    if (!docSnapshot.exists()) {
                        val userMap = hashMapOf(
                            "id" to userId,
                            "email" to (user.email ?: ""), // Ensure non-null for Firestore
                            "name" to (user.name ?: ""), // Ensure non-null for Firestore
                            "username" to (user.username ?: ""), // Ensure non-null for Firestore
                            "birthday" to (user.birthday ?: "") // Ensure non-null for Firestore
                        )
                        userDocRef.set(userMap).await()
                    }
                    // Return your custom success result
                    success(user)
                } else {
                    // Return your custom failure result
                    failure(Exception("ID de usuario de Firebase es nulo después de la autenticación de Facebook."))
                }
            }
        } catch (e: Exception) {
            // Return your custom failure result
            failure(e)
        }
    }
}