package com.example.runway.ui.feature.goal

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.runway.R
import com.example.runway.core.money.formatLkr
import com.example.runway.domain.usecase.GoalProjection
import com.example.runway.domain.usecase.GoalStatus
import com.example.runway.ui.components.monthYearLabel

/** The projection sentence and the phrase that carries the coral squiggle (null when nothing moved). */
data class ProjectionLine(val text: String, val squiggle: String?)

/**
 * One sentence under the runway on Home and Goal (BR-74). Only a landing date later than the plan gets
 * the squiggle; the copy stays calm either way (Doc 1 section 3.1).
 */
@Composable
fun projectionLine(projection: GoalProjection): ProjectionLine {
    val required = projection.requiredPerMonthMinor.formatLkr()
    val deadline = projection.deadline.monthYearLabel()
    val landing = projection.projectedDate
    return when {
        projection.status == GoalStatus.REACHED -> ProjectionLine(stringResource(R.string.goal_line_reached), null)
        projection.paceMinorPerMonth == null ->
            ProjectionLine(stringResource(R.string.goal_line_not_enough_data, required, deadline), null)
        landing == null -> ProjectionLine(stringResource(R.string.goal_line_no_landing, required, deadline), null)
        else -> {
            val months = projection.monthsAfterPlan ?: 0
            val phrase = when {
                months == 1 -> stringResource(R.string.goal_after_plan_one)
                months > 1 -> stringResource(R.string.goal_after_plan_many, months)
                months == -1 -> stringResource(R.string.goal_before_plan_one)
                months < -1 -> stringResource(R.string.goal_before_plan_many, -months)
                else -> stringResource(R.string.goal_on_plan)
            }
            ProjectionLine(
                text = stringResource(R.string.goal_line_landing, landing.monthYearLabel(), phrase),
                squiggle = phrase.takeIf { months > 0 },
            )
        }
    }
}
