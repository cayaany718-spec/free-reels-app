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

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.theme.PrimaryGold
import com.example.ui.theme.PrimaryCoral

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
          
          // Global App Update Checker Dialog
          AppUpdateDialog(viewModel = viewModel)
        }
      }
    }
  }
}

@Composable
fun AppUpdateDialog(
    viewModel: DramaViewModel
) {
    val context = LocalContext.current
    val updateAvailable by viewModel.updateAvailable.collectAsStateWithLifecycle()
    val updateVersionName by viewModel.updateVersionName.collectAsStateWithLifecycle()
    val updateReleaseNotes by viewModel.updateReleaseNotes.collectAsStateWithLifecycle()
    val updateDownloadUrl by viewModel.updateDownloadUrl.collectAsStateWithLifecycle()
    val isForceUpdate by viewModel.isForceUpdate.collectAsStateWithLifecycle()
    val isDownloading by viewModel.isDownloading.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()

    if (updateAvailable) {
        AlertDialog(
            onDismissRequest = {
                if (!isForceUpdate && !isDownloading) {
                    viewModel.dismissUpdateDialog()
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Cập nhật",
                        tint = PrimaryCoral,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = if (isDownloading) "Đang tải cập nhật..." else "Phát hiện cập nhật mới! 🚀",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isDownloading) {
                        Text(
                            text = "Vui lòng không đóng ứng dụng trong khi đang tải.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LinearProgressIndicator(
                            progress = downloadProgress,
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = PrimaryCoral,
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Đang tải xuống...",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "${(downloadProgress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGold
                            )
                        }
                    } else {
                        Text(
                            text = "Phiên bản mới nhất: v$updateVersionName",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryGold
                        )
                        
                        Text(
                            text = "Phiên bản hiện tại: v${viewModel.appVersionName}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Những thay đổi mới:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = updateReleaseNotes,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        if (isForceUpdate) {
                            Text(
                                text = "(*) Đây là bản cập nhật bắt buộc để tiếp tục sử dụng ứng dụng ổn định nhất.",
                                fontSize = 11.sp,
                                color = PrimaryCoral,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (!isDownloading) {
                    Button(
                        onClick = {
                            if (updateDownloadUrl.endsWith(".apk", ignoreCase = true)) {
                                viewModel.downloadAndInstallApk(context, updateDownloadUrl)
                            } else {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateDownloadUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Không thể mở liên kết tải xuống!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cập nhật ngay", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            dismissButton = {
                if (!isForceUpdate && !isDownloading) {
                    TextButton(
                        onClick = { viewModel.dismissUpdateDialog() }
                    ) {
                        Text("Để sau", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            },
            containerColor = Color(0xFF1E1D24),
            shape = RoundedCornerShape(16.dp)
        )
    }
}
