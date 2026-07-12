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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.example.data.CheckInResult
import com.example.data.WatchHistoryItem
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryGold
import com.example.viewmodel.DramaViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

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
    val balance by viewModel.userBalance.collectAsStateWithLifecycle(initialValue = null)
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    
    // Dynamically localized tabs for all 14 languages beautifully
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
    val tab2 = when(appLanguage) {
        com.example.data.AppLanguage.VIETNAMESE -> "Ví & Nhiệm vụ 🪙"
        com.example.data.AppLanguage.ENGLISH -> "Wallet & Quests 🪙"
        com.example.data.AppLanguage.CHINESE -> "钱包与任务 🪙"
        com.example.data.AppLanguage.SPANISH -> "Billetera y Tareas 🪙"
        com.example.data.AppLanguage.FRENCH -> "Portefeuille & Quêtes 🪙"
        com.example.data.AppLanguage.JAPANESE -> "ウォレットとクエスト 🪙"
        com.example.data.AppLanguage.KOREAN -> "지갑 및 미션 🪙"
        com.example.data.AppLanguage.RUSSIAN -> "Кошелек и Квесты 🪙"
        com.example.data.AppLanguage.GERMAN -> "Brieftasche & Aufgaben 🪙"
        com.example.data.AppLanguage.PORTUGUESE -> "Carteira e Tarefas 🪙"
        com.example.data.AppLanguage.ITALIAN -> "Portafoglio e Missioni 🪙"
        com.example.data.AppLanguage.TAGALOG -> "Wallet at Quests 🪙"
        com.example.data.AppLanguage.INDONESIAN -> "Dompet & Misi 🪙"
        com.example.data.AppLanguage.THAI -> "กระเป๋าเงินและภารกิจ 🪙"
    }
    val tabs = listOf(tab0, tab1, tab2)

    // Ad running state
    var isAdRunning by remember { mutableStateOf(false) }
    var adCountdown by remember { mutableStateOf(10) }

    // Collect daily check-in result flows with localization
    LaunchedEffect(viewModel) {
        viewModel.checkInStatus.collect { result ->
            when (result) {
                is CheckInResult.Success -> {
                    Toast.makeText(
                        context, 
                        viewModel.getString("toast_checkin_success", result.coinsEarned.toString()), 
                        Toast.LENGTH_LONG
                    ).show()
                }
                CheckInResult.AlreadyCheckedIn -> {
                    val alreadyMsg = viewModel.getString("already_checked_in")
                    Toast.makeText(context, alreadyMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!isAdRunning) {
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

                    // Wallet pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("🪙", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${balance?.coins ?: 0} ${viewModel.getString("wallet_balance")}",
                            color = PrimaryGold,
                            fontSize = 12.sp,
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
                        2 -> CoinsTabContent(
                            coins = balance?.coins ?: 0,
                            lastCheckInTime = balance?.lastCheckInTime ?: 0L,
                            viewModel = viewModel,
                            onCheckIn = { viewModel.performDailyCheckIn() },
                            onTriggerAd = {
                                isAdRunning = true
                                adCountdown = 10
                            }
                        )
                    }
                }
            }
        } else {
            // Re-use our magnificent ad simulator right inside the coins page!
            AdSimulatorView(
                countdown = adCountdown,
                viewModel = viewModel,
                onTick = { adCountdown-- },
                onFinished = {
                    isAdRunning = false
                    viewModel.watchAdAndEarnCoins()
                    Toast.makeText(context, viewModel.getString("watch_ad_earn"), Toast.LENGTH_LONG).show()
                }
            )
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

// 3. COINS & QUESTS TAB
@Composable
fun CoinsTabContent(
    coins: Int,
    lastCheckInTime: Long,
    viewModel: DramaViewModel,
    onCheckIn: () -> Unit,
    onTriggerAd: () -> Unit
) {
    // Determine check-in status
    val isTodayCheckedIn = remember(lastCheckInTime) {
        val fmt = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
        fmt.format(java.util.Date(lastCheckInTime)) == fmt.format(java.util.Date(System.currentTimeMillis())) && lastCheckInTime > 0L
    }

    val checkInInteraction = remember { MutableInteractionSource() }
    val isCheckInPressed by checkInInteraction.collectIsPressedAsState()
    val checkInScale by animateFloatAsState(
        targetValue = if (isCheckInPressed) 0.90f else 1.0f,
        animationSpec = spring(stiffness = 500f),
        label = "checkInScale"
    )

    val adInteraction = remember { MutableInteractionSource() }
    val isAdPressed by adInteraction.collectIsPressedAsState()
    val adScale by animateFloatAsState(
        targetValue = if (isAdPressed) 0.90f else 1.0f,
        animationSpec = spring(stiffness = 500f),
        label = "adScale"
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Gold balance banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryGold)
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = viewModel.getString("wallet_title").uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.Black.copy(alpha = 0.6f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🪙",
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$coins ${viewModel.getString("wallet_balance")}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    Text(
                        text = viewModel.getString("wallet_sub"),
                        fontSize = 11.sp,
                        color = Color.Black.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Quests header Title
        item {
            Text(
                text = viewModel.getString("quests_title"),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Quest 1: Daily Check-In
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = PrimaryGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = viewModel.getString("quest_checkin_title"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = viewModel.getString("quest_checkin_desc"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Check in CTA
                Button(
                    onClick = onCheckIn,
                    enabled = !isTodayCheckedIn,
                    interactionSource = checkInInteraction,
                    modifier = Modifier
                        .height(36.dp)
                        .graphicsLayer {
                            scaleX = checkInScale
                            scaleY = checkInScale
                        }
                        .testTag("daily_check_in_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGold,
                        disabledContainerColor = Color.White.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = if (isTodayCheckedIn) viewModel.getString("quest_checkin_btn_done") else viewModel.getString("quest_checkin_btn_claim"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTodayCheckedIn) Color.White.copy(alpha = 0.4f) else Color.Black
                    )
                }
            }
        }

        // Quest 2: Watch Ad Reward
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryCoral.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = PrimaryCoral,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = viewModel.getString("quest_ad_title"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = viewModel.getString("quest_ad_desc"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Watch Ad CTA
                Button(
                    onClick = onTriggerAd,
                    interactionSource = adInteraction,
                    modifier = Modifier
                        .height(36.dp)
                        .graphicsLayer {
                            scaleX = adScale
                            scaleY = adScale
                        }
                        .testTag("watch_ad_quest_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = viewModel.getString("quest_ad_btn"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Info box
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = viewModel.getString("coins_tip"),
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
