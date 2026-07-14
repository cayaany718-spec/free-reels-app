package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.WatchHistoryItem
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryGold
import com.example.viewmodel.DramaViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: DramaViewModel,
    onNavigateToDetail: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val favoritesList by viewModel.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    val historyList by viewModel.watchHistory.collectAsStateWithLifecycle(initialValue = emptyList())
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    
    // Dynamically localized tabs beautifully
    val tab0 = "${viewModel.getString("nav_library")} 💖"
    val tab1 = when(appLanguage) {
        com.example.data.AppLanguage.VIETNAMESE -> "Lịch sử 🕒"
        com.example.data.AppLanguage.ENGLISH -> "History 🕒"
        com.example.data.AppLanguage.CHINESE -> "历史 🕒"
        com.example.data.AppLanguage.SPANISH -> "Historial 🕒"
        com.example.data.AppLanguage.FRENCH -> "Historique 🕒"
        com.example.data.AppLanguage.JAPANESE -> "履歴 🕒"
        com.example.data.AppLanguage.KOREAN -> "기록 🕒"
        com.example.data.AppLanguage.RUSSIAN -> "История 🕒"
        com.example.data.AppLanguage.GERMAN -> "Verlauf 🕒"
        com.example.data.AppLanguage.PORTUGUESE -> "Histórico 🕒"
        com.example.data.AppLanguage.ITALIAN -> "Cronologia 🕒"
        com.example.data.AppLanguage.TAGALOG -> "Kasaysayan 🕒"
        com.example.data.AppLanguage.INDONESIAN -> "Riwayat 🕒"
        com.example.data.AppLanguage.THAI -> "ประวัติ 🕒"
    }
    val tab2 = "Gói VIP 👑"
    val tabs = listOf(tab0, tab1, tab2)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Screen Title Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = viewModel.getString("nav_library"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // VIP Badge pill
                val userProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (userProfile?.isVip == true) Color(0xFF2C1E22) else MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 0.5.dp,
                            color = if (userProfile?.isVip == true) PrimaryGold else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (userProfile?.isVip == true) "VIP Premium 👑" else "Thường 👤",
                        color = if (userProfile?.isVip == true) PrimaryGold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 2. Tabs Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = PrimaryCoral,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryCoral
                    )
                },
                divider = { Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) PrimaryCoral else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier.testTag("library_tab_$index")
                    )
                }
            }

            // 3. Tab Contents
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> FavoritesTabContent(favoritesList, viewModel, onNavigateToDetail)
                    1 -> HistoryTabContent(historyList, viewModel, onNavigateToDetail, onClearItem = { viewModel.deleteHistoryItem(it) }, onClearAll = { viewModel.clearAllHistory() })
                    2 -> VipTabContent(viewModel = viewModel)
                }
            }
        }
    }
}

// ======================== TABS CONTENT DEFINITIONS ========================

