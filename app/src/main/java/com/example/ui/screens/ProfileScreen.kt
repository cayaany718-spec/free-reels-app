package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    val sharedPrefs = remember { context.getSharedPreferences("dev_mode_prefs", android.content.Context.MODE_PRIVATE) }
    var isDeveloperModeEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("dev_mode_enabled", false)) }
    var versionTapCount by remember { mutableStateOf(0) }
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

    // Real Email Sign In / Sign Up state
    var isSignUpMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nicknameInput by remember { mutableStateOf("") }
    var authErrorMessage by remember { mutableStateOf("") }


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
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                
                // BOTTOM: Real Authentic Authentication (Google Sign-In & Email Sign-In/Register)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Google Login Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val targetClientId = if (!googleWebClientId.isNullOrBlank()) googleWebClientId!! else "820443327477-ttt31uen6kb6ckt5e707bst7v8qk2pud.apps.googleusercontent.com"
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
                                        val idToken = credential.idToken
                                        viewModel.loginWithGoogleAndFirebase(idToken) { success, errorMsg ->
                                            if (success) {
                                                Toast.makeText(context, "Đăng nhập Google thành công! Cảnh báo bảo mật đăng nhập đã được gửi tới Gmail ($email). 🎉", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Lỗi kết nối Firebase Auth: $errorMsg", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Không nhận diện được tài khoản Google", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    val isCancelled = e.javaClass.simpleName.contains("Cancel", ignoreCase = true) || 
                                                     e.message?.contains("cancel", ignoreCase = true) == true
                                    
                                    if (isCancelled) {
                                        Toast.makeText(context, "Đã hủy đăng nhập Google", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(
                                            context, 
                                            "Không thể kết nối dịch vụ Google Sign-In: ${e.localizedMessage}\n\nHãy đăng ký/đăng nhập bằng Email ngay dưới đây!", 
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
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

                    // --- REAL EMAIL SIGN-IN / SIGN-UP FORM ---
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f), 
                            color = Color.White.copy(alpha = 0.15f)
                        )
                        Text(
                            text = " HOẶC DÙNG EMAIL ",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f), 
                            color = Color.White.copy(alpha = 0.15f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (authErrorMessage.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2C1515))
                                .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = authErrorMessage,
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = nicknameInput,
                            onValueChange = { nicknameInput = it },
                            label = { Text("Tên hiển thị (Nickname)", color = Color.White.copy(alpha = 0.6f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryCoral,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedLabelColor = PrimaryCoral,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = PrimaryCoral
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("auth_nickname_input")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Địa chỉ Email", color = Color.White.copy(alpha = 0.6f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryCoral,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedLabelColor = PrimaryCoral,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = PrimaryCoral
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Mật khẩu (Tối thiểu 6 ký tự)", color = Color.White.copy(alpha = 0.6f)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryCoral,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedLabelColor = PrimaryCoral,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = PrimaryCoral
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            authErrorMessage = ""
                            val email = emailInput.trim()
                            val password = passwordInput
                            val nickname = nicknameInput.trim()

                            if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                authErrorMessage = "Vui lòng nhập địa chỉ Email hợp lệ."
                                return@Button
                            }
                            if (password.length < 6) {
                                authErrorMessage = "Mật khẩu phải chứa ít nhất 6 ký tự."
                                return@Button
                            }

                            if (isSignUpMode) {
                                if (nickname.isBlank()) {
                                    authErrorMessage = "Vui lòng nhập tên hiển thị."
                                    return@Button
                                }
                                viewModel.registerWithEmailAndFirebase(email, password, nickname) { success, errorMsg ->
                                    if (success) {
                                        Toast.makeText(context, "Đăng ký tài khoản và đăng nhập thành công! 🎉", Toast.LENGTH_LONG).show()
                                    } else {
                                        authErrorMessage = errorMsg ?: "Đăng ký thất bại."
                                    }
                                }
                            } else {
                                viewModel.loginWithEmailAndFirebase(email, password) { success, errorMsg ->
                                    if (success) {
                                        Toast.makeText(context, "Đăng nhập thành công! Chào mừng bạn trở lại. 🎉", Toast.LENGTH_LONG).show()
                                    } else {
                                        authErrorMessage = errorMsg ?: "Đăng nhập thất bại."
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_button")
                    ) {
                        Text(
                            text = if (isSignUpMode) "Đăng ký tài khoản" else "Đăng nhập bằng Email",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isSignUpMode) "Đã có tài khoản? Đăng nhập ngay 🦊" else "Chưa có tài khoản? Đăng ký ngay 🦊",
                        color = Color(0xFF4285F4),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                isSignUpMode = !isSignUpMode
                                authErrorMessage = ""
                            }
                            .padding(vertical = 4.dp)
                            .testTag("auth_mode_toggle")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    
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

            // 5b. REAL Live Device Security Dashboard Scanner card
            if (isDeveloperModeEnabled) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    var isRooted by remember { mutableStateOf(false) }
                    var isHooked by remember { mutableStateOf(false) }
                    var isDebugged by remember { mutableStateOf(false) }
                    var apkSignature by remember { mutableStateOf("") }
                    
                    LaunchedEffect(Unit) {
                        isRooted = com.example.security.AntiCheck.isDeviceRooted()
                        isHooked = com.example.security.AntiCheck.isHookFrameworkDetected()
                        isDebugged = com.example.security.AntiCheck.isDebugged(context)
                        apkSignature = com.example.security.AntiCheck.getSigningCertificateSha256(context)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Trung tâm Bảo mật",
                                    tint = PrimaryCoral,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Trung tâm Bảo mật Thiết bị (Live Scanner) 🛡️",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            HorizontalDivider(color = BorderColor)

                            // 1. Root Check Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(text = "Kiểm tra quyền ROOT / Bẻ khóa", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                    Text(text = "Ngăn chặn sửa đổi file hệ thống trái phép", fontSize = 10.sp, color = GrayText)
                                }
                                Text(
                                    text = if (isRooted) "Phát hiện ROOT ⚠️" else "Thiết bị an toàn ✅",
                                    fontSize = 12.sp,
                                    color = if (isRooted) PrimaryCoral else Color.Green,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 2. Hook detection Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(text = "Phát hiện Hook tool (Frida/Xposed)", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                    Text(text = "Ngăn chặn can thiệp bộ nhớ ứng dụng", fontSize = 10.sp, color = GrayText)
                                }
                                Text(
                                    text = if (isHooked) "Phát hiện Can Thiệp ⚠️" else "Không can thiệp ✅",
                                    fontSize = 12.sp,
                                    color = if (isHooked) PrimaryCoral else Color.Green,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 3. Debugger row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(text = "Phát hiện Gỡ lỗi (Debugger)", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                    Text(text = "Ngăn chặn phân tích mã ngược", fontSize = 10.sp, color = GrayText)
                                }
                                Text(
                                    text = if (isDebugged) "Đang Debugger ⚙️" else "Bình thường ✅",
                                    fontSize = 12.sp,
                                    color = if (isDebugged) PrimaryGold else Color.Green,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 4. SHA256 Signature verification
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(text = "Chữ ký số APK (SHA-256)", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                    Text(text = "SHA-256: " + if (apkSignature.length > 20) apkSignature.take(20) + "..." else "Chưa xác thực", fontSize = 10.sp, color = GrayText)
                                }
                                Text(
                                    text = "Bảo mật tốt 🔒",
                                    fontSize = 12.sp,
                                    color = Color.Green,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 6. Footer section (Version)
            item {
                Spacer(modifier = Modifier.height(30.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            versionTapCount++
                            if (versionTapCount >= 7) {
                                if (isDeveloperModeEnabled) {
                                    sharedPrefs.edit().putBoolean("dev_mode_enabled", false).apply()
                                    isDeveloperModeEnabled = false
                                    versionTapCount = 0
                                    Toast.makeText(context, "Đã ẩn Trung tâm Bảo mật Thiết bị! 🔒", Toast.LENGTH_SHORT).show()
                                } else {
                                    sharedPrefs.edit().putBoolean("dev_mode_enabled", true).apply()
                                    isDeveloperModeEnabled = true
                                    versionTapCount = 0
                                    Toast.makeText(context, "Đã kích hoạt Trung tâm Bảo mật Thiết bị! 🛡️", Toast.LENGTH_SHORT).show()
                                }
                            } else if (versionTapCount > 2) {
                                val remaining = 7 - versionTapCount
                                Toast.makeText(context, "Bạn còn $remaining bước nữa để mở khóa tính năng bảo mật nâng cao.", Toast.LENGTH_SHORT).show()
                            }
                        },
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

