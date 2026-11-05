package com.example.runway.data.remote.mapper

import com.example.runway.data.remote.dto.AccountDto
import com.example.runway.domain.model.Account
import com.example.runway.domain.model.AccountKind
import com.google.firebase.firestore.DocumentSnapshot
import timber.log.Timber

fun AccountDto.toDomain(id: String): Account = Account(
    id = id,
    name = name.ifBlank { "Account" },
    kind = kind.toEnumOr(AccountKind.BANK),
    openingBalanceMinor = openingBalanceMinor,
    checkpointBalanceMinor = checkpointBalanceMinor,
    checkpointDate = checkpointDate.toInstantOr(),
    archived = archived,
    createdAt = createdAt.toInstantOr(),
)

fun Account.toDto(): AccountDto = AccountDto(
    name = name,
    kind = kind.name,
    openingBalanceMinor = openingBalanceMinor,
    checkpointBalanceMinor = checkpointBalanceMinor,
    checkpointDate = checkpointDate.toTimestamp(),
    archived = archived,
    createdAt = createdAt.toTimestamp(),
)

/** One malformed document returns null (and is logged) instead of crashing the whole list. */
fun DocumentSnapshot.toAccountOrNull(): Account? =
    try {
        toObject(AccountDto::class.java)?.toDomain(id)
    } catch (e: RuntimeException) {
        Timber.w(e, "Skipping malformed account %s", id)
        null
    }
