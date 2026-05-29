package com.notifyforward.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.notifyforward.app.ui.about.AboutScreen
import com.notifyforward.app.ui.history.HistoryScreen
import com.notifyforward.app.ui.home.HomeScreen
import com.notifyforward.app.ui.rules.RulesScreen
import com.notifyforward.app.ui.settings.SettingsScreen

sealed class NavRoute(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Home     : NavRoute("home",     "首页", Icons.Default.Home)
    object Rules    : NavRoute("rules",    "规则", Icons.AutoMirrored.Default.Rule)
    object History  : NavRoute("history",  "记录", Icons.Default.History)
    object Settings : NavRoute("settings", "设置", Icons.Default.Settings)
    object About    : NavRoute("about",    "关于", Icons.Default.Info)
}

@Composable
fun MainNavGraph() {
    val navController = rememberNavController()
    val tabs = listOf(
        NavRoute.Home,
        NavRoute.Rules,
        NavRoute.History,
        NavRoute.Settings,
        NavRoute.About
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val entry by navController.currentBackStackEntryAsState()
                val current = entry?.destination
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current?.hierarchy?.any { it.route == tab.route } == true,
                        onClick  = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon  = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = NavRoute.Home.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(NavRoute.Home.route)     { HomeScreen() }
            composable(NavRoute.Rules.route)    { RulesScreen() }
            composable(NavRoute.History.route)  { HistoryScreen() }
            composable(NavRoute.Settings.route) { SettingsScreen() }
            composable(NavRoute.About.route)    { AboutScreen() }
        }
    }
}
