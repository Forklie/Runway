package com.example.runway.ui.feature.freelance

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object FreelanceDest
@Serializable data class ProjectDest(val projectId: String)

/** Freelance projects and one project's milestones (F-14). AppNavHost calls this in one line. */
fun NavGraphBuilder.freelanceScreens(nav: NavController) {
    composable<FreelanceDest> {
        FreelanceRoute(onBack = { nav.popBackStack() }, onOpen = { nav.openProject(it) })
    }
    composable<ProjectDest> {
        ProjectRoute(onBack = { nav.popBackStack() })
    }
}

fun NavController.openProject(id: String) {
    navigate(ProjectDest(id)) { launchSingleTop = true }
}
