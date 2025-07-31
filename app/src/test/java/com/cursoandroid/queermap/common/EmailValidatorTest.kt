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
    fun `isValidEmail returns true for standard email`() {
        val result = EmailValidator.isValidEmail("test@example.com")
        assertTrue(result)
    }

    @Test
    fun `isValidEmail returns true for email with dots and plus`() {
        val result = EmailValidator.isValidEmail("user.name+tag+sorting@example.com")
        assertTrue(result)
    }

    @Test
    fun `isValidEmail returns true for email with subdomain`() {
        val result = EmailValidator.isValidEmail("user@mail.example.co")
        assertTrue(result)
    }

    @Test
    fun `isValidEmail returns false for email without @ symbol`() {
        val result = EmailValidator.isValidEmail("invalidemail.com")
        assertFalse(result)
    }

    @Test
    fun `isValidEmail returns false for email with invalid domain`() {
        val result = EmailValidator.isValidEmail("user@example")
        assertFalse(result)
    }

    @Test
    fun `isValidEmail returns false for empty string`() {
        val result = EmailValidator.isValidEmail("")
        assertFalse(result)
    }

    @Test
    fun `isValidEmail returns false for email with special characters`() {
        val result = EmailValidator.isValidEmail("user@exam!ple.com")
        assertFalse(result)
    }

    @Test
    fun `isValidEmail returns false for email starting with dot`() {
        val result = EmailValidator.isValidEmail(".user@example.com")
        assertFalse(result)
    }

    @Test
    fun `isValidEmail returns false for email ending with dot`() {
        val result = EmailValidator.isValidEmail("user.@example.com")
        assertFalse(result)
    }
}
