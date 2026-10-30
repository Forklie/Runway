package com.example.runway.domain.repository

import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.FreelanceProject
import com.example.runway.domain.model.Milestone
import com.example.runway.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

/** Writes return after the LOCAL write, never after server acknowledgement. */
interface FreelanceRepository {
    fun observeProjects(): Flow<List<FreelanceProject>> // non-archived, newest first
    fun observeMilestones(): Flow<List<Milestone>>      // every project's

    suspend fun saveProject(project: FreelanceProject): AppResult<Unit>
    suspend fun saveMilestone(milestone: Milestone): AppResult<Unit>

    /** The PAID milestone and its INCOME transaction, in one atomic batch (BR-52, Doc 2 section 5.8). */
    suspend fun markPaid(milestone: Milestone, income: Transaction): AppResult<Unit>
}