// 1. FAVORITES TAB
@Composable
fun FavoritesTabContent(
    favorites: List<com.example.data.Drama>,
    viewModel: DramaViewModel,
    onNavigateToDetail: (Int) -> Unit
) {
    if (favorites.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = viewModel.getString("no_favorites_yet"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = viewModel.getString("no_favorites_desc"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        val chunks = favorites.chunked(2)
        items(chunks) { chunk ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (drama in chunk) {
                    Box(modifier = Modifier.weight(1f)) {
                        DramaGridCard(
                            drama = drama,
                            viewModel = viewModel,
                            onClick = { onNavigateToDetail(drama.id) }
                        )
                    }
                }
                if (chunk.size < 2) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// 2. WATCH HISTORY TAB
@Composable
fun HistoryTabContent(
    historyList: List<WatchHistoryItem>,
    viewModel: DramaViewModel,
    onNavigateToDetail: (Int) -> Unit,
    onClearItem: (Int) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current

    if (historyList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = viewModel.getString("history_empty"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = viewModel.getString("history_empty_desc"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Clear all history header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = viewModel.getString("history_saved"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Text(
                    text = viewModel.getString("clear_all"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryCoral,
                    modifier = Modifier
                        .clickable {
                            onClearAll()
                            Toast.makeText(context, viewModel.getString("history_cleared"), Toast.LENGTH_SHORT).show()
                        }
                        .testTag("clear_all_history_button")
                )
            }
        }

        // List watch cards
        items(historyList) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onNavigateToDetail(item.drama.id) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Small Cover image
                AsyncImage(
                    model = item.drama.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp, 72.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Detail column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.drama.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = viewModel.getString("watched_ep_label", item.episodeNumber, item.episodeTitle),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress Slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = item.progressPercentage.toFloat() / 100f,
                            color = PrimaryCoral,
                            trackColor = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = "${item.progressPercentage}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Remove item button
                IconButton(
                    onClick = { onClearItem(item.drama.id) },
                    modifier = Modifier.testTag("delete_history_item_${item.drama.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// 3. VIP PREMIUM TAB CONTENT
@Composable
fun VipTabContent(
    viewModel: DramaViewModel
) {
    val context = LocalContext.current
    val profile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val isVip = profile?.isVip == true

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. VIP Status Header Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF3A1C22), Color(0xFF1E1428))
                        )
                    )
                    .border(1.dp, PrimaryGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "👑",
                        fontSize = 44.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isVip) "BẠN LÀ THÀNH VIÊN VIP PREMIUM" else "NÂNG CẤP THÀNH VIÊN VIP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PrimaryGold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isVip) "Đặc quyền VIP của bạn đang hoạt động cực kỳ mượt mà. Xem phim vui vẻ nhé! ✨" else "Đăng ký gói VIP ngay hôm nay để có trải nghiệm thưởng thức phim tuyệt đỉnh!",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 2. VIP Privileges list
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Đặc Quyền Hội Hội Viên VIP:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )

                val itemsList = listOf(
                    Pair("🔓", "Mở khóa toàn bộ tập phim: Không giới hạn, xem thả ga trọn bộ các phim hot."),
                    Pair("🚫", "Không quảng cáo phiền toái: Thưởng thức liên tục không lo đứt quãng cảm xúc."),
                    Pair("⚡", "Xem sớm tập mới nhất: Cập nhật siêu tốc, là người đầu tiên biết diễn biến tiếp theo.")
                )

                itemsList.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(item.first, fontSize = 16.sp)
                        Column {
                            val parts = item.second.split(":")
                            Text(
                                text = parts[0],
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            if (parts.size > 1) {
                                Text(
                                    text = parts[1].trim(),
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. VIP Packages Selector Title
        item {
            Text(
                text = "Các Gói VIP Premium Linh Hoạt:",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // 4. VIP Package Cards
        item {
            val vipPlans = listOf(
                Triple("GÓI VIP NGÀY", "9.000đ", "Thích hợp xem phim lẻ, hiệu lực 24h"),
                Triple("GÓI VIP THÁNG", "49.000đ", "Khuyên dùng - Tiết kiệm nhất suốt 30 ngày"),
                Triple("GÓI VIP NĂM", "299.000đ", "Trải nghiệm đỉnh cao không giới hạn suốt 1 năm")
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                vipPlans.forEachIndexed { index, plan ->
                    val isCurrentActive = isVip && profile?.vipLevel == plan.first

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrentActive) Color(0xFF2C1E22) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isCurrentActive) 1.5.dp else 1.dp,
                                color = if (isCurrentActive) PrimaryCoral else Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                viewModel.purchaseVip(plan.first, when(index) {
                                    0 -> 1
                                    1 -> 30
                                    else -> 365
                                })
                                Toast.makeText(context, "Kích hoạt thành công ${plan.first}! 👑✨", Toast.LENGTH_LONG).show()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = plan.first,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isCurrentActive) PrimaryCoral else Color.White
                                )
                                if (index == 1) { // Month tag
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(PrimaryCoral)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "PHỔ BIẾN",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = plan.third,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.purchaseVip(plan.first, when(index) {
                                    0 -> 1
                                    1 -> 30
                                    else -> 365
                                })
                                Toast.makeText(context, "Kích hoạt thành công ${plan.first}! 👑✨", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCurrentActive) Color.White.copy(alpha = 0.1f) else PrimaryGold
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(
                                text = if (isCurrentActive) "Đang dùng" else plan.second,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentActive) Color.White.copy(alpha = 0.6f) else Color.Black
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
