package com.example.ui.screens

import android.widget.Toast
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.BorderStroke
import coil.compose.AsyncImage
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Drama
import com.example.data.Episode
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryGold
import com.example.viewmodel.Comment
import com.example.viewmodel.DramaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: DramaViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentDrama by viewModel.currentDrama.collectAsStateWithLifecycle()
    val currentEpisode by viewModel.currentEpisode.collectAsStateWithLifecycle()
    val unlockedList by viewModel.unlockedEpisodes.collectAsStateWithLifecycle()
    val balance by viewModel.userBalance.collectAsStateWithLifecycle(initialValue = null)
    val commentsMap by viewModel.commentsMap.collectAsStateWithLifecycle()

    if (currentDrama == null || currentEpisode == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryCoral)
        }
        return
    }

    val drama = currentDrama!!
    val episode = currentEpisode!!
    val episodes = remember(drama.id) { viewModel.getEpisodesForDrama(drama.id) }

    // Check if the current episode is unlocked
    val compositeId = "${drama.id}_${episode.episodeNumber}"
    val isUnlocked = episode.episodeNumber <= 3 || unlockedList.contains(compositeId)

    // Player controls state
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf(0f) } // 0f to 1f
    var videoDurationMs by remember { mutableStateOf(1L) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var showPlayerControls by remember { mutableStateOf(true) }

    // UI overlays
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showEpisodesSheet by remember { mutableStateOf(false) }

    // Simulated Ad state
    var isAdRunning by remember { mutableStateOf(false) }
    var adCountdown by remember { mutableStateOf(10) }

    // Synchronize play state when unlocked status changes
    LaunchedEffect(isUnlocked) {
        isPlaying = isUnlocked
    }

    // Auto-hide controls overlay
    LaunchedEffect(showPlayerControls, isPlaying) {
        if (showPlayerControls && isPlaying) {
            delay(4000)
            showPlayerControls = false
        }
    }

    // Coroutine to simulate video timeline progression
    LaunchedEffect(isPlaying, episode.id, isUnlocked) {
        if (isPlaying && isUnlocked) {
            while (true) {
                delay(1000)
                if (currentPositionMs < videoDurationMs) {
                    currentPositionMs += 1000
                    currentProgress = (currentPositionMs.toFloat() / videoDurationMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    // Video finished, auto play next
                    val nextEpNo = episode.episodeNumber + 1
                    val nextEp = episodes.find { it.episodeNumber == nextEpNo }
                    if (nextEp != null) {
                        viewModel.selectEpisode(drama, nextEp)
                        currentPositionMs = 0L
                        currentProgress = 0f
                        Toast.makeText(context, viewModel.getString("toast_switching_next"), Toast.LENGTH_SHORT).show()
                    } else {
                        isPlaying = false
                        currentPositionMs = 0L
                        currentProgress = 0f
                    }
                }
            }
        }
    }

    // Helper functions for previous and next episodes with continuous scroll support (MovieBox style!)
    val playNextEpisode: () -> Unit = {
        val nextEp = episodes.find { it.episodeNumber == episode.episodeNumber + 1 }
        if (nextEp != null) {
            viewModel.selectEpisode(drama, nextEp)
            currentPositionMs = 0L
            currentProgress = 0f
        } else {
            // End of current drama, let's continuous-scroll to the next drama's first episode!
            val dramasList = viewModel.allDramas.value
            val currentDramaIndex = dramasList.indexOfFirst { it.id == drama.id }
            val nextDrama = if (currentDramaIndex != -1 && currentDramaIndex < dramasList.lastIndex) {
                dramasList[currentDramaIndex + 1]
            } else {
                dramasList.firstOrNull() // Loop back to first drama
            }
            if (nextDrama != null) {
                val nextDramaEps = viewModel.getEpisodesForDrama(nextDrama.id)
                if (nextDramaEps.isNotEmpty()) {
                    viewModel.selectEpisode(nextDrama, nextDramaEps.first())
                    currentPositionMs = 0L
                    currentProgress = 0f
                    Toast.makeText(context, viewModel.getString("toast_next_drama", nextDrama.title), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, viewModel.getString("toast_last_episode"), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val playPrevEpisode: () -> Unit = {
        val prevEp = episodes.find { it.episodeNumber == episode.episodeNumber - 1 }
        if (prevEp != null) {
            viewModel.selectEpisode(drama, prevEp)
            currentPositionMs = 0L
            currentProgress = 0f
        } else {
            // Start of current drama, let's continuous-scroll to the previous drama's last episode!
            val dramasList = viewModel.allDramas.value
            val currentDramaIndex = dramasList.indexOfFirst { it.id == drama.id }
            val prevDrama = if (currentDramaIndex > 0) {
                dramasList[currentDramaIndex - 1]
            } else {
                dramasList.lastOrNull() // Loop to last drama
            }
            if (prevDrama != null) {
                val prevDramaEps = viewModel.getEpisodesForDrama(prevDrama.id)
                if (prevDramaEps.isNotEmpty()) {
                    viewModel.selectEpisode(prevDrama, prevDramaEps.last())
                    currentPositionMs = 0L
                    currentProgress = 0f
                    Toast.makeText(context, viewModel.getString("toast_prev_drama", prevDrama.title), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, viewModel.getString("toast_first_episode"), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Swipe up/down gesture support
    val draggableState = rememberDraggableState { delta ->
        // Swipe sensitivity thresholds
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (velocity < -300) {
                        // Swipe Up -> Next Episode
                        playNextEpisode()
                    } else if (velocity > 300) {
                        // Swipe Down -> Prev Episode
                        playPrevEpisode()
                    }
                }
            )
    ) {
        // 1. Real Video Player Layout (If unlocked)
        if (isUnlocked && !isAdRunning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showPlayerControls = !showPlayerControls }
            ) {
                // VideoView Wrapper
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setOnPreparedListener { mp ->
                                videoDurationMs = mp.duration.toLong().coerceAtLeast(1)
                                mp.isLooping = false
                                if (isMuted) {
                                    mp.setVolume(0f, 0f)
                                } else {
                                    mp.setVolume(1f, 1f)
                                }
                                mp.start()
                            }
                            setOnCompletionListener {
                                playNextEpisode()
                            }
                        }
                    },
                    update = { videoView ->
                        val uri = android.net.Uri.parse(episode.videoUrl)
                        if (videoView.tag != episode.videoUrl) {
                            videoView.setVideoURI(uri)
                            videoView.tag = episode.videoUrl
                            currentPositionMs = 0L
                            currentProgress = 0f
                        }
                        if (isPlaying) {
                            videoView.start()
                        } else {
                            videoView.pause()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Play/Pause Big Center Indicator Overlay
                if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { isPlaying = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        // 3. Locked Episode Premium Screen
        if (!isUnlocked && !isAdRunning) {
            LockedPremiumView(
                drama = drama,
                episode = episode,
                balance = balance?.coins ?: 0,
                viewModel = viewModel,
                onUnlock = {
                    viewModel.unlockEpisode(
                        dramaId = drama.id,
                        episodeNumber = episode.episodeNumber,
                        onSuccess = {
                            Toast.makeText(context, viewModel.getString("toast_unlock_success", episode.episodeNumber), Toast.LENGTH_SHORT).show()
                        },
                        onFailure = {
                            Toast.makeText(context, viewModel.getString("toast_insufficient_coins"), Toast.LENGTH_LONG).show()
                        }
                    )
                },
                onWatchAd = {
                    isAdRunning = true
                    adCountdown = 10
                    isPlaying = false
                }
            )
        }

        // 4. Interactive Full Screen Ad Simulator
        if (isAdRunning) {
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

        // 5. Ambient Top and Bottom gradient overlays for high readability
        if (!isAdRunning && isUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                        )
                    )
                    .align(Alignment.TopCenter)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .align(Alignment.BottomCenter)
            )
        }

        // 6. UI Navigation & Toolbar (Top bar)
        if (!isAdRunning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .testTag("player_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Balance indicator
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
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
        }

        // 7. Right-Side Action Controls Sidebar (Floating controls)
        if (isUnlocked && !isAdRunning) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .width(60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Favorite/Like drama
                val isFavFlow = viewModel.isFavorite(drama.id).collectAsStateWithLifecycle(initialValue = false)
                val isFav = isFavFlow.value
                val favInteraction = remember { MutableInteractionSource() }
                val isFavPressed by favInteraction.collectIsPressedAsState()
                val favButtonScale by animateFloatAsState(
                    targetValue = if (isFavPressed) 0.85f else 1.0f,
                    animationSpec = spring(stiffness = 500f),
                    label = "favButtonScale"
                )
                val favHeartScale by animateFloatAsState(
                    targetValue = if (isFav) 1.25f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.35f, stiffness = 250f),
                    label = "favHeartScale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = favButtonScale
                            scaleY = favButtonScale
                        }
                        .clickable(
                            interactionSource = favInteraction,
                            indication = null
                        ) { viewModel.toggleFavorite(drama.id) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = viewModel.getString("favorite_btn"),
                            tint = if (isFav) PrimaryCoral else Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = favHeartScale
                                    scaleY = favHeartScale
                                }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.getString("btn_like"),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Comments Action
                val currentDramaComments = commentsMap[drama.id] ?: emptyList()
                val commentInteraction = remember { MutableInteractionSource() }
                val isCommentPressed by commentInteraction.collectIsPressedAsState()
                val commentButtonScale by animateFloatAsState(
                    targetValue = if (isCommentPressed) 0.85f else 1.0f,
                    animationSpec = spring(stiffness = 500f),
                    label = "commentButtonScale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = commentButtonScale
                            scaleY = commentButtonScale
                        }
                        .clickable(
                            interactionSource = commentInteraction,
                            indication = null
                        ) { showCommentsSheet = true }
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Comment,
                            contentDescription = viewModel.getString("comments_title"),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${currentDramaComments.size}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Episode List Sheet Trigger
                val episodesInteraction = remember { MutableInteractionSource() }
                val isEpisodesPressed by episodesInteraction.collectIsPressedAsState()
                val episodesButtonScale by animateFloatAsState(
                    targetValue = if (isEpisodesPressed) 0.85f else 1.0f,
                    animationSpec = spring(stiffness = 500f),
                    label = "episodesButtonScale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = episodesButtonScale
                            scaleY = episodesButtonScale
                        }
                        .clickable(
                            interactionSource = episodesInteraction,
                            indication = null
                        ) { showEpisodesSheet = true }
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistPlay,
                            contentDescription = viewModel.getString("btn_select_episode"),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.getString("btn_select_episode"),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Share Action (Simulated)
                val shareInteraction = remember { MutableInteractionSource() }
                val isSharePressed by shareInteraction.collectIsPressedAsState()
                val shareButtonScale by animateFloatAsState(
                    targetValue = if (isSharePressed) 0.85f else 1.0f,
                    animationSpec = spring(stiffness = 500f),
                    label = "shareButtonScale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = shareButtonScale
                            scaleY = shareButtonScale
                        }
                        .clickable(
                            interactionSource = shareInteraction,
                            indication = null
                        ) {
                            Toast.makeText(context, viewModel.getString("toast_share_copied"), Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = viewModel.getString("btn_share"),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.getString("btn_share"),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 8. Immersive Bottom Text and Media Slider
        if (isUnlocked && !isAdRunning) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(bottom = 30.dp, start = 16.dp, end = 16.dp)
            ) {
                // Prev / Next Floating Overlay Arrows
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prev Button
                    IconButton(
                        onClick = playPrevEpisode,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    // Swipe Up Tip Indicator
                    Text(
                        text = viewModel.getString("swipe_up_hint"),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Next Button
                    IconButton(
                        onClick = playNextEpisode,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                // Drama Title
                Text(
                    text = "@${drama.title}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Episode Title
                val epPrefix = if (viewModel.appLanguage.value == com.example.data.AppLanguage.VIETNAMESE) "Tập" else "Episode"
                Text(
                    text = "$epPrefix ${episode.episodeNumber}: ${episode.title}",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Description snippet
                Text(
                    text = drama.description,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Seekbar & Timing Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Play / Pause Button
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Progress Slider
                    Slider(
                        value = currentProgress,
                        onValueChange = {
                            currentProgress = it
                            currentPositionMs = (it * videoDurationMs).toLong()
                        },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryCoral,
                            activeTrackColor = PrimaryCoral,
                            inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                        )
                    )

                    // Timer text
                    val currentStr = formatDuration(currentPositionMs)
                    val totalStr = episode.duration
                    Text(
                        text = "$currentStr / $totalStr",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 9. Comments Drawer Bottom Sheet
        if (showCommentsSheet) {
            CommentsDrawerSheet(
                comments = commentsMap[drama.id] ?: emptyList(),
                viewModel = viewModel,
                onAddComment = { author, text ->
                    viewModel.addComment(drama.id, author, text)
                },
                onClose = { showCommentsSheet = false }
            )
        }

        // 10. Episodes Picker Bottom Sheet
        if (showEpisodesSheet) {
            EpisodesDrawerSheet(
                episodes = episodes,
                activeEpisode = episode,
                unlockedList = unlockedList,
                viewModel = viewModel,
                onSelectEpisode = { selectedEp ->
                    viewModel.selectEpisode(drama, selectedEp)
                    showEpisodesSheet = false
                },
                onClose = { showEpisodesSheet = false }
            )
        }
    }
}

// 1. FORMAT TIME
private fun formatDuration(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / (1000 * 60)) % 60
    return String.format("%02d:%02d", min, sec)
}

// 2. LOCKED PREMIUM VIEW
@Composable
fun LockedPremiumView(
    drama: Drama,
    episode: Episode,
    balance: Int,
    viewModel: DramaViewModel,
    onUnlock: () -> Unit,
    onWatchAd: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F12)),
        contentAlignment = Alignment.Center
    ) {
        // Overlay blurred cover background
        AsyncImage(
            model = drama.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(24.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
        )

        // Lock Card
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1B1B1F))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated lock glow
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(PrimaryGold.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = PrimaryGold,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = viewModel.getString("locked_title", episode.episodeNumber),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = viewModel.getString("episode_locked_desc"),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // CTA Button 1: Unlock with coins
            Button(
                onClick = onUnlock,
                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("premium_unlock_button"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = viewModel.getString("unlock_with_coins", 10),
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CTA Button 2: Watch Ad to earn coins
            OutlinedButton(
                onClick = onWatchAd,
                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("watch_ad_button"),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, PrimaryCoral)
            ) {
                Icon(
                    imageVector = Icons.Default.OndemandVideo,
                    contentDescription = null,
                    tint = PrimaryCoral,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = viewModel.getString("watch_ad_30"),
                    fontWeight = FontWeight.Bold,
                    color = PrimaryCoral,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Divider(color = Color.White.copy(alpha = 0.1f))

            Spacer(modifier = Modifier.height(10.dp))

            // Real-time wallet check
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = viewModel.getString("current_balance"),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
                Text(
                    text = "$balance ${viewModel.getString("wallet_balance")}",
                    color = PrimaryGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// 3. ADVERTISEMENT SIMULATOR
@Composable
fun AdSimulatorView(
    countdown: Int,
    viewModel: DramaViewModel,
    onTick: () -> Unit,
    onFinished: () -> Unit
) {
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    // Ticker launch
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            onTick()
        } else {
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070709)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = viewModel.getString("ad_label"),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Countdown timer pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryCoral)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = viewModel.getString("ad_close_countdown", countdown),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Premium simulated ad content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E24))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MovieFilter,
                        contentDescription = null,
                        tint = PrimaryCoral,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "MovieBox VIP Premium",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.getString("ad_desc_premium"),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1.2f))

            // Brand tagline
            Text(
                text = "${viewModel.getString("ad_label")} sponsored by MovieBox Ads",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// 4. COMMENTS SHEET DRAWABLE
@Composable
fun CommentsDrawerSheet(
    comments: List<Comment>,
    viewModel: DramaViewModel,
    onAddComment: (String, String) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onClose() }
    ) {
        // Bottom drawer slide-in content
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Color(0xFF16161A))
                .clickable(enabled = false) {} // block click propagation
                .padding(top = 12.dp)
        ) {
            // Drag bar indicator
            Box(
                modifier = Modifier
                    .size(36.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .align(Alignment.CenterHorizontally)
            )

            // Sheet Title header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${viewModel.getString("comments_title")} (${comments.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Divider(color = Color.White.copy(alpha = 0.08f))

            // 1. Comments list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = viewModel.getString("no_comments_yet"),
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(comments) { comment ->
                        CommentRow(comment = comment)
                    }
                }
            }

            Divider(color = Color.White.copy(alpha = 0.08f))

            // 2. Input container for writing comments
            var authorName by remember { mutableStateOf("") }
            var commentText by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E22))
                    .padding(12.dp)
                    .navigationBarsPadding()
            ) {
                // Name Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = authorName,
                        onValueChange = { authorName = it },
                        placeholder = { Text(viewModel.getString("your_name_placeholder"), fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f)) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("comment_author_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryCoral,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 12.sp)
                    )

                    // Post trigger
                    Button(
                        onClick = {
                            if (authorName.isNotBlank() && commentText.isNotBlank()) {
                                onAddComment(authorName, commentText)
                                commentText = ""
                            }
                        },
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("post_comment_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                        shape = RoundedCornerShape(6.dp),
                        enabled = authorName.isNotBlank() && commentText.isNotBlank()
                    ) {
                        Text(viewModel.getString("send"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Comment Content Input
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text(viewModel.getString("input_comment"), fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("comment_text_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryCoral,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 12.sp)
                )
            }
        }
    }
}

@Composable
fun CommentRow(comment: Comment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Rounded avatar
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(comment.avatarColor)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = comment.author.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        // Details
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = comment.author,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
                Text(
                    text = comment.timestamp,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = comment.content,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = 16.sp
            )
        }

        // Likes indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${comment.likes}",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.35f)
            )
        }
    }
}

// 5. EPISODES DRAWER SHEET
@Composable
fun EpisodesDrawerSheet(
    episodes: List<Episode>,
    activeEpisode: Episode,
    unlockedList: List<String>,
    viewModel: DramaViewModel,
    onSelectEpisode: (Episode) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onClose() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Color(0xFF16161A))
                .clickable(enabled = false) {}
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = viewModel.getString("episodes_list"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable list of episodes
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(episodes) { episode ->
                    val compositeId = "${episode.dramaId}_${episode.episodeNumber}"
                    val isUnlocked = episode.episodeNumber <= 3 || unlockedList.contains(compositeId)
                    val isActive = episode.episodeNumber == activeEpisode.episodeNumber

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isActive) PrimaryCoral.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.04f)
                            )
                            .clickable { onSelectEpisode(episode) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isActive) PrimaryCoral
                                        else Color.White.copy(alpha = 0.08f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${episode.episodeNumber}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.8f)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = episode.title,
                                color = if (isActive) PrimaryCoral else Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                            )
                        }

                        // Unlock / Lock status indicators
                        if (isUnlocked) {
                            Text(
                                text = if (episode.episodeNumber <= 3) viewModel.getString("free_status") else viewModel.getString("open_status"),
                                color = if (episode.episodeNumber <= 3) PrimaryCoral else Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = PrimaryGold,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
