package com.cursoandroid.queermap.common

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class EmailValidatorTest {

    @Test
    fun `when email is valid then return true`() {
        // Given
        val email = "user@example.com"

        // When
        val result = EmailValidator.isValidEmail(email)

        // Then
        assertTrue(result)
    }

    @Test
    fun `when email has plus sign then return true`() {
        // Given
        val email = "user+test@example.com"

        // When
        val result = EmailValidator.isValidEmail(email)

        // Then
        assertTrue(result)
    }

    @Test
    fun `when email has underscore then return true`() {
        // Given
        val email = "user_test@example.com"

        // When
        val result = EmailValidator.isValidEmail(email)

        // Then
        assertTrue(result)
    }

    @Test
    fun `when email has invalid format then return false`() {
        // Given
        val email = "invalid-email"

        // When
        val result = EmailValidator.isValidEmail(email)

        // Then
        assertFalse(result)
    }

    @Test
    fun `when email is missing at symbol then return false`() {
        // Given
        val email = "userexample.com"

        // When
        val result = EmailValidator.isValidEmail(email)

        // Then
        assertFalse(result)
    }

    @Test
    fun `when email has invalid domain then return false`() {
        // Given
        val email = "user@example"

        // When
        val result = EmailValidator.isValidEmail(email)

        // Then
        assertFalse(result)
    }

    @Test
    fun `when email is empty string then return false`() {
        // Given
        val email = ""

        // When
        val result = EmailValidator.isValidEmail(email)

        // Then
        assertFalse(result)
    }
}
