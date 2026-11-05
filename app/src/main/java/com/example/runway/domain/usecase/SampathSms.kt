package com.example.runway.domain.usecase

import com.example.runway.core.money.MoneyParser
import com.example.runway.domain.model.AccountMovement
import com.example.runway.domain.model.MovementChannel
import com.example.runway.domain.model.MovementDirection
import java.time.Instant

/** The only senders trusted: the same wording from any other number doesn't count. */
val SAMPATH_SENDERS = setOf("8822", "SAMPATHTXN")

private val OPTIONS = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
private const val AMOUNT = """(\d[\d,]*(?:\.\d{1,2})?)"""

// "LKR 1,650.00 debited from AC **1234 via POS at SHOP 123 - For Inq Call ..."
private val CURRENT = Regex("""^LKR\s*$AMOUNT\s+(debited from|credited to|deposited to)\s+AC\s+\*+(\d+)\b(.*)""", OPTIONS)

// "Yr A/c **1234 debited Rs. 1,650.00 for POS SHOP -For inquiries call ..."
private val OLDER = Regex("""^Yr A/c\s+\*+(\d+)\s+(debited|credited)\s+Rs\.?\s*$AMOUNT\b(.*)""", OPTIONS)

/**
 * The movement a Sampath Bank SMS reports, or null for anything else: OTPs, promotions ("Spend
 * Rs.500 more"), fixed deposit notices, and "debited/ credited" where the direction is unknown.
 */
fun parseSampathSms(sender: String, body: String, at: Instant): AccountMovement? {
    if (SAMPATH_SENDERS.none { it.equals(sender.trim(), ignoreCase = true) }) return null
    val text = body.trim()
    CURRENT.find(text)?.destructured?.let { (amount, verb, suffix, rest) ->
        return movement(suffix, verb.startsWith("debited", ignoreCase = true), rest, amount, at)
    }
    OLDER.find(text)?.destructured?.let { (suffix, verb, amount, rest) ->
        return movement(suffix, verb.equals("debited", ignoreCase = true), rest, amount, at)
    }
    return null
}

// Where the description ends: the bank's footer, or a date line in newer messages
private val FOOTER = Regex("""\s*-?\s*(?:(?:For|To) Inq|For inquiries|\d{2}/\d{2}/\d{4})""", RegexOption.IGNORE_CASE)
private val CHANNEL_WORDS = Regex("""^(?:via POS at|for POS|via ATM at|via ATM|for eCom|for|on)\s+""", RegexOption.IGNORE_CASE)
private val TERMINAL_ID = Regex("""\s+\d{6,}$""")

/** "via POS at SHOP PVT 37000000 - For Inq Call ..." -> "SHOP PVT". */
private fun descriptionOf(rest: String): String =
    FOOTER.split(rest.trim(), 2)[0].replace(CHANNEL_WORDS, "").replace(TERMINAL_ID, "")
        .replace(Regex("""\s+"""), " ").trim(' ', '-')

private fun movement(suffix: String, debit: Boolean, rest: String, amount: String, at: Instant): AccountMovement? {
    val minor = MoneyParser.parseToMinor(amount) ?: return null
    val direction = if (debit) MovementDirection.OUT else MovementDirection.IN
    return AccountMovement(suffix, direction, channelOf(rest.trimStart()), minor, at, descriptionOf(rest))
}

private fun channelOf(rest: String): MovementChannel = when {
    rest.startsWith("via POS", ignoreCase = true) || rest.startsWith("for POS", ignoreCase = true) -> MovementChannel.CARD
    rest.startsWith("via ATM", ignoreCase = true) -> MovementChannel.ATM
    rest.startsWith("for eCom", ignoreCase = true) -> MovementChannel.ONLINE
    else -> MovementChannel.OTHER
}
