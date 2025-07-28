package com.cursoandroid.queermap.data.source.remote

import androidx.fragment.app.Fragment
import com.cursoandroid.queermap.util.exceptionOrNull
import com.cursoandroid.queermap.util.getOrNull
import com.cursoandroid.queermap.util.isFailure
import com.cursoandroid.queermap.util.isSuccess
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test


class FacebookSignInDataSourceImplTest {

    // Instance of the class under test
    private lateinit var facebookSignInDataSource: FacebookSignInDataSourceImpl

    // Mocks for dependencies
    private lateinit var mockCallbackManager: CallbackManager
    private lateinit var mockFragment: Fragment


    @Before
    fun setUp() {
        // Initialize MockK annotations and relax unit functions for easier mocking
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Create mock objects for CallbackManager and Fragment
        mockCallbackManager = mockk()
        mockFragment = mockk()

        // Mock the static LoginManager object as it's a singleton
        mockkObject(LoginManager)

        // Define mock behavior for LoginManager's registerCallback method
        every { LoginManager.getInstance().registerCallback(any(), any()) } returns Unit

        // Define mock behavior for LoginManager's logInWithReadPermissions method
        every {
            LoginManager.getInstance().logInWithReadPermissions(
                ofType(androidx.fragment.app.Fragment::class),
                ofType(List::class) as List<String>
            )
        } returns Unit

        // Initialize the class under test
        facebookSignInDataSource = FacebookSignInDataSourceImpl()
    }


    @After
    fun tearDown() {
        // Unmock the static LoginManager object after each test to prevent interference
        unmockkObject(LoginManager)
        // Clear all mocks to ensure a clean state for the next test
        clearAllMocks()
    }

    @Test
    fun `when registerCallback is called then FacebookCallback is registered with LoginManager`() {
        // Given: A slot to capture the FacebookCallback passed to LoginManager
        val callbackSlot = slot<FacebookCallback<LoginResult>>()

        // Configure mock behavior: when registerCallback is called, capture the callback
        every {
            LoginManager.getInstance().registerCallback(mockCallbackManager, capture(callbackSlot))
        } returns Unit

        // When: registerCallback is called on the data source
        facebookSignInDataSource.registerCallback(mockCallbackManager, mockk(relaxed = true))

        // Then: Verify that LoginManager's registerCallback was called exactly once with the captured callback
        verify(exactly = 1) {
            LoginManager.getInstance().registerCallback(mockCallbackManager, callbackSlot.captured)
        }
    }


    @Test
    fun `when logInWithReadPermissions is called then LoginManager is called with correct fragment and permissions`() {
        // Given: A list of permissions to be used for login
        val permissions = listOf("email", "public_profile")

        // When: logInWithReadPermissions is called on the data source
        facebookSignInDataSource.logInWithReadPermissions(mockFragment, permissions)

        // Then: Verify that LoginManager.getInstance().logInWithReadPermissions was called exactly once
        // with the provided fragment and permissions
        verify(exactly = 1) {
            LoginManager.getInstance().logInWithReadPermissions(mockFragment, permissions)
        }
    }

    @Test
    fun `when FacebookCallback onSuccess is triggered then accessTokenChannel emits success`() = runTest {
        // Given: A slot to capture the FacebookCallback registered by the data source
        val callbackSlot = slot<FacebookCallback<LoginResult>>()

        // Configure mock behavior: when registerCallback is called, capture the callback
        every {
            LoginManager.getInstance().registerCallback(any(), capture(callbackSlot))
        } returns Unit

        // Register a callback with the data source (this will capture the callback in callbackSlot)
        facebookSignInDataSource.registerCallback(mockCallbackManager, mockk(relaxed = true))

        // Mocks to simulate a successful LoginResult with an access token
        val testAccessToken = "test_facebook_access_token"
        val mockAccessToken: AccessToken = mockk {
            every { token } returns testAccessToken // Mock the token value
        }
        val mockLoginResult: LoginResult = mockk {
            every { accessToken } returns mockAccessToken // Mock the LoginResult to return the access token
        }

        // When: Directly trigger the onSuccess method of the captured FacebookCallback
        callbackSlot.captured.onSuccess(mockLoginResult)

        // Then: Verify that the accessTokenChannel emitted a Result.Success containing the correct token
        val result = facebookSignInDataSource.accessTokenChannel.first()
        assertTrue(result.isSuccess()) // Assert that the result is a success
        assertEquals(testAccessToken, result.getOrNull()) // Assert that the emitted token matches the test token
    }


