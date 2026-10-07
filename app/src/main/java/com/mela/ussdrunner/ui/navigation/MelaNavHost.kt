package com.mela.ussdrunner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mela.ussdrunner.ui.screens.editor.EditorScreen
import com.mela.ussdrunner.ui.screens.execution.ExecutionScreen
import com.mela.ussdrunner.ui.screens.help.HelpScreen
import com.mela.ussdrunner.ui.screens.home.HomeScreen
import com.mela.ussdrunner.ui.screens.settings.SettingsScreen

@Composable
fun MelaNavHost(startDestination: String = "home") {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = startDestination) {
        composable("home") {
            HomeScreen(
                onAdd = { nav.navigate("edit") },
                onEdit = { id -> nav.navigate("edit?id=$id") },
                onRun = { id -> nav.navigate("run/$id") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable(
            route = "edit?id={id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            EditorScreen(
                presetId = entry.arguments?.getString("id"),
                onDone = { nav.popBackStack() },
                onRun = { id ->
                    nav.popBackStack()
                    nav.navigate("run/$id")
                },
            )
        }
        composable(
            route = "run/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            ExecutionScreen(
                presetId = entry.arguments?.getString("id").orEmpty(),
                onClose = {
                    if (!nav.popBackStack()) {
                        nav.navigate("home") { launchSingleTop = true }
                    }
                },
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onHelp = { nav.navigate("help/help") },
                onAbout = { nav.navigate("help/about") },
                onPrivacy = { nav.navigate("help/privacy") },
            )
        }
        composable(
            route = "help/{page}",
            arguments = listOf(navArgument("page") { type = NavType.StringType }),
        ) { entry ->
            HelpScreen(
                page = entry.arguments?.getString("page") ?: "help",
                onBack = { nav.popBackStack() },
            )
        }
    }
}
