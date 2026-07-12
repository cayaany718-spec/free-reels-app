package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryCoral
import com.example.viewmodel.DramaViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val navController = rememberNavController()
        val viewModel: DramaViewModel = viewModel()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        
        // Listen to language changes dynamically for instant navbar updates
        val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
        
        // Hide bottom navigation bar for immersive views
        val showBottomBar = currentRoute in listOf("home", "reels", "library", "profile")

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          bottomBar = {
            if (showBottomBar) {
              NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("app_bottom_nav_bar")
              ) {
                // 1. Home Discover tab
                NavigationBarItem(
                  selected = currentRoute == "home",
                  onClick = {
                    if (currentRoute != "home") {
                      navController.navigate("home") {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                      }
                    }
                  },
                  icon = { Icon(imageVector = Icons.Default.Home, contentDescription = viewModel.getString("nav_home")) },
                  label = { Text(viewModel.getString("nav_home"), fontSize = 11.sp) },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryCoral,
                    selectedTextColor = PrimaryCoral,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                  ),
                  modifier = Modifier.testTag("nav_home_item")
                )

                // 2. Reels tab (Phim ngắn)
                NavigationBarItem(
                  selected = currentRoute == "reels",
                  onClick = {
                    if (currentRoute != "reels") {
                      navController.navigate("reels") {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                      }
                    }
                  },
                  icon = { Icon(imageVector = Icons.Default.PlayCircle, contentDescription = viewModel.getString("nav_reels")) },
                  label = { Text(viewModel.getString("nav_reels"), fontSize = 11.sp) },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryCoral,
                    selectedTextColor = PrimaryCoral,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                  ),
                  modifier = Modifier.testTag("nav_reels_item")
                )

                // 3. Library tab
                NavigationBarItem(
                  selected = currentRoute == "library",
                  onClick = {
                    if (currentRoute != "library") {
                      navController.navigate("library") {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                      }
                    }
                  },
                  icon = { Icon(imageVector = Icons.Default.VideoLibrary, contentDescription = viewModel.getString("nav_library")) },
                  label = { Text(viewModel.getString("nav_library"), fontSize = 11.sp) },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryCoral,
                    selectedTextColor = PrimaryCoral,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                  ),
                  modifier = Modifier.testTag("nav_library_item")
                )

                // 4. Profile tab
                NavigationBarItem(
                  selected = currentRoute == "profile",
                  onClick = {
                    if (currentRoute != "profile") {
                      navController.navigate("profile") {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                      }
                    }
                  },
                  icon = { Icon(imageVector = Icons.Default.Person, contentDescription = viewModel.getString("nav_profile")) },
                  label = { Text(viewModel.getString("nav_profile"), fontSize = 11.sp) },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryCoral,
                    selectedTextColor = PrimaryCoral,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                  ),
                  modifier = Modifier.testTag("nav_profile_item")
                )
              }
            }
          }
        ) { innerPadding ->
          AppNavigation(
            navController = navController,
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}
