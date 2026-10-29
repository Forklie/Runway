package com.example.runway.ui.feature.session

import com.example.runway.core.result.AppError
import com.example.runway.ui.feature.auth.LoginDest
import com.example.runway.ui.navigation.HomeDest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionRoutingTest {

    @Test
    fun `nothing to route to while loading or after a problem`() {
        assertNull(SessionState.Loading.startDestination())
        assertNull(SessionState.Problem(AppError.Network).startDestination())
    }

    @Test
    fun `logged out goes to login`() {
        assertEquals(LoginDest, SessionState.LoggedOut.startDestination())
    }

    @Test
    fun `ready goes home`() {
        assertEquals(HomeDest, SessionState.Ready.startDestination())
    }

    @Test
    fun `until onboarding exists it also goes home`() {
        // Day 3: change to OnboardingDest in SessionRouting.kt and in this test.
        assertEquals(HomeDest, SessionState.NeedsOnboarding.startDestination())
    }
}
