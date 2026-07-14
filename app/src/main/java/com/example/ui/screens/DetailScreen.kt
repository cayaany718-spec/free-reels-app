package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.data.Drama
import com.example.data.Episode
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryGold
import com.example.viewmodel.DramaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    dramaId: Int,
    viewModel: DramaViewModel,
    onNavigateBack: () -> Unit,
    onPlayEpisode: (Drama, Episode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dramasList by viewModel.allDramas.collectAsStateWithLifecycle()
    val drama = remember(dramaId, dramasList) { dramasList.find { it.id == dramaId } }
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    if (drama == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(viewModel.getString("no_drama_found"))
        }
        return
    }

    val episodes = remember(dramaId) { viewModel.getEpisodesForDrama(dramaId) }
    val isFavorite by viewModel.isFavorite(dramaId).collectAsStateWithLifecycle(initialValue = false)
    val unlockedList by viewModel.unlockedEpisodes.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // 1. Header Hero Panel with blur background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            // Blurred Background image
            AsyncImage(
                model = drama.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(16.dp)
            )

            // Overlays to blend with background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.3f), MaterialTheme.colorScheme.background),
                            startY = 0f
                        )
                    )
            )

            // Back button with smooth spring press scale
            val backInteraction = remember { MutableInteractionSource() }
            val isBackPressed by backInteraction.collectIsPressedAsState()
            val backScale by animateFloatAsState(
                targetValue = if (isBackPressed) 0.88f else 1f,
                animationSpec = spring(stiffness = 500f),
                label = "backScale"
            )

            IconButton(
                onClick = onNavigateBack,
                interactionSource = backInteraction,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = backScale
                        scaleY = backScale
                    }
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .align(Alignment.TopStart)
                    .testTag("detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Hero content: Floating Poster & Titles
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                // Floating portrait poster
                AsyncImage(
                    model = drama.coverUrl,
                    contentDescription = drama.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(110.dp)
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .testTag("detail_cover_poster")
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Metadata Info Column
                Column {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(PrimaryCoral)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = drama.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = drama.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${viewModel.getString("director_label")}: ${drama.author}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Stats row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = PrimaryGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${drama.rating}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Views",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${drama.views} ${viewModel.getString("views").lowercase()}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Main CTAs: Play & Favorite Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Play Episode 1 Button
            Button(
                onClick = {
                    if (episodes.isNotEmpty()) {
                        viewModel.selectEpisode(drama, episodes.first())
                        onPlayEpisode(drama, episodes.first())
                    }
                },
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .testTag("detail_play_now_button"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = viewModel.getString("watch_ep_1"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Favorite Toggle Button with a cute springy heartbeat pop
            val heartScale by animateFloatAsState(
                targetValue = if (isFavorite) 1.25f else 1.0f,
                animationSpec = spring(
                    dampingRatio = 0.35f,
                    stiffness = 250f
                ),
                label = "heartScale"
            )

            OutlinedButton(
                onClick = {
                    viewModel.toggleFavorite(drama.id)
                    val msg = if (isFavorite) viewModel.getString("toast_removed_favorite") else viewModel.getString("toast_added_favorite")
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("detail_favorite_button"),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (isFavorite) PrimaryCoral else MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isFavorite) PrimaryCoral.copy(alpha = 0.1f) else Color.Transparent
                )
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) PrimaryCoral else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.graphicsLayer {
                        scaleX = heartScale
                        scaleY = heartScale
                    }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isFavorite) viewModel.getString("liked") else viewModel.getString("favorite_btn"),
                    fontSize = 14.sp,
                    color = if (isFavorite) PrimaryCoral else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 3. Description Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = viewModel.getString("synopsis_title"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = drama.description,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }

        Divider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )

        // 4. Episodes Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${viewModel.getString("episodes_list")} (${episodes.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = viewModel.getString("free_episodes_hint"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryGold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Grid list of episodes in manual columns since Nested Scroll columns aren't possible
            val columnsCount = 4
            val chunks = episodes.chunked(columnsCount)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (chunk in chunks) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (episode in chunk) {
                            val compositeId = "${drama.id}_${episode.episodeNumber}"
                            val isUnlocked = episode.episodeNumber <= 3 || unlockedList.contains(compositeId)

                            val epInteraction = remember { MutableInteractionSource() }
                            val isEpPressed by epInteraction.collectIsPressedAsState()
                            val epScale by animateFloatAsState(
                                targetValue = if (isEpPressed) 0.92f else 1.0f,
                                animationSpec = spring(stiffness = 500f),
                                label = "epScale_${episode.episodeNumber}"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .graphicsLayer {
                                        scaleX = epScale
                                        scaleY = epScale
                                    }
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isUnlocked) MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isUnlocked) Color.Transparent else PrimaryGold.copy(alpha = 0.4f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable(
                                        interactionSource = epInteraction,
                                        indication = androidx.compose.foundation.LocalIndication.current
                                    ) {
                                        viewModel.selectEpisode(drama, episode)
                                        onPlayEpisode(drama, episode)
                                    }
                                    .testTag("episode_selector_${episode.episodeNumber}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "${episode.episodeNumber}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else PrimaryGold
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    if (isUnlocked) {
                                        Text(
                                            text = if (episode.episodeNumber <= 3) viewModel.getString("free_status") else viewModel.getString("open_status"),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (episode.episodeNumber <= 3) PrimaryCoral else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = PrimaryGold,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Fill empty cells for even weight distribution
                        val emptyCount = columnsCount - chunk.size
                        if (emptyCount > 0) {
                            for (i in 0 until emptyCount) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}
