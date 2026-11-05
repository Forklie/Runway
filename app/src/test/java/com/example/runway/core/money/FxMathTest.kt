package com.example.runway.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FxMathTest {

    @Test
    fun `USD 120 at 305 is LKR 36,600 (AT-07)`() {
        // USD 120.00 = 12_000 minor units
        assertEquals(3_660_000L, FxMath.toLkrMinor(12_000L, 305.0))
    }

    @Test
    fun `effective rate is LKR received divided by the foreign amount (AT-07)`() {
        assertEquals(305.0, FxMath.effectiveRate(3_660_000L, 12_000L)!!, 0.0001)
    }

    @Test
    fun `effective rate is null instead of dividing by zero`() {
        assertNull(FxMath.effectiveRate(3_660_000L, 0L))
    }

    @Test
    fun `a rate of 1 leaves LKR amounts unchanged (BR-03)`() {
        assertEquals(85_000L, FxMath.toLkrMinor(85_000L, 1.0))
    }

    @Test
    fun `rounds half up to a whole minor unit`() {
        assertEquals(1L, FxMath.toLkrMinor(1L, 0.5))
        assertEquals(2L, FxMath.toLkrMinor(3L, 0.5))
        assertEquals(3_663_000L, FxMath.toLkrMinor(12_000L, 305.25))
    }
}
