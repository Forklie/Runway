package com.example.runway.data.remote.mapper

import com.example.runway.data.remote.dto.GoalDto
import com.example.runway.domain.model.DefaultAccountIds
import com.example.runway.domain.model.Goal
import com.google.firebase.firestore.DocumentSnapshot
import timber.log.Timber

/** Null without a target or a deadline: the runway cannot be drawn from a half-written goal. */
fun GoalDto.toDomainOrNull(id: String): Goal? {
    val deadline = deadline ?: return null
    if (targetMinor <= 0L) return null
    return Goal(
        id = id,
        name = name.ifBlank { "My goal" },
        targetMinor = targetMinor,
        startDate = startDate.toInstantOr(deadline.toInstantOr()),
        deadline = deadline.toInstantOr(),
        fundAccountId = fundAccountId.ifBlank { DefaultAccountIds.GOAL_FUND },
        active = active,
    )
}

fun Goal.toDto(): GoalDto = GoalDto(
    name = name,
    targetMinor = targetMinor,
    startDate = startDate.toTimestamp(),
    deadline = deadline.toTimestamp(),
    fundAccountId = fundAccountId,
    active = active,
)

fun DocumentSnapshot.toGoalOrNull(): Goal? =
    try {
        toObject(GoalDto::class.java)?.toDomainOrNull(id)
    } catch (e: RuntimeException) {
        Timber.w(e, "Skipping malformed goal %s", id)
        null
    }
