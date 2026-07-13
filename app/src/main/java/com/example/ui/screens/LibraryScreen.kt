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
import com.example.data.UserBalanceEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryGold
import com.example.viewmodel.DramaViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset

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

        // 1.5. Interactive Lucky Wheel
        item {
            LuckyWheelCard(viewModel = viewModel)
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

// ======================== LUCKY WHEEL COMPONENTS ========================

data class WheelSector(
    val id: Int,
    val label: String,
    val coins: Int,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuckyWheelCard(
    viewModel: DramaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userBalanceState by viewModel.userBalance.collectAsState(initial = UserBalanceEntity())

    val sectors = remember {
        listOf(
            WheelSector(0, "+10 XU 🪙", 10, Color(0xFFFF6B81)),
            WheelSector(1, "+20 XU 🪙", 20, Color(0xFFFF9F43)),
            WheelSector(2, "+50 XU 🪙", 50, Color(0xFFFECA57)),
            WheelSector(3, "+100 XU! 🌟", 100, Color(0xFFFF4757)),
            WheelSector(4, "+5 XU 🪙", 5, Color(0xFF5F27CD)),
            WheelSector(5, "+15 XU 🪙", 15, Color(0xFF54A0FF)),
            WheelSector(6, "+30 XU 🪙", 30, Color(0xFF00D2D3)),
            WheelSector(7, "+200 XU! 👑", 200, Color(0xFF10AC84))
        )
    }

    var currentRotation by remember { mutableStateOf(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var showPrizeDialog by remember { mutableStateOf(false) }
    var showAdProgress by remember { mutableStateOf(false) }
    var wonSector by remember { mutableStateOf<WheelSector?>(null) }

    val animatedRotation by animateFloatAsState(
        targetValue = currentRotation,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 3500,
            easing = androidx.compose.animation.core.EaseOutQuart
        ),
        finishedListener = {
            if (isSpinning) {
                isSpinning = false
                showPrizeDialog = true
            }
        },
        label = "wheel_spin_anim"
    )

    // Simulated Ad Dialog
    if (showAdProgress) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = {
                Text(
                    "ĐANG TẢI QUẢNG CÁO... 📺",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(color = PrimaryCoral)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Vui lòng đợi giây lát để nhận thêm 1 lượt quay miễn phí!",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color(0xFF1F1D23)
        )
    }

    // Prize Dialog
    if (showPrizeDialog && wonSector != null) {
        AlertDialog(
            onDismissRequest = { /* Force action or dismiss */ },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.topUpCoins(wonSector!!.coins)
                        showPrizeDialog = false
                        Toast.makeText(
                            context,
                            "Bạn đã nhận thành công ${wonSector!!.coins} xu! 🎉",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral)
                ) {
                    Text("Nhận ngay 🥰", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    "CHÚC MỪNG BẠN! 🎉",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = PrimaryGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🧸🎀",
                        fontSize = 48.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "Vòng quay may mắn đã dừng lại ở ô:",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = wonSector!!.label,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = wonSector!!.color
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Xu đã được cộng trực tiếp vào ví của bạn để mở khóa các bộ phim truyền hình siêu hot! 🎬💖",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        lineHeight = 16.sp
                    )
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color(0xFF1F1D23)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lucky_wheel_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF19171C)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "🎡",
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Vòng Quay May Mắn",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Thử vận may - Nhận xu miễn phí mỗi ngày!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Spin Ticket count badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryCoral.copy(alpha = 0.15f))
                        .border(1.dp, PrimaryCoral.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Lượt: ${userBalanceState.spins} 🎫",
                        color = PrimaryCoral,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Wheel Outer Container with Shadow and Pointer
            Box(
                modifier = Modifier
                    .size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                // The Wheel itself
                Canvas(
                    modifier = Modifier
                        .size(230.dp)
                        .graphicsLayer { rotationZ = animatedRotation }
                        .border(4.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                ) {
                    val radius = size.minDimension / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    val sweepAngle = 360f / sectors.size

                    // Draw colored slices
                    for (i in sectors.indices) {
                        val startAngle = i * sweepAngle - sweepAngle / 2f
                        drawArc(
                            color = sectors[i].color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            size = Size(size.width, size.height)
                        )
                    }

                    // Draw separator lines
                    for (i in sectors.indices) {
                        val angleRad = Math.toRadians((i * sweepAngle - sweepAngle / 2f).toDouble())
                        val x = center.x + radius * Math.cos(angleRad).toFloat()
                        val y = center.y + radius * Math.sin(angleRad).toFloat()
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = center,
                            end = Offset(x, y),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    // Draw text labels radial inside slices
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 10.dp.toPx()
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }

                    for (i in sectors.indices) {
                        val angleDeg = i * sweepAngle
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val textRadius = radius * 0.65f
                        val tx = center.x + textRadius * Math.cos(angleRad).toFloat()
                        val ty = center.y + textRadius * Math.sin(angleRad).toFloat()

                        rotate(degrees = angleDeg + 90f, pivot = Offset(tx, ty)) {
                            drawContext.canvas.nativeCanvas.drawText(
                                sectors[i].label.substringBefore(" XU"), // simplify label to look neat on tiny wheel
                                tx,
                                ty + 3.dp.toPx(),
                                paint
                            )
                        }
                    }
                }

                // Stationary Pointer at top (pointing down)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 220.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val path = Path().apply {
                            moveTo(size.width / 2f, size.height)
                            lineTo(0f, 0f)
                            lineTo(size.width, 0f)
                            close()
                        }
                        drawPath(path, color = PrimaryGold)
                        // tiny border
                        drawPath(path, color = Color.White, style = Stroke(width = 1.5.dp.toPx()))
                    }
                }

                // Central Non-Rotating SPIN/QUAY Button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryCoral, Color(0xFFFF4757))
                            )
                        )
                        .border(3.dp, Color.White, CircleShape)
                        .clickable(enabled = !isSpinning) {
                            if (userBalanceState.spins <= 0) {
                                Toast.makeText(
                                    context,
                                    "Bạn đã hết lượt quay miễn phí! Hãy nhận thêm lượt ở bên dưới nhé 👇",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@clickable
                            }

                            isSpinning = true
                            val winningIndex = (0..7).random()
                            wonSector = sectors[winningIndex]

                            viewModel.useSpin(
                                onSuccess = {
                                    val extraRotations = 6
                                    val sweepAngle = 360f / sectors.size
                                    val currentFullRotations = (currentRotation / 360f).toInt()
                                    val nextBase = (currentFullRotations + extraRotations) * 360f
                                    currentRotation = nextBase - (winningIndex * sweepAngle)
                                },
                                onFailure = {
                                    isSpinning = false
                                    Toast.makeText(
                                        context,
                                        "Có lỗi xảy ra, vui lòng thử lại sau!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                        .testTag("lucky_wheel_spin_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "QUAY",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wheel status text helper
            Text(
                text = if (isSpinning) {
                    "Đang tìm kiếm vận may của bạn... ✨🍀"
                } else if (userBalanceState.spins > 0) {
                    "Bạn còn ${userBalanceState.spins} lượt quay miễn phí! 🎁"
                } else {
                    "Bạn đã hết lượt quay miễn phí hôm nay! 🥺"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSpinning) PrimaryGold else Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Get more spins options
            Text(
                text = "NHẬN THÊM LƯỢT QUAY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Option 1: Buy spins with Coins
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !isSpinning) {
                            viewModel.exchangeCoinsForSpins(
                                coinCost = 15,
                                spinAmount = 1,
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "Đổi lượt thành công! 🎉 Chúc bạn trúng lớn!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onFailure = {
                                    Toast.makeText(
                                        context,
                                        "Bạn không đủ xu! Tích lũy thêm xu hoặc xem quảng cáo nhé! 🪙",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF221F26)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        Color.White.copy(alpha = 0.05f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🪙 ➔ 🎫", fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Đổi 15 Xu",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Nhận +1 Lượt quay",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                // Option 2: Watch Ad for Free Spins
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !isSpinning) {
                            showAdProgress = true
                            coroutineScope.launch {
                                delay(2500)
                                showAdProgress = false
                                viewModel.watchAdAndEarnSpins()
                                Toast.makeText(
                                    context,
                                    "Xem quảng cáo hoàn tất! Nhận thành công +1 Lượt quay! 🎡🎉",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF221F26)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        Color.White.copy(alpha = 0.05f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "📺 ➔ 🎫", fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Xem QC nhận Lượt",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Miễn phí +1 Lượt",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
