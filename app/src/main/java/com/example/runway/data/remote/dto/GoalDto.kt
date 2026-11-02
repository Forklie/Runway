package com.example.runway.data.remote.dto

import com.google.firebase.Timestamp

/** users/{uid}/goals/{id} (Doc 2 section 6.2). The rules require targetMinor to be an int above 0. */
data class GoalDto(
    var name: String = "",
    var targetMinor: Long = 0L,
    var startDate: Timestamp? = null,
    var deadline: Timestamp? = null,
    var fundAccountId: String = "",
    var active: Boolean = true,
)
