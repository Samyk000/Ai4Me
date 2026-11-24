package ai.ai4me.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ai.ai4me.hardware.DeviceHardwareSpec
import ai.ai4me.hardware.HardwareRecommendation
import ai.ai4me.ui.screens.activity.ActivityScreen
import ai.ai4me.ui.screens.chat.ChatScreen
import ai.ai4me.ui.screens.home.HomeScreen
import ai.ai4me.ui.screens.models.ModelsScreen
import ai.ai4me.ui.screens.settings.SettingsScreen
import ai.ai4me.ui.theme.Ai4MeTheme

/**
 * Top-Level App Navigation Scaffold (Document 3 §3)
 * Hosts the 5 core destinations: Home, Chat, Models, Activity, Settings.
 */
@Composable
fun Ai4MeNavHost(
    hardwareSpec: DeviceHardwareSpec,
    recommendation: HardwareRecommendation,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavDestination.HOME.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = colors.surface,
                contentColor = colors.primary,
                tonalElevation = 2.dp,
                modifier = Modifier.heightIn(min = spacing.minTouchTarget)
            ) {
                NavDestination.entries.forEach { destination ->
                    val selected = currentRoute == destination.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != destination.route) {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.title
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                style = Ai4MeTheme.typography.caption
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = colors.primary,
                            selectedTextColor = colors.primary,
                            unselectedIconColor = colors.textMuted,
                            unselectedTextColor = colors.textMuted,
                            indicatorColor = colors.surfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavDestination.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavDestination.HOME.route) {
                HomeScreen(
                    hardwareSpec = hardwareSpec,
                    recommendation = recommendation,
                    onNavigateToChat = {
                        navController.navigate(NavDestination.CHAT.route)
                    },
                    onNavigateToModels = {
                        navController.navigate(NavDestination.MODELS.route)
                    }
                )
            }
            composable(NavDestination.CHAT.route) {
                ChatScreen()
            }
            composable(NavDestination.MODELS.route) {
                ModelsScreen()
            }
            composable(NavDestination.ACTIVITY.route) {
                ActivityScreen()
            }
            composable(NavDestination.SETTINGS.route) {
                SettingsScreen(
                    hardwareSpec = hardwareSpec,
                    recommendation = recommendation
                )
            }
        }
    }
}
