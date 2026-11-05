package com.example.runway.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Stored status (BR-50). OVERDUE is not here on purpose: it is derived, see [Milestone.isOverdue]. */
enum class MilestoneStatus { PLANNED, INVOICED, PAID }

/** users/{uid}/freelanceProjects/{id}. */
data class FreelanceProject(
    val id: String,
    val clientName: String,
    val title: String,
    val totalMinor: Long,
    val createdAt: Instant,
)

/** users/{uid}/milestones/{id}. A flat collection with [projectId], so overdue items need one query (Doc 2 section 6.1). */
data class Milestone(
    val id: String,
    val projectId: String,
    val label: String,
    val amountMinor: Long,
    val status: MilestoneStatus,
    val invoiceDate: Instant?,
    val dueDate: Instant?,
    val paidDate: Instant?,
    val linkedTxId: String?, // the INCOME transaction that paid it (BR-52)
) {
    /** BR-51: invoiced, and due before today. Never stored. */
    fun isOverdue(today: LocalDate, zone: ZoneId): Boolean =
        status == MilestoneStatus.INVOICED && dueDate != null && dueDate.atZone(zone).toLocalDate() < today
}
