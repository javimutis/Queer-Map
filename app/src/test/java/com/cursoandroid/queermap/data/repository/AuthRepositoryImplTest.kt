package com.cursoandroid.queermap.data.repository

import com.cursoandroid.queermap.data.source.AuthRemoteDataSource
import com.cursoandroid.queermap.data.source.local.SharedPreferencesDataSource
import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.util.failure // Explicitly import your custom failure
import com.cursoandroid.queermap.util.success // Explicitly import your custom success
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {

    // Mocks de las dependencias
    private val mockRemoteDataSource: AuthRemoteDataSource = mockk()
    private val mockSharedPreferencesDataSource: SharedPreferencesDataSource = mockk()
    private val mockFirebaseAuth: FirebaseAuth = mockk()
    private val mockFirebaseFirestore: FirebaseFirestore = mockk()

    // Clase a testear
    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setup() {
        // Inicializa el repositorio con los mocks antes de cada test
        authRepository = AuthRepositoryImpl(
            mockRemoteDataSource,
            mockSharedPreferencesDataSource,
            mockFirebaseAuth,
            mockFirebaseFirestore
        )
    }

    @Test
    fun `loginWithEmailAndPassword devuelve exito cuando remoteDataSource tiene exito`() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val user = User("123", "Test User", "testuser", email, "01/01/2000")
        coEvery { mockRemoteDataSource.loginWithEmailAndPassword(email, password) } returns success(
            user
        )

        // Act
        val result = authRepository.loginWithEmailAndPassword(email, password)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
    }

    @Test
    fun `loginWithEmailAndPassword devuelve fallo cuando remoteDataSource falla`() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val exception = Exception("Error de autenticación")
        coEvery { mockRemoteDataSource.loginWithEmailAndPassword(email, password) } returns failure(
            exception
        )

        // Act
        val result = authRepository.loginWithEmailAndPassword(email, password)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception.message, result.exceptionOrNull()?.message)
    }

    @Test
    fun `verifyUserInFirestore devuelve exito si el usuario existe`() = runTest {
        // Arrange
        val uid = "user123"
        val mockDocumentSnapshot = mockk<com.google.firebase.firestore.DocumentSnapshot>()
        every { mockDocumentSnapshot.exists() } returns true
        coEvery { mockRemoteDataSource.verifyUserInFirestore(uid) } returns success(
            mockDocumentSnapshot
        )

        // Act
        val result = authRepository.verifyUserInFirestore(uid)

        // Assert
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun `verifyUserInFirestore devuelve exito si el usuario no existe`() = runTest {
        // Arrange
        val uid = "user123"
        val mockDocumentSnapshot = mockk<com.google.firebase.firestore.DocumentSnapshot>()
        every { mockDocumentSnapshot.exists() } returns false
        coEvery { mockRemoteDataSource.verifyUserInFirestore(uid) } returns success(
            mockDocumentSnapshot
        )

        // Act
        val result = authRepository.verifyUserInFirestore(uid)

        // Assert
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == false)
    }

    @Test
    fun `verifyUserInFirestore devuelve fallo si remoteDataSource falla`() = runTest {
        // Arrange
        val uid = "user123"
        val exception = Exception("Error de red")
        coEvery { mockRemoteDataSource.verifyUserInFirestore(uid) } returns failure(exception)

        // Act
        val result = authRepository.verifyUserInFirestore(uid)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception.message, result.exceptionOrNull()?.message)
    }

    @Test
    fun `sendResetPasswordEmail devuelve exito`() = runTest {
        // Arrange
        val email = "test@example.com"
        coEvery { mockRemoteDataSource.sendPasswordResetEmail(email) } returns success(Unit)

        // Act
        val result = authRepository.sendResetPasswordEmail(email)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
    }

    @Test
    fun `sendResetPasswordEmail devuelve fallo`() = runTest {
        // Arrange
        val email = "test@example.com"
        val exception = Exception("Email no encontrado")
        coEvery { mockRemoteDataSource.sendPasswordResetEmail(email) } returns failure(exception)

        // Act
        val result = authRepository.sendResetPasswordEmail(email)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception.message, result.exceptionOrNull()?.message)
    }

    @Test
    fun `firebaseAuthWithGoogle devuelve exito`() = runTest {
        // Arrange
        val idToken = "some_id_token"
        val user = User("456", "Google User", "googleuser", "google@example.com", "01/01/2000")
        coEvery { mockRemoteDataSource.firebaseAuthWithGoogle(idToken) } returns success(user)

        // Act
        val result = authRepository.firebaseAuthWithGoogle(idToken)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
    }

    @Test
    fun `firebaseAuthWithGoogle devuelve fallo`() = runTest {
        // Arrange
        val idToken = "some_id_token"
        val exception = Exception("Error con Google Auth")
        coEvery { mockRemoteDataSource.firebaseAuthWithGoogle(idToken) } returns failure(exception)

        // Act

        val result = authRepository.firebaseAuthWithGoogle(idToken)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception.message, result.exceptionOrNull()?.message)
    }

    @Test
    fun `firebaseAuthWithFacebook devuelve exito`() = runTest {
        // Arrange
        val token = "some_fb_token"
        val user = User("789", "Facebook User", "fbuser", "fb@example.com", "01/01/2000")
        coEvery { mockRemoteDataSource.firebaseAuthWithFacebook(token) } returns success(user)

        // Act
        val result = authRepository.firebaseAuthWithFacebook(token)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
    }

    @Test
    fun `firebaseAuthWithFacebook devuelve fallo`() = runTest {
        // Arrange
        val token = "some_fb_token"
        val exception = Exception("Error con Facebook Auth")
        coEvery { mockRemoteDataSource.firebaseAuthWithFacebook(token) } returns failure(exception)

        // Act
        val result = authRepository.firebaseAuthWithFacebook(token)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception.message, result.exceptionOrNull()?.message)
    }

    @Test
    fun `saveCredentials llama al metodo correspondiente en sharedPreferencesDataSource`() {
        // Arrange
        val email = "user@example.com"
        val password = "secure_password"
        every { mockSharedPreferencesDataSource.saveCredentials(email, password) } just Runs

        // Act
        authRepository.saveCredentials(email, password)

        // Assert (verificación implícita por `just Runs` y que no lanza excepción)
        // Puedes añadir `verify { mockSharedPreferencesDataSource.saveCredentials(email, password) }` si deseas ser explícito
    }

    @Test
    fun `loadSavedCredentials devuelve las credenciales de sharedPreferencesDataSource`() {
        // Arrange
        val email = "saved@example.com"
        val password = "saved_password"
        every { mockSharedPreferencesDataSource.loadSavedCredentials() } returns Pair(
            email,
            password
        )

        // Act
        val result = authRepository.loadSavedCredentials()

        // Assert
        assertEquals(email, result.first)
        assertEquals(password, result.second)
    }

    @Test
    fun `registerUser devuelve exito cuando el registro y guardado en Firestore son exitosos`() =
        runTest {
            // Arrange
            val user = User(null, "New User", "newuser", "new@example.com", "02/02/2000")
            val password = "StrongPassword123"
            val mockUid = "new_user_uid"

            // Mock para FirebaseAuth.createUserWithEmailAndPassword
            val mockAuthResult = mockk<AuthResult>()
            val mockFirebaseUser = mockk<com.google.firebase.auth.FirebaseUser>()
            every { mockAuthResult.user } returns mockFirebaseUser
            every { mockFirebaseUser.uid } returns mockUid

            // Simular el Task de Firebase Authentication
            coEvery {
                mockFirebaseAuth.createUserWithEmailAndPassword(user.email!!, password).await()
            } returns mockAuthResult

            // Mock para FirebaseFirestore.collection("users").document(userId).set
            val mockCollectionReference = mockk<CollectionReference>()
            val mockDocumentReference = mockk<DocumentReference>()
            val mockVoidTask: Task<Void> = mockk() // Mock de la tarea que devuelve set()

            every { mockFirebaseFirestore.collection("users") } returns mockCollectionReference
            every { mockCollectionReference.document(mockUid) } returns mockDocumentReference
            coEvery { mockDocumentReference.set(any<Map<String, Any>>()) } returns mockVoidTask
            coEvery { mockVoidTask.await() } returns Unit // Use returns Unit for suspend functions returning Unit

            // Act
            val result = authRepository.registerUser(user, password)

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(Unit, result.getOrNull())
        }

    @Test
    fun `registerUser devuelve fallo si el usuario es nulo despues del registro en Auth`() =
        runTest {
            // Arrange
            val user = User(null, "New User", "newuser", "new@example.com", "02/02/2000")
            val password = "StrongPassword123"

            // Mock para FirebaseAuth.createUserWithEmailAndPassword con usuario nulo
            val mockAuthResult = mockk<AuthResult>()
            every { mockAuthResult.user } returns null // Simula que el usuario es nulo

            coEvery {
                mockFirebaseAuth.createUserWithEmailAndPassword(user.email!!, password).await()
            } returns mockAuthResult

            // Act
            val result = authRepository.registerUser(user, password)

            // Assert
            assertTrue(result.isFailure)
            assertEquals("Usuario nulo después del registro.", result.exceptionOrNull()?.message)
        }

    @Test
    fun `registerUser maneja FirebaseAuthUserCollisionException`() = runTest {
        // Arrange
        val user = User(null, "New User", "newuser", "existing@example.com", "02/02/2000")
        val password = "StrongPassword123"
        val exception =
            FirebaseAuthUserCollisionException("email-already-in-use", "Email ya registrado.")

        coEvery {
            mockFirebaseAuth.createUserWithEmailAndPassword(user.email!!, password).await()
        } throws exception

        // Act
        val result = authRepository.registerUser(user, password)

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Este email ya está registrado.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `registerUser maneja FirebaseAuthWeakPasswordException`() = runTest {
        // Arrange
        val user = User(null, "New User", "newuser", "new@example.com", "02/02/2000")
        val password = "weak"
        val exception = FirebaseAuthWeakPasswordException("weak-password", "Contraseña muy débil.")

        coEvery {
            mockFirebaseAuth.createUserWithEmailAndPassword(user.email!!, password).await()
        } throws exception

        // Act
        val result = authRepository.registerUser(user, password)

        // Assert
        assertTrue(result.isFailure)
        assertEquals("La contraseña es demasiado débil.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `registerUser maneja FirebaseAuthInvalidCredentialsException`() = runTest {
        // Arrange
        val user = User(null, "New User", "newuser", "invalid@example.com", "02/02/2000")
        val password = "password"
        val exception = FirebaseAuthInvalidCredentialsException("invalid-email", "Email inválido.")

        coEvery {
            mockFirebaseAuth.createUserWithEmailAndPassword(user.email!!, password).await()
        } throws exception

        // Act
        val result = authRepository.registerUser(user, password)

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Email o contraseña inválidos.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `registerUser maneja otras excepciones generales`() = runTest {
        // Arrange
        val user = User(null, "New User", "newuser", "new@example.com", "02/02/2000")
        val password = "StrongPassword123"
        val exception = Exception("Otro error inesperado.")

        coEvery {
            mockFirebaseAuth.createUserWithEmailAndPassword(user.email!!, password).await()
        } throws exception

        // Act
        val result = authRepository.registerUser(user, password)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception.message, result.exceptionOrNull()?.message)
    }

    @Test
    fun `updateUserProfile devuelve exito cuando la actualizacion en Firestore es exitosa`() =
        runTest {
            // Arrange
            val uid = "test_uid"
            val user = User(
                uid,
                "Updated Name",
                "updatedusername",
                "updated@example.com",
                "03/03/2003"
            )

            val mockCollectionReference = mockk<CollectionReference>()
            val mockDocumentReference = mockk<DocumentReference>()
            val mockVoidTask: Task<Void> = mockk() // Mock de la tarea que devuelve set()

            // Usamos slot para capturar el map que se pasa a set() y verificar su contenido
            val mapSlot = slot<Map<String, Any>>()

            every { mockFirebaseFirestore.collection("users") } returns mockCollectionReference
            every { mockCollectionReference.document(uid) } returns mockDocumentReference
            coEvery { mockDocumentReference.set(capture(mapSlot), any<SetOptions>()) } returns mockVoidTask
            coEvery { mockVoidTask.await() } returns Unit // Use returns Unit for suspend functions returning Unit

            // Act
            val result = authRepository.updateUserProfile(uid, user)

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(Unit, result.getOrNull())

            // Verifica el contenido del mapa pasado a Firestore
            assertTrue(mapSlot.isCaptured)
            val capturedMap = mapSlot.captured
            assertEquals("Updated Name", capturedMap["name"])
            assertEquals("updatedusername", capturedMap["username"])
            assertEquals("03/03/2003", capturedMap["birthday"])
            assertEquals("updated@example.com", capturedMap["email"]) // Email debe incluirse
            assertFalse(capturedMap.containsKey("id")) // ID no debe incluirse explícitamente si es el ID del documento
        }

    @Test
    fun `updateUserProfile devuelve fallo cuando la actualizacion en Firestore falla`() = runTest {
        // Arrange
        val uid = "test_uid"
        val user = User(
            uid,
            "Updated Name",
            "updatedusername",
            "updated@example.com",
            "03/03/2003"
        )
        val exception = Exception("Error de conexión a Firestore")

        val mockCollectionReference = mockk<CollectionReference>()
        val mockDocumentReference = mockk<DocumentReference>()
        val mockVoidTask: Task<Void> = mockk() // Mock de la tarea que devuelve set()

        every { mockFirebaseFirestore.collection("users") } returns mockCollectionReference
        every { mockCollectionReference.document(uid) } returns mockDocumentReference
        coEvery { mockDocumentReference.set(any<Map<String, Any>>(), any<SetOptions>()) } returns mockVoidTask
        coEvery { mockVoidTask.await() } throws exception // Await() throws an exception

        // Act
        val result = authRepository.updateUserProfile(uid, user)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception.message, result.exceptionOrNull()?.message)
    }

    @Test
    fun `updateUserProfile no incluye email si es nulo en el User`() = runTest {
        // Arrange
        val uid = "test_uid"
        val user = User(
            uid,
            "Updated Name",
            "updatedusername",
            null,
            "03/03/2003"
        ) // Email es nulo

        val mockCollectionReference = mockk<CollectionReference>()
        val mockDocumentReference = mockk<DocumentReference>()
        val mapSlot = slot<Map<String, Any>>()
        val mockVoidTask: Task<Void> = mockk() // Mock de la tarea que devuelve set()

        every { mockFirebaseFirestore.collection("users") } returns mockCollectionReference
        every { mockCollectionReference.document(uid) } returns mockDocumentReference
        coEvery { mockDocumentReference.set(capture(mapSlot), any<SetOptions>()) } returns mockVoidTask
        coEvery { mockVoidTask.await() } returns Unit // Use returns Unit for suspend functions returning Unit

        // Act
        val result = authRepository.updateUserProfile(uid, user)

        // Assert
        assertTrue(result.isSuccess)
        assertTrue(mapSlot.isCaptured)
        val capturedMap = mapSlot.captured
        assertFalse(capturedMap.containsKey("email")) // Verifica que 'email' NO está en el mapa
    }
}