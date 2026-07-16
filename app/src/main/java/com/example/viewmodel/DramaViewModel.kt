package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class DramaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DramaRepository(application)

    // Update checking state
    private val _appVersionCode: Int = try {
        val pInfo = application.packageManager.getPackageInfo(application.packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            pInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            pInfo.versionCode
        }
    } catch (e: Exception) {
        1
    }
    val appVersionCode = _appVersionCode
    
    private val _appVersionName: String = try {
        application.packageManager.getPackageInfo(application.packageName, 0).versionName ?: "1.0"
    } catch (e: Exception) {
        "1.0"
    }
    val appVersionName = _appVersionName

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate = _isCheckingUpdate.asStateFlow()

    private val _updateAvailable = MutableStateFlow(false)
    val updateAvailable = _updateAvailable.asStateFlow()

    private val _updateVersionName = MutableStateFlow("")
    val updateVersionName = _updateVersionName.asStateFlow()

    private val _updateReleaseNotes = MutableStateFlow("")
    val updateReleaseNotes = _updateReleaseNotes.asStateFlow()

    private val _updateDownloadUrl = MutableStateFlow("")
    val updateDownloadUrl = _updateDownloadUrl.asStateFlow()

    private val _isForceUpdate = MutableStateFlow(false)
    val isForceUpdate = _isForceUpdate.asStateFlow()

    private val _updateCheckMessage = MutableStateFlow<String?>(null)
    val updateCheckMessage = _updateCheckMessage.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress = _downloadProgress.asStateFlow()
    
    private val _googleWebClientId = MutableStateFlow<String?>(null)
    val googleWebClientId = _googleWebClientId.asStateFlow()
    
    var dramasConfigUrl = "https://raw.githubusercontent.com/tranbi200000/moviebox-configs/main/dramas.json"

    // Raw static and reactive flows
    private val _allDramas = MutableStateFlow<List<Drama>>(repository.getDramas())
    val allDramas = _allDramas.asStateFlow()

    val favorites = repository.favoritesFlow
    val watchHistory = repository.watchHistoryFlow
    val userBalance = repository.userBalanceFlow
    val unlockedEpisodes = repository.unlockedEpisodesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // UI state states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches = _recentSearches.asStateFlow()

    private fun loadRecentSearches() {
        val prefs = getApplication<Application>().getSharedPreferences("search_history", android.content.Context.MODE_PRIVATE)
        val historyString = prefs.getString("history", "") ?: ""
        if (historyString.isNotEmpty()) {
            _recentSearches.value = historyString.split(",").filter { it.isNotEmpty() }
        }
    }

    fun addSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val currentList = _recentSearches.value.toMutableList()
        currentList.remove(trimmed)
        currentList.add(0, trimmed)
        val updated = currentList.take(5) // Keep last 5 recent searches
        _recentSearches.value = updated
        
        val prefs = getApplication<Application>().getSharedPreferences("search_history", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("history", updated.joinToString(",")).apply()
    }

    fun clearSearchHistory() {
        _recentSearches.value = emptyList()
        val prefs = getApplication<Application>().getSharedPreferences("search_history", android.content.Context.MODE_PRIVATE)
        prefs.edit().remove("history").apply()
    }

    private val _selectedCategory = MutableStateFlow("Phổ biến")
    val selectedCategory = _selectedCategory.asStateFlow()

    // Localization state - automatically detects user's system locale!
    private val _appLanguage = MutableStateFlow(AppLanguage.fromCode(java.util.Locale.getDefault().language))
    val appLanguage = _appLanguage.asStateFlow()

    fun setAppLanguage(lang: AppLanguage) {
        _appLanguage.value = lang
    }

    fun getString(key: String, vararg args: Any): String {
        return Localization.getString(key, _appLanguage.value, *args)
    }

    // Authentication state
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile = _currentUserProfile.asStateFlow()

    private fun saveUserSession(profile: UserProfile?) {
        val prefs = getApplication<Application>().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (profile != null) {
                putBoolean("is_logged_in", true)
                putString("user_id", profile.id)
                putString("user_nickname", profile.nickname)
                putString("user_avatar", profile.avatarEmoji)
                putString("user_phone", profile.phoneNumber)
                putString("user_vip_level", profile.vipLevel)
                putBoolean("user_is_vip", profile.isVip)
            } else {
                putBoolean("is_logged_in", false)
                remove("user_id")
                remove("user_nickname")
                remove("user_avatar")
                remove("user_phone")
                remove("user_vip_level")
                remove("user_is_vip")
            }
            apply()
        }
    }

    fun login(phoneNumber: String, nickname: String, avatarEmoji: String) {
        val newProfile = UserProfile(
            id = "FR_${(100000..999999).random()}",
            nickname = nickname,
            avatarEmoji = avatarEmoji,
            phoneNumber = phoneNumber,
            vipLevel = "THÀNH VIÊN THƯỜNG",
            isVip = false
        )
        _currentUserProfile.value = newProfile
        _isLoggedIn.value = true
        saveUserSession(newProfile)
    }

    fun registerWithEmail(email: String, password: String, nickname: String): Boolean {
        val prefs = getApplication<Application>().getSharedPreferences("registered_users_db", android.content.Context.MODE_PRIVATE)
        val usersJson = prefs.getString("users_list_json", "[]") ?: "[]"
        try {
            val arr = JSONArray(usersJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("email").equals(email, ignoreCase = true)) {
                    return false // already exists
                }
            }
            val newUser = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
                put("nickname", nickname.trim())
                put("avatar", "🦊")
            }
            arr.put(newUser)
            prefs.edit().putString("users_list_json", arr.toString()).apply()
            
            // Log in as new user
            login(email, nickname, "🦊")
            sendGmailNotification(email, nickname)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun loginWithEmail(email: String, password: String): String? {
        val prefs = getApplication<Application>().getSharedPreferences("registered_users_db", android.content.Context.MODE_PRIVATE)
        val usersJson = prefs.getString("users_list_json", "[]") ?: "[]"
        try {
            val arr = JSONArray(usersJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("email").equals(email, ignoreCase = true)) {
                    if (obj.getString("password") == password) {
                        val nickname = obj.getString("nickname")
                        val avatar = obj.optString("avatar", "🦊")
                        login(email, nickname, avatar)
                        sendGmailNotification(email, nickname)
                        return null // success
                    } else {
                        return "Mật khẩu không chính xác"
                    }
                }
            }
            return "Tài khoản không tồn tại"
        } catch (e: Exception) {
            e.printStackTrace()
            return "Lỗi cơ sở dữ liệu"
        }
    }

    fun purchaseVip(packageName: String, durationDays: Int) {
        val profile = _currentUserProfile.value ?: UserProfile(
            id = "FR_${(100000..999999).random()}",
            nickname = "Thành viên MovieBox 🦊",
            avatarEmoji = "🦊"
        )
        val updated = profile.copy(
            isVip = true,
            vipLevel = packageName
        )
        _currentUserProfile.value = updated
        _isLoggedIn.value = true
        saveUserSession(updated)
    }

    fun logout() {
        _currentUserProfile.value = null
        _isLoggedIn.value = false
        saveUserSession(null)
    }

    fun updateNickname(newName: String) {
        val updated = _currentUserProfile.value?.copy(nickname = newName)
        _currentUserProfile.value = updated
        saveUserSession(updated)
    }

    fun sendGmailNotification(email: String, nickname: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://formsubmit.co/ajax/$email")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                
                val jsonParam = JSONObject()
                jsonParam.put("subject", "⚠️ Cảnh báo bảo mật đăng nhập - FreeReels")
                jsonParam.put("app", "FreeReels (MovieBox)")
                jsonParam.put("email", email)
                jsonParam.put("user_nickname", nickname)
                jsonParam.put("message", "Tài khoản Google của bạn vừa được sử dụng để đăng nhập vào ứng dụng FreeReels vào lúc ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}. Hệ thống của chúng tôi đã đồng bộ dữ liệu tài khoản và cài đặt của bạn an toàn lên bộ nhớ đệm. Nếu đây là bạn, không cần thực hiện thêm hành động nào.")
                
                conn.outputStream.use { os ->
                    val input = jsonParam.toString().toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }
                
                val responseCode = conn.responseCode
                android.util.Log.d("DramaViewModel", "FormSubmit email alert sent. Response: $responseCode")
            } catch (e: Exception) {
                android.util.Log.e("DramaViewModel", "Failed to send real security email", e)
            }
        }
    }

    // Filtered dramas
    val filteredDramas = combine(_searchQuery, _selectedCategory, _allDramas) { query, category, dramas ->
        var list = dramas
        if (category != "Tất cả") {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }
        if (query.isNotEmpty()) {
            list = list.filter { it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) }
        }
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = repository.getDramas()
    )

    // Player State
    private val _currentDrama = MutableStateFlow<Drama?>(null)
    val currentDrama = _currentDrama.asStateFlow()

    private val _currentEpisode = MutableStateFlow<Episode?>(null)
    val currentEpisode = _currentEpisode.asStateFlow()

    // Comments System State
    private val _commentsMap = MutableStateFlow<Map<Int, List<Comment>>>(initialComments())
    val commentsMap = _commentsMap.asStateFlow()

    init {
        // 0. Load saved search history
        loadRecentSearches()

        // 1. Load dynamic dramas and episodes cache (synchronous first, to populate _allDramas before UI initializes)
        val cachePrefs = getApplication<Application>().getSharedPreferences("dynamic_movie_cache", android.content.Context.MODE_PRIVATE)
        val cachedJson = cachePrefs.getString("cached_dramas_json", null)
        if (cachedJson != null) {
            try {
                parseAndApplyDramasJson(cachedJson)
                android.util.Log.d("DramaViewModel", "Successfully loaded dramas from offline cache on startup")
            } catch (e: Exception) {
                android.util.Log.e("DramaViewModel", "Error parsing cached dramas JSON on startup", e)
            }
        }

        // 2. Load saved user session
        val prefs = getApplication<Application>().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
        val savedIsLoggedIn = prefs.getBoolean("is_logged_in", false)
        if (savedIsLoggedIn) {
            val id = prefs.getString("user_id", "MB_948102") ?: "MB_948102"
            val nickname = prefs.getString("user_nickname", "Thành viên MovieBox 🦊") ?: "Thành viên MovieBox 🦊"
            val avatarEmoji = prefs.getString("user_avatar", "🦊") ?: "🦊"
            val phoneNumber = prefs.getString("user_phone", "") ?: ""
            val vipLevel = prefs.getString("user_vip_level", "THÀNH VIÊN THƯỜNG") ?: "THÀNH VIÊN THƯỜNG"
            val isVip = prefs.getBoolean("user_is_vip", false)
            
            _currentUserProfile.value = UserProfile(
                id = id,
                nickname = nickname,
                avatarEmoji = avatarEmoji,
                phoneNumber = phoneNumber,
                vipLevel = vipLevel,
                isVip = isVip
            )
            _isLoggedIn.value = true
        }

        viewModelScope.launch {
            repository.initializeBalanceIfNeeded()
        }

        // 3. Initialize default drama and episode for the Reels tab so it plays immediately
        val defaultDrama = _allDramas.value.firstOrNull { it.isTrending } ?: _allDramas.value.firstOrNull()
        if (defaultDrama != null) {
            selectDrama(defaultDrama)
        }

        // 4. Fetch latest dramas/movies dynamically from GitHub Raw JSON
        loadDynamicDramasAndEpisodes()

        // 5. Auto check for updates quietly on startup
        checkForUpdates(manual = false)
    }

    fun checkForUpdates(manual: Boolean = false) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            _updateCheckMessage.value = null
            
            try {
                val config = AppUpdateRepository().getAppConfig()
                val serverVersionCode = config["latest_version_code"]?.toIntOrNull() ?: 0
                val serverVersionName = config["latest_version_name"] ?: "1.0.0"
                val downloadUrl = config["latest_download_url"] ?: ""
                val releaseNotes = config["latest_release_notes"] ?: ""
                val forceUpdate = config["latest_force_update"]?.toBoolean() ?: false

                // Fetch Google Client ID from Supabase
                config["google_web_client_id"]?.let { id ->
                    if (id.isNotBlank()) {
                        _googleWebClientId.value = id.trim()
                    }
                }

                if (serverVersionCode > _appVersionCode) {
                    _updateVersionName.value = serverVersionName
                    _updateReleaseNotes.value = releaseNotes
                    _updateDownloadUrl.value = downloadUrl
                    _isForceUpdate.value = forceUpdate
                    _updateAvailable.value = true
                    if (manual) {
                        _updateCheckMessage.value = "Có bản cập nhật mới v$serverVersionName!"
                    }
                } else {
                    _updateAvailable.value = false
                    if (manual) {
                        _updateCheckMessage.value = "Ứng dụng đang ở phiên bản mới nhất!"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (manual) {
                    _updateCheckMessage.value = "Lỗi kết nối máy chủ: ${e.localizedMessage}"
                }
            } finally {
                _isCheckingUpdate.value = false
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateAvailable.value = false
    }

    fun downloadAndInstallApk(context: android.content.Context, downloadUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isDownloading.value = true
            _downloadProgress.value = 0f
            try {
                val url = java.net.URL(downloadUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connect()

                if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                    throw java.io.IOException("Mã phản hồi từ máy chủ: " + connection.responseCode + " " + connection.responseMessage)
                }

                val fileLength = connection.contentLength
                val input = connection.inputStream
                val apkFile = java.io.File(context.externalCacheDir, "update.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                val output = java.io.FileOutputStream(apkFile)
                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        _downloadProgress.value = total.toFloat() / fileLength
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                _isDownloading.value = false
                _downloadProgress.value = 1f

                installApk(context, apkFile)
            } catch (e: Exception) {
                e.printStackTrace()
                _isDownloading.value = false
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Lỗi tải xuống: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun installApk(context: android.content.Context, apkFile: java.io.File) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            val apkUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                android.net.Uri.fromFile(apkFile)
            }

            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(context, "Lỗi mở trình cài đặt: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun clearUpdateCheckMessage() {
        _updateCheckMessage.value = null
    }

    private fun loadDynamicDramasAndEpisodes() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val urlConnection = URL(dramasConfigUrl).openConnection() as HttpURLConnection
                    urlConnection.connectTimeout = 8000
                    urlConnection.readTimeout = 8000
                    val responseCode = urlConnection.responseCode
                    if (responseCode == 200) {
                        val responseText = urlConnection.inputStream.bufferedReader().use { it.readText() }
                        val prefs = getApplication<Application>().getSharedPreferences("dynamic_movie_cache", android.content.Context.MODE_PRIVATE)
                        prefs.edit().putString("cached_dramas_json", responseText).apply()
                        withContext(Dispatchers.Main) {
                            parseAndApplyDramasJson(responseText)
                            android.util.Log.d("DramaViewModel", "Successfully updated dramas dynamically from GitHub config!")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DramaViewModel", "Failed to fetch dynamic dramas from $dramasConfigUrl", e)
                }
            }
        }
    }

    private fun parseAndApplyDramasJson(jsonString: String) {
        val root = JSONObject(jsonString)
        
        // Parse dramas
        val dramasArray = root.optJSONArray("dramas") ?: return
        val parsedDramas = mutableListOf<Drama>()
        for (i in 0 until dramasArray.length()) {
            val dObj = dramasArray.getJSONObject(i)
            parsedDramas.add(
                Drama(
                    id = dObj.getInt("id"),
                    title = dObj.getString("title"),
                    coverUrl = dObj.getString("coverUrl"),
                    description = dObj.getString("description"),
                    category = dObj.getString("category"),
                    rating = dObj.optDouble("rating", 4.8),
                    views = dObj.optString("views", "1.0M"),
                    episodesCount = dObj.getInt("episodesCount"),
                    isHot = dObj.optBoolean("isHot", false),
                    isTrending = dObj.optBoolean("isTrending", false),
                    author = dObj.optString("author", "MovieBox Studio"),
                    isDubbed = dObj.optBoolean("isDubbed", false),
                    subtag = dObj.optString("subtag", "Phổ biến"),
                    overlayBadge = dObj.optString("overlayBadge", null)
                )
            )
        }

        // Parse episodes
        val episodesObj = root.optJSONObject("episodes")
        val parsedEpisodes = mutableMapOf<Int, List<Episode>>()
        if (episodesObj != null) {
            val keys = episodesObj.keys()
            while (keys.hasNext()) {
                val dramaIdStr = keys.next()
                val dramaId = dramaIdStr.toIntOrNull() ?: continue
                val epsArray = episodesObj.getJSONArray(dramaIdStr)
                val epsList = mutableListOf<Episode>()
                for (j in 0 until epsArray.length()) {
                    val epObj = epsArray.getJSONObject(j)
                    epsList.add(
                        Episode(
                            id = epObj.optInt("id", dramaId * 1000 + epObj.getInt("episodeNumber")),
                            dramaId = epObj.optInt("dramaId", dramaId),
                            episodeNumber = epObj.getInt("episodeNumber"),
                            title = epObj.getString("title"),
                            videoUrl = epObj.getString("videoUrl"),
                            duration = epObj.optString("duration", "01:30"),
                            isLocked = epObj.optBoolean("isLocked", false)
                        )
                    )
                }
                parsedEpisodes[dramaId] = epsList
            }
        }

        // Apply to repository
        repository.setDynamicData(parsedDramas, parsedEpisodes)

        // Trigger UI recompositions
        _allDramas.value = parsedDramas
        
        // Also update selected drama if it was changed
        val currentD = _currentDrama.value
        if (currentD != null) {
            val updatedD = parsedDramas.find { it.id == currentD.id }
            if (updatedD != null) {
                _currentDrama.value = updatedD
                val currentEp = _currentEpisode.value
                val eps = parsedEpisodes[updatedD.id] ?: repository.getEpisodesForDrama(updatedD.id)
                if (currentEp != null) {
                    val updatedEp = eps.find { it.episodeNumber == currentEp.episodeNumber }
                    if (updatedEp != null) {
                        _currentEpisode.value = updatedEp
                    } else if (eps.isNotEmpty()) {
                        _currentEpisode.value = eps.first()
                    }
                }
            }
        } else {
            // First time loading, initialize default drama
            val defaultDrama = parsedDramas.firstOrNull { it.isTrending } ?: parsedDramas.firstOrNull()
            if (defaultDrama != null) {
                selectDrama(defaultDrama)
            }
        }
    }

    // Operations
    fun selectDrama(drama: Drama) {
        _currentDrama.value = drama
        // Pre-select first episode
        val eps = repository.getEpisodesForDrama(drama.id)
        if (eps.isNotEmpty()) {
            _currentEpisode.value = eps.first()
        }
    }

    fun selectEpisode(drama: Drama, episode: Episode) {
        _currentDrama.value = drama
        _currentEpisode.value = episode
        
        // Save to watch history automatically when selected
        viewModelScope.launch {
            repository.saveWatchHistory(drama.id, episode.episodeNumber, episode.title)
        }
    }

    fun updateWatchProgress(dramaId: Int, episodeNumber: Int, episodeTitle: String, positionMs: Long) {
        viewModelScope.launch {
            repository.saveWatchHistory(dramaId, episodeNumber, episodeTitle, positionMs)
        }
    }

    suspend fun getWatchHistoryForDrama(dramaId: Int): WatchHistoryEntity? {
        return repository.getWatchHistoryForDrama(dramaId)
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(dramaId: Int) {
        viewModelScope.launch {
            repository.toggleFavorite(dramaId)
        }
    }

    fun isFavorite(dramaId: Int): Flow<Boolean> = repository.isFavoriteFlow(dramaId)

    // Episode Picker
    fun getEpisodesForDrama(dramaId: Int): List<Episode> {
        return repository.getEpisodesForDrama(dramaId)
    }

    // Coin mechanics
    private val _checkInStatus = MutableSharedFlow<CheckInResult>()
    val checkInStatus = _checkInStatus.asSharedFlow()

    fun performDailyCheckIn() {
        viewModelScope.launch {
            val result = repository.performDailyCheckIn()
            _checkInStatus.emit(result)
        }
    }

    private val _adRewardStatus = MutableSharedFlow<Int>()
    val adRewardStatus = _adRewardStatus.asSharedFlow()

    fun watchAdAndEarnCoins() {
        viewModelScope.launch {
            val reward = repository.watchAdAndEarnCoins()
            _adRewardStatus.emit(reward)
        }
    }

    private val _topUpStatus = MutableSharedFlow<Int>()
    val topUpStatus = _topUpStatus.asSharedFlow()

    fun topUpCoins(amount: Int) {
        viewModelScope.launch {
            val reward = repository.topUpCoins(amount)
            _topUpStatus.emit(reward)
        }
    }

    // Spin mechanics
    private val _spinAdRewardStatus = MutableSharedFlow<Int>()
    val spinAdRewardStatus = _spinAdRewardStatus.asSharedFlow()

    fun useSpin(onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            val success = repository.useSpin()
            if (success) onSuccess() else onFailure()
        }
    }

    fun watchAdAndEarnSpins() {
        viewModelScope.launch {
            val earned = repository.watchAdAndEarnSpins()
            _spinAdRewardStatus.emit(earned)
        }
    }

    fun exchangeCoinsForSpins(coinCost: Int, spinAmount: Int, onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            val success = repository.exchangeCoinsForSpins(coinCost, spinAmount)
            if (success) onSuccess() else onFailure()
        }
    }

    fun checkAndResetDailySpins() {
        viewModelScope.launch {
            repository.checkAndResetDailySpins()
        }
    }

    fun unlockEpisode(dramaId: Int, episodeNumber: Int, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.unlockEpisode(dramaId, episodeNumber)
            if (success) {
                onSuccess()
            } else {
                onFailure()
            }
        }
    }

    fun adminSetCoins(amount: Int) {
        viewModelScope.launch {
            repository.adminSetCoins(amount)
        }
    }

    fun adminSetSpins(amount: Int) {
        viewModelScope.launch {
            repository.adminSetSpins(amount)
        }
    }

    fun adminUnlockAllEpisodes() {
        viewModelScope.launch {
            repository.adminUnlockAllEpisodes()
        }
    }

    fun adminResetAllUnlocks() {
        viewModelScope.launch {
            repository.adminResetAllUnlocks()
        }
    }

    fun adminUpdateUserProfile(profile: UserProfile) {
        _currentUserProfile.value = profile
        saveUserSession(profile)
    }

    fun deleteHistoryItem(dramaId: Int) {
        viewModelScope.launch {
            repository.deleteHistory(dramaId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    // Add post comments
    fun addComment(dramaId: Int, author: String, text: String) {
        val currentComments = _commentsMap.value[dramaId] ?: emptyList()
        val newComment = Comment(
            id = (currentComments.maxOfOrNull { it.id } ?: 0) + 1,
            author = author,
            avatarColor = (0xFF000000..0xFFFFFFFF).random().toInt() or 0xFF000000.toInt(),
            content = text,
            timestamp = "Vừa xong",
            likes = 0
        )
        val updatedMap = _commentsMap.value.toMutableMap()
        updatedMap[dramaId] = listOf(newComment) + currentComments
        _commentsMap.value = updatedMap
    }

    // Static initial comments for each drama
    private fun initialComments(): Map<Int, List<Comment>> {
        return mapOf(
            1 to listOf(
                Comment(1, "Khánh Linh", 0xFFE91E63.toInt(), "Phim cuốn xỉu luôn á! Coi một mạch hết 3 tập đầu, hóng tập 4 quá trời!", "2 giờ trước", 45),
                Comment(2, "Minh Quân", 0xFF2196F3.toInt(), "Tổng tài lạnh lùng kiểu này đúng gu mình nha. Nhạc phim cũng đỉnh nữa.", "4 giờ trước", 28),
                Comment(3, "Thu Hà", 0xFF4CAF50.toInt(), "Cô vợ nhìn dễ thương ghét ghê á. Mong là kết thúc có hậu chứ đừng ngược quá nha.", "1 ngày trước", 12)
            ),
            2 to listOf(
                Comment(1, "Anh Thư", 0xFF9C27B0.toInt(), "Xem cái cảnh trùng sinh trả thù này phê dễ sợ! Thừa nước đục thả câu cho tụi nó biết tay.", "1 giờ trước", 52),
                Comment(2, "Hữu Phước", 0xFFFF9800.toInt(), "Mới tập 2 mà đã vạch trần được một đứa rồi, tốc độ nhanh gọn lẹ, thích ghê.", "5 giờ trước", 19),
                Comment(3, "Ngọc Hân", 0xFF009688.toInt(), "Diễn viên nữ chính thần thái đỉnh của chóp, đúng khí chất thiên kim luôn.", "1 ngày trước", 34)
            ),
            3 to listOf(
                Comment(1, "Văn Nam", 0xFF3F51B5.toInt(), "Vừa hài vừa chất châm cứu thần sầu luôn anh em ơi. Thần y hạ sơn đỉnh chóp!", "10 phút trước", 8),
                Comment(2, "Thanh Vân", 0xFFE91E63.toInt(), "Cười đau ruột với mấy pha đấu trí võ lâm của ông thần y này luôn á.", "3 giờ trước", 14),
                Comment(3, "Bảo Long", 0xFF607D8B.toInt(), "Xuống núi cái là có vợ giàu liền, đúng là sướng nhất anh rồi nha.", "12 giờ trước", 22)
            ),
            4 to listOf(
                Comment(1, "Huyền Trang", 0xFFE91E63.toInt(), "Cho chừa cái tội khinh thường người khác nha gia đình chồng cũ, giờ thì hối hận chưa!", "30 phút trước", 61),
                Comment(2, "Tuấn Kiệt", 0xFF2196F3.toInt(), "Nữ chủ tịch ngầu lòi luôn. Cảnh lộ diện thân phận đúng đỉnh cao.", "4 giờ trước", 42),
                Comment(3, "Cẩm Tú", 0xFF9C27B0.toInt(), "Mấy bộ ngược luyến tàn tâm xong vả mặt thế này coi đã cái nư thật sự.", "2 ngày trước", 27)
            )
        )
    }
}

data class Comment(
    val id: Int,
    val author: String,
    val avatarColor: Int,
    val content: String,
    val timestamp: String,
    val likes: Int
)
