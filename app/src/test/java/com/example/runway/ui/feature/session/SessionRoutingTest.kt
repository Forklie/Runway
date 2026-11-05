package com.example.runway.ui.feature.session

import com.example.runway.core.result.AppError
import com.example.runway.ui.feature.auth.LoginDest
import com.example.runway.ui.feature.auth.RegisterDest
import com.example.runway.ui.feature.auth.ResetPasswordDest
import com.example.runway.ui.feature.onboarding.OnboardingDest
import com.example.runway.ui.navigation.ActivityDest
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
    fun `a new user goes to onboarding (FR-02)`() {
        assertEquals(OnboardingDest, SessionState.NeedsOnboarding.startDestination())
    }

    @Test
    fun `every auth screen is in the auth group`() {
        assertEquals(RouteGroup.Auth, routeGroupOf(LoginDest))
        assertEquals(RouteGroup.Auth, routeGroupOf(RegisterDest))
        assertEquals(RouteGroup.Auth, routeGroupOf(ResetPasswordDest))
    }

    @Test
    fun `onboarding is its own group`() {
        assertEquals(RouteGroup.Onboarding, routeGroupOf(OnboardingDest))
    }

    @Test
    fun `everything else is the main app`() {
        assertEquals(RouteGroup.Main, routeGroupOf(HomeDest))
        assertEquals(RouteGroup.Main, routeGroupOf(ActivityDest))
    }

    @Test
    fun `finishing onboarding moves the user from onboarding to main`() {
        val before = routeGroupOf(SessionState.NeedsOnboarding.startDestination()!!)
        val after = routeGroupOf(SessionState.Ready.startDestination()!!)
        assertEquals(RouteGroup.Onboarding, before)
        assertEquals(RouteGroup.Main, after)
    }
}