    @Test
    fun `when FacebookCallback onCancel is triggered then accessTokenChannel emits failure`() = runTest {
        // Given: A slot to capture the FacebookCallback
        val callbackSlot = slot<FacebookCallback<LoginResult>>()

        // Configure mock behavior: when registerCallback is called, capture the callback
        every {
            LoginManager.getInstance().registerCallback(any(), capture(callbackSlot))
        } returns Unit

        // Register a callback with the data source
        facebookSignInDataSource.registerCallback(mockCallbackManager, mockk(relaxed = true))

        // When: Launch a coroutine to trigger onCancel and collect the channel's first emission
        val job = launch {
            // Simulate a call to onCancel on the captured callback
            callbackSlot.captured.onCancel()
        }

        // Then: Verify that the accessTokenChannel emitted a Result.Failure with the expected message
        val result = facebookSignInDataSource.accessTokenChannel.first()
        assertTrue(result.isFailure()) // Assert that the result is a failure
        assertNotNull(result.exceptionOrNull()) // Assert that an exception is present
        assertEquals("Inicio de sesión cancelado.", result.exceptionOrNull()?.message) // Assert the exception message

        job.cancel() // Cancel the coroutine job
    }

    @Test
    fun `when FacebookCallback onError is triggered then accessTokenChannel emits failure`() = runTest {
        // Given: A slot to capture the FacebookCallback and a test exception
        val callbackSlot = slot<FacebookCallback<LoginResult>>()
        val testException = FacebookException("Something went wrong with Facebook login")

        // Configure mock behavior: when registerCallback is called, capture the callback
        every {
            LoginManager.getInstance().registerCallback(any(), capture(callbackSlot))
        } returns Unit

        // Register a callback with the data source
        facebookSignInDataSource.registerCallback(mockCallbackManager, mockk(relaxed = true))

        // When: Launch a coroutine to trigger onError and collect the channel's first emission
        val job = launch {
            // Simulate a call to onError on the captured callback
            callbackSlot.captured.onError(testException)
        }

        // Then: Verify that the accessTokenChannel emitted a Result.Failure with the correct exception
        val result = facebookSignInDataSource.accessTokenChannel.first()
        assertTrue(result.isFailure()) // Assert that the result is a failure
        assertEquals(testException, result.exceptionOrNull()) // Assert that the emitted exception is the test exception
        assertEquals("Something went wrong with Facebook login", result.exceptionOrNull()?.message) // Assert the exception message

        job.cancel() // Cancel the coroutine job
    }

    @Test
    fun `when FacebookCallback onSuccess is triggered with empty accessToken then accessTokenChannel emits failure`() = runTest {
        // Given: A slot to capture the FacebookCallback
        val callbackSlot = slot<FacebookCallback<LoginResult>>()

        // Configure mock behavior: when registerCallback is called, capture the callback
        every {
            LoginManager.getInstance().registerCallback(any(), capture(callbackSlot))
        } returns Unit

        // Register a callback with the data source
        facebookSignInDataSource.registerCallback(mockCallbackManager, mockk(relaxed = true))

        // Mocks to simulate a LoginResult with an empty access token
        val mockAccessToken: AccessToken = mockk()
        every { mockAccessToken.token } returns "" // Mock the token to be an empty string

        val mockLoginResult: LoginResult = mockk()
        every { mockLoginResult.accessToken } returns mockAccessToken // Mock LoginResult to return the empty token

        // When: Directly trigger the onSuccess method of the captured FacebookCallback
        callbackSlot.captured.onSuccess(mockLoginResult)

        // Then: Verify that the accessTokenChannel emitted a Result.Failure due to the empty token
        val result = facebookSignInDataSource.accessTokenChannel.first()
        assertTrue(result.isFailure()) // Assert that the result is a failure
        assertEquals("Token de acceso de Facebook nulo.", result.exceptionOrNull()?.message) // Assert the specific error message
    }
}
