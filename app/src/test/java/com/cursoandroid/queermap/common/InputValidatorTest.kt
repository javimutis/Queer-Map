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

    // Email tests
    @Test
    fun `isValidEmail returns true for valid email`() {
        assertTrue(validator.isValidEmail("user@example.com"))
    }

    @Test
    fun `isValidEmail returns false for invalid email`() {
        assertFalse(validator.isValidEmail("invalid-email"))
    }

    // Password length tests
    @Test
    fun `isValidPassword returns true for password of minimum length`() {
        assertTrue(validator.isValidPassword("123456"))
    }

    @Test
    fun `isValidPassword returns false for short password`() {
        assertFalse(validator.isValidPassword("123"))
    }

    // Strong password tests
    @Test
    fun `isStrongPassword returns true for complex valid password`() {
        assertTrue(validator.isStrongPassword("Aa1@abcd"))
    }

    @Test
    fun `isStrongPassword returns false for password without uppercase`() {
        assertFalse(validator.isStrongPassword("aa1@abcd"))
    }

    @Test
    fun `isStrongPassword returns false for password without lowercase`() {
        assertFalse(validator.isStrongPassword("AA1@ABCD"))
    }

    @Test
    fun `isStrongPassword returns false for password without digit`() {
        assertFalse(validator.isStrongPassword("Aa@bcdef"))
    }

    @Test
    fun `isStrongPassword returns false for password without special char`() {
        assertFalse(validator.isStrongPassword("Aa1bcdef"))
    }

    @Test
    fun `isStrongPassword returns false for short password`() {
        assertFalse(validator.isStrongPassword("Aa1@a"))
    }

    // Username tests
    @Test
    fun `isValidUsername returns true for non-blank username`() {
        assertTrue(validator.isValidUsername("username"))
    }

    @Test
    fun `isValidUsername returns false for blank username`() {
        assertFalse(validator.isValidUsername(""))
    }

    // Full name tests
    @Test
    fun `isValidFullName returns true for non-blank name`() {
        assertTrue(validator.isValidFullName("Jane Doe"))
    }

    @Test
    fun `isValidFullName returns false for blank name`() {
        assertFalse(validator.isValidFullName(""))
    }

    // Birthday tests
    @Test
    fun `isValidBirthday returns true for non-blank birthday`() {
        assertTrue(validator.isValidBirthday("2000-01-01"))
    }

    @Test
    fun `isValidBirthday returns false for blank birthday`() {
        assertFalse(validator.isValidBirthday(""))
    }
}
