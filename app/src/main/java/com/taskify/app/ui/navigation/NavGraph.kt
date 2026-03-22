package com.taskify.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.taskify.app.ui.screens.addedittask.AddEditTaskScreen
import com.taskify.app.ui.screens.settings.SettingsScreen
import com.taskify.app.ui.screens.taskdetail.TaskDetailScreen
import com.taskify.app.ui.screens.tasklist.TaskListScreen
import com.taskify.app.ui.theme.ThemeViewModel

sealed class Screen(val route: String) {
    data object TaskList : Screen("task_list")
    data object AddTask : Screen("add_task?initialTitle={initialTitle}") {
        fun createRoute(initialTitle: String = "") =
            "add_task?initialTitle=${java.net.URLEncoder.encode(initialTitle, "UTF-8")}"
    }
    data object EditTask : Screen("edit_task/{taskId}") {
        fun createRoute(taskId: String) = "edit_task/$taskId"
    }
    data object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: String) = "task_detail/$taskId"
    }
    data object Settings : Screen("settings")
}

@Composable
fun TaskifyNavGraph(
    navController: NavHostController,
    // Activity-scoped so theme changes propagate immediately to MainActivity
    themeViewModel: ThemeViewModel
) {
    NavHost(navController = navController, startDestination = Screen.TaskList.route) {

        composable(Screen.TaskList.route) {
            TaskListScreen(
                onNavigateToDetail = { navController.navigate(Screen.TaskDetail.createRoute(it)) },
                onNavigateToAddTask = { navController.navigate(Screen.AddTask.createRoute(it)) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(
            route = Screen.AddTask.route,
            arguments = listOf(navArgument("initialTitle") {
                type = NavType.StringType; defaultValue = ""
            })
        ) { backStackEntry ->
            AddEditTaskScreen(
                taskId = null,
                initialTitle = backStackEntry.arguments?.getString("initialTitle") ?: "",
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditTask.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            AddEditTaskScreen(
                taskId = backStackEntry.arguments?.getString("taskId"),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            TaskDetailScreen(
                taskId = backStackEntry.arguments?.getString("taskId") ?: "",
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate(Screen.EditTask.createRoute(it)) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                themeViewModel = themeViewModel
            )
        }
    }
}
