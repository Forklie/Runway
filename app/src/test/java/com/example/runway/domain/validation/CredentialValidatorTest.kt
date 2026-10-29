package com.example.runway.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialValidatorTest {

    @Test
    fun `a normal email is accepted, even with spaces around it`() {
        assertNull(CredentialValidator.validateEmail("kavindu@example.com"))
        assertNull(CredentialValidator.validateEmail("  kavindu@example.com  "))
    }

    @Test
    fun `a blank email is reported as empty`() {
        assertEquals(EmailIssue.Empty, CredentialValidator.validateEmail(""))
        assertEquals(EmailIssue.Empty, CredentialValidator.validateEmail("   "))
    }

    @Test
    fun `emails without an at sign or a domain dot are invalid`() {
        assertEquals(EmailIssue.Invalid, CredentialValidator.validateEmail("kavindu"))
        assertEquals(EmailIssue.Invalid, CredentialValidator.validateEmail("kavindu@example"))
        assertEquals(EmailIssue.Invalid, CredentialValidator.validateEmail("@example.com"))
        assertEquals(EmailIssue.Invalid, CredentialValidator.validateEmail("kav indu@example.com"))
    }

    @Test
    fun `new passwords need 6 characters`() {
        assertEquals(PasswordIssue.Empty, CredentialValidator.validateNewPassword(""))
        assertEquals(PasswordIssue.TooShort, CredentialValidator.validateNewPassword("12345"))
        assertNull(CredentialValidator.validateNewPassword("123456"))
    }

    @Test
    fun `spaces count towards the password length`() {
        assertNull(CredentialValidator.validateNewPassword("      "))
    }

    @Test
    fun `sign-in only requires a filled password`() {
        assertEquals(PasswordIssue.Empty, CredentialValidator.validateSignInPassword(""))
        assertNull(CredentialValidator.validateSignInPassword("abc"))
    }

    @Test
    fun `a name cannot be blank`() {
        assertEquals(NameIssue.Empty, CredentialValidator.validateName("  "))
        assertNull(CredentialValidator.validateName("Kavindu"))
    }
}
