package com.example.runway.domain.usecase

import com.example.runway.domain.model.MovementChannel
import com.example.runway.domain.model.MovementDirection
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Wording taken from real messages. Merchants, numbers and amounts here are made up. */
class SampathSmsTest {

    private val at = Instant.parse("2026-11-05T10:00:00Z")
    private fun parse(body: String, sender: String = "8822") = parseSampathSms(sender, body, at)

    @Test
    fun `card debit`() {
        val m = parse("LKR 1,650.00 debited from AC **1234 via POS at TEST SHOP PVT 37000000\n- For Inq Call 0110 000000, Sampath Bank")!!
        assertEquals("1234", m.accountSuffix)
        assertEquals(MovementDirection.OUT, m.direction)
        assertEquals(MovementChannel.CARD, m.channel)
        assertEquals(165_000L, m.amountMinor)
        assertEquals(at, m.at)
        assertEquals("TEST SHOP PVT", m.description) // terminal id and footer dropped
    }

    @Test
    fun `description from each wording`() {
        assertEquals("SALARY", parse("LKR 1.00 credited to AC **1234 for SALARY To Inq Call 0110 000000 Get protected -Do not Share OTP")!!.description)
        assertEquals("PICKME", parse("LKR 1.00 credited to AC **1234 for PICKME - 37419306\n22/05/2026 23:36:52\nTo Inq Call 0112303050")!!.description)
        assertEquals("TESTSTORE 12", parse("LKR 1.00 debited from AC **1234 for eCom TESTSTORE 12 - For Inq Call 0110 000000, Sampath Bank")!!.description)
        assertEquals("TEST BRANCH", parse("LKR 1.00 debited from AC **1234 via ATM at TEST BRANCH For Inq Call 0110 000000, Sampath Bank")!!.description)
        assertEquals("TEST SHOP", parse("Yr A/c **1234 debited Rs. 1.00 for POS TEST SHOP -For inquiries call 0110 000000, Sampath Bank")!!.description)
        assertEquals("", parse("LKR 1.00 debited from AC **1234 - For Inq Call 0110 000000, Sampath Bank")!!.description)
    }

    @Test
    fun `newer footer and a credit`() {
        val m = parse("LKR 80,000.50 credited to AC **1234 for SALARY To Inq Call 0110 000000 Get protected -Do not Share OTP", "SAMPATHTXN")!!
        assertEquals(MovementDirection.IN, m.direction)
        assertEquals(MovementChannel.OTHER, m.channel)
        assertEquals(8_000_050L, m.amountMinor)
    }

    @Test
    fun `atm, online and cash deposit`() {
        assertEquals(MovementChannel.ATM, parse("LKR 5,000.00 debited from AC **1234 via ATM at TEST BRANCH For Inq Call 0110 000000, Sampath Bank")!!.channel)
        assertEquals(MovementChannel.ONLINE, parse("LKR 99.50 debited from AC **1234 for eCom TESTSTORE 12 - For Inq Call 0110 000000, Sampath Bank")!!.channel)
        assertEquals(MovementDirection.IN, parse("LKR 1,000.00 deposited to AC **1234 on 2026-01-01 - For Inq Call 0110 000000, Sampath Bank")!!.direction)
    }

    @Test
    fun `older format`() {
        val debit = parse("Yr A/c **1234 debited Rs. 250.00 for POS TEST SHOP -For inquiries call 0110 000000, Sampath Bank")!!
        assertEquals(MovementDirection.OUT, debit.direction)
        assertEquals(MovementChannel.CARD, debit.channel)
        assertEquals(25_000L, debit.amountMinor)
        val credit = parse("Yr A/c **1234 credited Rs. 1,000.00 for TEST -For inquiries call 0110 000000, Sampath Bank")!!
        assertEquals(MovementDirection.IN, credit.direction)
        assertEquals(MovementChannel.ATM, parse("Yr A/c **1234 debited Rs. 500.00 via ATM 30May-For inquiries call 0110 000000, Sampath Bank")!!.channel)
    }

    @Test
    fun `messages that move no money are ignored`() {
        listOf(
            "Spend Rs.500.00 more to earn raffle entry from \"Test\" promotion for in-store and Online purchases",
            "You have earned 1 entry to the Monthly Draw",
            "FD **1234 renewed LKR 100,000.00 for 12 Month(s) - For Inq Call 0110 000000, Sampath Bank",
            "FD **1234 for LKR 100,000.00 closed - For Inq Call 0110 000000, Sampath Bank",
            "New FD opened under Acc no **1234 for LKR 100,000.00 - For Inq Call 0110 000000, Sampath Bank",
            "Your OTP is 123456. Do not share it",
        ).forEach { assertNull(it, parse(it)) }
    }

    @Test
    fun `unknown direction, other currencies and bad amounts are ignored`() {
        assertNull(parse("Yr A/c **1234 debited/ credited Rs. 100.00 for TEST -Sampath Bank"))
        assertNull(parse("USD 20.00 debited from AC **1234 via POS at TEST - For Inq Call 0110 000000, Sampath Bank"))
        assertNull(parse("LKR 0.00 debited from AC **1234 via POS at TEST"))
    }

    @Test
    fun `other senders never count, whatever the wording`() {
        val body = "LKR 1,650.00 debited from AC **1234 via POS at TEST - For Inq Call 0110 000000, Sampath Bank"
        assertNull(parse(body, sender = "+94771234567"))
        assertNull(parse(body, sender = "COMBANK"))
        assertEquals(165_000L, parse(body, sender = "sampathtxn")!!.amountMinor)
    }
}
