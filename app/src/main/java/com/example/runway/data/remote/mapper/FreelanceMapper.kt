package com.example.runway.data.remote.mapper

import com.example.runway.data.remote.dto.FreelanceProjectDto
import com.example.runway.data.remote.dto.MilestoneDto
import com.example.runway.domain.model.FreelanceProject
import com.example.runway.domain.model.Milestone
import com.example.runway.domain.model.MilestoneStatus
import com.google.firebase.firestore.DocumentSnapshot
import timber.log.Timber

fun FreelanceProjectDto.toDomain(id: String) = FreelanceProject(
    id = id,
    clientName = clientName,
    title = title,
    totalMinor = totalMinor,
    createdAt = createdAt.toInstantOr(),
    archived = archived,
)

fun FreelanceProject.toDto() = FreelanceProjectDto(
    clientName = clientName,
    title = title,
    totalMinor = totalMinor,
    createdAt = createdAt.toTimestamp(),
    archived = archived,
)

/** An unknown status reads as PLANNED: only PAID ever counts as income (BR-54). */
fun MilestoneDto.toDomain(id: String) = Milestone(
    id = id,
    projectId = projectId,
    label = label,
    amountMinor = amountMinor,
    status = status.toEnumOr(MilestoneStatus.PLANNED),
    invoiceDate = invoiceDate?.toInstantOr(),
    dueDate = dueDate?.toInstantOr(),
    paidDate = paidDate?.toInstantOr(),
    linkedTxId = linkedTxId,
)

fun Milestone.toDto() = MilestoneDto(
    projectId = projectId,
    label = label,
    amountMinor = amountMinor,
    status = status.name,
    invoiceDate = invoiceDate?.toTimestamp(),
    dueDate = dueDate?.toTimestamp(),
    paidDate = paidDate?.toTimestamp(),
    linkedTxId = linkedTxId,
)

fun DocumentSnapshot.toFreelanceProjectOrNull(): FreelanceProject? =
    try {
        toObject(FreelanceProjectDto::class.java)?.toDomain(id)
    } catch (e: RuntimeException) {
        Timber.w(e, "Skipping malformed project %s", id)
        null
    }

fun DocumentSnapshot.toMilestoneOrNull(): Milestone? =
    try {
        toObject(MilestoneDto::class.java)?.toDomain(id)
    } catch (e: RuntimeException) {
        Timber.w(e, "Skipping malformed milestone %s", id)
        null
    }
