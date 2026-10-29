package com.example.runway.ui.feature.auth

import com.example.runway.R
import com.example.runway.core.result.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Where each auth error is shown: under its field, or in the banner above the button. */
class AuthFormStateTest {

    @Test
    fun `duplicate email goes under the email field`() {
        val state = AuthFormState(formError = AppError.EmailInUse)
        assertEquals(R.string.error_email_in_use, state.emailErrorRes())
        assertNull(state.passwordErrorRes())
        assertNull(state.bannerErrorRes())
    }

    @Test
    fun `weak password goes under the password field`() {
        val state = AuthFormState(formError = AppError.WeakPassword)
        assertEquals(R.string.error_weak_password, state.passwordErrorRes())
        assertNull(state.emailErrorRes())
        assertNull(state.bannerErrorRes())
    }

    @Test
    fun `wrong password and offline go in the banner`() {
        assertEquals(R.string.error_invalid_credentials, AuthFormState(formError = AppError.InvalidCredentials).bannerErrorRes())
        assertEquals(R.string.error_network, AuthFormState(formError = AppError.Network).bannerErrorRes())
    }

    @Test
    fun `no error means no messages`() {
        val state = AuthFormState()
        assertNull(state.emailErrorRes())
        assertNull(state.passwordErrorRes())
        assertNull(state.bannerErrorRes())
    }
}
