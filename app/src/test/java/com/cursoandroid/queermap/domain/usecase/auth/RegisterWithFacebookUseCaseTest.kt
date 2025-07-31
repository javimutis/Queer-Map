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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.Exception


class RegisterWithFacebookUseCaseTest {

    // Repositorio de autenticación simulado
    @MockK
    private lateinit var authRepository: AuthRepository

    // Firestore y sus referencias simuladas para controlar el comportamiento de la base de datos
    @MockK
    private lateinit var firestore: FirebaseFirestore

    @MockK
    private lateinit var usersCollection: CollectionReference

    @MockK
    private lateinit var userDocument: DocumentReference

    @MockK
    private lateinit var documentSnapshot: DocumentSnapshot

    // El caso de uso que vamos a probar
    private lateinit var registerWithFacebookUseCase: RegisterWithFacebookUseCase

    @Before
    fun setUp() {
        // Inicializa los mocks para esta suite de pruebas
        MockKAnnotations.init(this)

        // Configuración básica del comportamiento simulado de Firestore
        every { firestore.collection("users") } returns usersCollection
        every { usersCollection.document(any()) } returns userDocument
        coEvery { userDocument.get() } returns Tasks.forResult(documentSnapshot)
        coEvery { userDocument.set(any<Map<String, Any>>()) } returns Tasks.forResult(null)

        // Instancia el caso de uso con sus dependencias simuladas
        registerWithFacebookUseCase = RegisterWithFacebookUseCase(authRepository, firestore)
    }

    @Test
    fun `when facebook registration succeeds and user does not exist in firestore then create user and return success`() =
        runTest {
            // Given: Autenticación exitosa y el usuario no existe en Firestore
            val accessToken = "some_facebook_access_token"
            val firebaseUser = User("fbUid123", "fb@example.com", null, "Facebook User", null)
            coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns success(firebaseUser)
            every { documentSnapshot.exists() } returns false // Simula que el documento no existe

            // When: Se ejecuta el caso de uso
            val result = registerWithFacebookUseCase(accessToken)

            // Then: Se verifica que se haya llamado a la autenticación, se haya consultado el documento
            // y, crucialmente, se haya creado el documento en Firestore
            coVerify(exactly = 1) { authRepository.firebaseAuthWithFacebook(accessToken) }
            coVerify(exactly = 1) { userDocument.get() }
            coVerify(exactly = 1) { userDocument.set(any<Map<String, Any>>()) }

            // Se verifica que el resultado sea exitoso y contenga el usuario esperado
            assertTrue(result.isSuccess())
            assertEquals(firebaseUser, result.getOrNull())
        }

    @Test
    fun `when facebook registration succeeds and user already exists in firestore then return success without creating user`() =
        runTest {
            // Given: Autenticación exitosa y el usuario ya existe en Firestore
            val accessToken = "some_facebook_access_token"
            val firebaseUser = User("fbUid456", "existing.fb@example.com", "existingfbuser", "Existing FB User", "01/01/1995")
            coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns success(firebaseUser)
            every { documentSnapshot.exists() } returns true // Simula que el documento ya existe

            // When: Se ejecuta el caso de uso
            val result = registerWithFacebookUseCase(accessToken)

            // Then: Se verifica que se autenticó y se consultó el documento, pero NO se llamó a `set`
            // para crear un nuevo usuario.
            coVerify(exactly = 1) { authRepository.firebaseAuthWithFacebook(accessToken) }
            coVerify(exactly = 1) { userDocument.get() }
            coVerify(exactly = 0) { userDocument.set(any<Map<String, Any>>()) }

            // Se verifica que el resultado sea exitoso y contenga el usuario esperado
            assertTrue(result.isSuccess())
            assertEquals(firebaseUser, result.getOrNull())
        }

