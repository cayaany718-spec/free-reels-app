package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DramaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DramaRepository(application)

    // Raw static and reactive flows
    val allDramas = repository.getDramas()
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

    fun login(phoneNumber: String, nickname: String, avatarEmoji: String) {
        _currentUserProfile.value = UserProfile(
            id = "FR_${(100000..999999).random()}",
            nickname = nickname,
            avatarEmoji = avatarEmoji,
            phoneNumber = phoneNumber,
            vipLevel = "THÀNH VIÊN VIP PREMIUM",
            isVip = true
        )
        _isLoggedIn.value = true
    }

    fun logout() {
        _currentUserProfile.value = null
        _isLoggedIn.value = false
    }

    fun updateNickname(newName: String) {
        _currentUserProfile.value = _currentUserProfile.value?.copy(nickname = newName)
    }

    // Filtered dramas
    val filteredDramas = combine(_searchQuery, _selectedCategory) { query, category ->
        var list = allDramas
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
        initialValue = allDramas
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
        viewModelScope.launch {
            repository.initializeBalanceIfNeeded()
        }
        // Initialize default drama and episode for the Reels tab so it plays immediately
        val defaultDrama = allDramas.firstOrNull { it.isTrending } ?: allDramas.firstOrNull()
        if (defaultDrama != null) {
            selectDrama(defaultDrama)
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
