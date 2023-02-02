package com.zhenxiang.superimage.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.zhenxiang.superimage.home.HomePage
import com.zhenxiang.superimage.settings.SettingsPage

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RootNavigation(navController: NavHostController) = AnimatedNavHost(
    navController = navController,
    startDestination = RootNavigationRoutes.Home.route
) {
    noAnimationComposable(RootNavigationRoutes.Home.route) { HomePage(viewModel(), navController) }
    noAnimationComposable(RootNavigationRoutes.Settings.route) { SettingsPage(viewModel(), navController) }
}

enum class RootNavigationRoutes(val route: String) {
    Home("home"),
    Settings("settings")
}