    @Test
    fun `when firebase auth with facebook fails then return failure`() =
        runTest {
            // Given: Fallo en la autenticación
            val accessToken = "some_facebook_access_token"
            val expectedException = Exception("Facebook authentication failed")
            coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns failure(expectedException)

            // When: Se ejecuta el caso de uso
            val result = registerWithFacebookUseCase(accessToken)

            // Then: Se verifica que solo se intentó la autenticación, y que NINGUNA función de Firestore
            // fue llamada.
            coVerify(exactly = 1) { authRepository.firebaseAuthWithFacebook(accessToken) }
            verify(exactly = 0) { firestore.collection(any()) }

            // Se verifica que el resultado sea un fallo con la excepción esperada
            assertTrue(result.isFailure())
            assertEquals(expectedException, result.exceptionOrNull())
        }

    @Test
    fun `when firebase user id is null after auth then return failure`() =
        runTest {
            // Given: Autenticación exitosa, pero con un ID de usuario nulo
            val accessToken = "some_facebook_access_token"
            val firebaseUserWithNullId = User(null, "fb_null_id@example.com", null, "FB User", null)
            coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns success(firebaseUserWithNullId)

            // When: Se ejecuta el caso de uso
            val result = registerWithFacebookUseCase(accessToken)

            // Then: Se verifica que solo se hizo la llamada de autenticación y que NINGUNA función de Firestore
            // fue llamada, ya que la lógica se detiene al encontrar el ID nulo.
            coVerify(exactly = 1) { authRepository.firebaseAuthWithFacebook(accessToken) }
            verify(exactly = 0) { firestore.collection(any()) }

            // Se verifica que el resultado sea un fallo con el mensaje de error esperado
            assertTrue(result.isFailure())
            assertEquals("ID de usuario de Firebase es nulo después de la autenticación de Facebook.", result.exceptionOrNull()?.message)
        }

    @Test
    fun `when firestore get document fails then return failure`() =
        runTest {
            // Given: Fallo al obtener el documento de usuario en Firestore
            val accessToken = "some_facebook_access_token"
            val firebaseUser = User("fbUid789", "fb_get_fail@example.com", null, "FB User", null)
            val firestoreException = Exception("Firestore get failed for FB user")
            coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns success(firebaseUser)
            // Simula un fallo en la llamada `get()`
            coEvery { userDocument.get() } returns Tasks.forException(firestoreException)

            // When: Se ejecuta el caso de uso
            val result = registerWithFacebookUseCase(accessToken)

            // Then: Se verifica que se hizo la autenticación y la llamada a `get`,
            // pero NO se intentó crear el documento
            coVerify(exactly = 1) { authRepository.firebaseAuthWithFacebook(accessToken) }
            coVerify(exactly = 1) { userDocument.get() }
            coVerify(exactly = 0) { userDocument.set(any<Map<String, Any>>()) }

            // Se verifica que el resultado sea un fallo con la excepción de Firestore
            assertTrue(result.isFailure())
            assertEquals(firestoreException, result.exceptionOrNull())
        }

    @Test
    fun `when firestore set document fails then return failure`() =
        runTest {
            // Given: Autenticación exitosa, usuario nuevo, pero fallo al crear el documento
            val accessToken = "some_facebook_access_token"
            val firebaseUser = User("fbUid910", "fb_set_fail@example.com", null, "FB User", null)
            val firestoreException = Exception("Firestore set failed for FB user")
            coEvery { authRepository.firebaseAuthWithFacebook(accessToken) } returns success(firebaseUser)
            every { documentSnapshot.exists() } returns false // Simula que el documento no existe
            // Simula un fallo en la llamada `set()`
            coEvery { userDocument.set(any<Map<String, Any>>()) } returns Tasks.forException(firestoreException)

            // When: Se ejecuta el caso de uso
            val result = registerWithFacebookUseCase(accessToken)

            // Then: Se verifica que se intentó la autenticación, se consultó el documento
            // y se intentó crear el documento, lo cual falló.
            coVerify(exactly = 1) { authRepository.firebaseAuthWithFacebook(accessToken) }
            coVerify(exactly = 1) { userDocument.get() }
            coVerify(exactly = 1) { userDocument.set(any<Map<String, Any>>()) }

            // Se verifica que el resultado sea un fallo con la excepción de Firestore
            assertTrue(result.isFailure())
            assertEquals(firestoreException, result.exceptionOrNull())
        }
}
