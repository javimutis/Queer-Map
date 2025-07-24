package com.cursoandroid.queermap.domain.usecase.auth

import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.Exception

// IMPORTANTE: Asegúrate de que TODAS estas importaciones estén presentes y sean correctas.
// Estas son las importaciones clave para tu clase Result personalizada y sus extensiones.
import com.cursoandroid.queermap.util.Result
import com.cursoandroid.queermap.util.success
import com.cursoandroid.queermap.util.failure
import com.cursoandroid.queermap.util.isSuccess
import com.cursoandroid.queermap.util.isFailure
import com.cursoandroid.queermap.util.getOrNull
import com.cursoandroid.queermap.util.exceptionOrNull

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
        MockKAnnotations.init(this) // Inicializa las anotaciones de MockK

        // Mock de referencias a colección y documento de Firestore
        every { firestore.collection("users") } returns usersCollection
        every { usersCollection.document(any()) } returns userDocument

        // Mock de operaciones Firestore: get y set
        coEvery { userDocument.get() } returns Tasks.forResult(documentSnapshot)
        coEvery { userDocument.set(any<Map<String, Any>>()) } returns Tasks.forResult(null)

        // Inicialización del use case con los mocks
        registerWithGoogleUseCase = RegisterWithGoogleUseCase(authRepository, firestore)
    }

    @Test
    fun `when google registration succeeds and user does not exist in firestore then create user and return success`() = runTest {
        // Given: usuario autenticado con Google y no existe en Firestore
        val idToken = "some_id_token"
        val firebaseUser = User("testUid", "test@example.com", null, "Test User", null)
        // Usa tu función helper 'success'
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns success(firebaseUser)
        every { documentSnapshot.exists() } returns false

        // When: se ejecuta el use case
        val result = registerWithGoogleUseCase(idToken)

        // Then: se verifica autenticación, lectura y escritura en Firestore
        coVerify(exactly = 1) { authRepository.firebaseAuthWithGoogle(idToken) }
        coVerify(exactly = 1) { userDocument.get() }
        coVerify(exactly = 1) { userDocument.set(any<Map<String, Any>>()) }

        // Usa tus extensiones de Result
        assertTrue(result.isSuccess())
        assertEquals(firebaseUser, result.getOrNull())
    }

    @Test
    fun `when google registration succeeds and user already exists in firestore then return success without creating user`() = runTest {
        // Given: usuario autenticado con Google y ya existe en Firestore
        val idToken = "some_id_token"
        val firebaseUser = User("testUid", "test@example.com", "existinguser", "Existing User", "01/01/1990")
        // Usa tu función helper 'success'
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns success(firebaseUser)
        every { documentSnapshot.exists() } returns true

        // When: se ejecuta el use case
        val result = registerWithGoogleUseCase(idToken)

        // Then: no se realiza escritura, solo lectura
        coVerify(exactly = 1) { authRepository.firebaseAuthWithGoogle(idToken) }
        coVerify(exactly = 1) { userDocument.get() }
        coVerify(exactly = 0) { userDocument.set(any<Map<String, Any>>()) }

        // Usa tus extensiones de Result
        assertTrue(result.isSuccess())
        assertEquals(firebaseUser, result.getOrNull())
    }

    @Test
    fun `when firebase auth with google fails then return failure`() = runTest {
        // Given: autenticación de Google falla
        val idToken = "some_id_token"
        val expectedException = Exception("Firebase authentication failed")
        // Usa tu función helper 'failure'
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns failure(expectedException)

        // When: se ejecuta el use case
        val result = registerWithGoogleUseCase(idToken)

        // Then: no se accede a Firestore
        coVerify(exactly = 1) { authRepository.firebaseAuthWithGoogle(idToken) }
        verify(exactly = 0) { firestore.collection(any()) }

        // Usa tus extensiones de Result
        assertTrue(result.isFailure())
        assertEquals(expectedException, result.exceptionOrNull())
    }

    @Test
    fun `when firebase user id is null after auth then return failure`() = runTest {
        // Given: usuario autenticado pero con ID nulo
        val idToken = "some_id_token"
        val firebaseUserWithNullId = User(null, "test@example.com", null, "Test User", null)
        // Usa tu función helper 'success'
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns success(firebaseUserWithNullId)

        // When: se ejecuta el use case
        val result = registerWithGoogleUseCase(idToken)

        // Then: se evita interacción con Firestore
        coVerify(exactly = 1) { authRepository.firebaseAuthWithGoogle(idToken) }
        verify(exactly = 0) { firestore.collection(any()) }

        // Usa tus extensiones de Result
        assertTrue(result.isFailure())
        assertEquals("ID de usuario de Firebase es nulo después de la autenticación de Google.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `when firestore get document fails then return failure`() = runTest {
        // Given: error al obtener documento desde Firestore
        val idToken = "some_id_token"
        val firebaseUser = User("testUid", "test@example.com", null, "Test User", null)
        val firestoreException = Exception("Firestore get failed")
        // Usa tu función helper 'success'
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns success(firebaseUser)
        coEvery { userDocument.get() } returns Tasks.forException(firestoreException)

        // When: se ejecuta el use case
        val result = registerWithGoogleUseCase(idToken)

        // Then: se valida que no se escribe el documento
        coVerify(exactly = 1) { authRepository.firebaseAuthWithGoogle(idToken) }
        coVerify(exactly = 1) { userDocument.get() }
        coVerify(exactly = 0) { userDocument.set(any<Map<String, Any>>()) }

        // Usa tus extensiones de Result
        assertTrue(result.isFailure())
        assertEquals(firestoreException, result.exceptionOrNull())
    }

    @Test
    fun `when firestore set document fails then return failure`() = runTest {
        // Given: error al guardar documento en Firestore
        val idToken = "some_id_token"
        val firebaseUser = User("testUid", "test@example.com", null, "Test User", null)
        val firestoreException = Exception("Firestore set failed")
        // Usa tu función helper 'success'
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns success(firebaseUser)
        every { documentSnapshot.exists() } returns false
        coEvery { userDocument.set(any<Map<String, Any>>()) } returns Tasks.forException(firestoreException)

        // When: se ejecuta el use case
        val result = registerWithGoogleUseCase(idToken)

        // Then: se valida lectura y escritura, y se captura el error
        coVerify(exactly = 1) { authRepository.firebaseAuthWithGoogle(idToken) }
        coVerify(exactly = 1) { userDocument.get() }
        coVerify(exactly = 1) { userDocument.set(any<Map<String, Any>>()) }

        // Usa tus extensiones de Result
        assertTrue(result.isFailure())
        assertEquals(firestoreException, result.exceptionOrNull())
    }
}