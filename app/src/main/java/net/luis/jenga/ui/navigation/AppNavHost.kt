package net.luis.jenga.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import net.luis.jenga.JengaApp
import net.luis.jenga.ui.distribution.DistributionEditorScreen
import net.luis.jenga.ui.distribution.DistributionListScreen
import net.luis.jenga.ui.game.GamePlayScreen
import net.luis.jenga.ui.game.GameSetupScreen
import net.luis.jenga.ui.game.GameViewModel
import net.luis.jenga.ui.main.MainScreen
import net.luis.jenga.ui.settings.SettingsScreen
import net.luis.jenga.ui.tasks.TaskDetailScreen
import net.luis.jenga.ui.tasks.TaskListScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    app: JengaApp,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = MainRoute,
        modifier = modifier
    ) {
        composable<MainRoute> {
            MainScreen(
                onNavigateTasks = { navController.navigate(TaskListRoute) },
                onNavigateDistributions = { navController.navigate(DistributionListRoute) },
                onNavigatePlay = { navController.navigate(GameSetupRoute) },
                onNavigateSettings = { navController.navigate(SettingsRoute) }
            )
        }

        composable<TaskListRoute> {
            TaskListScreen(
                app = app,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTask = { taskId ->
                    navController.navigate(TaskDetailRoute(taskId))
                },
                onAddTask = { navController.navigate(TaskDetailRoute()) }
            )
        }

        composable<TaskDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TaskDetailRoute>()
            TaskDetailScreen(
                app = app,
                taskId = route.taskId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<GameSetupRoute> { backStackEntry ->
            val gameViewModel: GameViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = GameViewModel.Factory(app)
            )
            GameSetupScreen(
                viewModel = gameViewModel,
                onNavigateBack = { navController.popBackStack() },
                onStartGame = { navController.navigate(GamePlayRoute) }
            )
        }

        composable<GamePlayRoute> {
            val gameSetupEntry = remember(navController) {
                navController.getBackStackEntry<GameSetupRoute>()
            }
            val gameViewModel: GameViewModel = viewModel(
                viewModelStoreOwner = gameSetupEntry,
                factory = GameViewModel.Factory(app)
            )
            GamePlayScreen(
                viewModel = gameViewModel,
                onEndGame = { navController.popBackStack(MainRoute, inclusive = false) }
            )
        }

        composable<DistributionListRoute> {
            DistributionListScreen(
                app = app,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDistribution = { id ->
                    navController.navigate(DistributionEditorRoute(id))
                },
                onAddDistribution = { navController.navigate(DistributionEditorRoute()) }
            )
        }

        composable<DistributionEditorRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<DistributionEditorRoute>()
            DistributionEditorScreen(
                app = app,
                distributionId = route.distributionId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                app = app,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
