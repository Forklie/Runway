package com.example.runway.data.remote.dto

import com.google.firebase.Timestamp

/** users/{uid}/freelanceProjects/{id} (Doc 2 section 6.2). */
data class FreelanceProjectDto(
    var clientName: String = "",
    var title: String = "",
    var totalMinor: Long = 0L,
    var createdAt: Timestamp? = null,
)

/** users/{uid}/milestones/{id} (Doc 2 section 6.2). */
data class MilestoneDto(
    var projectId: String = "",
    var label: String = "",
    var amountMinor: Long = 0L,
    var status: String = "",
    var invoiceDate: Timestamp? = null,
    var dueDate: Timestamp? = null,
    var paidDate: Timestamp? = null,
    var linkedTxId: String? = null,
)
