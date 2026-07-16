package com.example.ui.screens

import android.widget.Toast
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Size
import android.media.AudioManager

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
    val userProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
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
    val isUnlocked = episode.episodeNumber == 1 || userProfile?.isVip == true

    // Player controls state
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf(0f) } // 0f to 1f
    var videoDurationMs by remember { mutableStateOf(1L) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var showPlayerControls by remember { mutableStateOf(true) }

    // Professional player features:
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var isAutoPlayEnabled by remember { mutableStateOf(true) }
    
    // Swipe gestures state
    var showGestureIndicator by remember { mutableStateOf(false) }
    var gestureIndicatorType by remember { mutableStateOf("") } // "volume" or "brightness"
    var gestureIndicatorValue by remember { mutableStateOf(0) }
    var containerWidth by remember { mutableStateOf(0f) }
    
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    var showVipPurchaseDialog by remember { mutableStateOf(false) }

    var mediaPlayerRef by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    // Auto-Resume playback states & logic
    var savedPositionMs by remember { mutableStateOf(0L) }
    var showResumePrompt by remember { mutableStateOf(false) }

    LaunchedEffect(episode.id) {
        // Fetch saved watch history for this drama/episode
        try {
            val history = viewModel.getWatchHistoryForDrama(drama.id)
            if (history != null && history.episodeNumber == episode.episodeNumber && history.videoPositionMs > 3000L) {
                savedPositionMs = history.videoPositionMs
                showResumePrompt = true
                delay(7000)
                showResumePrompt = false
            } else {
                savedPositionMs = 0L
                showResumePrompt = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    DisposableEffect(episode.id) {
        onDispose {
            if (currentPositionMs > 1000L) {
                viewModel.updateWatchProgress(drama.id, episode.episodeNumber, episode.title, currentPositionMs)
            }
        }
    }

    LaunchedEffect(currentPositionMs) {
        mediaPlayerRef?.let { mp ->
            try {
                val diff = Math.abs(mp.currentPosition.toLong() - currentPositionMs)
                if (diff > 4000) { // Seek if significant gap (slider or resume clicked)
                    mp.seekTo(currentPositionMs.toInt())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(playbackSpeed, mediaPlayerRef) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            mediaPlayerRef?.let { mp ->
                try {
                    mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

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
    LaunchedEffect(isPlaying, episode.id, isUnlocked, playbackSpeed, mediaPlayerRef) {
        if (isPlaying && isUnlocked) {
            while (true) {
                val stepMs = (1000 / playbackSpeed).toLong().coerceAtLeast(100L)
                delay(stepMs)
                
                val actualPos = mediaPlayerRef?.let {
                    try {
                        if (it.isPlaying) it.currentPosition.toLong() else null
                    } catch (e: Exception) {
                        null
                    }
                }

                if (actualPos != null) {
                    currentPositionMs = actualPos
                    currentProgress = (currentPositionMs.toFloat() / videoDurationMs.toFloat()).coerceIn(0f, 1f)
                } else if (currentPositionMs < videoDurationMs) {
                    currentPositionMs += 1000
                    currentProgress = (currentPositionMs.toFloat() / videoDurationMs.toFloat()).coerceIn(0f, 1f)
                }

                // Periodically save watch progress (e.g. every 5 seconds)
                if (currentPositionMs > 1000L) {
                    viewModel.updateWatchProgress(drama.id, episode.episodeNumber, episode.title, currentPositionMs)
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
            var gestureBrightness by remember { mutableStateOf<Float?>(null) }
            var gestureVolume by remember { mutableStateOf<Float?>(null) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { containerWidth = it.width.toFloat() }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val isLeftHalf = offset.x < containerWidth / 2
                                val isGestureArea = offset.x < containerWidth * 0.25f || offset.x > containerWidth * 0.75f
                                
                                if (isGestureArea) {
                                    showGestureIndicator = true
                                    gestureIndicatorType = if (isLeftHalf) "brightness" else "volume"
                                    
                                    if (isLeftHalf) {
                                        val activity = context as? android.app.Activity
                                        val currentBrightness = activity?.window?.attributes?.screenBrightness ?: 0.5f
                                        gestureBrightness = if (currentBrightness < 0f) 0.5f else currentBrightness
                                        gestureIndicatorValue = ((gestureBrightness ?: 0.5f) * 100).toInt()
                                    } else {
                                        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        gestureVolume = currentVol.toFloat() / maxVolume.toFloat()
                                        gestureIndicatorValue = ((gestureVolume ?: 0.5f) * 100).toInt()
                                    }
                                } else {
                                    showGestureIndicator = false
                                }
                            },
                            onDrag = { change, dragAmount ->
                                if (showGestureIndicator) {
                                    change.consume()
                                    val delta = -dragAmount.y * 0.003f
                                    
                                    if (gestureIndicatorType == "brightness") {
                                        gestureBrightness = ((gestureBrightness ?: 0.5f) + delta).coerceIn(0.01f, 1f)
                                        val newBright = gestureBrightness ?: 0.5f
                                        gestureIndicatorValue = (newBright * 100).toInt()
                                        
                                        val activity = context as? android.app.Activity
                                        activity?.runOnUiThread {
                                            activity.window?.attributes?.let { layoutParams ->
                                                layoutParams.screenBrightness = newBright
                                                activity.window.attributes = layoutParams
                                            }
                                        }
                                    } else {
                                        gestureVolume = ((gestureVolume ?: 0.5f) + delta).coerceIn(0f, 1f)
                                        val newVolRatio = gestureVolume ?: 0.5f
                                        val newVol = (newVolRatio * maxVolume).toInt()
                                        gestureIndicatorValue = (newVolRatio * 100).toInt()
                                        
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                    }
                                }
                            },
                            onDragEnd = {
                                gestureBrightness = null
                                gestureVolume = null
                                scope.launch {
                                    delay(800)
                                    showGestureIndicator = false
                                }
                            },
                            onDragCancel = {
                                gestureBrightness = null
                                gestureVolume = null
                                showGestureIndicator = false
                            }
                        )
                    }
                    .clickable { showPlayerControls = !showPlayerControls }
            ) {
                // VideoView Wrapper
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setOnPreparedListener { mp ->
                                mediaPlayerRef = mp
                                videoDurationMs = mp.duration.toLong().coerceAtLeast(1)
                                mp.isLooping = false
                                if (isMuted) {
                                    mp.setVolume(0f, 0f)
                                } else {
                                    mp.setVolume(1f, 1f)
                                }
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    try {
                                        mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                mp.start()
                            }
                            setOnCompletionListener {
                                if (isAutoPlayEnabled) {
                                    playNextEpisode()
                                } else {
                                    isPlaying = false
                                    currentPositionMs = 0L
                                    currentProgress = 0f
                                }
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
                viewModel = viewModel,
                onShowVipDialog = {
                    showVipPurchaseDialog = true
                }
            )
        }

        if (showVipPurchaseDialog) {
            VipPurchaseDialog(
                viewModel = viewModel,
                onDismiss = { showVipPurchaseDialog = false }
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

                // VIP Status indicator
                val profile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
                if (profile?.isVip == true) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2C1E22))
                            .border(0.5.dp, PrimaryGold, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👑", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "VIP Premium",
                            color = PrimaryGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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

                Spacer(modifier = Modifier.height(12.dp))

                // Playback Speed & Auto-Play Toggle Row (Antigravity Pro premium styling)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed Options
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tốc độ:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        listOf(0.75f, 1.0f, 1.5f, 2.0f).forEach { speed ->
                            val isSelected = playbackSpeed == speed
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else Color.Black.copy(alpha = 0.4f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { playbackSpeed = speed }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${speed}x",
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // AutoPlay Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable { isAutoPlayEnabled = !isAutoPlayEnabled }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isAutoPlayEnabled) Icons.Default.Autorenew else Icons.Default.Block,
                            contentDescription = null,
                            tint = if (isAutoPlayEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isAutoPlayEnabled) "Tự phát tiếp: BẬT" else "Tự phát tiếp: TẮT",
                            color = if (isAutoPlayEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                viewModel = viewModel,
                onSelectEpisode = { selectedEp ->
                    viewModel.selectEpisode(drama, selectedEp)
                    showEpisodesSheet = false
                },
                onClose = { showEpisodesSheet = false }
            )
        }

        // 11. Custom Gesture HUD Indicator (Antigravity Pro design)
        if (showGestureIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (gestureIndicatorType == "brightness") Icons.Default.Brightness5 else Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = if (gestureIndicatorType == "brightness") "Độ sáng: $gestureIndicatorValue%" else "Âm lượng: $gestureIndicatorValue%",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // Custom micro progress bar
                    LinearProgressIndicator(
                        progress = { gestureIndicatorValue / 100f },
                        modifier = Modifier
                            .width(80.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }

        // 12. Elegant Auto-Resume Playback Prompt (Netflix-style Glassmorphic HUD)
        if (showResumePrompt && savedPositionMs > 3000L) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        currentPositionMs = savedPositionMs
                        currentProgress = (savedPositionMs.toFloat() / videoDurationMs.toFloat()).coerceIn(0f, 1f)
                        mediaPlayerRef?.seekTo(savedPositionMs.toInt())
                        showResumePrompt = false
                        Toast.makeText(context, "Đã tiếp tục phát từ ${formatDuration(savedPositionMs)}", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Tiếp tục xem tập này?",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Phát từ ${formatDuration(savedPositionMs)} (Bấm để xem tiếp)",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }
                    IconButton(
                        onClick = { showResumePrompt = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
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
    viewModel: DramaViewModel,
    onShowVipDialog: () -> Unit
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
                text = "Tập phim VIP Premium 👑",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tập này yêu cầu quyền thành viên VIP. Nâng cấp để xem tiếp và thưởng thức không giới hạn kho phim đặc sắc!",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // CTA Button: Buy VIP
            Button(
                onClick = onShowVipDialog,
                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("premium_unlock_button"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Mua Gói VIP Ngay ✨",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun VipPurchaseDialog(
    viewModel: DramaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedPackage by remember { mutableStateOf(1) } // 0: Day, 1: Month, 2: Year

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFF1E1D24),
        modifier = Modifier.testTag("vip_purchase_dialog"),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Crown Icon with ambient glow
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(PrimaryGold.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "VIP Premium",
                        tint = PrimaryGold,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "NÂNG CẤP VIP PREMIUM 👑",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Mở khóa kho tàng phim ngắn siêu kịch tính không quảng cáo",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Packages List
                val packages = listOf(
                    Triple("GÓI VIP NGÀY", "9.000đ", "Thử nghiệm 24h"),
                    Triple("GÓI VIP THÁNG", "49.000đ", "Khuyên dùng - Tiết kiệm 80%"),
                    Triple("GÓI VIP NĂM", "299.000đ", "Siêu hời - Tốt nhất")
                )

                packages.forEachIndexed { index, pkg ->
                    val isSelected = selectedPackage == index
                    val borderBrush = if (isSelected) {
                        Brush.linearGradient(colors = listOf(PrimaryCoral, PrimaryGold))
                    } else {
                        Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.1f)))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFF2E2226) else Color(0xFF151419))
                            .border(1.5.dp, borderBrush, RoundedCornerShape(14.dp))
                            .clickable { selectedPackage = index }
                            .padding(14.dp)
                            .testTag("vip_package_card_$index")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = pkg.first,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) PrimaryCoral else Color.White
                                    )
                                    if (index == 1) { // Month is hot/recommended
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(PrimaryCoral)
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "HOT",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = pkg.third,
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                            Text(
                                text = pkg.second,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // CTA Button
                Button(
                    onClick = {
                        val chosen = packages[selectedPackage]
                        val name = chosen.first
                        viewModel.purchaseVip(name, when(selectedPackage) {
                            0 -> 1
                            1 -> 30
                            else -> 365
                        })
                        Toast.makeText(context, "Đăng ký thành công ${chosen.first}! Kích hoạt VIP Premium ⚡🎉", Toast.LENGTH_LONG).show()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("activate_vip_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Kích hoạt VIP Premium ⚡",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(onClick = onDismiss) {
                    Text("Đóng", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
        }
    )
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
    viewModel: DramaViewModel,
    onSelectEpisode: (Episode) -> Unit,
    onClose: () -> Unit
) {
    val userProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val isVip = userProfile?.isVip == true
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
                    val isUnlocked = episode.episodeNumber == 1 || isVip
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
