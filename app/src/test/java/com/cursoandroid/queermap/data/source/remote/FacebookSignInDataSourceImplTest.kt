package com.cursoandroid.queermap.data.source.remote

import android.os.Looper
import androidx.fragment.app.Fragment
import com.cursoandroid.queermap.util.Result
import com.cursoandroid.queermap.util.success
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
@OptIn(ExperimentalCoroutinesApi::class)
class FacebookSignInDataSourceTest {

    private lateinit var dataSource: FacebookSignInDataSourceImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        dataSource = FacebookSignInDataSourceImpl()
    }

    @Test
    fun `registerCallback should register the given callback with LoginManager`() {
        // Given
        val mockCallbackManager = mockk<CallbackManager>()
        val mockFacebookCallback = mockk<FacebookCallback<LoginResult>>()

        mockkObject(LoginManager.Companion)
        val mockLoginManager = mockk<LoginManager>(relaxed = true)
        every { LoginManager.getInstance() } returns mockLoginManager

        // When
        dataSource.registerCallback(mockCallbackManager, mockFacebookCallback)

        // Then
        verify(exactly = 1) {
            mockLoginManager.registerCallback(mockCallbackManager, mockFacebookCallback)
        }
    }

    @Test
    fun `logInWithReadPermissions should call LoginManager with provided fragment and permissions`() {
        // Given
        val mockFragment = mockk<Fragment>()
        val permissions = listOf("email", "public_profile")

        mockkObject(LoginManager.Companion)
        val mockLoginManager = mockk<LoginManager>(relaxed = true)
        every { LoginManager.getInstance() } returns mockLoginManager

        // When
        dataSource.logInWithReadPermissions(mockFragment, permissions)

        // Then
        verify(exactly = 1) {
            mockLoginManager.logInWithReadPermissions(mockFragment, permissions)
        }
    }

    @Test
    fun `accessTokenChannel should emit Result when offered manually`() = runTest {
        // Given
        val accessToken = "mock_token"
        val result = success(accessToken)
        val internalChannel = dataSource.javaClass
            .getDeclaredField("_accessTokenChannel")
            .apply { isAccessible = true }
            .get(dataSource) as kotlinx.coroutines.channels.Channel<Result<String>>

        // When
        internalChannel.trySend(result)

        // Then
        val emitted = dataSource.accessTokenChannel.first()
        assertTrue(emitted is Result.Success)
        assertEquals(accessToken, (emitted as Result.Success).data)
    }
}
