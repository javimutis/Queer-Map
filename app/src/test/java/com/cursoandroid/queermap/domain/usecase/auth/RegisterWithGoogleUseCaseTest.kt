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

    // Se usan @MockK para crear mocks de las dependencias.
    // Esto permite aislar la lógica del use case del comportamiento real de sus dependencias.
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

    // Instancia del caso de uso que vamos a probar.
    private lateinit var registerWithGoogleUseCase: RegisterWithGoogleUseCase

    @Before
    fun setUp() {
        // Inicializa los mocks anotados en la clase.
        MockKAnnotations.init(this)

        // Configuración del comportamiento simulado de Firestore.
        // Cuando se llama a `firestore.collection("users")`, se devuelve nuestro mock `usersCollection`.
        every { firestore.collection("users") } returns usersCollection
        // Cuando se llama a `usersCollection.document(any())`, se devuelve nuestro mock `userDocument`.
        every { usersCollection.document(any()) } returns userDocument
        // Mock de las operaciones asíncronas de Firestore.
        coEvery { userDocument.get() } returns Tasks.forResult(documentSnapshot)
        coEvery { userDocument.set(any<Map<String, Any>>()) } returns Tasks.forResult(null)

        // Inicializa el caso de uso con las dependencias mockeadas.
        registerWithGoogleUseCase = RegisterWithGoogleUseCase(authRepository, firestore)
    }

    @Test
    fun `when google registration succeeds and user does not exist in firestore then create user and return success`() = runTest {
        // Given: Se simula un registro de Google exitoso y que el usuario no existe en Firestore.
        val idToken = "some_id_token"
        val firebaseUser = User("testUid", "test@example.com", null, "Test User", null)
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns success(firebaseUser)
        every { documentSnapshot.exists() } returns false // Simula que el documento no existe.

        // When: Se ejecuta el caso de uso.
        val result = registerWithGoogleUseCase(idToken)

        // Then: Se verifica que se llamaron los métodos esperados.
        // `coVerify` se usa para funciones suspend.
        coVerify(exactly = 1) { authRepository.firebaseAuthWithGoogle(idToken) }
        coVerify(exactly = 1) { userDocument.get() }
        coVerify(exactly = 1) { userDocument.set(any<Map<String, Any>>()) } // Se verifica que se intentó crear el usuario.

        // Se verifica el resultado.
        assertTrue(result.isSuccess())
        assertEquals(firebaseUser, result.getOrNull())
    }

    @Test
    fun `when google registration succeeds and user already exists in firestore then return success without creating user`() = runTest {
        // Given: Se simula un registro de Google exitoso y que el usuario ya existe en Firestore.
        val idToken = "some_id_token"
        val firebaseUser = User("testUid", "test@example.com", "existinguser", "Existing User", "01/01/1990")
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns success(firebaseUser)
        every { documentSnapshot.exists() } returns true // Simula que el documento ya existe.

        // When: Se ejecuta el caso de uso.
        val result = registerWithGoogleUseCase(idToken)

        // Then: Se verifica que NO se intentó crear el usuario.
        coVerify(exactly = 1) { authRepository.firebaseAuthWithGoogle(idToken) }
        coVerify(exactly = 1) { userDocument.get() }
        coVerify(exactly = 0) { userDocument.set(any<Map<String, Any>>()) } // La clave: NO se llama a set.

        // Se verifica el resultado.
        assertTrue(result.isSuccess())
        assertEquals(firebaseUser, result.getOrNull())
    }

    @Test
    fun `when firebase auth with google fails then return failure`() = runTest {
        // Given: Se simula un fallo en la autenticación de Google.
        val idToken = "some_id_token"
        val expectedException = Exception("Firebase authentication failed")
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns failure(expectedException)

        // When: Se ejecuta el caso de uso.
        val result = registerWithGoogleUseCase(idToken)

        // Then: Se verifica que NO se intentó interactuar con Firestore.
        coVerify(exactly = 1) { authRepository.firebaseAuthWithGoogle(idToken) }
        verify(exactly = 0) { firestore.collection(any()) }

        // Se verifica el resultado.
        assertTrue(result.isFailure())
        assertEquals(expectedException, result.exceptionOrNull())
    }

    @Test
    fun `when firebase user id is null after auth then return failure`() = runTest {
        // Given: Se simula una autenticación exitosa, pero con un ID de usuario nulo.
        val idToken = "some_id_token"
        val firebaseUserWithNullId = User(null, "test@example.com", null, "Test User", null)
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns success(firebaseUserWithNullId)

        // When: Se ejecuta el caso de uso.
        val result = registerWithGoogleUseCase(idToken)

        // Then: Se verifica que la interacción con Firestore no ocurrió.
        coVerify(exactly = 1) { authRepository.firebaseAuthWithGoogle(idToken) }
        verify(exactly = 0) { firestore.collection(any()) }

        // Se verifica que el resultado es un fallo con el mensaje de error esperado.
        assertTrue(result.isFailure())
        assertEquals("ID de usuario de Firebase es nulo después de la autenticación de Google.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `when firestore get document fails then return failure`() = runTest {
        // Given: Se simula un fallo al obtener el documento de Firestore.
        val idToken = "some_id_token"
        val firebaseUser = User("testUid", "test@example.com", null, "Test User", null)
        val firestoreException = Exception("Firestore get failed")
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns success(firebaseUser)
        coEvery { userDocument.get() } returns Tasks.forException(firestoreException) // Mock de un fallo.

        // When: Se ejecuta el caso de uso.
        val result = registerWithGoogleUseCase(idToken)

        // Then: Se verifica que la ejecución se detuvo en la llamada a `get` y no continuó.
        coVerify(exactly = 1) { authRepository.firebaseAuthWithGoogle(idToken) }
        coVerify(exactly = 1) { userDocument.get() }
        coVerify(exactly = 0) { userDocument.set(any<Map<String, Any>>()) }

        // Se verifica que el resultado es un fallo con la excepción de Firestore.
        assertTrue(result.isFailure())
        assertEquals(firestoreException, result.exceptionOrNull())
    }

    @Test
    fun `when firestore set document fails then return failure`() = runTest {
        // Given: Se simula un fallo al guardar el documento en Firestore.
        val idToken = "some_id_token"
        val firebaseUser = User("testUid", "test@example.com", null, "Test User", null)
        val firestoreException = Exception("Firestore set failed")
        coEvery { authRepository.firebaseAuthWithGoogle(idToken) } returns success(firebaseUser)
        every { documentSnapshot.exists() } returns false
        coEvery { userDocument.set(any<Map<String, Any>>()) } returns Tasks.forException(firestoreException) // Mock de un fallo.

        // When: Se ejecuta el caso de uso.
        val result = registerWithGoogleUseCase(idToken)

        // Then: Se verifica que se intentó guardar el documento y que falló.
        coVerify(exactly = 1) { authRepository.firebaseAuthWithGoogle(idToken) }
        coVerify(exactly = 1) { userDocument.get() }
        coVerify(exactly = 1) { userDocument.set(any<Map<String, Any>>()) }

        // Se verifica que el resultado es un fallo con la excepción de Firestore.
        assertTrue(result.isFailure())
        assertEquals(firestoreException, result.exceptionOrNull())
    }
}
