package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryGold
import kotlinx.coroutines.delay
import kotlin.random.Random

// Representing a floating particle
data class CuteParticle(
    val id: Int,
    val initialX: Float, // percentage across screen (0f .. 1f)
    val speed: Float,
    val size: Float,
    val isHeart: Boolean
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Loading states
    var progress by remember { mutableStateOf(0f) }
    var loadingTextIndex by remember { mutableStateOf(0) }
    
    val loadingTexts = listOf(
        "Đang nạp năng lượng dễ thương... 🔋✨",
        "Đang sắp xếp bắp rang bơ siêu ngọt... 🍿💖",
        "Chuẩn bị rạp phim mini siêu xịn... 🎬🧸",
        "Đang tìm kiếm những thước phim ngọt ngào nhất... 🌟🎀",
        "Chào mừng bạn đến với MovieBox! 🥰💫"
    )

    // Cycle text every 800ms
    LaunchedEffect(Unit) {
        while (progress < 1.0f) {
            delay(800)
            if (progress < 1.0f) {
                loadingTextIndex = (loadingTextIndex + 1) % loadingTexts.size
            }
        }
    }

    // Smoothly simulate progress bar loading over ~2.5 seconds
    LaunchedEffect(Unit) {
        val steps = listOf(0.1f, 0.25f, 0.45f, 0.65f, 0.85f, 1.0f)
        for (step in steps) {
            val waitTime = Random.nextLong(250, 450)
            delay(waitTime)
            progress = step
        }
        delay(300) // Small pause at 100% for satisfaction
        onNavigateToHome()
    }

    // 2. Beautiful Infinite Animations
    val infiniteTransition = rememberInfiniteTransition(label = "cute_loop")

    // Mascot bouncing animation
    val mascotBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -18f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_bounce"
    )

    // Mascot breathing/scale animation
    val mascotScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_scale"
    )

    // Swaying antenna animation
    val antennaSway by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "antenna_sway"
    )

    // Rotating star badge animation
    val starRotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "star_rotate"
    )

    // Background cosmic aura pulsing
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_scale"
    )

    // 3. Floating particles (Hearts & Stars) drifting up
    val particles = remember {
        List(15) { index ->
            CuteParticle(
                id = index,
                initialX = Random.nextFloat(),
                speed = Random.nextFloat() * 0.15f + 0.1f,
                size = Random.nextFloat() * 12f + 8f,
                isHeart = Random.nextBoolean()
            )
        }
    }

    // Particle Y transition
    val particleOffsetMultiplier by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = -0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_y"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("splash_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        // --- DECORATIVE AMBIENT AURAS ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Central radial soft pink glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PrimaryCoral.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.6f * auraScale
                        )
                    )
                    // Bottom gold neon soft glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PrimaryGold.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height),
                            radius = size.minDimension * 0.5f
                        )
                    )
                }
        )

        // --- DRIFTING FLOATING PARTICLES ---
        particles.forEach { particle ->
            val animatedY = (particleOffsetMultiplier + (particle.id * 0.06f)) % 1.2f
            val alpha = if (animatedY < 0.2f) animatedY / 0.2f else if (animatedY > 0.8f) (1f - animatedY) / 0.2f else 1f
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val px = particle.initialX * size.width
                        val py = (1f - animatedY) * size.height
                        
                        // Draw custom tiny star or heart
                        if (particle.isHeart) {
                            // Simple heart shape drawing
                            val path = Path().apply {
                                val radius = particle.size
                                moveTo(px, py)
                                cubicTo(px - radius, py - radius, px - radius * 2, py + radius / 2, px, py + radius * 2)
                                cubicTo(px + radius * 2, py + radius / 2, px + radius, py - radius, px, py)
                                close()
                            }
                            drawPath(path, color = PrimaryCoral.copy(alpha = alpha * 0.4f))
                        } else {
                            // Simple star drawing
                            val path = Path().apply {
                                val r = particle.size
                                val innerR = r / 2.5f
                                for (i in 0..10) {
                                    val angle = i * Math.PI / 5 - Math.PI / 2
                                    val currR = if (i % 2 == 0) r else innerR
                                    val x = px + (currR * kotlin.math.cos(angle)).toFloat()
                                    val y = py + (currR * kotlin.math.sin(angle)).toFloat()
                                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                                }
                                close()
                            }
                            drawPath(path, color = PrimaryGold.copy(alpha = alpha * 0.4f))
                        }
                    }
            )
        }

        // --- MAIN CONTENT AREA ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            
            // --- CUTE MASCOT PANEL ---
            Box(
                modifier = Modifier
                    .height(240.dp)
                    .width(220.dp)
                    .offset(y = mascotBounce.dp)
                    .scale(mascotScale),
                contentAlignment = Alignment.Center
            ) {
                // Background cute glow rings
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = PrimaryCoral.copy(alpha = 0.06f),
                        radius = 110.dp.toPx()
                    )
                    drawCircle(
                        color = PrimaryGold.copy(alpha = 0.04f),
                        radius = 130.dp.toPx(),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // 1. Antenna with sparkling star on top
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = (-75).dp)
                        .rotate(antennaSway),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Golden star tip
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = PrimaryGold,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(starRotate)
                        )
                        // Little metal rod
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(16.dp)
                                .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                        )
                    }
                }

                // 2. Bunny/Cat Ears behind the TV head
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-55).dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left cute ear
                    Box(
                        modifier = Modifier
                            .padding(start = 36.dp)
                            .width(36.dp)
                            .height(55.dp)
                            .rotate(-20f)
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                            .background(PrimaryCoral)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                            .background(Color(0xFFFFB6C1)) // Cute pink inside ear
                    )
                    // Right cute ear
                    Box(
                        modifier = Modifier
                            .padding(end = 36.dp)
                            .width(36.dp)
                            .height(55.dp)
                            .rotate(20f)
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                            .background(PrimaryCoral)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                            .background(Color(0xFFFFB6C1))
                    )
                }

                // 3. The TV/Reels cute body cabinet
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(PrimaryCoral, Color(0xFFFF6B81))
                            )
                        )
                        .border(3.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(36.dp))
                        .padding(12.dp)
                ) {
                    // Inner screen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color(0xFF231E24)) // Dark warm screen
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing the smiling anime eyes and rosy cheeks inside screen
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Rosy Blush cheeks (left/right)
                            drawCircle(
                                color = Color(0xFFFF69B4).copy(alpha = 0.7f),
                                radius = 9.dp.toPx(),
                                center = Offset(w * 0.18f, h * 0.65f)
                            )
                            drawCircle(
                                color = Color(0xFFFF69B4).copy(alpha = 0.7f),
                                radius = 9.dp.toPx(),
                                center = Offset(w * 0.82f, h * 0.65f)
                            )

                            // Cute smiling eyes: curved lines (happy arcs)
                            val eyePathLeft = Path().apply {
                                val leftX = w * 0.22f
                                val rightX = w * 0.38f
                                val centerY = h * 0.45f
                                moveTo(leftX, centerY)
                                cubicTo(leftX + 5.dp.toPx(), centerY - 8.dp.toPx(), rightX - 5.dp.toPx(), centerY - 8.dp.toPx(), rightX, centerY)
                            }
                            drawPath(
                                path = eyePathLeft,
                                color = Color.White,
                                style = Stroke(width = 3.5f.dp.toPx(), cap = StrokeCap.Round)
                            )

                            val eyePathRight = Path().apply {
                                val leftX = w * 0.62f
                                val rightX = w * 0.78f
                                val centerY = h * 0.45f
                                moveTo(leftX, centerY)
                                cubicTo(leftX + 5.dp.toPx(), centerY - 8.dp.toPx(), rightX - 5.dp.toPx(), centerY - 8.dp.toPx(), rightX, centerY)
                            }
                            drawPath(
                                path = eyePathRight,
                                color = Color.White,
                                style = Stroke(width = 3.5f.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Super cute anime mouth: w-shape or tiny open tongue
                            val mouthPath = Path().apply {
                                val midX = w * 0.5f
                                val midY = h * 0.68f
                                moveTo(midX - 12.dp.toPx(), midY - 2.dp.toPx())
                                quadraticTo(midX - 6.dp.toPx(), midY + 4.dp.toPx(), midX, midY)
                                quadraticTo(midX + 6.dp.toPx(), midY + 4.dp.toPx(), midX + 12.dp.toPx(), midY - 2.dp.toPx())
                            }
                            drawPath(
                                path = mouthPath,
                                color = Color.White,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Central shining heart symbol representing "Reels Love"
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = PrimaryCoral,
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.BottomCenter)
                                .offset(y = (-4).dp)
                                .scale(mascotScale)
                        )
                    }
                }

                // Tiny glowing ribbon bow tie at the bottom of the mascot
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-15).dp)
                        .width(42.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(PrimaryGold)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- APP NAME WITH GORGEOUS GLOW ---
            Text(
                text = "MovieBox",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .testTag("splash_app_title")
                    .drawBehind {
                        // Beautiful shadow glow text backing
                        drawCircle(
                            color = PrimaryCoral.copy(alpha = 0.25f),
                            radius = 45.dp.toPx(),
                            center = center
                        )
                    }
            )

            Text(
                text = "Phim ngắn hay • Cực dễ thương",
                fontSize = 13.sp,
                color = PrimaryGold,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 48.dp)
            )

            // --- PROGRESS BAR WITH SLIDING HEART ---
            val progressAnimated by animateFloatAsState(
                targetValue = progress,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 120f),
                label = "progress_anim"
            )

            Column(
                modifier = Modifier
                    .width(260.dp)
                    .testTag("splash_progress_section"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Row container for heart and bar to track alignment
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    // Track path background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                    )

                    // Filled path with beautiful rainbow coral gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressAnimated)
                            .height(10.dp)
                            .align(Alignment.CenterStart)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        PrimaryGold,
                                        PrimaryCoral,
                                        Color(0xFFFF69B4)
                                    )
                                )
                            )
                    )

                    // Sliding heart pointer on top of progress line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressAnimated)
                            .align(Alignment.TopStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFFF69B4),
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 10.dp, y = (-2).dp)
                                .scale(mascotScale * 1.1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Percentage loading counter
                Text(
                    text = "${(progressAnimated * 100).toInt()}%",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Beautifully animated typing text carousel
                Box(
                    modifier = Modifier.height(45.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = loadingTextIndex,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) + slideInVertically(
                                initialOffsetY = { 20 }
                            ) togetherWith fadeOut(
                                animationSpec = tween(300)
                            ) + slideOutVertically(
                                targetOffsetY = { -20 }
                            )
                        },
                        label = "loading_text_switch"
                    ) { index ->
                        Text(
                            text = loadingTexts[index],
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
