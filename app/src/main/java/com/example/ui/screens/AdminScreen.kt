package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.UserProfile
import com.example.ui.theme.*
import com.example.viewmodel.DramaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminConsoleDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    viewModel: DramaViewModel
) {
    if (!showDialog) return

    val context = LocalContext.current
    val balance by viewModel.userBalance.collectAsStateWithLifecycle(initialValue = null)
    val unlockedEpisodes by viewModel.unlockedEpisodes.collectAsStateWithLifecycle(initialValue = emptyList())
    val favoritesList by viewModel.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    val watchHistoryList by viewModel.watchHistory.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentUserProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()

    // Temp profile edit state
    var editNickname by remember { mutableStateOf(currentUserProfile?.nickname ?: "") }
    var editPhone by remember { mutableStateOf(currentUserProfile?.phoneNumber ?: "") }
    var editIsVip by remember { mutableStateOf(currentUserProfile?.isVip ?: true) }
    var editVipLevel by remember { mutableStateOf(currentUserProfile?.vipLevel ?: "THÀNH VIÊN VIP PREMIUM") }
    var editEmoji by remember { mutableStateOf(currentUserProfile?.avatarEmoji ?: "🦊") }

    // Direct input coin state
    var inputCoinsStr by remember { mutableStateOf("") }
    var inputSpinsStr by remember { mutableStateOf("") }

    // Sync temp state with actual profile when active profile changes
    LaunchedEffect(currentUserProfile) {
        currentUserProfile?.let {
            editNickname = it.nickname
            editPhone = it.phoneNumber
            editIsVip = it.isVip
            editVipLevel = it.vipLevel
            editEmoji = it.avatarEmoji
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // Full-screen style dialogue
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0E13)),
            color = Color(0xFF0F0E13)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Header Bar with terminal aesthetic
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF16151B))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.0.dp)
                                .clip(CircleShape)
                                .background(Color.Green)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ROOT@SHORTFLIX:~/ADMIN_CONSOLE",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                Divider(color = BorderColor)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(10.dp)) }

                    // STATS OVERVIEW CARD
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1920)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "📊 TRẠNG THÁI CƠ SỞ DỮ LIỆU THẬT (ROOM SQLITE)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Database Name: ", fontSize = 11.sp, color = GrayText)
                                        Text("shortflix_database", fontSize = 12.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("SQLite Version: ", fontSize = 11.sp, color = GrayText)
                                        Text("v3.0 - Room 2.6+", fontSize = 12.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = BorderColor.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Table statistics
                                Text(
                                    text = "Số lượng dòng trong các Bảng cơ sở dữ liệu:",
                                    fontSize = 11.sp,
                                    color = GrayText
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatItem("Bảng xu/quay\n(user_balance)", "1 dòng")
                                    StatItem("Mở khóa phim\n(unlocked)", "${unlockedEpisodes.size} dòng")
                                    StatItem("Yêu thích\n(favorites)", "${favoritesList.size} dòng")
                                    StatItem("Lịch sử\n(watch_history)", "${watchHistoryList.size} dòng")
                                }
                            }
                        }
                    }

                    // WALLET & BALANCE MANAGER
                    item {
                        AdminSectionHeader(title = "⚙️ QUẢN LÝ SỐ DƯ VÍ (USER_BALANCE TABLE)")
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurface)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Số Xu hiện tại:", fontSize = 11.sp, color = GrayText)
                                    Text("🪙 ${balance?.coins ?: 0} Xu", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Số Lượt quay hiện tại:", fontSize = 11.sp, color = GrayText)
                                    Text("🎯 ${balance?.spins ?: 0} Lượt", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryCoral)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = BorderColor)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Interactive Adjustments for Coins
                            Text("Điều chỉnh Xu nhanh:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AdjustButton(text = "+50 Xu", color = PrimaryGold) { viewModel.adminSetCoins((balance?.coins ?: 0) + 50) }
                                AdjustButton(text = "+200 Xu", color = PrimaryGold) { viewModel.adminSetCoins((balance?.coins ?: 0) + 200) }
                                AdjustButton(text = "+1000 Xu", color = PrimaryGold) { viewModel.adminSetCoins((balance?.coins ?: 0) + 1000) }
                                AdjustButton(text = "Đặt về 50", color = Color.Gray) { viewModel.adminSetCoins(50) }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Interactive Adjustments for Spins
                            Text("Điều chỉnh Lượt quay nhanh:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AdjustButton(text = "+1 Lượt", color = PrimaryCoral) { viewModel.adminSetSpins((balance?.spins ?: 0) + 1) }
                                AdjustButton(text = "+5 Lượt", color = PrimaryCoral) { viewModel.adminSetSpins((balance?.spins ?: 0) + 5) }
                                AdjustButton(text = "+10 Lượt", color = PrimaryCoral) { viewModel.adminSetSpins((balance?.spins ?: 0) + 10) }
                                AdjustButton(text = "Đặt về 3", color = Color.Gray) { viewModel.adminSetSpins(3) }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // Direct Inputs
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = inputCoinsStr,
                                    onValueChange = { inputCoinsStr = it },
                                    label = { Text("Nhập số Xu", fontSize = 10.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val value = inputCoinsStr.toIntOrNull()
                                            if (value != null && value >= 0) {
                                                viewModel.adminSetCoins(value)
                                                Toast.makeText(context, "Đã đặt số xu thành $value", Toast.LENGTH_SHORT).show()
                                                inputCoinsStr = ""
                                            } else {
                                                Toast.makeText(context, "Vui lòng nhập số hợp lệ!", Toast.LENGTH_SHORT).show()
                                            }
                                        }) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = "Áp dụng xu")
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = PrimaryGold
                                    )
                                )

                                OutlinedTextField(
                                    value = inputSpinsStr,
                                    onValueChange = { inputSpinsStr = it },
                                    label = { Text("Nhập số Lượt", fontSize = 10.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val value = inputSpinsStr.toIntOrNull()
                                            if (value != null && value >= 0) {
                                                viewModel.adminSetSpins(value)
                                                Toast.makeText(context, "Đã đặt lượt quay thành $value", Toast.LENGTH_SHORT).show()
                                                inputSpinsStr = ""
                                            } else {
                                                Toast.makeText(context, "Vui lòng nhập số hợp lệ!", Toast.LENGTH_SHORT).show()
                                            }
                                        }) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = "Áp dụng quay")
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = PrimaryCoral
                                    )
                                )
                            }
                        }
                    }

                    // UNLOCKED CONTENT MANAGER
                    item {
                        AdminSectionHeader(title = "🔑 QUẢN LÝ MỞ KHÓA TẬP PHIM (UNLOCKED_EPISODES TABLE)")
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurface)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Số lượng tập phim đang mở khóa: ${unlockedEpisodes.size}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Mặc định tập 1-3 của tất cả các phim là miễn phí. Các tập sau yêu cầu 10 xu để mở khóa hoặc có thể mở khóa hàng loạt tại đây.",
                                color = GrayText,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.adminUnlockAllEpisodes()
                                        Toast.makeText(context, "⚡ Đã mở khóa TOÀN BỘ tập phim thành công!", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("🔑 Mở khóa hết phim", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.adminResetAllUnlocks()
                                        Toast.makeText(context, "🔒 Đã khóa lại toàn bộ tập phim (Chỉ giữ miễn phí tập 1-3)", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("🔒 Khóa lại toàn bộ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    // USER PROFILE EDITOR (IN-MEMORY OR PERSISTED)
                    item {
                        AdminSectionHeader(title = "👤 CHỈNH SỬA THÔNG TIN NGƯỜI DÙNG (USER PROFILE)")
                        if (currentUserProfile == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E1C24))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Chưa có tài khoản đăng nhập. Vui lòng đăng nhập trước ở màn hình Hồ sơ!", color = PrimaryCoral, fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurface)
                                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Cấu hình tài khoản ID: ${currentUserProfile?.id}",
                                    color = PrimaryGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )

                                OutlinedTextField(
                                    value = editNickname,
                                    onValueChange = { editNickname = it },
                                    label = { Text("Biệt danh (Nickname)") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = editPhone,
                                    onValueChange = { editPhone = it },
                                    label = { Text("Số điện thoại") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = editVipLevel,
                                    onValueChange = { editVipLevel = it },
                                    label = { Text("Danh hiệu / Danh xưng VIP") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // VIP Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Kích hoạt trạng thái VIP", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("Cấp đặc quyền xem không quảng cáo", color = GrayText, fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = editIsVip,
                                        onCheckedChange = { editIsVip = it },
                                        colors = SwitchDefaults.colors(checkedTrackColor = PrimaryCoral)
                                    )
                                }

                                // Mascot Picker
                                Text("Linh vật Mascot đại diện:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                val mascots = listOf("🦊", "🦁", "🐼", "🐰", "🐱", "🐨", "🐸", "🐷")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(mascots) { mascot ->
                                        val isSelected = editEmoji == mascot
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) PrimaryCoral.copy(alpha = 0.25f) else SurfaceVariant)
                                                .border(
                                                    width = 1.5.dp,
                                                    color = if (isSelected) PrimaryCoral else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable { editEmoji = mascot },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(mascot, fontSize = 18.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = {
                                        val updatedProfile = UserProfile(
                                            id = currentUserProfile?.id ?: "FR_99999",
                                            nickname = editNickname,
                                            avatarEmoji = editEmoji,
                                            phoneNumber = editPhone,
                                            vipLevel = editVipLevel,
                                            isVip = editIsVip
                                        )
                                        viewModel.adminUpdateUserProfile(updatedProfile)
                                        Toast.makeText(context, "✅ Đã lưu cấu hình tài khoản thành công!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("💾 Cập nhật Hồ sơ Người dùng", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // WATCH HISTORY & FAVORITES CLEAR
                    item {
                        AdminSectionHeader(title = "🧹 DỌN DẸP LỊCH SỬ & YÊU THÍCH")
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurface)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Lịch sử xem phim đang có: ${watchHistoryList.size} dòng dữ liệu\nDanh sách yêu thích đang có: ${favoritesList.size} dòng dữ liệu",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Button(
                                onClick = {
                                    viewModel.clearAllHistory()
                                    Toast.makeText(context, "🧹 Đã xóa sạch Lịch sử xem phim trong SQLite!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("🧹 Xóa sạch Lịch sử xem phim", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }

                // Bottom Footer with version info
                Divider(color = BorderColor)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF16151B))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SHORTFLIX REALTIME ROOM-DATABASE INSPECTOR PRO V1.0.0",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AdminSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.5f),
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryGold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 8.sp,
            color = GrayText,
            textAlign = TextAlign.Center,
            lineHeight = 10.sp
        )
    }
}

@Composable
fun AdjustButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
