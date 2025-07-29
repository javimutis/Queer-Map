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
}
