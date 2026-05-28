package net.luis.jenga.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object MainRoute

@Serializable
object TaskListRoute

@Serializable
data class TaskDetailRoute(val taskId: Long = -1L)

@Serializable
object GameSetupRoute

@Serializable
object GamePlayRoute

@Serializable
object DistributionListRoute

@Serializable
data class DistributionEditorRoute(val distributionId: Long = -1L)

@Serializable
object SettingsRoute
