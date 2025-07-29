package com.cursoandroid.queermap.data.source.remote

import com.cursoandroid.queermap.domain.model.User
import com.cursoandroid.queermap.util.Result
import com.google.firebase.auth.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
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
class FirebaseAuthDataSourceTest {

    private val mockAuth: FirebaseAuth = mockk(relaxed = true)
    private val mockFirestore: FirebaseFirestore = mockk(relaxed = true)
    private lateinit var dataSource: FirebaseAuthDataSource

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        dataSource = FirebaseAuthDataSource(mockAuth, mockFirestore)
    }

    @Test
    fun `when loginWithEmailAndPassword succeeds and user exists in Firestore then return User from Firestore`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val uid = "12345"
        val mockResult: AuthResult = mockk()
        val mockFirebaseUser: FirebaseUser = mockk()
        val mockDocSnapshot: DocumentSnapshot = mockk()
        val expectedUser = User(uid, "Test User", null, email, null)

        every { mockResult.user } returns mockFirebaseUser
        every { mockFirebaseUser.uid } returns uid
        every { mockFirebaseUser.displayName } returns "Test User"
        every { mockFirebaseUser.email } returns email

        every { mockAuth.signInWithEmailAndPassword(email, password) } returns Tasks.forResult(mockResult)
        every { mockFirestore.collection("users").document(uid).get() } returns Tasks.forResult(mockDocSnapshot)
        every { mockDocSnapshot.exists() } returns true
        every { mockDocSnapshot.toObject(User::class.java) } returns expectedUser

        // When
        val result = dataSource.loginWithEmailAndPassword(email, password)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedUser, (result as Result.Success).data)
    }

    @Test
    fun `when loginWithEmailAndPassword succeeds but user does not exist in Firestore then return User from Firebase`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val uid = "12345"
        val mockResult: AuthResult = mockk()
        val mockFirebaseUser: FirebaseUser = mockk()
        val mockDocSnapshot: DocumentSnapshot = mockk()

        every { mockResult.user } returns mockFirebaseUser
        every { mockFirebaseUser.uid } returns uid
        every { mockFirebaseUser.displayName } returns "Test User"
        every { mockFirebaseUser.email } returns email

        every { mockAuth.signInWithEmailAndPassword(email, password) } returns Tasks.forResult(mockResult)
        every { mockFirestore.collection("users").document(uid).get() } returns Tasks.forResult(mockDocSnapshot)
        every { mockDocSnapshot.exists() } returns false

        // When
        val result = dataSource.loginWithEmailAndPassword(email, password)

        // Then
        assertTrue(result is Result.Success)
        val user = (result as Result.Success).data
        assertEquals(uid, user.id)
        assertEquals("Test User", user.name)
        assertEquals(email, user.email)
    }

    @Test
    fun `when loginWithEmailAndPassword fails then return failure`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "wrongPassword"
        val exception = FirebaseAuthInvalidCredentialsException("ERROR", "Invalid password")

        every { mockAuth.signInWithEmailAndPassword(email, password) } returns Tasks.forException(exception)

        // When
        val result = dataSource.loginWithEmailAndPassword(email, password)

        // Then
        assertTrue(result is Result.Failure)
        assertEquals("Invalid password", (result as Result.Failure).exception.message)
    }

    @Test
    fun `when loginWithEmailAndPassword returns null user then return failure`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val mockResult: AuthResult = mockk()

        every { mockResult.user } returns null
        every { mockAuth.signInWithEmailAndPassword(email, password) } returns Tasks.forResult(mockResult)

        // When
        val result = dataSource.loginWithEmailAndPassword(email, password)

        // Then
        assertTrue(result is Result.Failure)
        assertEquals("Usuario no encontrado.", (result as Result.Failure).exception.message)
    }

    @Test
    fun `when verifyUserInFirestore succeeds then return DocumentSnapshot`() = runTest {
        val uid = "12345"
        val mockDocSnapshot: DocumentSnapshot = mockk()
        every { mockDocSnapshot.exists() } returns true
        every { mockFirestore.collection("users").document(uid).get() } returns Tasks.forResult(mockDocSnapshot)

        val result = dataSource.verifyUserInFirestore(uid)

        assertTrue(result is Result.Success)
        assertEquals(mockDocSnapshot, (result as Result.Success).data)
    }

    @Test
    fun `when verifyUserInFirestore fails then return failure`() = runTest {
        val uid = "12345"
        val exception = Exception("Firestore error")
        every { mockFirestore.collection("users").document(uid).get() } returns Tasks.forException(exception)

        val result = dataSource.verifyUserInFirestore(uid)

        assertTrue(result is Result.Failure)
        assertEquals("Firestore error", (result as Result.Failure).exception.message)
    }

    @Test
    fun `when sendPasswordResetEmail succeeds then return Unit`() = runTest {
        val email = "test@example.com"
        every { mockAuth.sendPasswordResetEmail(email) } returns Tasks.forResult(null)

        val result = dataSource.sendPasswordResetEmail(email)

        assertTrue(result is Result.Success)
        assertEquals(Unit, (result as Result.Success).data)
    }

    @Test
    fun `when sendPasswordResetEmail fails then return failure`() = runTest {
        val email = "test@example.com"
        val exception = FirebaseAuthException("ERROR_CODE", "Email not found")
        every { mockAuth.sendPasswordResetEmail(email) } returns Tasks.forException(exception)

        val result = dataSource.sendPasswordResetEmail(email)

        assertTrue(result is Result.Failure)
        assertEquals("Email not found", (result as Result.Failure).exception.message)
    }
    @Test
    fun `when firebaseAuthWithGoogle succeeds then return User`() = runTest {
        val idToken = "fake-google-token"
        val mockCredential = mockk<AuthCredential>()
        val mockResult: AuthResult = mockk()
        val mockUser: FirebaseUser = mockk()

        every { mockUser.uid } returns "uid123"
        every { mockUser.displayName } returns "Google User"
        every { mockUser.email } returns "google@example.com"

        every { mockResult.user } returns mockUser
        mockkStatic(GoogleAuthProvider::class)
        every { GoogleAuthProvider.getCredential(idToken, null) } returns mockCredential
        every { mockAuth.signInWithCredential(mockCredential) } returns Tasks.forResult(mockResult)

        val result = dataSource.firebaseAuthWithGoogle(idToken)

        assertTrue(result is Result.Success)
        val user = (result as Result.Success).data
        assertEquals("uid123", user.id)
        assertEquals("Google User", user.name)
        assertEquals("google@example.com", user.email)
    }
    @Test
    fun `when firebaseAuthWithGoogle returns null user then return failure`() = runTest {
        val idToken = "fake-google-token"
        val mockCredential = mockk<AuthCredential>()
        val mockResult: AuthResult = mockk()

        every { mockResult.user } returns null
        mockkStatic(GoogleAuthProvider::class)
        every { GoogleAuthProvider.getCredential(idToken, null) } returns mockCredential
        every { mockAuth.signInWithCredential(mockCredential) } returns Tasks.forResult(mockResult)

        val result = dataSource.firebaseAuthWithGoogle(idToken)

        assertTrue(result is Result.Failure)
        assertEquals("Autenticación con Google fallida: Usuario nulo.", (result as Result.Failure).exception.message)
    }
    @Test
    fun `when firebaseAuthWithGoogle fails then return failure`() = runTest {
        val idToken = "fake-google-token"
        val mockCredential = mockk<AuthCredential>()
        val exception = Exception("Google sign-in failed")

        mockkStatic(GoogleAuthProvider::class)
        every { GoogleAuthProvider.getCredential(idToken, null) } returns mockCredential
        every { mockAuth.signInWithCredential(mockCredential) } returns Tasks.forException(exception)

        val result = dataSource.firebaseAuthWithGoogle(idToken)

        assertTrue(result is Result.Failure)
        assertEquals("Google sign-in failed", (result as Result.Failure).exception.message)
    }
    @Test
    fun `when firebaseAuthWithFacebook succeeds then return User`() = runTest {
        val accessToken = "fake-facebook-token"
        val mockCredential = mockk<AuthCredential>()
        val mockResult: AuthResult = mockk()
        val mockUser: FirebaseUser = mockk()

        every { mockUser.uid } returns "uid456"
        every { mockUser.displayName } returns "Facebook User"
        every { mockUser.email } returns "fb@example.com"

        every { mockResult.user } returns mockUser
        mockkStatic(FacebookAuthProvider::class)
        every { FacebookAuthProvider.getCredential(accessToken) } returns mockCredential
        every { mockAuth.signInWithCredential(mockCredential) } returns Tasks.forResult(mockResult)

        val result = dataSource.firebaseAuthWithFacebook(accessToken)

        assertTrue(result is Result.Success)
        val user = (result as Result.Success).data
        assertEquals("uid456", user.id)
        assertEquals("Facebook User", user.name)
        assertEquals("fb@example.com", user.email)
    }
    @Test
    fun `when firebaseAuthWithFacebook returns null user then return failure`() = runTest {
        val accessToken = "fake-facebook-token"
        val mockCredential = mockk<AuthCredential>()
        val mockResult: AuthResult = mockk()

        every { mockResult.user } returns null
        mockkStatic(FacebookAuthProvider::class)
        every { FacebookAuthProvider.getCredential(accessToken) } returns mockCredential
        every { mockAuth.signInWithCredential(mockCredential) } returns Tasks.forResult(mockResult)

        val result = dataSource.firebaseAuthWithFacebook(accessToken)

        assertTrue(result is Result.Failure)
        assertEquals("Autenticación con Facebook fallida: Usuario nulo.", (result as Result.Failure).exception.message)
    }

}
