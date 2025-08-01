package com.cursoandroid.queermap.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class ResultTest {

    @Test
    fun `Success class holds correct data`() {
        val data = "Hello"
        val result = Result.Success(data)
        assertEquals(data, (result as Result.Success).data)
    }

    @Test
    fun `Failure class holds correct exception and message`() {
        val exception = IOException("Network error")
        val message = "Something went wrong"
        val result = Result.Failure(exception, message)
        assertEquals(exception, (result as Result.Failure).exception)
        assertEquals(message, result.message)
    }

    @Test
    fun `Failure class holds correct exception without message`() {
        val exception = NullPointerException("NPE")
        val result = Result.Failure(exception)
        assertEquals(exception, (result as Result.Failure).exception)
        assertNull(result.message)
    }

    @Test
    fun `success function creates Success result`() {
        val value = 123
        val result = success(value)
        assertTrue(result is Result.Success)
        assertEquals(value, (result as Result.Success).data)
    }

    @Test
    fun `failure function creates Failure result`() {
        val exception = IllegalArgumentException("Invalid arg")
        val result = failure<String>(exception) // Specify type for 'nothing'
        assertTrue(result is Result.Failure)
        assertEquals(exception, (result as Result.Failure).exception)
        assertNull(result.message) // Default message is null
    }
    // Dentro de la clase ResultTest { ... }

    @Test
    fun `onSuccess executes action when result is Success`() {
        var invoked = false
        val data = "Success data"
        val result = success(data)

        result.onSuccess { value ->
            invoked = true
            assertEquals(data, value)
        }.onFailure {
            // This should not be invoked
            fail("onFailure should not be called for Success result")
        }
        assertTrue(invoked)
    }

    @Test
    fun `onSuccess does not execute action when result is Failure`() {
        var invoked = false
        val exception = RuntimeException("Error")
        val result = failure<String>(exception)

        result.onSuccess {
            invoked = true
        }
        assertFalse(invoked)
    }

    @Test
    fun `onFailure executes action when result is Failure`() {
        var invoked = false
        val exception = IOException("Network error")
        val result = failure<String>(exception)

        result.onFailure { e ->
            invoked = true
            assertEquals(exception, e)
        }.onSuccess {
            // This should not be invoked
            fail("onSuccess should not be called for Failure result")
        }
        assertTrue(invoked)
    }

    @Test
    fun `onFailure does not execute action when result is Success`() {
        var invoked = false
        val data = "Success data"
        val result = success(data)

        result.onFailure {
            invoked = true
        }
        assertFalse(invoked)
    }
    // Dentro de la clase ResultTest { ... }

    @Test
    fun `getOrThrow returns data for Success`() {
        val data = 42
        val result = success(data)
        assertEquals(data, result.getOrThrow())
    }

    @Test(expected = IOException::class)
    fun `getOrThrow throws exception for Failure`() {
        val exception = IOException("Test exception")
        val result = failure<Int>(exception)
        result.getOrThrow() // This should throw IOException
    }

    @Test
    fun `getOrNull returns data for Success`() {
        val data = "Some string"
        val result = success(data)
        assertEquals(data, result.getOrNull())
    }

    @Test
    fun `getOrNull returns null for Failure`() {
        val exception = IllegalStateException("Test state")
        val result = failure<String>(exception)
        assertNull(result.getOrNull())
    }

    @Test
    fun `exceptionOrNull returns null for Success`() {
        val data = true
        val result = success(data)
        assertNull(result.exceptionOrNull())
    }

    @Test
    fun `exceptionOrNull returns exception for Failure`() {
        val exception = NoSuchElementException("No element")
        val result = failure<Unit>(exception)
        assertEquals(exception, result.exceptionOrNull())
    }
    @Test
    fun `onSuccess does not invoke block for Failure but still returns original Failure`() {
        val exception = RuntimeException("Some error")
        val result = failure<String>(exception)

        var successCalled = false

        val returned = result.onSuccess {
            successCalled = true
        }

        assertFalse(successCalled)
        assertTrue(returned is Result.Failure)
        assertEquals(exception, (returned as Result.Failure).exception)
    }
    @Test
    fun `onFailure does not invoke block for Success but still returns original Success`() {
        val result = success("OK")

        var failureCalled = false

        val returned = result.onFailure {
            failureCalled = true
        }

        assertFalse(failureCalled)
        assertTrue(returned is Result.Success)
        assertEquals("OK", (returned as Result.Success).data)
    }


}