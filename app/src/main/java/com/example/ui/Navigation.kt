package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.viewmodel.DramaViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: DramaViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier
    ) {
        // Splash Screen
        composable("splash") {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // Discover Screen
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToDetail = { dramaId ->
                    navController.navigate("detail/$dramaId")
                },
                onNavigateToSearch = {
                    navController.navigate("search")
                },
                onNavigateToLibrary = {
                    navController.navigate("library")
                }
            )
        }

        // Search Screen
        composable("search") {
            SearchScreen(
                viewModel = viewModel,
                onNavigateToDetail = { dramaId ->
                    navController.navigate("detail/$dramaId")
                }
            )
        }

        // Library (History, Favs, Coins) Screen
        composable("library") {
            LibraryScreen(
                viewModel = viewModel,
                onNavigateToDetail = { dramaId ->
                    navController.navigate("detail/$dramaId")
                }
            )
        }

        // Detailed Drama profile
        composable(
            route = "detail/{dramaId}",
            arguments = listOf(navArgument("dramaId") { type = NavType.IntType })
        ) { backStackEntry ->
            val dramaId = backStackEntry.arguments?.getInt("dramaId") ?: 0
            DetailScreen(
                dramaId = dramaId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onPlayEpisode = { drama, episode ->
                    viewModel.selectEpisode(drama, episode)
                    navController.navigate("player")
                }
            )
        }

        // Immersive Short Video Player
        composable("player") {
            PlayerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Immersive Short Video Reels tab
        composable("reels") {
            PlayerScreen(
                viewModel = viewModel,
                onNavigateBack = { 
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        // Profile Screen
        composable("profile") {
            ProfileScreen(
                viewModel = viewModel,
                onNavigateToDetail = { dramaId ->
                    navController.navigate("detail/$dramaId")
                }
            )
        }
    }
}
