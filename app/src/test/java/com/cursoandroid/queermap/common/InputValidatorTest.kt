package com.cursoandroid.queermap.common

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
class InputValidatorTest {

    private lateinit var validator: InputValidator

    @Before
    fun setUp() {
        validator = InputValidator()
    }

    @Test
    fun `when email is valid then return true`() {
        // Given
        val email = "user@example.com"

        // When
        val result = validator.isValidEmail(email)

        // Then
        assertTrue(result)
    }

    @Test
    fun `when email is invalid then return false`() {
        // Given
        val email = "invalid-email"

        // When
        val result = validator.isValidEmail(email)

        // Then
        assertFalse(result)
    }

    @Test
    fun `when password meets minimum length then return true`() {
        // Given
        val password = "123456"

        // When
        val result = validator.isValidPassword(password)

        // Then
        assertTrue(result)
    }

    @Test
    fun `when password is too short then return false`() {
        // Given
        val password = "123"

        // When
        val result = validator.isValidPassword(password)

        // Then
        assertFalse(result)
    }

    @Test
    fun `when password is strong then return true`() {
        // Given
        val password = "Aa1@abcd"

        // When
        val result = validator.isStrongPassword(password)

        // Then
        assertTrue(result)
    }

    @Test
    fun `when password lacks uppercase then return false`() {
        // Given
        val password = "aa1@abcd"

        // When
        val result = validator.isStrongPassword(password)

        // Then
        assertFalse(result)
    }

    @Test
    fun `when password lacks lowercase then return false`() {
        // Given
        val password = "AA1@ABCD"

        // When
        val result = validator.isStrongPassword(password)

        // Then
        assertFalse(result)
    }

    @Test
    fun `when password lacks digit then return false`() {
        // Given
        val password = "Aa@bcdef"

        // When
        val result = validator.isStrongPassword(password)

        // Then
        assertFalse(result)
    }

    @Test
    fun `when password lacks special character then return false`() {
        // Given
        val password = "Aa1bcdef"

        // When
        val result = validator.isStrongPassword(password)

        // Then
        assertFalse(result)
    }

    @Test
    fun `when password is too short to be strong then return false`() {
        // Given
        val password = "Aa1@a"

        // When
        val result = validator.isStrongPassword(password)

        // Then
        assertFalse(result)
    }

    @Test
    fun `when username is not blank then return true`() {
        // Given
        val username = "username"

        // When
        val result = validator.isValidUsername(username)

        // Then
        assertTrue(result)
    }

    @Test
    fun `when username is blank then return false`() {
        // Given
        val username = ""

        // When
        val result = validator.isValidUsername(username)

        // Then
        assertFalse(result)
    }

    @Test
    fun `when full name is not blank then return true`() {
        // Given
        val fullName = "Jane Doe"

        // When
        val result = validator.isValidFullName(fullName)

        // Then
        assertTrue(result)
    }

    @Test
    fun `when full name is blank then return false`() {
        // Given
        val fullName = ""

        // When
        val result = validator.isValidFullName(fullName)

        // Then
        assertFalse(result)
    }

    @Test
    fun `when birthday is not blank then return true`() {
        // Given
        val birthday = "2000-01-01"

        // When
        val result = validator.isValidBirthday(birthday)

        // Then
        assertTrue(result)
    }

    @Test
    fun `when birthday is blank then return false`() {
        // Given
        val birthday = ""

        // When
        val result = validator.isValidBirthday(birthday)

        // Then
        assertFalse(result)
    }
}
