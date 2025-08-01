package com.cursoandroid.queermap.domain.usecase.auth

import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.util.Result
import com.cursoandroid.queermap.util.success
import com.cursoandroid.queermap.util.failure
import com.cursoandroid.queermap.util.isSuccess
import com.cursoandroid.queermap.util.isFailure
import com.cursoandroid.queermap.util.getOrNull
import com.cursoandroid.queermap.util.exceptionOrNull
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.Exception

class RegisterWithGoogleUseCaseTest {

    @MockK
    private lateinit var authRepository: AuthRepository
    @MockK
    private lateinit var firestore: FirebaseFirestore
    @MockK
    private lateinit var usersCollection: CollectionReference
    @MockK
    private lateinit var userDocument: DocumentReference
    @MockK
    private lateinit var documentSnapshot: DocumentSnapshot

    private lateinit var registerWithGoogleUseCase: RegisterWithGoogleUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { firestore.collection("users") } returns usersCollection
        every { usersCollection.document(any()) } returns userDocument
        coEvery { userDocument.get() } returns Tasks.forResult(documentSnapshot)
        coEvery { userDocument.set(any<Map<String, Any>>()) } returns Tasks.forResult(null)

        registerWithGoogleUseCase = RegisterWithGoogleUseCase(authRepository, firestore)
    }

    @Test
    fun `when google registration succeeds and user does not exist in firestore then create user and return success`() = runTest {
        val idToken = "some_id_token"
        val firebaseUser = User("testUid", "test@example.com", null, "Test User", null)
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns success(firebaseUser)
        every { documentSnapshot.exists() } returns false

        val result = registerWithGoogleUseCase(idToken)

        coVerify(exactly = 1) { authRepository.firebaseAuthWithGoogle(idToken) }
        coVerify(exactly = 1) { userDocument.get() }
        coVerify(exactly = 1) { userDocument.set(any<Map<String, Any>>()) }

        assertTrue(result.isSuccess())
        assertEquals(firebaseUser, result.getOrNull())
    }

    @Test
    fun `when google registration succeeds and user already exists in firestore then return success without creating user`() = runTest {
        val idToken = "some_id_token"
        val firebaseUser = User("testUid", "test@example.com", "existinguser", "Existing User", "01/01/1990")
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns success(firebaseUser)
        every { documentSnapshot.exists() } returns true

        val result = registerWithGoogleUseCase(idToken)

        coVerify(exactly = 1) { authRepository.firebaseAuthWithGoogle(idToken) }
        coVerify(exactly = 1) { userDocument.get() }
        coVerify(exactly = 0) { userDocument.set(any<Map<String, Any>>()) }

        assertTrue(result.isSuccess())
        assertEquals(firebaseUser, result.getOrNull())
    }

    @Test
    fun `when firebase auth with google fails then return failure`() = runTest {
        val idToken = "some_id_token"
        val expectedException = Exception("Firebase authentication failed")
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns failure(expectedException)

        val result = registerWithGoogleUseCase(idToken)

        coVerify(exactly = 1) { authRepository.firebaseAuthWithGoogle(idToken) }
        verify(exactly = 0) { firestore.collection(any()) }

        assertTrue(result.isFailure())
        assertEquals(expectedException, result.exceptionOrNull())
    }

    @Test
    fun `when firebase user id is null after auth then return failure`() = runTest {
        val idToken = "some_id_token"
        val firebaseUserWithNullId = User(null, "test@example.com", null, "Test User", null)
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns success(firebaseUserWithNullId)

        val result = registerWithGoogleUseCase(idToken)

        coVerify(exactly = 1) { authRepository.firebaseAuthWithGoogle(idToken) }
        verify(exactly = 0) { firestore.collection(any()) }

        assertTrue(result.isFailure())
        assertEquals("ID de usuario de Firebase es nulo después de la autenticación de Google.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `when firestore get document fails then return failure`() = runTest {
        val idToken = "some_id_token"
        val firebaseUser = User("testUid", "test@example.com", null, "Test User", null)
        val firestoreException = Exception("Firestore get failed")
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns success(firebaseUser)
        coEvery { userDocument.get() } returns Tasks.forException(firestoreException)

        val result = registerWithGoogleUseCase(idToken)

        coVerify(exactly = 1) { authRepository.firebaseAuthWithGoogle(idToken) }
        coVerify(exactly = 1) { userDocument.get() }
        coVerify(exactly = 0) { userDocument.set(any<Map<String, Any>>()) }

        assertTrue(result.isFailure())
        assertEquals(firestoreException, result.exceptionOrNull())
    }

    @Test
    fun `when firestore set document fails then return failure`() = runTest {
        val idToken = "some_id_token"
        val firebaseUser = User("testUid", "test@example.com", null, "Test User", null)
        val firestoreException = Exception("Firestore set failed")
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns success(firebaseUser)
        every { documentSnapshot.exists() } returns false
        coEvery { userDocument.set(any<Map<String, Any>>()) } returns Tasks.forException(firestoreException)

        val result = registerWithGoogleUseCase(idToken)

        coVerify(exactly = 1) { authRepository.firebaseAuthWithGoogle(idToken) }
        coVerify(exactly = 1) { userDocument.get() }
        coVerify(exactly = 1) { userDocument.set(any<Map<String, Any>>()) }

        assertTrue(result.isFailure())
        assertEquals(firestoreException, result.exceptionOrNull())
    }
}
