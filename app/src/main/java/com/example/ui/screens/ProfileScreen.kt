package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.shortdrama.R
import com.example.data.Drama
import com.example.data.UserProfile
import com.example.data.AppLanguage
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryGold
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.SurfaceVariant
import com.example.ui.theme.BorderColor
import com.example.ui.theme.GrayText
import com.example.viewmodel.DramaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: DramaViewModel,
    onNavigateToDetail: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val balance by viewModel.userBalance.collectAsStateWithLifecycle(initialValue = null)
    val favoritesList by viewModel.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    
    // Core authentication and localization flows from ViewModel
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val currentUserProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    // Local states for the login input
    var loginPhone by remember { mutableStateOf("") }
    var loginNickname by remember { mutableStateOf("") }
    var selectedMascotEmoji by remember { mutableStateOf("🦊") }
    
    // Edit profile state
    var isEditingName by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    
    // Dialog and settings states
    var showLanguageDialog by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var feedbackText by remember { mutableStateOf("") }

    // Collect top-up success flows to notify user with localization
    LaunchedEffect(viewModel) {
        viewModel.topUpStatus.collect { amount ->
            Toast.makeText(
                context, 
                viewModel.getString("toast_topup_success", amount.toString()), 
                Toast.LENGTH_LONG
            ).show()
        }
    }

    if (!isLoggedIn) {
        // --- 1. SUPER CUTE & LOVELY LOGIN SCREEN (CHƯA ĐĂNG NHẬP) ---
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F11)) // Immersive dark cinematic backdrop
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(30.dp))
                
                // Cute Logo and Heading
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = viewModel.getString("app_title"),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "🍿✨",
                        fontSize = 26.sp
                    )
                }
                
                Text(
                    text = viewModel.getString("app_subtitle"),
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // The super cute generated 3D fox mascot hero visual
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryCoral.copy(alpha = 0.3f), PrimaryGold.copy(alpha = 0.3f))
                            )
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_cute_login_hero_1783837722932),
                        contentDescription = "Cute Fox Mascot watching movie",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Cute small bubble overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryCoral)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Cute Fox 🦊",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Beautiful interactive form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = viewModel.getString("login_card_title"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Nickname field with cute icon
                    Column {
                        Text(
                            text = viewModel.getString("login_nickname_label"),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = loginNickname,
                            onValueChange = { loginNickname = it },
                            placeholder = { Text(viewModel.getString("login_nickname_placeholder"), fontSize = 12.sp, color = Color.Gray) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = "Nickname Icon",
                                    tint = PrimaryGold
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PrimaryCoral,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = SurfaceVariant,
                                unfocusedContainerColor = SurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_nickname_input")
                        )
                    }

                    // Phone number field
                    Column {
                        Text(
                            text = viewModel.getString("login_phone_label"),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = loginPhone,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) {
                                    loginPhone = input
                                }
                            },
                            placeholder = { Text(viewModel.getString("login_phone_placeholder"), fontSize = 12.sp, color = Color.Gray) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Phone Icon",
                                    tint = PrimaryCoral
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PrimaryCoral,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = SurfaceVariant,
                                unfocusedContainerColor = SurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_phone_input")
                        )
                    }

                    // Mascot list picker
                    Column {
                        Text(
                            text = "${viewModel.getString("login_mascot_label")}: $selectedMascotEmoji",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val emojis = listOf("🦊", "🐱", "🐼", "🐰", "🐯", "🐨", "🦄", "🐹", "🦁", "🐧")
                            items(emojis) { emoji ->
                                val isSelected = selectedMascotEmoji == emoji
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) PrimaryCoral.copy(alpha = 0.25f) else SurfaceVariant)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) PrimaryCoral else BorderColor,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedMascotEmoji = emoji }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emoji,
                                        fontSize = 22.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bouncy submit button
                    Button(
                        onClick = {
                            if (loginNickname.isBlank()) {
                                Toast.makeText(context, viewModel.getString("alert_nickname_empty"), Toast.LENGTH_SHORT).show()
                            } else if (loginPhone.length < 9) {
                                Toast.makeText(context, viewModel.getString("alert_phone_invalid"), Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.login(loginPhone, loginNickname, selectedMascotEmoji)
                                Toast.makeText(
                                    context, 
                                    viewModel.getString("alert_login_success", loginNickname), 
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = viewModel.getString("login_submit"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = viewModel.getString("login_security_notice"),
                    fontSize = 10.sp,
                    color = GrayText.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    } else {
        // --- 2. GORGEOUS REGISTERED DASHBOARD SCREEN (ĐÃ ĐĂNG NHẬP) ---
        val profile = currentUserProfile ?: UserProfile()

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F11))
                .padding(horizontal = 16.dp)
        ) {
            // 1. Header Title
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = viewModel.getString("profile_title"),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    // Cute sparkle tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryCoral.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = viewModel.getString("profile_online"),
                            color = PrimaryCoral,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. Personal Information Card with user's selected mascot and nickname
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Mascot Avatar with sparkling background gradient
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(PrimaryCoral, PrimaryGold)
                                    )
                                )
                                .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profile.avatarEmoji,
                                fontSize = 38.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Name, ID, VIP details
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isEditingName) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextField(
                                        value = inputName,
                                        onValueChange = { inputName = it },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = SurfaceVariant,
                                            unfocusedContainerColor = SurfaceVariant,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedIndicatorColor = PrimaryCoral,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("profile_name_input")
                                    )
                                    IconButton(
                                        onClick = {
                                            if (inputName.isNotBlank()) {
                                                viewModel.updateNickname(inputName)
                                                isEditingName = false
                                                Toast.makeText(context, viewModel.getString("toast_name_updated"), Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.testTag("save_name_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Save",
                                            tint = PrimaryGold
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = profile.nickname,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Sửa tên",
                                        tint = Color.Gray,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                inputName = profile.nickname
                                                isEditingName = true
                                            }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${viewModel.getString("profile_id")}: ${profile.id}",
                                fontSize = 12.sp,
                                color = GrayText
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // VIP Badge Indicator
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2C1E21))
                                    .border(0.5.dp, PrimaryCoral, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "VIP Icon",
                                        tint = PrimaryGold,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = viewModel.getString("profile_vip"),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Wallet / Coin Purse Card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF2E1C1F), DarkSurface),
                                radius = 600f
                            )
                        )
                        .border(1.dp, PrimaryCoral.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🪙",
                                    fontSize = 26.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = viewModel.getString("wallet_title"),
                                        fontSize = 12.sp,
                                        color = GrayText
                                    )
                                    Text(
                                        text = "${balance?.coins ?: 0} ${viewModel.getString("wallet_balance")}",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGold,
                                        modifier = Modifier.testTag("profile_coin_text")
                                    )
                                }
                            }

                            // Checkin button
                            Button(
                                onClick = { viewModel.performDailyCheckIn() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("profile_checkin_button")
                            ) {
                                Text(viewModel.getString("btn_checkin"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = BorderColor)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = viewModel.getString("coins_save_promo"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrayText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Top-up packages row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val packages = listOf(
                                Triple(100, "9.000đ", viewModel.getString("pkg_popular")),
                                Triple(500, "45.000đ", viewModel.getString("pkg_good_deal")),
                                Triple(1000, "89.000đ", viewModel.getString("pkg_super_cheap"))
                            )

                            packages.forEach { pkg ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceVariant)
                                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.topUpCoins(pkg.first) }
                                        .padding(8.dp)
                                        .testTag("topup_package_${pkg.first}"),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "+${pkg.first} ${viewModel.getString("wallet_balance")}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = pkg.second,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(PrimaryCoral.copy(alpha = 0.15f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = pkg.third.uppercase(),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryCoral
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Favorites List
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${viewModel.getString("my_movies")} (${favoritesList.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = viewModel.getString("liked"),
                        fontSize = 12.sp,
                        color = PrimaryCoral,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (favoritesList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎬", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = viewModel.getString("empty_favorites"),
                                fontSize = 12.sp,
                                color = GrayText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(favoritesList) { drama ->
                            Column(
                                modifier = Modifier
                                    .width(90.dp)
                                    .clickable { onNavigateToDetail(drama.id) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(90.dp)
                                        .aspectRatio(0.72f)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = drama.coverUrl,
                                        contentDescription = drama.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = drama.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 5. Settings, Controls, and Logout Row
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = viewModel.getString("settings_title"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrayText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                ) {
                    // Multi-Language Toggle Row (Supports all 14 languages)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLanguageDialog = true }
                            .padding(16.dp)
                            .testTag("language_selector_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Ngôn ngữ",
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = viewModel.getString("settings_lang"),
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${appLanguage.flag} ${appLanguage.displayName}",
                                fontSize = 13.sp,
                                color = PrimaryCoral,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.NavigateNext,
                                contentDescription = "Go",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Divider(color = BorderColor)

                    // Push Notification switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Thông báo",
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = viewModel.getString("settings_notify"),
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = {
                                notificationsEnabled = it
                                val toastMsg = if (it) viewModel.getString("toast_notify_on") else viewModel.getString("toast_notify_off")
                                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryCoral,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = SurfaceVariant
                            ),
                            modifier = Modifier.testTag("notification_switch")
                        )
                    }

                    Divider(color = BorderColor)

                    // Help and feedback button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHelpDialog = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Feedback,
                                contentDescription = "Góp ý",
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = viewModel.getString("settings_feedback"),
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.NavigateNext,
                            contentDescription = "Go",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Divider(color = BorderColor)

                    // 🛑 LOVELY LOGOUT BUTTON 🛑
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.logout()
                                Toast.makeText(context, viewModel.getString("alert_logout_success"), Toast.LENGTH_SHORT).show()
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Đăng xuất",
                                tint = PrimaryCoral
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = viewModel.getString("settings_logout"),
                                fontSize = 14.sp,
                                color = PrimaryCoral,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.NavigateNext,
                            contentDescription = "Go",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 6. Footer section (Version)
            item {
                Spacer(modifier = Modifier.height(30.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "FreeReels App v1.2.0-Production",
                        fontSize = 11.sp,
                        color = GrayText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.getString("copyright"),
                        fontSize = 10.sp,
                        color = GrayText.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    // Comprehensive 14 Languages Cute Picker Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Text(
                    text = viewModel.getString("settings_lang"),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
            },
            text = {
                Box(modifier = Modifier.heightIn(max = 350.dp)) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(AppLanguage.values()) { language ->
                            val isSelected = language == appLanguage
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PrimaryCoral.copy(alpha = 0.15f) else SurfaceVariant)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) PrimaryCoral else BorderColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        viewModel.setAppLanguage(language)
                                        showLanguageDialog = false
                                        Toast.makeText(
                                            context,
                                            viewModel.getString("toast_lang_changed", language.displayName),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = language.flag,
                                        fontSize = 24.sp
                                    )
                                    Text(
                                        text = language.displayName,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = PrimaryCoral,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showLanguageDialog = false }
                ) {
                    Text(viewModel.getString("dialog_feedback_cancel"), color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Help Center Feedback Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(
                    text = viewModel.getString("dialog_feedback_title"),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = viewModel.getString("dialog_feedback_desc"),
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        placeholder = { Text(viewModel.getString("dialog_feedback_placeholder"), fontSize = 12.sp, color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("feedback_text_field"),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryCoral,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = SurfaceVariant,
                            unfocusedContainerColor = SurfaceVariant
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (feedbackText.isNotBlank()) {
                            feedbackText = ""
                            showHelpDialog = false
                            Toast.makeText(context, viewModel.getString("toast_feedback_success"), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, viewModel.getString("toast_feedback_empty"), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                    modifier = Modifier.testTag("submit_feedback_button")
                ) {
                    Text(viewModel.getString("dialog_feedback_submit"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showHelpDialog = false }
                ) {
                    Text(viewModel.getString("dialog_feedback_cancel"), color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }
}
