package com.cursoandroid.queermap.domain.usecase.auth

import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
// Importaciones de tu clase Result personalizada y sus funciones de extensión
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.Exception

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterWithFacebookUseCaseTest {

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

    private lateinit var registerWithFacebookUseCase: RegisterWithFacebookUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { firestore.collection("users") } returns usersCollection
        every { usersCollection.document(any()) } returns userDocument
        coEvery { userDocument.get() } returns Tasks.forResult(documentSnapshot)
        coEvery { userDocument.set(any<Map<String, Any>>()) } returns Tasks.forResult(null)

        registerWithFacebookUseCase = RegisterWithFacebookUseCase(authRepository, firestore)
    }

    @Test
    fun `when facebook registration succeeds and user does not exist in firestore then create user and return success`() =
        runTest {
            val accessToken = "valid_token"
            val firebaseUser = User("fbUid123", "fb@example.com", null, "Facebook User", null)
            coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns success(firebaseUser)
            every { documentSnapshot.exists() } returns false

            val result = registerWithFacebookUseCase(accessToken)

            coVerify(exactly = 1) { authRepository.firebaseAuthWithFacebook(accessToken) }
            coVerify(exactly = 1) { userDocument.get() }
            coVerify(exactly = 1) { userDocument.set(any()) }
            assertTrue(result.isSuccess())
            assertEquals(firebaseUser, result.getOrNull())
        }

    @Test
    fun `when facebook registration succeeds and user already exists in firestore then return success without creating user`() =
        runTest {
            val accessToken = "valid_token"
            val firebaseUser = User("fbUid456", "existing.fb@example.com", "existingfbuser", "Existing FB User", "01/01/1995")
            coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns success(firebaseUser)
            every { documentSnapshot.exists() } returns true

            val result = registerWithFacebookUseCase(accessToken)

            coVerify(exactly = 1) { authRepository.firebaseAuthWithFacebook(accessToken) }
            coVerify(exactly = 1) { userDocument.get() }
            coVerify(exactly = 0) { userDocument.set(any()) }
            assertTrue(result.isSuccess())
            assertEquals(firebaseUser, result.getOrNull())
        }

    @Test
    fun `when firebase auth with facebook fails then return failure`() =
        runTest {
            val accessToken = "invalid_token"
            val ex = Exception("Facebook authentication failed")
            coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns failure(ex)

            val result = registerWithFacebookUseCase(accessToken)

            coVerify(exactly = 1) { authRepository.firebaseAuthWithFacebook(accessToken) }
            verify(exactly = 0) { firestore.collection(any()) }
            assertTrue(result.isFailure())
            assertEquals(ex, result.exceptionOrNull())
        }

    @Test
    fun `when firebase user id is null after auth then return failure`() =
        runTest {
            val accessToken = "token_with_null_id"
            val firebaseUserNullId = User(null, "fb_null_id@example.com", null, "FB User", null)
            coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns success(firebaseUserNullId)

            val result = registerWithFacebookUseCase(accessToken)

            coVerify(exactly = 1) { authRepository.firebaseAuthWithFacebook(accessToken) }
            verify(exactly = 0) { firestore.collection(any()) }
            assertTrue(result.isFailure())
            assertEquals("ID de usuario de Firebase es nulo después de la autenticación de Facebook.", result.exceptionOrNull()?.message)
        }

    @Test
    fun `when firestore get document fails then return failure`() =
        runTest {
            val accessToken = "token"
            val firebaseUser = User("fbUid789", "fb_get_fail@example.com", null, "FB User", null)
            val firestoreException = Exception("Firestore get failed for FB user")
            coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns success(firebaseUser)
            coEvery { userDocument.get() } returns Tasks.forException(firestoreException)

            val result = registerWithFacebookUseCase(accessToken)

            coVerify(exactly = 1) { authRepository.firebaseAuthWithFacebook(accessToken) }
            coVerify(exactly = 1) { userDocument.get() }
            coVerify(exactly = 0) { userDocument.set(any()) }
            assertTrue(result.isFailure())
            assertEquals(firestoreException, result.exceptionOrNull())
        }

    @Test
    fun `when firestore set document fails then return failure`() =
        runTest {
            val accessToken = "token"
            val firebaseUser = User("fbUid910", "fb_set_fail@example.com", null, "FB User", null)
            val firestoreException = Exception("Firestore set failed for FB user")
            coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns success(firebaseUser)
            every { documentSnapshot.exists() } returns false
            coEvery { userDocument.set(any<Map<String, Any>>()) } returns Tasks.forException(firestoreException)

            val result = registerWithFacebookUseCase(accessToken)

            coVerify(exactly = 1) { authRepository.firebaseAuthWithFacebook(accessToken) }
            coVerify(exactly = 1) { userDocument.get() }
            coVerify(exactly = 1) { userDocument.set(any()) }
            assertTrue(result.isFailure())
            assertEquals(firestoreException, result.exceptionOrNull())
        }
}

