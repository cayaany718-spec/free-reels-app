package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Drama
import com.example.data.MockData
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryGold
import com.example.viewmodel.DramaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DramaViewModel,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dramas by viewModel.filteredDramas.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val balance by viewModel.userBalance.collectAsStateWithLifecycle(initialValue = null)
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    // Main top tab: "drama" or "anime"
    var activeTopTab by remember { mutableStateOf("drama") }

    // Filter dramas based on activeTopTab & selectedCategory
    val displayDramas = remember(dramas, activeTopTab, selectedCategory) {
        if (activeTopTab == "anime") {
            dramas.filter { it.category == "Hoạt hình" }
        } else {
            // For "drama", we exclude "Hoạt hình" and show filtered list based on the categories
            dramas.filter { it.category != "Hoạt hình" }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F11)) // Immersive black background exactly like screenshot
    ) {
        // 1. Search Bar at the Top (Vietnamese standard layout)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search Input Box (Pill styled)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF222226)) // Grey rounded box
                        .clickable { onNavigateToSearch() }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = viewModel.getString("search_title"),
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = viewModel.getString("search_placeholder"),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Coin balance pill next to the search bar for quick navigation
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color(0xFF2C1E21)) // Reddish gold highlight
                        .clickable { onNavigateToLibrary() }
                        .border(1.dp, PrimaryCoral.copy(alpha = 0.3f), RoundedCornerShape(17.dp))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🪙",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${balance?.coins ?: 0}",
                        color = PrimaryGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("header_balance_text")
                    )
                }
            }
        }

        // 2. Main Top Tabs: "drama" & "anime"
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("drama", "anime").forEach { tabId ->
                    val isActive = activeTopTab == tabId
                    val tabLabel = if (tabId == "drama") viewModel.getString("tab_short_drama") else viewModel.getString("tab_animation")
                    
                    val textColor by animateColorAsState(
                        targetValue = if (isActive) Color.White else Color.White.copy(alpha = 0.45f),
                        animationSpec = tween(durationMillis = 250),
                        label = "tabTextColor"
                    )
                    val indicatorWidth by animateDpAsState(
                        targetValue = if (isActive) 36.dp else 0.dp,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f),
                        label = "tabIndicatorWidth"
                    )
                    val indicatorColor by animateColorAsState(
                        targetValue = if (isActive) Color.White else Color.Transparent,
                        animationSpec = tween(durationMillis = 200),
                        label = "tabIndicatorColor"
                    )
                    val tabScale by animateFloatAsState(
                        targetValue = if (isActive) 1.05f else 0.95f,
                        animationSpec = spring(stiffness = 500f),
                        label = "tabScale"
                    )

                    Column(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = tabScale
                                scaleY = tabScale
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { activeTopTab = tabId }
                            .padding(end = 24.dp, bottom = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tabLabel,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Active indicator line
                        Box(
                            modifier = Modifier
                                .width(indicatorWidth)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(1.2.dp))
                                .background(indicatorColor)
                        )
                    }
                }
            }
        }

        // 3. Horizontal Category Pills (Phổ biến, Mới nhất, Sắp ra mắt, Nữ giới, Nam giới)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val actualCategories = MockData.categories.filter { it != "Tất cả" }
                    items(actualCategories) { category ->
                        val isSelected = category == selectedCategory && activeTopTab == "drama"
                        val chipBg by animateColorAsState(
                            targetValue = if (isSelected) Color(0xFF2E2E35) else Color(0xFF1B1B1F),
                            animationSpec = tween(200),
                            label = "chipBg"
                        )
                        val chipText by animateColorAsState(
                            targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                            animationSpec = tween(200),
                            label = "chipText"
                        )
                        val chipScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.04f else 0.98f,
                            animationSpec = spring(stiffness = 600f),
                            label = "chipScale"
                        )

                        // Localize category labels beautifully based on language
                        val localizedCategory = when (category) {
                            "Phổ biến" -> viewModel.getString("pkg_popular").replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                            "Mới nhất" -> viewModel.getString("no_drama_category")
                            "Sắp ra mắt" -> viewModel.getString("category_upcoming")
                            "Nữ giới" -> viewModel.getString("category_female")
                            "Nam giới" -> viewModel.getString("category_male")
                            else -> category
                        }

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = chipScale
                                    scaleY = chipScale
                                }
                                .clip(RoundedCornerShape(6.dp)) // Rectangular with minor round corners like screenshot
                                .background(chipBg)
                                .clickable {
                                    activeTopTab = "drama" // Automatically focus back to drama when clicking categories
                                    viewModel.setCategory(category)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("category_chip_$category")
                        ) {
                            Text(
                                text = localizedCategory,
                                color = chipText,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Dropdown Arrow decoration on the right
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Xem thêm",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { /* Could toggle category view */ }
                )
            }
        }

        // 4. 3-Column Immersive Vertical Grid of Dramas
        item {
            AnimatedContent(
                targetState = activeTopTab,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, delayMillis = 80)) + 
                     androidx.compose.animation.scaleIn(initialScale = 0.96f, animationSpec = tween(220, delayMillis = 80))) togetherWith
                    fadeOut(animationSpec = tween(120))
                },
                label = "GridContentTransition"
            ) { targetTab ->
                val filteredList = remember(dramas, targetTab, selectedCategory) {
                    if (targetTab == "anime") {
                        dramas.filter { it.category == "Hoạt hình" }
                    } else {
                        dramas.filter { it.category != "Hoạt hình" }
                    }
                }

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🎬",
                                fontSize = 40.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = viewModel.getString("no_drama_found"),
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val chunks = filteredList.chunked(3)
                        for (chunk in chunks) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (drama in chunk) {
                                    Box(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        DramaGridCard(
                                            drama = drama,
                                            viewModel = viewModel,
                                            onClick = { onNavigateToDetail(drama.id) }
                                        )
                                    }
                                }
                                // Fill remaining columns if chunk is incomplete
                                val emptyCols = 3 - chunk.size
                                for (i in 0 until emptyCols) {
                                    Box(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom space to avoid navigation overlap
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}


