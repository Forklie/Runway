package com.example.runway.core.event

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AppEventsTest {

    @Test
    fun `a rejected write reaches whoever is listening`() = runTest {
        AppEvents.events.test {
            AppEvents.emit(AppEvent.WriteRejected)
            assertEquals(AppEvent.WriteRejected, awaitItem())
        }
    }

    @Test
    fun `an event with nobody listening is dropped, not replayed later`() = runTest {
        AppEvents.emit(AppEvent.WriteRejected)
        AppEvents.events.test {
            expectNoEvents()
        }
    }
}
