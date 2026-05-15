package com.linuxquest.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.linuxquest.ui.screens.*

object Routes {
    const val HOME = "home"
    const val LEVEL_SELECT = "level_select"
    const val GAME = "game/{levelId}"
    const val LEVEL_COMPLETE = "level_complete/{levelId}/{password}"
    const val ACHIEVEMENTS = "achievements"
    const val MANUAL = "manual"
    const val SETTINGS = "settings"

    fun game(levelId: Int) = "game/$levelId"
    fun levelComplete(levelId: Int, password: String) = "level_complete/$levelId/$password"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartGame = { levelId -> navController.navigate(Routes.game(levelId)) },
                onSelectLevel = { navController.navigate(Routes.LEVEL_SELECT) },
                onOpenManual = { navController.navigate(Routes.MANUAL) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenAchievements = { navController.navigate(Routes.ACHIEVEMENTS) }
            )
        }

        composable(Routes.LEVEL_SELECT) {
            LevelSelectScreen(
                onLevelSelected = { levelId -> navController.navigate(Routes.game(levelId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.GAME,
            arguments = listOf(navArgument("levelId") { type = NavType.IntType })
        ) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getInt("levelId") ?: 0
            GameScreen(
                levelId = levelId,
                onLevelComplete = { password ->
                    navController.navigate(Routes.levelComplete(levelId, password)) {
                        popUpTo(Routes.LEVEL_SELECT)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.LEVEL_COMPLETE,
            arguments = listOf(
                navArgument("levelId") { type = NavType.IntType },
                navArgument("password") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getInt("levelId") ?: 0
            val password = backStackEntry.arguments?.getString("password") ?: ""
            LevelCompleteScreen(
                levelId = levelId,
                password = password,
                onNextLevel = {
                    navController.navigate(Routes.game(levelId + 1)) {
                        popUpTo(Routes.LEVEL_SELECT)
                    }
                },
                onBackToLevels = {
                    navController.navigate(Routes.LEVEL_SELECT) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        composable(Routes.ACHIEVEMENTS) {
            AchievementsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.MANUAL) {
            ManualScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
