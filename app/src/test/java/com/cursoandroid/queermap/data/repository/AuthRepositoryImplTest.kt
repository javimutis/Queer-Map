package com.cursoandroid.queermap.data.repository

import com.cursoandroid.queermap.data.source.AuthRemoteDataSource
import com.cursoandroid.queermap.data.source.local.SharedPreferencesDataSource
import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.util.Result
import com.cursoandroid.queermap.util.failure
import com.cursoandroid.queermap.util.success
import com.cursoandroid.queermap.util.getOrThrow
import com.google.firebase.auth.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.android.gms.tasks.Tasks
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], instrumentedPackages = ["com.google.firebase"])
@LooperMode(LooperMode.Mode.PAUSED)
@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    private val mockRemoteDataSource = mockk<AuthRemoteDataSource>(relaxed = true)
    private val mockSharedPreferencesDataSource = mockk<SharedPreferencesDataSource>(relaxed = true)
    private val mockFirebaseAuth = mockk<FirebaseAuth>(relaxed = true)
    private val mockFirestore = mockk<FirebaseFirestore>(relaxed = true)

    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        repository = AuthRepositoryImpl(
            mockRemoteDataSource,
            mockSharedPreferencesDataSource,
            mockFirebaseAuth,
            mockFirestore
        )
    }

    // loginWithEmailAndPassword solo delega en remoteDataSource
    @Test
    fun `loginWithEmailAndPassword calls remoteDataSource and returns its result`() = runTest {
        val email = "email@test.com"
        val password = "password"
        val expectedResult = success(User("id", "name", null, email, null))
        coEvery { mockRemoteDataSource.loginWithEmailAndPassword(email, password) } returns expectedResult

        val result = repository.loginWithEmailAndPassword(email, password)

        coVerify(exactly = 1) { mockRemoteDataSource.loginWithEmailAndPassword(email, password) }
        assertEquals(expectedResult, result)
    }

    // verifyUserInFirestore casos éxito y fallo
    @Test
    fun `verifyUserInFirestore returns success true when snapshot exists`() = runTest {
        val uid = "uid123"
        val mockSnapshot = mockk<DocumentSnapshot>()
        every { mockSnapshot.exists() } returns true
        coEvery { mockRemoteDataSource.verifyUserInFirestore(uid) } returns success(mockSnapshot)

        val result = repository.verifyUserInFirestore(uid)

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data)
    }

    @Test
    fun `verifyUserInFirestore returns success false when snapshot does not exist`() = runTest {
        val uid = "uid123"
        val mockSnapshot = mockk<DocumentSnapshot>()
        every { mockSnapshot.exists() } returns false
        coEvery { mockRemoteDataSource.verifyUserInFirestore(uid) } returns success(mockSnapshot)

        val result = repository.verifyUserInFirestore(uid)

        assertTrue(result is Result.Success)
        assertFalse((result as Result.Success).data)
    }

    @Test
    fun `verifyUserInFirestore returns failure when remoteDataSource fails`() = runTest {
        val uid = "uid123"
        val ex = Exception("firestore error")
        coEvery { mockRemoteDataSource.verifyUserInFirestore(uid) } returns failure(ex)

        val result = repository.verifyUserInFirestore(uid)

        assertTrue(result is Result.Failure)
        assertEquals("firestore error", (result as Result.Failure).exception.message)
    }

    // sendResetPasswordEmail delega en remoteDataSource
    @Test
    fun `sendResetPasswordEmail calls remoteDataSource and returns result`() = runTest {
        val email = "email@test.com"
        val expected = success(Unit)
        coEvery { mockRemoteDataSource.sendPasswordResetEmail(email) } returns expected

        val result = repository.sendResetPasswordEmail(email)

        coVerify(exactly = 1) { mockRemoteDataSource.sendPasswordResetEmail(email) }
        assertEquals(expected, result)
    }

    // firebaseAuthWithGoogle delega en remoteDataSource
    @Test
    fun `firebaseAuthWithGoogle calls remoteDataSource and returns result`() = runTest {
        val token = "token"
        val expected = success(User("id", "name", null, "email@test.com", null))
        coEvery { mockRemoteDataSource.firebaseAuthWithGoogle(token) } returns expected

        val result = repository.firebaseAuthWithGoogle(token)

        coVerify(exactly = 1) { mockRemoteDataSource.firebaseAuthWithGoogle(token) }
        assertEquals(expected, result)
    }

    // firebaseAuthWithFacebook delega en remoteDataSource
    @Test
    fun `firebaseAuthWithFacebook calls remoteDataSource and returns result`() = runTest {
        val token = "token"
        val expected = success(User("id", "name", null, "email@test.com", null))
        coEvery { mockRemoteDataSource.firebaseAuthWithFacebook(token) } returns expected

        val result = repository.firebaseAuthWithFacebook(token)

        coVerify(exactly = 1) { mockRemoteDataSource.firebaseAuthWithFacebook(token) }
        assertEquals(expected, result)
    }

    // saveCredentials llama a sharedPreferencesDataSource
    @Test
    fun `saveCredentials calls sharedPreferencesDataSource`() {
        val email = "email@test.com"
        val password = "password"
        every { mockSharedPreferencesDataSource.saveCredentials(email, password) } just Runs

        repository.saveCredentials(email, password)

        verify(exactly = 1) { mockSharedPreferencesDataSource.saveCredentials(email, password) }
    }

    // loadSavedCredentials llama a sharedPreferencesDataSource y retorna resultado
    @Test
    fun `loadSavedCredentials calls sharedPreferencesDataSource and returns result`() {
        val pair = Pair("email@test.com", "password")
        every { mockSharedPreferencesDataSource.loadSavedCredentials() } returns pair

        val result = repository.loadSavedCredentials()

        verify(exactly = 1) { mockSharedPreferencesDataSource.loadSavedCredentials() }
        assertEquals(pair, result)
    }

    // registerUser

    @Test
    fun `registerUser success creates user and saves in firestore`() = runTest {
        val user = User(id = "", name = "Test", username = "testuser", email = "test@example.com", birthday = "2000-01-01")
        val password = "password123"
        val mockAuthResult = mockk<com.google.firebase.auth.AuthResult>()
        val mockFirebaseUser = mockk<com.google.firebase.auth.FirebaseUser>()
        val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>()
        val mockDoc = mockk<com.google.firebase.firestore.DocumentReference>()

        every { mockFirebaseUser.uid } returns "uid123"
        every { mockAuthResult.user } returns mockFirebaseUser
        every { mockFirebaseUser.email } returns user.email
        every { mockFirebaseUser.displayName } returns user.name
        coEvery { mockFirebaseAuth.createUserWithEmailAndPassword(user.email!!, password) } returns Tasks.forResult(mockAuthResult)
        every { mockFirestore.collection("users") } returns mockCollection
        every { mockCollection.document("uid123") } returns mockDoc
        coEvery { mockDoc.set(any<Map<String, Any>>()) } returns Tasks.forResult(null)

        val result = repository.registerUser(user, password)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { mockFirebaseAuth.createUserWithEmailAndPassword(user.email!!, password) }
        coVerify(exactly = 1) { mockDoc.set(any<Map<String, Any>>()) }
    }

    @Test
    fun `registerUser returns failure when user is null after registration`() = runTest {
        val user = User(id = "", name = "Test", username = "testuser", email = "test@example.com", birthday = "2000-01-01")
        val password = "password123"
        val mockAuthResult = mockk<com.google.firebase.auth.AuthResult>()

        every { mockAuthResult.user } returns null
        coEvery { mockFirebaseAuth.createUserWithEmailAndPassword(user.email!!, password) } returns Tasks.forResult(mockAuthResult)

        val result = repository.registerUser(user, password)

        assertTrue(result is Result.Failure)
        assertEquals("Usuario nulo después del registro.", (result as Result.Failure).exception.message)
    }

    @Test
    fun `registerUser returns failure when firestore set fails`() = runTest {
        val user = User(id = "", name = "Test", username = "testuser", email = "test@example.com", birthday = "2000-01-01")
        val password = "password123"
        val mockAuthResult = mockk<com.google.firebase.auth.AuthResult>()
        val mockFirebaseUser = mockk<com.google.firebase.auth.FirebaseUser>()
        val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>()
        val mockDoc = mockk<com.google.firebase.firestore.DocumentReference>()
        val exception = Exception("firestore set error")

        every { mockFirebaseUser.uid } returns "uid123"
        every { mockAuthResult.user } returns mockFirebaseUser
        coEvery { mockFirebaseAuth.createUserWithEmailAndPassword(user.email!!, password) } returns Tasks.forResult(mockAuthResult)
        every { mockFirestore.collection("users") } returns mockCollection
        every { mockCollection.document("uid123") } returns mockDoc
        coEvery { mockDoc.set(any<Map<String, Any>>()) } returns Tasks.forException(exception)

        val result = repository.registerUser(user, password)

        assertTrue(result is Result.Failure)
        assertEquals("firestore set error", (result as Result.Failure).exception.message)
    }

    @Test
    fun `registerUser returns mapped exception for user collision`() = runTest {
        val user = User(id = "", name = "Test", username = "testuser", email = "test@example.com", birthday = "2000-01-01")
        val password = "password123"
        val ex = FirebaseAuthUserCollisionException("code", "collision")

        coEvery { mockFirebaseAuth.createUserWithEmailAndPassword(user.email!!, password) } returns Tasks.forException(ex)

        val result = repository.registerUser(user, password)

        assertTrue(result is Result.Failure)
        assertEquals("Este email ya está registrado.", (result as Result.Failure).exception.message)
    }

    @Test
    fun `registerUser returns mapped exception for weak password`() = runTest {
        val user = User(id = "", name = "Test", username = "testuser", email = "test@example.com", birthday = "2000-01-01")
        val password = "weak"
        val ex = FirebaseAuthWeakPasswordException("code", "weak password", "weak_password")

        coEvery { mockFirebaseAuth.createUserWithEmailAndPassword(user.email!!, password) } returns Tasks.forException(ex)

        val result = repository.registerUser(user, password)

        assertTrue(result is Result.Failure)
        assertEquals("La contraseña es demasiado débil.", (result as Result.Failure).exception.message)
    }


    @Test
    fun `registerUser returns mapped exception for invalid credentials`() = runTest {
        val user = User(id = "", name = "Test", username = "testuser", email = "test@example.com", birthday = "2000-01-01")
        val password = "invalid"
        val ex = FirebaseAuthInvalidCredentialsException("code", "invalid credentials")

        coEvery { mockFirebaseAuth.createUserWithEmailAndPassword(user.email!!, password) } returns Tasks.forException(ex)

        val result = repository.registerUser(user, password)

        assertTrue(result is Result.Failure)
        assertEquals("Email o contraseña inválidos.", (result as Result.Failure).exception.message)
    }

    // updateUserProfile

    @Test
    fun `updateUserProfile success updates firestore`() = runTest {
        val uid = "uid123"
        val user = User(id = uid, name = "Name", username = "username", email = "email@test.com", birthday = "2000-01-01")
        val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>()
        val mockDoc = mockk<com.google.firebase.firestore.DocumentReference>()

        every { mockFirestore.collection("users") } returns mockCollection
        every { mockCollection.document(uid) } returns mockDoc
        coEvery { mockDoc.set(any<Map<String, Any>>(), SetOptions.merge()) } returns Tasks.forResult(null)

        val result = repository.updateUserProfile(uid, user)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { mockDoc.set(any<Map<String, Any>>(), SetOptions.merge()) }
    }

    @Test
    fun `updateUserProfile failure returns failure`() = runTest {
        val uid = "uid123"
        val user = User(id = uid, name = "Name", username = "username", email = "email@test.com", birthday = "2000-01-01")
        val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>()
        val mockDoc = mockk<com.google.firebase.firestore.DocumentReference>()
        val ex = Exception("update failed")

        every { mockFirestore.collection("users") } returns mockCollection
        every { mockCollection.document(uid) } returns mockDoc
        coEvery { mockDoc.set(any<Map<String, Any>>(), SetOptions.merge()) } returns Tasks.forException(ex)

        val result = repository.updateUserProfile(uid, user)

        assertTrue(result is Result.Failure)
        assertEquals("update failed", (result as Result.Failure).exception.message)
    }
}
