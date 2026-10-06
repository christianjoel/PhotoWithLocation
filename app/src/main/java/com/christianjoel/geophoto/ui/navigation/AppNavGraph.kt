package com.christianjoel.geophoto.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.christianjoel.geophoto.ui.camera.CameraScreen
import com.christianjoel.geophoto.ui.permission.PermissionScreen
import com.christianjoel.geophoto.ui.photo.PhotoPreviewScreen
import com.christianjoel.geophoto.viewmodel.PhotoViewModel

@Composable
fun AppNavGraph(viewModel: PhotoViewModel) {
    val navController = rememberNavController()
    val hasPermissions by viewModel.hasPermissions

    val startDestination = if (hasPermissions) Route.Camera else Route.Permission

    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute == null || currentRoute == Route.Permission) {
                navController.navigate(Route.Camera) {
                    popUpTo(Route.Permission) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        } else {
            navController.navigate(Route.Permission) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable(Route.Permission) {
            PermissionScreen()
        }

        composable(
            route = Route.Camera,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            CameraScreen(
                viewModel = viewModel,
                onPreviewClick = {
                    viewModel.retryLocation()
                    navController.navigate(Route.Preview)
                },
                onError = { }
            )
        }

        composable(Route.Preview) {
            PhotoPreviewScreen(
                viewModel = viewModel,
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Route.Camera) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
