package com.cursoandroid.queermap.domain.usecase.auth

import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class RegisterWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) {
    suspend operator fun invoke(idToken: String): Result<User> {
        return try {
            val firebaseUserResult = authRepository.firebaseAuthWithGoogle(idToken)

            firebaseUserResult.getOrThrow().let { user ->
                val userId = user.id
                if (userId != null) {
                    val userDocRef = firestore.collection("users").document(userId)
                    val docSnapshot = userDocRef.get().await()

                    if (!docSnapshot.exists()) {
                        val userMap = hashMapOf(
                            "id" to userId,
                            "email" to user.email,
                            "name" to user.name,
                            "username" to user.username,
                            "birthday" to user.birthday
                        )
                        userDocRef.set(userMap).await()
                    }
                    Result.success(user)
                } else {
                    Result.failure(Exception("ID de usuario de Firebase es nulo después de la autenticación de Google."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}