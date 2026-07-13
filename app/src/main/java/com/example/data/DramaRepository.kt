package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DramaRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val favoriteDao = db.favoriteDao()
    private val watchHistoryDao = db.watchHistoryDao()
    private val unlockedEpisodeDao = db.unlockedEpisodeDao()
    private val userBalanceDao = db.userBalanceDao()

    // 1. Get static list of dramas
    fun getDramas(): List<Drama> = MockData.dramas

    fun getDramaById(id: Int): Drama? = MockData.dramas.find { it.id == id }

    fun getEpisodesForDrama(dramaId: Int): List<Episode> {
        val staticEps = MockData.episodes[dramaId]
        if (staticEps != null) return staticEps
        
        // Dynamically generate episodes if not statically mapped
        val drama = getDramaById(dramaId) ?: return emptyList()
        val count = drama.episodesCount
        return (1..count).map { num ->
            Episode(
                id = dramaId * 1000 + num,
                dramaId = dramaId,
                episodeNumber = num,
                title = "Tập $num - Sóng Gió Thượng Lưu",
                videoUrl = when (num % 5) {
                    0 -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
                    1 -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
                    2 -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"
                    3 -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4"
                    else -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4"
                },
                duration = "01:${20 + num}",
                isLocked = num > 1 // lock episodes after episode 1 for simulation/VIP coin spend
            )
        }
    }

    // 2. Favorites reactive operations
    val favoritesFlow: Flow<List<Drama>> = favoriteDao.getAllFavoritesFlow().map { entities ->
        entities.mapNotNull { entity ->
            getDramaById(entity.dramaId)
        }
    }

    fun isFavoriteFlow(dramaId: Int): Flow<Boolean> = favoriteDao.isFavoriteFlow(dramaId)

    suspend fun toggleFavorite(dramaId: Int) {
        val isFav = favoriteDao.isFavorite(dramaId)
        if (isFav) {
            favoriteDao.deleteFavoriteById(dramaId)
        } else {
            favoriteDao.insertFavorite(FavoriteEntity(dramaId))
        }
    }

    // 3. Watch History operations
    val watchHistoryFlow: Flow<List<WatchHistoryItem>> = watchHistoryDao.getAllHistoryFlow().map { entities ->
        entities.mapNotNull { entity ->
            val drama = getDramaById(entity.dramaId) ?: return@mapNotNull null
            val totalEpisodes = getEpisodesForDrama(entity.dramaId).size
            WatchHistoryItem(
                drama = drama,
                episodeNumber = entity.episodeNumber,
                episodeTitle = entity.episodeTitle,
                lastWatchedTime = entity.lastWatchedTime,
                progressPercentage = (entity.episodeNumber.toFloat() / totalEpisodes.toFloat() * 100).toInt().coerceIn(0, 100)
            )
        }
    }

    suspend fun saveWatchHistory(dramaId: Int, episodeNumber: Int, episodeTitle: String) {
        val history = WatchHistoryEntity(
            dramaId = dramaId,
            episodeNumber = episodeNumber,
            episodeTitle = episodeTitle,
            lastWatchedTime = System.currentTimeMillis()
        )
        watchHistoryDao.insertHistory(history)
    }

    suspend fun deleteHistory(dramaId: Int) {
        watchHistoryDao.deleteHistoryById(dramaId)
    }

    suspend fun clearAllHistory() {
        watchHistoryDao.clearAllHistory()
    }

    // 4. Unlocked Episode operations
    val unlockedEpisodesFlow: Flow<List<String>> = unlockedEpisodeDao.getAllUnlockedFlow().map { entities ->
        entities.map { it.compositeId }
    }

    suspend fun isEpisodeUnlocked(dramaId: Int, episodeNumber: Int): Boolean {
        // Episodes 1-3 are always free
        if (episodeNumber <= 3) return true
        val compositeId = "${dramaId}_${episodeNumber}"
        return unlockedEpisodeDao.isEpisodeUnlocked(compositeId)
    }

    suspend fun unlockEpisode(dramaId: Int, episodeNumber: Int): Boolean {
        val compositeId = "${dramaId}_${episodeNumber}"
        val balance = userBalanceDao.getBalance() ?: UserBalanceEntity()
        val cost = 10
        if (balance.coins >= cost) {
            // Deduct coins
            val updatedBalance = balance.copy(coins = balance.coins - cost)
            userBalanceDao.updateBalance(updatedBalance)
            // Add to unlocked list
            unlockedEpisodeDao.insertUnlock(UnlockedEpisodeEntity(compositeId))
            return true
        }
        return false
    }

    // 5. User balance & coin mechanics
    val userBalanceFlow: Flow<UserBalanceEntity> = userBalanceDao.getBalanceFlow().map { it ?: UserBalanceEntity() }

    suspend fun initializeBalanceIfNeeded() {
        val balance = userBalanceDao.getBalance()
        if (balance == null) {
            userBalanceDao.updateBalance(UserBalanceEntity())
        } else {
            // Also check and auto-reset daily free spins upon initial launch
            checkAndResetDailySpins()
        }
    }

    suspend fun useSpin(): Boolean {
        val balance = userBalanceDao.getBalance() ?: UserBalanceEntity()
        if (balance.spins > 0) {
            val updatedBalance = balance.copy(
                spins = balance.spins - 1,
                lastSpinTime = System.currentTimeMillis()
            )
            userBalanceDao.updateBalance(updatedBalance)
            return true
        }
        return false
    }

    suspend fun watchAdAndEarnSpins(): Int {
        val balance = userBalanceDao.getBalance() ?: UserBalanceEntity()
        val updatedBalance = balance.copy(spins = balance.spins + 1)
        userBalanceDao.updateBalance(updatedBalance)
        return 1
    }

    suspend fun exchangeCoinsForSpins(coinCost: Int, spinAmount: Int): Boolean {
        val balance = userBalanceDao.getBalance() ?: UserBalanceEntity()
        if (balance.coins >= coinCost) {
            val updatedBalance = balance.copy(
                coins = balance.coins - coinCost,
                spins = balance.spins + spinAmount
            )
            userBalanceDao.updateBalance(updatedBalance)
            return true
        }
        return false
    }

    suspend fun checkAndResetDailySpins(): UserBalanceEntity {
        val balance = userBalanceDao.getBalance() ?: UserBalanceEntity()
        val currentTime = System.currentTimeMillis()
        if (balance.lastSpinTime > 0L && !isSameDay(balance.lastSpinTime, currentTime)) {
            val updatedBalance = balance.copy(
                spins = 3,
                lastSpinTime = currentTime
            )
            userBalanceDao.updateBalance(updatedBalance)
            return updatedBalance
        }
        return balance
    }

    suspend fun performDailyCheckIn(): CheckInResult {
        val balance = userBalanceDao.getBalance() ?: UserBalanceEntity()
        val currentTime = System.currentTimeMillis()
        
        // Simple 24h check or date check
        val isSameDay = isSameDay(balance.lastCheckInTime, currentTime)
        if (isSameDay && balance.lastCheckInTime > 0L) {
            return CheckInResult.AlreadyCheckedIn
        }

        val bonusCoins = 20
        val updatedBalance = balance.copy(
            coins = balance.coins + bonusCoins,
            lastCheckInTime = currentTime
        )
        userBalanceDao.updateBalance(updatedBalance)
        return CheckInResult.Success(bonusCoins)
    }

    suspend fun watchAdAndEarnCoins(): Int {
        val balance = userBalanceDao.getBalance() ?: UserBalanceEntity()
        val reward = 30
        val updatedBalance = balance.copy(coins = balance.coins + reward)
        userBalanceDao.updateBalance(updatedBalance)
        return reward
    }

    suspend fun topUpCoins(amount: Int): Int {
        val balance = userBalanceDao.getBalance() ?: UserBalanceEntity()
        val updatedBalance = balance.copy(coins = balance.coins + amount)
        userBalanceDao.updateBalance(updatedBalance)
        return amount
    }

    // --- Administrator Backdoor Methods ---
    suspend fun adminSetCoins(amount: Int) {
        val balance = userBalanceDao.getBalance() ?: UserBalanceEntity()
        val updatedBalance = balance.copy(coins = amount)
        userBalanceDao.updateBalance(updatedBalance)
    }

    suspend fun adminSetSpins(amount: Int) {
        val balance = userBalanceDao.getBalance() ?: UserBalanceEntity()
        val updatedBalance = balance.copy(spins = amount)
        userBalanceDao.updateBalance(updatedBalance)
    }

    suspend fun adminUnlockAllEpisodes() {
        val dramas = getDramas()
        for (drama in dramas) {
            val episodesCount = drama.episodesCount
            for (num in 1..episodesCount) {
                val compositeId = "${drama.id}_${num}"
                unlockedEpisodeDao.insertUnlock(UnlockedEpisodeEntity(compositeId))
            }
        }
    }

    suspend fun adminResetAllUnlocks() {
        unlockedEpisodeDao.clearAllUnlocks()
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val fmt = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
        return fmt.format(java.util.Date(time1)) == fmt.format(java.util.Date(time2))
    }
}

// Helper structures
data class WatchHistoryItem(
    val drama: Drama,
    val episodeNumber: Int,
    val episodeTitle: String,
    val lastWatchedTime: Long,
    val progressPercentage: Int
)

sealed class CheckInResult {
    data class Success(val coinsEarned: Int) : CheckInResult()
    object AlreadyCheckedIn : CheckInResult()
}
