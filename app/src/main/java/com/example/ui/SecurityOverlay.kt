package com.example.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.AntiCheck
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryGold

@Composable
fun SecurityOverlay(
    isDeveloperMode: Boolean, // Usually BuildConfig.DEBUG
    onBypass: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Run security checks
    var isRooted by remember { mutableStateOf(false) }
    var isHooked by remember { mutableStateOf(false) }
    var isDebugged by remember { mutableStateOf(false) }
    var apkSignature by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isRooted = AntiCheck.isDeviceRooted()
        isHooked = AntiCheck.isHookFrameworkDetected()
        isDebugged = AntiCheck.isDebugged(context)
        apkSignature = AntiCheck.getSigningCertificateSha256(context)
    }

    val securityViolation = isRooted || isHooked || (isDebugged && !isDeveloperMode)

    if (securityViolation) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E0A0A),
                            Color(0xFF0F0505)
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Large glowing security shield icon
                Icon(
                    imageVector = Icons.Default.GppBad,
                    contentDescription = "Security Alert",
                    tint = PrimaryCoral,
                    modifier = Modifier.size(96.dp)
                )

                Text(
                    text = "HỆ THỐNG PHÁT HIỆN CAN THIỆP",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryCoral,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Để đảm bảo an toàn bản quyền và tránh hack/mod app, ứng dụng sẽ ngừng hoạt động khi phát hiện các mối đe dọa bảo mật sau:",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                // List of detected alerts
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Root alert
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Root",
                                tint = if (isRooted) PrimaryCoral else Color.Green,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Thiết bị đã ROOT / Bẻ khóa",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isRooted) "Cảnh báo: Đã phát hiện file nhị phân 'su'!" else "An toàn: Không phát hiện Root.",
                                    fontSize = 11.sp,
                                    color = if (isRooted) PrimaryCoral else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Hook alert
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Mod Tool",
                                tint = if (isHooked) PrimaryCoral else Color.Green,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Công cụ Hack/Mod (Frida, Xposed)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isHooked) "Cảnh báo: Bộ nhớ chứa thư viện can thiệp mã nguồn!" else "An toàn: Không phát hiện hook tool.",
                                    fontSize = 11.sp,
                                    color = if (isHooked) PrimaryCoral else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Debug/Signature alert
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Signature",
                                tint = if (isDebugged && !isDeveloperMode) PrimaryCoral else PrimaryGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Kiểm tra Chữ ký & Debug",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Chữ ký SHA-256 APK hiện tại: ${apkSignature.take(16)}...",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isDeveloperMode) {
                    // Developer bypass section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PrimaryGold.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "MÔI TRƯỜNG THỬ NGHIỆM (DEBUG)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGold
                            )
                            Text(
                                text = "Bạn đang chạy app ở chế độ Debug trên emulator/AI Studio. Bạn có thể bỏ qua để tiếp tục kiểm tra giao diện.",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                            
                            Button(
                                onClick = onBypass,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Bỏ qua cảnh báo (Chỉ có ở Debug)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Exit App Button (Always visible)
                Button(
                    onClick = { activity?.finishAffinity() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Thoát")
                        Text("Thoát Ứng Dụng", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
