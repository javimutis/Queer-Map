package com.cursoandroid.queermap.domain.usecase.auth

import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
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

class RegisterWithFacebookUseCaseTest {

    // Repositorio de autenticación mockeado
    @MockK
    private lateinit var authRepository: AuthRepository

    // Firestore y referencias mockeadas
    @MockK
    private lateinit var firestore: FirebaseFirestore
    @MockK
    private lateinit var usersCollection: CollectionReference
    @MockK
    private lateinit var userDocument: DocumentReference
    @MockK
    private lateinit var documentSnapshot: DocumentSnapshot

    // Use case a testear
    private lateinit var registerWithFacebookUseCase: RegisterWithFacebookUseCase

    @Before
    fun setUp() {
        // Inicializa mocks
        MockKAnnotations.init(this)

        // Setup del comportamiento de Firestore simulado
        every { firestore.collection("users") } returns usersCollection
        every { usersCollection.document(any()) } returns userDocument
        coEvery { userDocument.get() } returns Tasks.forResult(documentSnapshot)
        coEvery { userDocument.set(any<Map<String, Any>>()) } returns Tasks.forResult(null)

        // Instancia del use case con dependencias mock
        registerWithFacebookUseCase = RegisterWithFacebookUseCase(authRepository, firestore)
    }

    @Test
    fun `when facebook registration succeeds and user does not exist in firestore then create user and return success`() =
        runTest {
            // Given: Facebook login exitoso, usuario no existe en Firestore
            val accessToken = "some_facebook_access_token"
            val firebaseUser = User("fbUid123", "fb@example.com", null, "Facebook User", null)
            coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns Result.success(firebaseUser)
            every { documentSnapshot.exists() } returns false

            // When: Se ejecuta el use case
            val result = registerWithFacebookUseCase(accessToken)

            // Then: Se autentica, obtiene doc, crea usuario en Firestore
            coVerify(exactly = 1) { authRepository.firebaseAuthWithFacebook(accessToken) }
            coVerify(exactly = 1) { userDocument.get() }
            coVerify(exactly = 1) { userDocument.set(any<Map<String, Any>>()) }

            assertTrue(result.isSuccess)
            assertEquals(firebaseUser, result.getOrNull())
        }

    @Test
    fun `when facebook registration succeeds and user already exists in firestore then return success without creating user`() =
        runTest {
            // Given: Facebook login exitoso, usuario ya existe
            val accessToken = "some_facebook_access_token"
            val firebaseUser = User("fbUid456", "existing.fb@example.com", "existingfbuser", "Existing FB User", "01/01/1995")
            coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns Result.success(firebaseUser)
            every { documentSnapshot.exists() } returns true

            // When: Se ejecuta el use case
            val result = registerWithFacebookUseCase(accessToken)

            // Then: No se crea el usuario en Firestore
            coVerify(exactly = 1) { authRepository.firebaseAuthWithFacebook(accessToken) }
            coVerify(exactly = 1) { userDocument.get() }
            coVerify(exactly = 0) { userDocument.set(any<Map<String, Any>>()) }

            assertTrue(result.isSuccess)
            assertEquals(firebaseUser, result.getOrNull())
        }

    @Test
    fun `when firebase auth with facebook fails then return failure`() = runTest {
        // Given: Fallo en autenticación con Facebook
        val accessToken = "some_facebook_access_token"
        val expectedException = Exception("Facebook authentication failed")
        coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns Result.failure(expectedException)

        // When: Se ejecuta el use case
        val result = registerWithFacebookUseCase(accessToken)

        // Then: No se accede a Firestore
        coVerify(exactly = 1) { authRepository.firebaseAuthWithFacebook(accessToken) }
        verify(exactly = 0) { firestore.collection(any()) }

        assertTrue(result.isFailure)
        assertEquals(expectedException, result.exceptionOrNull())
    }

    @Test
    fun `when firebase user id is null after auth then return failure`() = runTest {
        // Given: Usuario autenticado pero sin ID
        val accessToken = "some_facebook_access_token"
        val firebaseUserWithNullId = User(null, "fb_null_id@example.com", null, "FB User", null)
        coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns Result.success(firebaseUserWithNullId)

        // When: Se ejecuta el use case
        val result = registerWithFacebookUseCase(accessToken)

        // Then: No se consulta Firestore, retorna fallo
        coVerify(exactly = 1) { authRepository.firebaseAuthWithFacebook(accessToken) }
        verify(exactly = 0) { firestore.collection(any()) }

        assertTrue(result.isFailure)
        assertEquals(
            "ID de usuario de Firebase es nulo después de la autenticación de Facebook.",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun `when firestore get document fails then return failure`() = runTest {
        // Given: Fallo al obtener documento en Firestore
        val accessToken = "some_facebook_access_token"
        val firebaseUser = User("fbUid789", "fb_get_fail@example.com", null, "FB User", null)
        val firestoreException = Exception("Firestore get failed for FB user")
        coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns Result.success(firebaseUser)
        coEvery { userDocument.get() } returns Tasks.forException(firestoreException)

        // When: Se ejecuta el use case
        val result = registerWithFacebookUseCase(accessToken)

        // Then: No se intenta crear documento, retorna fallo
        coVerify(exactly = 1) { authRepository.firebaseAuthWithFacebook(accessToken) }
        coVerify(exactly = 1) { userDocument.get() }
        coVerify(exactly = 0) { userDocument.set(any<Map<String, Any>>()) }

        assertTrue(result.isFailure)
        assertEquals(firestoreException, result.exceptionOrNull())
    }

    @Test
    fun `when firestore set document fails then return failure`() = runTest {
        // Given: Fallo al crear documento en Firestore
        val accessToken = "some_facebook_access_token"
        val firebaseUser = User("fbUid910", "fb_set_fail@example.com", null, "FB User", null)
        val firestoreException = Exception("Firestore set failed for FB user")
        coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns Result.success(firebaseUser)
        every { documentSnapshot.exists() } returns false
        coEvery { userDocument.set(any<Map<String, Any>>()) } returns Tasks.forException(firestoreException)

        // When: Se ejecuta el use case
        val result = registerWithFacebookUseCase(accessToken)

        // Then: Falla al intentar guardar documento
        coVerify(exactly = 1) { authRepository.firebaseAuthWithFacebook(accessToken) }
        coVerify(exactly = 1) { userDocument.get() }
        coVerify(exactly = 1) { userDocument.set(any<Map<String, Any>>()) }

        assertTrue(result.isFailure)
        assertEquals(firestoreException, result.exceptionOrNull())
    }
}