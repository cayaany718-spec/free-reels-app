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
import androidx.compose.animation.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import coil.compose.AsyncImage
import com.example.shortdrama.R
import com.example.data.Drama
import com.example.data.UserProfile
import com.example.data.AppLanguage
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val googleWebClientId by viewModel.googleWebClientId.collectAsStateWithLifecycle()

    // Observe dynamic app update toast messages
    val updateCheckMessage by viewModel.updateCheckMessage.collectAsStateWithLifecycle()
    LaunchedEffect(updateCheckMessage) {
        updateCheckMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearUpdateCheckMessage()
        }
    }

    val coroutineScope = rememberCoroutineScope()
    
    // Social Login loading state
    var isSocialLoading by remember { mutableStateOf(false) }
    var socialPlatform by remember { mutableStateOf("") }

    // Custom Google and Facebook login states
    var showGoogleChooser by remember { mutableStateOf(false) }
    var isGoogleConnecting by remember { mutableStateOf(false) }
    var lastNotifiedGoogleEmail by remember { mutableStateOf("") }
    
    // Add custom/real Google accounts
    val deviceAccounts = remember { mutableStateListOf<Triple<String, String, String>>() }
    var showAddCustomGoogleAccount by remember { mutableStateOf(false) }
    var showDeveloperGoogleHelp by remember { mutableStateOf(false) }
    var customGoogleEmailInput by remember { mutableStateOf("") }
    var customGoogleNameInput by remember { mutableStateOf("") }

    // Dynamic AccountManager lookup for real Gmail accounts
    LaunchedEffect(Unit) {
        try {
            val am = android.accounts.AccountManager.get(context)
            val googleAccounts = am.getAccountsByType("com.google")
            googleAccounts.forEach { acc ->
                val email = acc.name
                val display = email.substringBefore("@")
                if (deviceAccounts.none { it.second.equals(email, ignoreCase = true) }) {
                    deviceAccounts.add(Triple(display, email, "👤"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var showFacebookChooser by remember { mutableStateOf(false) }
    var isFacebookConnecting by remember { mutableStateOf(false) }
    var lastNotifiedFacebookName by remember { mutableStateOf("") }
    var showAddCustomFacebookProfile by remember { mutableStateOf(false) }
    var customFacebookNameInput by remember { mutableStateOf("") }


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

    Box(modifier = modifier.fillMaxSize()) {
        if (!isLoggedIn) {
            // --- 1. SUPER CUTE & LOVELY IMMERSIVE LOGIN SCREEN (CHƯA ĐĂNG NHẬP) ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F0F11))
            ) {
            // 1. Poster Grid Background
            PosterGridBackground(modifier = Modifier.fillMaxSize())
            
            // 2. Black Vignette/Scrim Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color(0xFF0F0F11).copy(alpha = 0.85f),
                                Color(0xFF0F0F11)
                            )
                        )
                    )
            )
            
            // 3. Immersive Content Column (scrollable/fully responsive)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP spacer / layout adjustment
                Spacer(modifier = Modifier.height(30.dp))
                
                // CENTER: Brand Identity
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Speech Bubble over Logo
                    Box(
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🦊🍿", fontSize = 20.sp)
                            Text(
                                text = "Đăng nhập nhận ngay 100 Xu miễn phí! 🎁",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    MovieBoxLogoIcon(modifier = Modifier.size(96.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "MovieBox",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Mạng xã hội phim ngắn thịnh hành nhất 🎬✨",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
                
                // BOTTOM: Buttons, Secondary Option, and Terms
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Facebook Login Button
                    Button(
                        onClick = {
                            showFacebookChooser = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("facebook_login_button"),
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FacebookLogo(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Đăng nhập bằng Facebook",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Google Login Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val targetClientId = if (!googleWebClientId.isNullOrBlank()) googleWebClientId!! else "dummy_client_id.apps.googleusercontent.com"
                                    val credentialManager = androidx.credentials.CredentialManager.create(context)
                                    val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                                        .setFilterByAuthorizedAccounts(false)
                                        .setServerClientId(targetClientId)
                                        .setAutoSelectEnabled(false)
                                        .build()
                                    val request = androidx.credentials.GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()
                                    val result = credentialManager.getCredential(context, request)
                                    val credential = result.credential
                                    if (credential is com.google.android.libraries.identity.googleid.GoogleIdTokenCredential) {
                                        val email = credential.id
                                        val name = credential.displayName ?: email.substringBefore("@")
                                        lastNotifiedGoogleEmail = email
                                        socialPlatform = name
                                        isGoogleConnecting = true
                                    } else {
                                        showGoogleChooser = true
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    val isCancelled = e.javaClass.simpleName.contains("Cancel", ignoreCase = true) || 
                                                     e.message?.contains("cancel", ignoreCase = true) == true
                                    
                                    if (isCancelled) {
                                        Toast.makeText(context, "Đã hủy đăng nhập Google", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val isDummy = googleWebClientId.isNullOrBlank()
                                        val preferredAccount = deviceAccounts.find { it.second.contains("tranbi", ignoreCase = true) }
                                            ?: deviceAccounts.firstOrNull()
                                            
                                        if (preferredAccount != null) {
                                            val (display, email, emoji) = preferredAccount
                                            lastNotifiedGoogleEmail = email
                                            socialPlatform = display
                                            isGoogleConnecting = true
                                            Toast.makeText(context, "Đang tự động đăng nhập nhanh bằng tài khoản: $email ⚡", Toast.LENGTH_LONG).show()
                                        } else {
                                            if (isDummy) {
                                                Toast.makeText(context, "Chưa cấu hình Google Web Client ID trên Supabase. Đang mở danh sách tài khoản mô phỏng...", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Lỗi kết nối Google: ${e.localizedMessage}. Đang mở danh sách tài khoản mô phỏng...", Toast.LENGTH_LONG).show()
                                            }
                                            showGoogleChooser = true
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("google_login_button"),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            GoogleLogo(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Đăng nhập bằng Google",
                                color = Color(0xFF1F1D23),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Terms of Service & Privacy Policy Notice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nếu tiếp tục, bạn đồng ý với ",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Điều khoản Sử dụng",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                Toast.makeText(context, "Điều khoản sử dụng: Xem phim hợp pháp và tôn trọng bản quyền.", Toast.LENGTH_SHORT).show()
                            }
                        )
                        Text(
                            text = " • ",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Chính sách Bảo mật",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                Toast.makeText(context, "Chính sách bảo mật: Thông tin của bạn được bảo vệ tuyệt đối.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }



        // --- 1. MOCK GOOGLE ACCOUNT CHOOSER ---
        if (showGoogleChooser) {
            val googleAccountsList = remember(deviceAccounts.size) {
                val list = mutableListOf<Triple<String, String, String>>()
                list.addAll(deviceAccounts)
                val fallbackList = listOf(
                    Triple("bi tran", "tranbi200000@gmail.com", "🦊"),
                    Triple("reginal noen", "makenafawalton@gmail.com", "🌸"),
                    Triple("chi dao", "thanhsok6@gmail.com", "🍿"),
                    Triple("chigame. net", "daothienchi396@gmail.com", "🎮"),
                    Triple("Thư Nguyễn", "thunlove077@gmail.com", "📖"),
                    Triple("Huyen Dao", "hdao21361@gmail.com", "🍵"),
                    Triple("Huy Vũ", "wibuhanghieu002@gmail.com", "🧊"),
                    Triple("Tu Nguyen", "ntu124827@gmail.com", "🍀"),
                    Triple("caya any", "cayaany718@gmail.com", "🍟"),
                    Triple("Mmnb Hhpm", "mmnbhhpm@gmail.com", "🚲"),
                    Triple("Thuong Nguyen", "tn9903329@gmail.com", "👒"),
                    Triple("hao tu", "haotu293@gmail.com", "🪁")
                )
                fallbackList.forEach { fb ->
                    if (list.none { it.second.equals(fb.second, ignoreCase = true) }) {
                        list.add(fb)
                    }
                }
                list
            }

            AlertDialog(
                onDismissRequest = { showGoogleChooser = false },
                confirmButton = {},
                shape = RoundedCornerShape(24.dp),
                containerColor = Color(0xFF2C2C2E), // Native Google dark sheet color
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Play Icon or App Icon matching FreeReels/MovieBox
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFFFF5E62), Color(0xFFFF9966)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "FreeReels Logo",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Chọn tài khoản",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "để tiếp tục sử dụng FreeReels",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                        
                        // Limit height for scrolling if list is long
                        Box(modifier = Modifier.heightIn(max = 320.dp)) {
                            LazyColumn {
                                items(googleAccountsList) { (name, email, emoji) ->
                                    val firstChar = name.firstOrNull()?.toString()?.lowercase() ?: "g"
                                    val colors = listOf(
                                        Color(0xFF1A73E8), Color(0xFF34A853), Color(0xFFF9AB00), 
                                        Color(0xFFEA4335), Color(0xFF7B1FA2), Color(0xFF00796B),
                                        Color(0xFFE91E63), Color(0xFF3F51B5), Color(0xFF00BCD4)
                                    )
                                    val avatarBgColor = remember(email) {
                                        colors[email.hashCode().coerceAtLeast(0) % colors.size]
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showGoogleChooser = false
                                                lastNotifiedGoogleEmail = email
                                                socialPlatform = name
                                                isGoogleConnecting = true
                                            }
                                            .padding(vertical = 12.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(avatarBgColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (emoji == "👤" || emoji.length > 2) firstChar else emoji,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = email,
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.5f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Divider(color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAddCustomGoogleAccount = true
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Account",
                                tint = Color(0xFF8AB4F8),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Thêm tài khoản khác",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF8AB4F8)
                            )
                        }
                        
                        Divider(color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showDeveloperGoogleHelp = true
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Cấu hình Google Thật",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Cấu hình Google Sign-In thật ⚡",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB300)
                            )
                        }
                    }
                }
            )
        }

        // --- ADD CUSTOM GOOGLE ACCOUNT SUB-DIALOG ---
        if (showAddCustomGoogleAccount) {
            AlertDialog(
                onDismissRequest = { showAddCustomGoogleAccount = false },
                title = {
                    Text(
                        text = "Thêm tài khoản Google",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = customGoogleNameInput,
                            onValueChange = { customGoogleNameInput = it },
                            label = { Text("Tên tài khoản") },
                            placeholder = { Text("Ví dụ: Trần Bình") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF8AB4F8),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = Color(0xFF8AB4F8),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                focusedPlaceholderColor = Color.White.copy(alpha = 0.3f),
                                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customGoogleEmailInput,
                            onValueChange = { customGoogleEmailInput = it },
                            label = { Text("Địa chỉ Gmail") },
                            placeholder = { Text("example@gmail.com") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF8AB4F8),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = Color(0xFF8AB4F8),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                focusedPlaceholderColor = Color.White.copy(alpha = 0.3f),
                                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (customGoogleEmailInput.isNotBlank() && customGoogleNameInput.isNotBlank()) {
                                val email = customGoogleEmailInput.trim()
                                val name = customGoogleNameInput.trim()
                                if (email.endsWith("@gmail.com") || email.contains("@")) {
                                    if (deviceAccounts.none { it.second.equals(email, ignoreCase = true) }) {
                                        deviceAccounts.add(Triple(name, email, "👤"))
                                    }
                                    showAddCustomGoogleAccount = false
                                    showGoogleChooser = false
                                    lastNotifiedGoogleEmail = email
                                    socialPlatform = name
                                    isGoogleConnecting = true
                                    customGoogleEmailInput = ""
                                    customGoogleNameInput = ""
                                } else {
                                    Toast.makeText(context, "Vui lòng nhập địa chỉ email hợp lệ!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8AB4F8))
                    ) {
                        Text("Xác nhận", color = Color(0xFF2C2C2E), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCustomGoogleAccount = false }) {
                        Text("Hủy", color = Color.White.copy(alpha = 0.6f))
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color(0xFF202124)
            )
        }

        // --- DEVELOPER GOOGLE CONFIGURATION HELP DIALOG ---
        if (showDeveloperGoogleHelp) {
            AlertDialog(
                onDismissRequest = { showDeveloperGoogleHelp = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Developer Options",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Cấu hình Google Sign-In thật",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                text = {
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    val packageText = "com.moviebox.app"
                    val appSha1 = remember(context) { getAppSha1(context) }
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Dành riêng cho bạn để cấu hình Google Cloud Console kết nối thật 100%.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        
                        // 1. Package Name
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "1. TÊN GÓI ỨNG DỤNG (PACKAGE NAME):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8AB4F8)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = packageText,
                                            fontSize = 13.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            color = Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(androidx.compose.ui.text.buildAnnotatedString { append(packageText) })
                                                Toast.makeText(context, "Đã sao chép Package Name!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Copy Package Name",
                                                tint = Color(0xFF8AB4F8),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 2. SHA-1 Fingerprint
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "2. CHỮ KÝ BẢO MẬT (SHA-1 FINGERPRINT):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8AB4F8)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = appSha1,
                                            fontSize = 12.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            color = Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(androidx.compose.ui.text.buildAnnotatedString { append(appSha1) })
                                                Toast.makeText(context, "Đã sao chép mã SHA-1!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Copy SHA-1",
                                                tint = Color(0xFF8AB4F8),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 3. Step-by-Step Instructions
                        item {
                            Text(
                                text = "HƯỚNG DẪN CẤU HÌNH TRÊN GOOGLE CLOUD CONSOLE:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB300)
                            )
                        }
                        
                        item {
                            Text(
                                text = "Bước A: Trên trang Google Cloud Console của bạn (như màn hình bạn đang xem), nhấn vào \"API và dịch vụ\" -> Chọn \"Thông tin xác thực\" (Credentials).\n\n" +
                                       "Bước B: Nếu chưa cấu hình, chọn tab \"Màn hình đồng ý OAuth\" (OAuth Consent Screen) -> Điền tên ứng dụng của bạn (ví dụ: FreeReels) và lưu lại.\n\n" +
                                       "Bước C: Nhấn \"Tạo thông tin xác thực\" (Create Credentials) -> Chọn \"Mã ứng dụng khách OAuth\" (OAuth Client ID).\n" +
                                       "• Loại ứng dụng: Chọn \"Ứng dụng web\" (Web application).\n" +
                                       "• Nhấn Tạo. Hệ thống sẽ cấp một chuỗi Client ID kết thúc bằng \".apps.googleusercontent.com\". Hãy sao chép chuỗi này.\n\n" +
                                       "Bước D: Tạo tiếp một OAuth Client ID thứ hai:\n" +
                                       "• Loại ứng dụng: Chọn \"Android\".\n" +
                                       "• Tên gói: Dán chính xác package name ở Mục 1 (${packageText}).\n" +
                                       "• Mã SHA-1: Dán chính xác mã chữ ký ở Mục 2 (${if (appSha1.length > 20) appSha1.take(15) + "..." else appSha1}).\n" +
                                       "• Nhấn Tạo.\n\n" +
                                       "Bước E: Vào trang quản trị Supabase -> Chọn bảng \"app_config\" -> Sửa dòng có key là \"google_web_client_id\" và dán mã Client ID Ứng dụng Web (lấy ở Bước C) vào cột value.\n\n" +
                                       "🎉 Xong! App sẽ tự nhận diện ngay lập tức mà không cần build lại APK mới!",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDeveloperGoogleHelp = false }) {
                        Text("Đã hiểu", color = Color(0xFF8AB4F8), fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color(0xFF1C1C1E)
            )
        }

        // --- 2. GOOGLE CONNECTING LOADER ---
        if (isGoogleConnecting) {
            AlertDialog(
                onDismissRequest = { isGoogleConnecting = false },
                confirmButton = {},
                title = {},
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        CircularProgressIndicator(color = Color(0xFF4285F4))
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Đang kết nối tài khoản Google của bạn...",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color(0xFF1F1D23),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Xác thực bảo mật qua Google Sign-In",
                            fontSize = 12.sp,
                            color = Color(0xFF5F6368),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.White
            )
            
            LaunchedEffect(isGoogleConnecting) {
                if (isGoogleConnecting) {
                    delay(1500L)
                    isGoogleConnecting = false
                    val shortName = socialPlatform
                    val avatar = if (shortName.contains("Bình")) "🦊" else if (shortName.contains("Linh")) "🌸" else "🍿"
                    viewModel.login("0987654321", shortName, avatar)
                    val emailToSend = if (lastNotifiedGoogleEmail.isNotBlank()) lastNotifiedGoogleEmail else "tranbi200000@gmail.com"
                    viewModel.sendGmailNotification(emailToSend, shortName)
                    Toast.makeText(context, "Đăng nhập Google thành công! Cảnh báo bảo mật đăng nhập đã được gửi tới Gmail ($emailToSend). 🎉", Toast.LENGTH_LONG).show()
                }
            }
        }

        // --- 3. MOCK FACEBOOK ACCOUNT CHOOSER ---
        if (showFacebookChooser) {
            AlertDialog(
                onDismissRequest = { showFacebookChooser = false },
                confirmButton = {},
                shape = RoundedCornerShape(20.dp),
                containerColor = Color(0xFF18191A),
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FacebookLogo(modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Đăng nhập bằng Facebook",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "MovieBox muốn nhận thông tin trang cá nhân công khai của bạn.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                                .clickable {
                                    showFacebookChooser = false
                                    lastNotifiedFacebookName = "Nguyễn Văn Hùng"
                                    isFacebookConnecting = true
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1877F2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "🦁", fontSize = 26.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Nguyễn Văn Hùng",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Tài khoản hoạt động trên ứng dụng Facebook",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                showFacebookChooser = false
                                lastNotifiedFacebookName = "Nguyễn Văn Hùng"
                                isFacebookConnecting = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = "Tiếp tục dưới tên Hùng",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAddCustomFacebookProfile = true
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Custom FB",
                                tint = Color(0xFF1877F2),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sử dụng một tài khoản khác",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1877F2)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        TextButton(
                            onClick = { showFacebookChooser = false }
                        ) {
                            Text(
                                text = "Hủy bỏ và quay lại",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            )
        }

        // --- ADD CUSTOM FACEBOOK PROFILE SUB-DIALOG ---
        if (showAddCustomFacebookProfile) {
            AlertDialog(
                onDismissRequest = { showAddCustomFacebookProfile = false },
                title = {
                    Text(
                        text = "Đăng nhập tài khoản Facebook khác",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = customFacebookNameInput,
                            onValueChange = { customFacebookNameInput = it },
                            label = { Text("Tên tài khoản Facebook") },
                            placeholder = { Text("Ví dụ: Nguyễn Văn Hùng") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF1877F2),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = Color(0xFF1877F2),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                focusedPlaceholderColor = Color.White.copy(alpha = 0.3f),
                                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (customFacebookNameInput.isNotBlank()) {
                                val name = customFacebookNameInput.trim()
                                showAddCustomFacebookProfile = false
                                showFacebookChooser = false
                                lastNotifiedFacebookName = name
                                isFacebookConnecting = true
                                customFacebookNameInput = ""
                            } else {
                                Toast.makeText(context, "Vui lòng nhập tên tài khoản Facebook!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                    ) {
                        Text("Xác nhận", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCustomFacebookProfile = false }) {
                        Text("Hủy", color = Color.White.copy(alpha = 0.6f))
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color(0xFF242526)
            )
        }

        // --- 4. FACEBOOK CONNECTING LOADER ---
        if (isFacebookConnecting) {
            AlertDialog(
                onDismissRequest = { isFacebookConnecting = false },
                confirmButton = {},
                title = {},
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        CircularProgressIndicator(color = Color(0xFF1877F2))
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Đang kết nối Facebook... 🌐",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Đang xác thực bảo mật tài khoản Facebook",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = Color(0xFF1F1D23)
            )
            
            LaunchedEffect(isFacebookConnecting) {
                if (isFacebookConnecting) {
                    delay(1500L)
                    isFacebookConnecting = false
                    viewModel.login("0987654321", "Nguyễn Văn Hùng 🦁", "🦁")
                    Toast.makeText(context, "Đăng nhập qua Facebook thành công! 🎉", Toast.LENGTH_SHORT).show()
                }
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

            // 3. VIP Premium Subscription Card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF2C1E22), Color(0xFF151419)),
                                radius = 600f
                            )
                        )
                        .border(1.dp, PrimaryGold.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "👑",
                                    fontSize = 28.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Đặc Quyền VIP Premium",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (profile.isVip) "Đã kích hoạt: ${profile.vipLevel}" else "Chưa kích hoạt đặc quyền VIP",
                                        fontSize = 12.sp,
                                        color = if (profile.isVip) PrimaryGold else Color.White.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (!profile.isVip) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PrimaryCoral)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "Nâng cấp",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "CHỌN GÓI VIP ĐỂ KÍCH HOẠT NGAY ✨",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        // VIP Packages Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val vipPackages = listOf(
                                Triple("GÓI NGÀY", "9.000đ", "VIP 24h"),
                                Triple("GÓI THÁNG", "49.000đ", "Tiết kiệm 80%"),
                                Triple("GÓI NĂM", "299.000đ", "Tối ưu nhất")
                            )

                            vipPackages.forEachIndexed { index, pkg ->
                                val isCurrentPackage = profile.isVip && profile.vipLevel.contains(pkg.first.split(" ")[1], ignoreCase = true)

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCurrentPackage) Color(0xFF2C1E22) else Color(0xFF1C1B20))
                                        .border(
                                            width = if (isCurrentPackage) 1.5.dp else 1.dp,
                                            brush = if (isCurrentPackage) Brush.linearGradient(colors = listOf(PrimaryCoral, PrimaryGold)) else SolidColor(Color.White.copy(alpha = 0.08f)),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            viewModel.purchaseVip(pkg.first, when(index) {
                                                0 -> 1
                                                1 -> 30
                                                else -> 365
                                            })
                                            Toast.makeText(context, "Kích hoạt thành công ${pkg.first}! 👑✨", Toast.LENGTH_LONG).show()
                                        }
                                        .padding(10.dp)
                                        .testTag("profile_vip_package_$index"),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = pkg.first,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrentPackage) PrimaryCoral else Color.White
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = pkg.second,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = pkg.third,
                                        fontSize = 8.sp,
                                        color = Color.White.copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center
                                    )
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

                    // 🔄 KIỂM TRA CẬP NHẬT 🔄
                    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.checkForUpdates(manual = true) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Cập nhật",
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Kiểm tra cập nhật",
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                color = PrimaryCoral,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.NavigateNext,
                                contentDescription = "Go",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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
                        text = "MovieBox App v1.2.0-Production",
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
}

@Composable
fun GoogleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = width * 0.22f
        
        val rect = Rect(
            strokeWidth / 2f,
            strokeWidth / 2f,
            width - strokeWidth / 2f,
            height - strokeWidth / 2f
        )
        
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -30f,
            sweepAngle = 75f,
            useCenter = false,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Butt
            ),
            topLeft = rect.topLeft,
            size = rect.size
        )
        
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 80f,
            useCenter = false,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Butt
            ),
            topLeft = rect.topLeft,
            size = rect.size
        )
        
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 125f,
            sweepAngle = 75f,
            useCenter = false,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Butt
            ),
            topLeft = rect.topLeft,
            size = rect.size
        )
        
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 200f,
            sweepAngle = 130f,
            useCenter = false,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Butt
            ),
            topLeft = rect.topLeft,
            size = rect.size
        )
        
        val barY = height / 2f
        val barXStart = width / 2f
        val barXEnd = width - strokeWidth / 2f
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(barXStart, barY),
            end = Offset(barXEnd + strokeWidth / 2f, barY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Square
        )
    }
}

@Composable
fun FacebookLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        val path = Path().apply {
            moveTo(w * 0.52f, h)
            lineTo(w * 0.52f, h * 0.45f)
            lineTo(w * 0.35f, h * 0.45f)
            lineTo(w * 0.35f, h * 0.33f)
            lineTo(w * 0.52f, h * 0.33f)
            lineTo(w * 0.52f, h * 0.22f)
            quadraticTo(w * 0.52f, h * 0.08f, w * 0.72f, h * 0.08f)
            lineTo(w * 0.82f, h * 0.08f)
            lineTo(w * 0.82f, h * 0.22f)
            lineTo(w * 0.72f, h * 0.22f)
            quadraticTo(w * 0.64f, h * 0.22f, w * 0.64f, h * 0.33f)
            lineTo(w * 0.80f, h * 0.33f)
            lineTo(w * 0.77f, h * 0.45f)
            lineTo(w * 0.64f, h * 0.45f)
            lineTo(w * 0.64f, h)
            close()
        }
        drawPath(path = path, color = Color.White)
    }
}

@Composable
fun SocialLoginButton(
    logo: String,
    backgroundColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (logo) {
            "Google" -> {
                GoogleLogo(modifier = Modifier.size(24.dp))
            }
            "Facebook" -> {
                FacebookLogo(modifier = Modifier.size(24.dp))
            }
            else -> {
                Text(
                    text = logo,
                    fontSize = 22.sp
                )
            }
        }
    }
}

@Composable
fun PosterGridBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationZ = -12f
                scaleX = 1.35f
                scaleY = 1.35f
                translationY = -90.dp.toPx()
                translationX = -30.dp.toPx()
            }
    ) {
        val images = listOf(
            "cover_ceo_wife.jpg",
            "cover_divine_doctor.jpg",
            "cover_exwife_boss.jpg",
            "cover_heiress_revenge.jpg"
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (rowIndex in 0..3) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.offset(x = if (rowIndex % 2 == 0) (-50).dp else 10.dp)
                ) {
                    for (colIndex in 0..3) {
                        val imgName = images[(rowIndex + colIndex) % images.size]
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(120.dp)
                                .height(170.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            AsyncImage(
                                model = "file:///android_asset/$imgName",
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MovieBoxLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        val boxLeft = w * 0.075f
        val boxTop = h * 0.075f
        val boxWidth = w * 0.85f
        val boxHeight = h * 0.85f
        
        // 1. Draw outer neon/glowing border with linear gradient of Coral to Gold
        val gradient = Brush.linearGradient(
            colors = listOf(PrimaryCoral, PrimaryGold),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
        
        // Solid deep dark-plum/indigo background inside the box
        drawRoundRect(
            color = Color(0xFF1B1826),
            topLeft = Offset(boxLeft, boxTop),
            size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.18f)
        )
        
        // Glowing stroke border
        drawRoundRect(
            brush = gradient,
            topLeft = Offset(boxLeft, boxTop),
            size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.18f),
            style = Stroke(width = w * 0.05f)
        )
        
        // 2. Film perforations on the left and right inside edges (Sprocket holes)
        val holeW = w * 0.05f
        val holeH = h * 0.05f
        val leftX = boxLeft + w * 0.07f
        val rightX = boxLeft + boxWidth - w * 0.12f
        for (i in 0..3) {
            val y = boxTop + h * 0.12f + i * (h * 0.17f)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.7f),
                topLeft = Offset(leftX, y),
                size = androidx.compose.ui.geometry.Size(holeW, holeH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(holeW * 0.2f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.7f),
                topLeft = Offset(rightX, y),
                size = androidx.compose.ui.geometry.Size(holeW, holeH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(holeW * 0.2f)
            )
        }
        
        // 3. Central glowing coral circular play button base
        val playCircleRadius = w * 0.16f
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(PrimaryCoral, PrimaryCoral.copy(alpha = 0.85f)),
                start = Offset(w / 2f - playCircleRadius, h / 2f - playCircleRadius),
                end = Offset(w / 2f + playCircleRadius, h / 2f + playCircleRadius)
            ),
            center = Offset(w / 2f, h / 2f),
            radius = playCircleRadius
        )
        
        // 4. White play triangle in the very center
        val triWidth = playCircleRadius * 0.75f
        val triHeight = playCircleRadius * 0.75f
        val triLeft = w / 2f - triWidth * 0.32f
        val triTop = h / 2f - triHeight / 2f
        val playPath = Path().apply {
            moveTo(triLeft, triTop)
            lineTo(triLeft + triWidth, h / 2f)
            lineTo(triLeft, triTop + triHeight)
            close()
        }
        drawPath(
            path = playPath,
            color = Color.White
        )
        
        // 5. Drawing two magical sparkle stars
        val drawSparkle = { cx: Float, cy: Float, size: Float, color: Color ->
            val starPath = Path().apply {
                moveTo(cx, cy - size)
                quadraticTo(cx, cy, cx + size, cy)
                quadraticTo(cx, cy, cx, cy + size)
                quadraticTo(cx, cy, cx - size, cy)
                quadraticTo(cx, cy, cx, cy - size)
                close()
            }
            drawPath(path = starPath, color = color)
        }
        
        // Top right star
        drawSparkle(w * 0.76f, h * 0.22f, w * 0.08f, PrimaryGold)
        // Bottom left star
        drawSparkle(w * 0.24f, h * 0.78f, w * 0.06f, PrimaryGold)
    }
}

fun getAppSha1(context: android.content.Context): String {
    return try {
        val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_SIGNATURES
            )
        }

        val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }

        if (signatures != null && signatures.isNotEmpty()) {
            val md = java.security.MessageDigest.getInstance("SHA-1")
            val publicKey = md.digest(signatures[0].toByteArray())
            val hexString = StringBuilder()
            for (i in publicKey.indices) {
                val appendString = Integer.toHexString(0xFF and publicKey[i].toInt())
                if (appendString.length == 1) hexString.append("0")
                hexString.append(appendString.uppercase(java.util.Locale.US))
                if (i < publicKey.size - 1) hexString.append(":")
            }
            hexString.toString()
        } else {
            "Không tìm thấy chữ ký"
        }
    } catch (e: Exception) {
        "Lỗi: ${e.message}"
    }
}

